package org.example;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/api/galeria")
@CrossOrigin(origins = "*")
public class GaleriaController {

    private static final String PASTA_UPLOADS = "uploads/";

    // --- 1. SERVIR FOTOS ---
    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> servirFoto(HttpServletRequest request) {
        try {
            String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String nomeArquivo = path.substring(path.indexOf("/uploads/") + 9);
            Path base = Paths.get("uploads").toAbsolutePath().normalize();
            Path caminhoFisico = base.resolve(nomeArquivo).normalize();
            if (!caminhoFisico.startsWith(base)) {
                return ResponseEntity.notFound().build();
            }

            Resource recurso = new UrlResource(caminhoFisico.toUri());
            if (recurso.exists() && recurso.isReadable()) {
                String contentType = Files.probeContentType(caminhoFisico);
                if (contentType == null) {
                    contentType = "image/png";
                }
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .body(recurso);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- 2. UPLOAD DE FOTOS ---
    @PostMapping("/upload")
    public ResponseEntity<String> receberFotoProjeto(
            @RequestParam("titulo") String titulo,
            @RequestParam("categoria") String categoria,
            @RequestParam("foto") MultipartFile arquivoFoto) {
        try {
            File diretorio = new File(PASTA_UPLOADS);
            if (!diretorio.exists()) {
                diretorio.mkdirs();
            }

            String nomeOriginal = arquivoFoto.getOriginalFilename() != null ? arquivoFoto.getOriginalFilename() : "imagem.jpg";
            String nomeArquivo = System.currentTimeMillis() + "_" + nomeOriginal.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            Path caminhoFisico = Paths.get(PASTA_UPLOADS + nomeArquivo);
            Files.write(caminhoFisico, arquivoFoto.getBytes());

            String caminhoBanco = "uploads/" + nomeArquivo;
            try {
                salvarNoBanco(titulo, categoria, caminhoBanco);
            } catch (Exception erroBanco) {
                Files.deleteIfExists(Paths.get(PASTA_UPLOADS + nomeArquivo));
                throw erroBanco;
            }

            return ResponseEntity.ok("Projeto '" + titulo + "' publicado no site com sucesso!");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao salvar a imagem no disco: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro no banco de dados: " + e.getMessage());
        }
    }

    // --- 3. LISTAR TODAS AS FOTOS (REINTEGRADO) ---
    @GetMapping("/listar")
    public ResponseEntity<List<Map<String, String>>> listarTodasFotos() {
        List<Map<String, String>> listaFotos = new ArrayList<>();
        String sql = "SELECT id, titulo, categoria, caminho_imagem FROM galeria ORDER BY data_publicacao DESC";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, String> foto = new HashMap<>();
                foto.put("id", String.valueOf(rs.getInt("id")));
                foto.put("titulo", rs.getString("titulo"));
                foto.put("categoria", rs.getString("categoria"));
                foto.put("url", "/api/galeria/" + rs.getString("caminho_imagem"));
                listaFotos.add(foto);
            }

            return ResponseEntity.ok(listaFotos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- 4. LISTAR FOTOS POR CATEGORIA ---
    @GetMapping("/listar/{categoria}")
    public ResponseEntity<List<Map<String, String>>> listarFotosCategoria(@PathVariable String categoria) {
        List<Map<String, String>> listaFotos = new ArrayList<>();
        String sql = "SELECT id, titulo, categoria, caminho_imagem FROM galeria WHERE LOWER(categoria) = LOWER(?) ORDER BY data_publicacao DESC";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoria);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> foto = new HashMap<>();
                foto.put("id", String.valueOf(rs.getInt("id")));
                foto.put("titulo", rs.getString("titulo"));
                foto.put("categoria", rs.getString("categoria"));
                foto.put("url", "/api/galeria/" + rs.getString("caminho_imagem"));
                listaFotos.add(foto);
            }

            return ResponseEntity.ok(listaFotos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- 5. DELETAR FOTO ---
    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<String> deletarFoto(@PathVariable int id) {
        String sql = "DELETE FROM galeria WHERE id = ?";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String caminho = null;
            try (PreparedStatement busca = conn.prepareStatement("SELECT caminho_imagem FROM galeria WHERE id = ?")) {
                busca.setInt(1, id);
                try (ResultSet rs = busca.executeQuery()) {
                    if (rs.next()) caminho = rs.getString(1);
                }
            }

            stmt.setInt(1, id);
            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 0) {
                apagarArquivo(caminho);
                return ResponseEntity.ok("Foto excluída com sucesso.");
            } else {
                return ResponseEntity.status(404).body("Foto não encontrada.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao excluir: " + e.getMessage());
        }
    }

    // Apaga a foto do disco (só dentro da pasta uploads)
    private void apagarArquivo(String caminho) {
        if (caminho == null) return;
        try {
            Path base = Paths.get("uploads").toAbsolutePath().normalize();
            Path alvo = base.resolve(caminho.replaceFirst("^/?uploads/", "")).normalize();
            if (alvo.startsWith(base)) Files.deleteIfExists(alvo);
        } catch (Exception ignorado) {
            // o registro já foi removido; se o arquivo não sair, não atrapalha o site
        }
    }

    private void salvarNoBanco(String titulo, String categoria, String caminhoImagem) throws Exception {
        String sql = "INSERT INTO galeria (titulo, categoria, caminho_imagem) VALUES (?, ?, ?)";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, titulo);
            stmt.setString(2, categoria);
            stmt.setString(3, caminhoImagem);
            stmt.executeUpdate();
        }
    }
}