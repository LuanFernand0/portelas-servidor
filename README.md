# Portela's Marcenaria: servidor (API)

Servidor do site e do painel da Portela's Marcenaria (Itaipu, Niterói - RJ).
Feito em Java 17 com Spring Boot 3 e MySQL.

## O que ele faz
- Galeria de projetos: listar, enviar e excluir fotos.
- Mostruário de cores MDF: listar, cadastrar (uma a uma ou em lote) e excluir chapas.
- Clientes e orçamentos, com geração de proposta em PDF.
- Login do painel: a senha é conferida aqui no servidor; clientes, orçamentos e as ações de enviar/excluir exigem login.
- Atualizações do banco: os arquivos `.sql` da pasta `atualizacoes` rodam sozinhos uma única vez ao ligar o servidor.

## Como rodar
1. Copie `config-local.exemplo.properties` para `config-local.properties` e preencha as senhas do MySQL e do painel.
2. Crie o banco `marcenaria_portelas` no MySQL.
3. Rode a classe `MarcenariaApplication` (ou `mvn spring-boot:run`). O servidor sobe na porta 8080.

Para gerar o arquivo que roda online: `mvn package` (gera o `.jar` na pasta `target`).

O site (Astro) fica em outro repositório e conversa com este servidor pela porta 8080.
