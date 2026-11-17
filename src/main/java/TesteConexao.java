import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class TesteConexao {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GeradorPDF gerador = new GeradorPDF();

        System.out.println("========================================");
        System.out.println("   SISTEMA MARCENARIA PORTELA'S         ");
        System.out.println("========================================");

        boolean rodando = true;

        while (rodando) {
            System.out.println("\nESCOLHA UMA OPÇÃO:");
            System.out.println("1 - Gerar Novo Orçamento (PDF) e Salvar Cliente");
            System.out.println("2 - Sair");
            System.out.print("Sua opção: ");

            int opcao = scanner.nextInt();
            scanner.nextLine();

            if (opcao == 1) {
                System.out.println("\n--- DADOS DO CLIENTE ---");
                System.out.print("Nome do Cliente: ");
                String nome = scanner.nextLine();

                System.out.print("Telefone/WhatsApp: ");
                String telefone = scanner.nextLine();

                System.out.print("Endereço completo: ");
                String endereco = scanner.nextLine();

                List<ItemServico> itensDoOrcamento = new ArrayList<>();
                boolean adicionandoMoveis = true;

                System.out.println("\n--- DADOS DOS SERVIÇOS ---");

                while (adicionandoMoveis) {
                    System.out.print("Descrição do Serviço/Móvel (Ex: Gabinete de Banheiro): ");
                    String servico = scanner.nextLine();

                    System.out.print("Valor deste item (Ex: 2860,00 - use vírgula): ");
                    double valor = scanner.nextDouble();
                    scanner.nextLine();

                    itensDoOrcamento.add(new ItemServico(servico, valor));

                    System.out.print("Deseja adicionar MAIS UM móvel neste mesmo orçamento? (1 - Sim / 2 - Não): ");
                    int resposta = scanner.nextInt();
                    scanner.nextLine();

                    if (resposta == 2) {
                        adicionandoMoveis = false;
                    }
                }

                salvarClienteRapido(nome, telefone, endereco);

                System.out.println("Calculando totais e gerando a Proposta Comercial em PDF...");
                gerador.criarOrcamentoPdf(nome, endereco, itensDoOrcamento);

            } else if (opcao == 2) {
                System.out.println("\nFechando o sistema. Bom trabalho!");
                rodando = false;
            } else {
                System.out.println("\nOpção inválida!");
            }
        }
        scanner.close();
    }

    private static void salvarClienteRapido(String nome, String telefone, String endereco) {
        String url = "jdbc:mysql://localhost:3306/marcenaria_portelas";
        String usuario = "root";
        String senha = System.getenv().getOrDefault("DB_SENHA", ""); // senha do banco vem da variável DB_SENHA
        String sql = "INSERT INTO clientes (nome, telefone, endereco) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, usuario, senha);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            stmt.setString(2, telefone);
            stmt.setString(3, endereco);
            stmt.executeUpdate();
            System.out.println("✅ Dados do cliente salvos no sistema!");

        } catch (Exception e) {
            System.out.println("Erro ao salvar no banco: " + e.getMessage());
        }
    }
}