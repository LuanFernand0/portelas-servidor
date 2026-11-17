import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private String url = "jdbc:mysql://localhost:3306/marcenaria_portelas";
    private String usuario = "root";
    private String senha = System.getenv().getOrDefault("DB_SENHA", ""); // senha do banco vem da variável DB_SENHA

    // Método para obter uma conexão com o banco de dados
    private Connection obterConexao() throws Exception {
        return DriverManager.getConnection(url, usuario, senha);
    }

    // Método para LISTAR todos os clientes do banco de dados
    public List<Cliente> listarTodos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM clientes";

        try (Connection conn = obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Pegamos os dados da linha atual do banco
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                String telefone = rs.getString("telefone");

                // Usamos o nosso "molde" para criar um objeto Cliente de verdade
                Cliente cliente = new Cliente(id, nome, telefone);

                // Guardamos esse cliente na nossa lista
                clientes.add(cliente);
            }

        } catch (Exception e) {
            System.out.println("Erro ao listar clientes: " + e.getMessage());
            e.printStackTrace();



        }

        return clientes;
    }

    // ---------------------------------------------------------
    // Método 2: SALVAR e retornar o ID gerado pelo banco
    // ---------------------------------------------------------
    public int inserir(Cliente cliente) {
        String sql = "INSERT INTO clientes (nome, telefone) VALUES (?, ?)";
        int idGerado = -1;

        try (Connection conn = obterConexao();
             // Avisamos ao MySQL que queremos a chave (ID) de volta
             PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getTelefone());
            stmt.executeUpdate();

            // Capturando o ID que o MySQL acabou de criar
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    idGerado = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao salvar cliente: " + e.getMessage());
            e.printStackTrace();
        }
        return idGerado; // Retorna o número para usarmos na tela
    }
        }

