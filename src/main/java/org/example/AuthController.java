package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login do painel. A senha fica só no servidor (application.properties),
 * nunca no site. Quem acerta recebe um "token" que o painel envia em toda ação.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final long VALIDADE_MS = 12L * 60 * 60 * 1000; // 12 horas
    private static final Map<String, Long> TOKENS = new ConcurrentHashMap<>();
    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Value("${portelas.admin.usuario}")
    private String usuarioCorreto;

    @Value("${portelas.admin.senha:}")
    private String senhaCorreta;

    public static class Login {
        public String usuario;
        public String senha;
    }

    @PostMapping("/login")
    public ResponseEntity<?> entrar(@RequestBody Login dados) throws InterruptedException {
        if (senhaCorreta == null || senhaCorreta.isBlank()) {
            // Sem senha configurada (config-local.properties ou variável PORTELAS_ADMIN_SENHA), ninguém entra.
            return ResponseEntity.status(503).body(Map.of("erro", "A senha do painel não está configurada no servidor."));
        }
        boolean ok = dados != null && iguais(dados.usuario, usuarioCorreto) && iguais(dados.senha, senhaCorreta);
        if (!ok) {
            Thread.sleep(800); // atrasa tentativas de adivinhar a senha
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário ou senha incorretos."));
        }
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        TOKENS.put(token, System.currentTimeMillis() + VALIDADE_MS);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/sair")
    public ResponseEntity<String> sair(@RequestHeader(value = "Authorization", required = false) String cabecalho) {
        String token = extrair(cabecalho);
        if (token != null) TOKENS.remove(token);
        return ResponseEntity.ok("Sessão encerrada.");
    }

    /** Usado pelo AuthInterceptor para conferir cada pedido do painel. */
    public static boolean tokenValido(String cabecalho) {
        String token = extrair(cabecalho);
        if (token == null) return false;
        Long expira = TOKENS.get(token);
        if (expira == null) return false;
        if (expira < System.currentTimeMillis()) {
            TOKENS.remove(token);
            return false;
        }
        return true;
    }

    private static String extrair(String cabecalho) {
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) return null;
        String t = cabecalho.substring(7).trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean iguais(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
