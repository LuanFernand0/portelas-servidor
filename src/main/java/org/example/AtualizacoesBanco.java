package org.example;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

/**
 * Aplica sozinho, ao ligar o servidor, os arquivos .sql da pasta "atualizacoes".
 * Cada arquivo roda uma única vez (fica anotado na tabela atualizacoes_aplicadas).
 */
@Component
public class AtualizacoesBanco implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        Path pasta = Paths.get("atualizacoes");
        if (!Files.isDirectory(pasta)) return;

        try (Connection conn = Banco.conectar()) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS atualizacoes_aplicadas ("
                        + "nome VARCHAR(190) PRIMARY KEY, aplicada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ") CHARACTER SET utf8mb4");
            }

            List<Path> arquivos;
            try (Stream<Path> s = Files.list(pasta)) {
                arquivos = s.filter(p -> p.getFileName().toString().endsWith(".sql")).sorted().toList();
            }

            for (Path arquivo : arquivos) {
                String nome = arquivo.getFileName().toString();
                if (jaAplicada(conn, nome)) continue;

                String sql = Files.readString(arquivo, StandardCharsets.UTF_8);
                conn.setAutoCommit(false);
                try (Statement st = conn.createStatement()) {
                    for (String comando : sql.split(";\\s*(\\r?\\n|$)")) {
                        String limpo = comando.lines()
                                .filter(l -> !l.trim().startsWith("--"))
                                .reduce("", (a, b) -> a + "\n" + b).trim();
                        if (!limpo.isEmpty()) st.execute(limpo);
                    }
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO atualizacoes_aplicadas (nome) VALUES (?)")) {
                        ps.setString(1, nome);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    System.out.println("✔ Atualização do banco aplicada: " + nome);
                } catch (Exception e) {
                    conn.rollback();
                    System.out.println("❌ Falha ao aplicar " + nome + ": " + e.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            System.out.println("Aviso: atualizações do banco não verificadas: " + e.getMessage());
        }
    }

    private boolean jaAplicada(Connection conn, String nome) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM atualizacoes_aplicadas WHERE nome = ?")) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
