package org.example;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Mostruário de cores (chapas de MDF): listar, enviar e excluir.
 * Mesmo padrão do GaleriaController. As imagens ficam na pasta "uploads/"
 * e são servidas pela rota que a galeria já tem (/api/galeria/uploads/...).
 */
@RestController
@DependsOn("banco") // a conexão precisa estar pronta antes de criar a tabela
@RequestMapping("/api/mostruario")
@CrossOrigin(origins = "*")
public class MostruarioController {

    private static final String PASTA_UPLOADS = "uploads/";

    // Cria a tabela sozinha na primeira vez que o servidor sobe (não faz nada se ela já existir)
    @PostConstruct
    public void prepararTabela() {
        String sql = "CREATE TABLE IF NOT EXISTS mostruario ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "nome VARCHAR(120) NOT NULL, "
                + "fabricante VARCHAR(80) NOT NULL, "
                + "categoria VARCHAR(80) NOT NULL, "
                + "url VARCHAR(255) NOT NULL"
                + ") CHARACTER SET utf8mb4";
        try (Connection conn = Banco.conectar();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            System.out.println("Aviso: não foi possível preparar a tabela mostruario: " + e.getMessage());
        }
    }

    // --- 1. LISTAR TODAS AS CHAPAS ---
    @GetMapping("/listar")
    public ResponseEntity<List<Map<String, String>>> listar() {
        List<Map<String, String>> lista = new ArrayList<>();
        String sql = "SELECT id, nome, fabricante, categoria, url FROM mostruario ORDER BY fabricante, categoria, nome";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, String> chapa = new HashMap<>();
                chapa.put("id", String.valueOf(rs.getInt("id")));
                chapa.put("nome", rs.getString("nome"));
                chapa.put("fabricante", rs.getString("fabricante"));
                chapa.put("categoria", rs.getString("categoria"));
                chapa.put("url", montarUrl(rs.getString("url")));
                lista.add(chapa);
            }
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- 2. ENVIAR UMA CHAPA ---
    @PostMapping("/upload")
    public ResponseEntity<String> receber(
            @RequestParam("nome") String nome,
            @RequestParam("fabricante") String fabricante,
            @RequestParam("categoria") String categoria,
            @RequestParam("foto") MultipartFile arquivoFoto) {
        try {
            if (arquivoFoto.isEmpty()) {
                return ResponseEntity.badRequest().body("Nenhuma imagem enviada.");
            }
            File diretorio = new File(PASTA_UPLOADS);
            if (!diretorio.exists()) {
                diretorio.mkdirs();
            }

            String nomeOriginal = arquivoFoto.getOriginalFilename() != null ? arquivoFoto.getOriginalFilename() : "chapa.jpg";
            String nomeArquivo = System.currentTimeMillis() + "_" + nomeOriginal.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            Files.write(Paths.get(PASTA_UPLOADS + nomeArquivo), arquivoFoto.getBytes());

            try {
                salvarNoBanco(nome.trim(), fabricante.trim(), categoria.trim(), "uploads/" + nomeArquivo);
            } catch (Exception erroBanco) {
                Files.deleteIfExists(Paths.get(PASTA_UPLOADS + nomeArquivo));
                throw erroBanco;
            }
            return ResponseEntity.ok("Chapa '" + nome + "' cadastrada com sucesso!");
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao salvar a imagem no disco: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro no banco de dados: " + e.getMessage());
        }
    }

    // --- 3. EXCLUIR UMA CHAPA ---
    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<String> deletar(@PathVariable int id) {
        try (Connection conn = Banco.conectar()) {
            String caminho = null;
            try (PreparedStatement busca = conn.prepareStatement("SELECT url FROM mostruario WHERE id = ?")) {
                busca.setInt(1, id);
                try (ResultSet rs = busca.executeQuery()) {
                    if (rs.next()) caminho = rs.getString(1);
                }
            }

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM mostruario WHERE id = ?")) {
                del.setInt(1, id);
                if (del.executeUpdate() == 0) {
                    return ResponseEntity.status(404).body("Chapa não encontrada.");
                }
            }

            // Apaga o arquivo da imagem só se nenhuma outra chapa estiver usando ele
            if (caminho != null) {
                try (PreparedStatement uso = conn.prepareStatement("SELECT COUNT(*) FROM mostruario WHERE url = ?")) {
                    uso.setString(1, caminho);
                    try (ResultSet rs = uso.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) apagarArquivo(caminho);
                    }
                }
            }
            return ResponseEntity.ok("Chapa excluída com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao excluir: " + e.getMessage());
        }
    }

    private void salvarNoBanco(String nome, String fabricante, String categoria, String caminho) throws Exception {
        String sql = "INSERT INTO mostruario (nome, fabricante, categoria, url) VALUES (?, ?, ?, ?)";
        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, fabricante);
            stmt.setString(3, categoria);
            stmt.setString(4, caminho);
            stmt.executeUpdate();
        }
    }

    // Endereço da imagem para o site, sem "localhost": o site completa com o endereço do servidor
    private String montarUrl(String caminho) {
        if (caminho == null) return "";
        if (caminho.startsWith("http") || caminho.startsWith("/")) return caminho;
        return "/api/galeria/" + caminho;
    }

    private void apagarArquivo(String caminho) {
        try {
            Path base = Paths.get("uploads").toAbsolutePath().normalize();
            Path alvo = base.resolve(caminho.replaceFirst("^/?uploads/", "")).normalize();
            if (alvo.startsWith(base)) Files.deleteIfExists(alvo);
        } catch (Exception ignorado) {
            // se não der para apagar o arquivo, o registro já foi removido; segue em frente
        }
    }
}