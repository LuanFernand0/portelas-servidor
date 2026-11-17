import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class ProjetoDAO {

    private String url = "jdbc:mysql://localhost:3306/marcenaria_portelas";
    private String usuario = "root";
    private String senha = System.getenv().getOrDefault("DB_SENHA", ""); // senha do banco vem da variável DB_SENHA

    // Método para obter conexão
    private Connection obterConexao() throws Exception {
        return DriverManager.getConnection(url, usuario, senha);
    }

    // ---------------------------------------------------------
    // Método para SALVAR o orçamento finalizado no banco
    // ---------------------------------------------------------
    public void inserir(Projeto projeto) {
        // O comando SQL que insere na tabela projetos
        String sql = "INSERT INTO projetos (cliente_id, descricao, valor_total) VALUES (?, ?, ?)";

        try (Connection conn = obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Preenchendo os pontos de interrogação com os dados do objeto Projeto
            stmt.setInt(1, projeto.getClienteId());
            stmt.setString(2, projeto.getDescricao());
            stmt.setDouble(3, projeto.getValorTotal()); // Pega o valor já com a matemática aplicada

            stmt.executeUpdate();
            System.out.println("SUCESSO: Orçamento '" + projeto.getDescricao() + "' gravado no banco de dados!");

        } catch (Exception e) {
            System.out.println("Erro ao salvar o projeto: " + e.getMessage());
            e.printStackTrace();
        }
    }
}