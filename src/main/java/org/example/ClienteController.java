package org.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    public static class Cliente {
        public int id;
        public String nome;
        public String telefone;
        public String endereco;

        // Construtor vazio essencial para desserialização do JSON
        public Cliente() {}

        public Cliente(int id, String nome, String telefone, String endereco) {
            this.id = id;
            this.nome = nome;
            this.telefone = telefone;
            this.endereco = endereco;
        }
    }


    @PostMapping("/cadastrar")
    public ResponseEntity<String> cadastrarCliente(@RequestBody Cliente cliente) {
        String sql = "INSERT INTO clientes (nome, telefone, endereco) VALUES (?, ?, ?)";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cliente.nome);
            stmt.setString(2, cliente.telefone);
            stmt.setString(3, cliente.endereco);
            stmt.executeUpdate();
            return ResponseEntity.ok("Cliente " + cliente.nome + " cadastrado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao cadastrar: " + e.getMessage());
        }
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Cliente>> listarClientes() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id, nome, telefone, endereco FROM clientes ORDER BY nome ASC";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Cliente(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("telefone"),
                        rs.getString("endereco")
                ));
            }
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/excluir/{id}")
    public ResponseEntity<String> excluirCliente(@PathVariable("id") int id) {
        try (Connection conn = Banco.conectar()) {
            String sql = "DELETE FROM clientes WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
                return ResponseEntity.ok("Cliente excluído com sucesso!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro do MySQL: " + e.getMessage());
        }
    }
}