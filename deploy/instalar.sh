#!/usr/bin/env bash
# Instala (ou atualiza) o site e o servidor da Portela's Marcenaria num Ubuntu 24.04 ou 22.04.
# Uso, como root:
#   curl -fsSL https://raw.githubusercontent.com/LuanFernand0/portelas-servidor/main/deploy/instalar.sh | bash -s -- seudominio.com.br
# Sem domínio (só pelo IP), rode sem o último argumento.
# Rodar de novo atualiza o site e o servidor com a versão mais nova do GitHub (senhas e fotos são mantidas).
set -euo pipefail

DOMINIO="${1:-}"
BASE=/opt/portelas
SERVIDOR="$BASE/servidor"
SITE="$BASE/site"
WEB=/var/www/portelas
REPO_SERVIDOR=https://github.com/LuanFernand0/portelas-servidor.git
REPO_SITE=https://github.com/LuanFernand0/portelas-site.git
BANCO=marcenaria_portelas

passo() { echo; echo "==> $*"; }
senha() { tr -dc 'A-Za-z0-9' </dev/urandom | head -c "${1:-24}"; }

[ "$(id -u)" = 0 ] || { echo "Rode como root."; exit 1; }

passo "Instalando programas (Java 17, Maven, MySQL, Node.js, Caddy)"
export DEBIAN_FRONTEND=noninteractive
apt-get update -q
apt-get install -y -q curl ca-certificates gnupg
# Ubuntu 22.04 (ex.: VPS da HostGator) não traz o Caddy nos repositórios: usa o repositório oficial do Caddy.
if ! apt-cache show caddy >/dev/null 2>&1; then
  curl -fsSL https://dl.cloudsmith.io/public/caddy/stable/gpg.key | gpg --dearmor --yes -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -fsSL https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt > /etc/apt/sources.list.d/caddy-stable.list
  apt-get update -q
fi
apt-get install -y -q openjdk-17-jdk-headless maven mysql-server git caddy ufw rsync unattended-upgrades
if ! command -v node >/dev/null || [ "$(node -p 'process.versions.node.split(".")[0]')" -lt 20 ]; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y -q nodejs
fi

passo "Baixando o código do GitHub"
id portelas >/dev/null 2>&1 || useradd --system --home "$BASE" --shell /usr/sbin/nologin portelas
mkdir -p "$BASE"
git config --system --add safe.directory "$SERVIDOR" 2>/dev/null || true
git config --system --add safe.directory "$SITE" 2>/dev/null || true
for par in "$SERVIDOR|$REPO_SERVIDOR" "$SITE|$REPO_SITE"; do
  dir="${par%%|*}"; url="${par##*|}"
  if [ -d "$dir/.git" ]; then git -C "$dir" fetch -q origin main && git -C "$dir" reset -q --hard origin/main
  else git clone -q "$url" "$dir"; fi
done

passo "Preparando o banco de dados"
systemctl enable --now mysql
CONFIG="$SERVIDOR/config-local.properties"
SENHA_ADMIN_NOVA=""
if [ ! -f "$CONFIG" ]; then
  SENHA_BANCO="$(senha 28)"
  SENHA_ADMIN_NOVA="Portelas-$(senha 12)"
  mysql -e "CREATE DATABASE IF NOT EXISTS $BANCO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  mysql -e "CREATE USER IF NOT EXISTS 'portelas'@'localhost' IDENTIFIED BY '$SENHA_BANCO';"
  mysql -e "ALTER USER 'portelas'@'localhost' IDENTIFIED BY '$SENHA_BANCO';"
  mysql -e "GRANT ALL PRIVILEGES ON $BANCO.* TO 'portelas'@'localhost'; FLUSH PRIVILEGES;"
  cat > "$CONFIG" <<CONF
# Gerado pelo instalar.sh. Não vai para o GitHub.
server.address=127.0.0.1
spring.datasource.username=portelas
spring.datasource.password=$SENHA_BANCO
portelas.admin.usuario=admin
portelas.admin.senha=$SENHA_ADMIN_NOVA
CONF
  chmod 600 "$CONFIG"
fi

# Dados vindos do computador (opcional): /root/portelas-banco.sql e /root/uploads
if [ -f /root/portelas-banco.sql ]; then
  passo "Importando o banco de dados enviado (/root/portelas-banco.sql)"
  sed -E '/^(CREATE DATABASE|USE )/d' /root/portelas-banco.sql | mysql "$BANCO"
  mv /root/portelas-banco.sql "/root/portelas-banco.importado-$(date +%Y%m%d-%H%M%S).sql"
fi
mkdir -p "$SERVIDOR/uploads"
if [ -d /root/uploads ]; then
  passo "Copiando as fotos enviadas (/root/uploads)"
  rsync -a /root/uploads/ "$SERVIDOR/uploads/"
  mv /root/uploads "/root/uploads.copiado-$(date +%Y%m%d-%H%M%S)"
fi

passo "Gerando o servidor (Java)"
( cd "$SERVIDOR" && mvn -q -B -DskipTests package )
cp "$SERVIDOR"/target/marcenaria-sistema-*.jar "$SERVIDOR/app.jar"
chown -R portelas:portelas "$SERVIDOR"
chmod 600 "$CONFIG"

cat > /etc/systemd/system/portelas.service <<UNIT
[Unit]
Description=Servidor da Portela's Marcenaria
After=network.target mysql.service
Requires=mysql.service

[Service]
User=portelas
WorkingDirectory=$SERVIDOR
ExecStart=/usr/bin/java -Xms128m -Xmx512m -jar $SERVIDOR/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable portelas
systemctl restart portelas

passo "Gerando o site (Astro)"
( cd "$SITE" && npm ci --no-audit --no-fund --loglevel=error && npm run build --silent )
mkdir -p "$WEB"
rsync -a --delete "$SITE/dist/" "$WEB/"

passo "Configurando o endereço e o HTTPS (Caddy)"
if [ -n "$DOMINIO" ]; then ENDERECO="$DOMINIO, www.$DOMINIO"; else ENDERECO=":80"; fi
cat > /etc/caddy/Caddyfile <<CADDY
$ENDERECO {
	encode gzip
	request_body {
		max_size 30MB
	}
	handle /api/* {
		reverse_proxy 127.0.0.1:8080
	}
	handle /uploads/* {
		reverse_proxy 127.0.0.1:8080
	}
	handle {
		root * $WEB
		try_files {path} {path}/index.html {path}.html
		file_server
	}
}
CADDY
caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile >/dev/null
systemctl enable caddy
systemctl reload caddy || systemctl restart caddy

passo "Firewall (só SSH, HTTP e HTTPS)"
ufw allow OpenSSH >/dev/null
ufw allow 80/tcp >/dev/null
ufw allow 443/tcp >/dev/null
ufw --force enable >/dev/null

passo "Conferindo"
for i in $(seq 1 60); do curl -fs http://127.0.0.1:8080/api/mostruario/listar >/dev/null && break; sleep 2; done
if curl -fs http://127.0.0.1:8080/api/mostruario/listar >/dev/null; then echo "Servidor Java: OK"; else echo "Servidor Java: NÃO respondeu. Veja: journalctl -u portelas -n 80"; fi
curl -fsI http://127.0.0.1/ -H "Host: ${DOMINIO:-localhost}" >/dev/null && echo "Site: OK" || echo "Site: verificar Caddy (journalctl -u caddy -n 50)"

echo
echo "=============================================================="
if [ -n "$DOMINIO" ]; then echo "Endereço: https://$DOMINIO  (o HTTPS ativa assim que o domínio apontar para este servidor)"
else echo "Endereço: http://$(hostname -I | awk '{print $1}')"; fi
if [ -n "$SENHA_ADMIN_NOVA" ]; then
  echo "Painel: usuário admin | senha $SENHA_ADMIN_NOVA"
  echo "GUARDE ESTA SENHA. Ela fica em $CONFIG"
fi
echo "=============================================================="
