public class Projeto {

    private int id;
    private int clienteId;
    private String descricao;
    private double metrosQuadrados;
    private boolean isBranco;
    private double valorTotal;

    // NOVO: Variável para guardar o quanto a marcenaria gastou para fabricar
    private double custoMateriais;

    public Projeto(int id, int clienteId, String descricao, double metrosQuadrados, boolean isBranco) {
        this.id = id;
        this.clienteId = clienteId;
        this.descricao = descricao;
        this.metrosQuadrados = metrosQuadrados;
        this.isBranco = isBranco;
        this.valorTotal = calcularValorFinal();

        // Todo projeto nasce com 0 de custo, até adicionarmos as peças
        this.custoMateriais = 0.0;
    }

    private double calcularValorFinal() {
        double valorCalculado = this.metrosQuadrados * 1600.00;
        if (!this.isBranco) {
            valorCalculado = valorCalculado * 1.25;
        }
        return valorCalculado;
    }

    // -------------------------------------------------------------------
    // NOVOS MÉTODOS DE INTELIGÊNCIA FINANCEIRA
    // -------------------------------------------------------------------



    // Método que calcula a diferença entre o preço de venda e o custo
    public double calcularLucroLiquido() {
        return this.valorTotal - this.custoMateriais;
    }

    // -------------------------------------------------------------------

    // Getters
    public int getId() { return id; }
    public int getClienteId() { return clienteId; }
    public String getDescricao() { return descricao; }
    public double getMetrosQuadrados() { return metrosQuadrados; }
    public boolean isBranco() { return isBranco; }
    public double getValorTotal() { return valorTotal; }
    public double getCustoMateriais() { return custoMateriais; } // Novo Getter
}