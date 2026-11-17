import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.util.List;

public class GeradorPDF {

    public void criarOrcamentoPdf(String nomeCliente, String enderecoCliente, List<ItemServico> itens) {
        Document documento = new Document();
        String nomeArquivo = "Orçamento_" + nomeCliente.replaceAll(" ", "_") + ".pdf";

        try {
            PdfWriter.getInstance(documento, new FileOutputStream(nomeArquivo));
            documento.open();

            // Configuração de Fontes
            Font fonteCabecalho = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            Font fonteTituloPrincipal = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.NORMAL);
            Font fonteSubtitulo = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font fonteNormal = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL);
            Font fonteNegrito = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

            // Cores da Tabela
            BaseColor corFundoCabecalho = new BaseColor(44, 76, 88); // Azul escuro/Petróleo da imagem
            BaseColor corFundoTotal = new BaseColor(230, 230, 230);  // Cinza claro

            // 1. LOGO
            try {
                Image logo = Image.getInstance("logo.png");
                logo.setAlignment(Element.ALIGN_CENTER);
                logo.scaleToFit(350, 120);
                documento.add(logo);
                documento.add(new Paragraph("\n"));
            } catch (Exception e) {
                System.out.println("⚠️ Imagem 'logo.png' não encontrada. Gerando sem logo.");
            }

            // 2. CABEÇALHO DE CONTATO
            Paragraph infoEmpresa = new Paragraph("PORTELA MARCENARIA | CNPJ 27.665.88/0001-85\n" +
                    "Estrada Francisco da Cruz Nunes, 2254 – Itaipu – Niterói – RJ\n" +
                    "E-mail: portelasmarcenaria@gmail.com\n\n", fonteCabecalho);
            infoEmpresa.setAlignment(Element.ALIGN_CENTER);
            documento.add(infoEmpresa);

            // 3. TÍTULO DA PROPOSTA
            Paragraph titulo = new Paragraph("PROPOSTA DE ORÇAMENTO PARA SERVIÇO\n\n", fonteTituloPrincipal);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            // 4. DADOS DO CLIENTE
            documento.add(new Paragraph("DADOS DO CLIENTE", fonteSubtitulo));
            documento.add(new Paragraph("NOME: " + nomeCliente, fonteNormal));
            documento.add(new Paragraph("ENDEREÇO: " + enderecoCliente + "\n\n", fonteNormal));

            // 5. TABELA DE ITENS
            PdfPTable tabela = new PdfPTable(2);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{3.5f, 1.5f});

            // Cabeçalho da Tabela (Azul escuro)
            PdfPCell cellDescCabecalho = new PdfPCell(new Phrase("DESCRIÇÃO DO ITEM", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE)));
            cellDescCabecalho.setBackgroundColor(corFundoCabecalho);
            cellDescCabecalho.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellDescCabecalho.setPadding(8f);
            tabela.addCell(cellDescCabecalho);

            PdfPCell cellValorCabecalho = new PdfPCell(new Phrase("VALOR UNITÁRIO\n(R$)", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE)));
            cellValorCabecalho.setBackgroundColor(corFundoCabecalho);
            cellValorCabecalho.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellValorCabecalho.setPadding(8f);
            tabela.addCell(cellValorCabecalho);

            double valorTotal = 0.0;

            // Preenchendo os itens
            for (ItemServico item : itens) {
                PdfPCell cellDesc = new PdfPCell(new Phrase(item.getDescricao(), fonteNormal));
                cellDesc.setPadding(6f);
                tabela.addCell(cellDesc);

                PdfPCell cellValor = new PdfPCell(new Phrase(String.format("R$ %.2f", item.getValor()), fonteNormal));
                cellValor.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellValor.setPadding(6f);
                tabela.addCell(cellValor);

                valorTotal += item.getValor();
            }

            // Linha do Total (Cinza Claro)
            PdfPCell cellTextoTotal = new PdfPCell(new Phrase("TOTAL GERAL DO PEDIDO", fonteNegrito));
            cellTextoTotal.setBackgroundColor(corFundoTotal);
            cellTextoTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellTextoTotal.setPadding(8f);
            tabela.addCell(cellTextoTotal);

            PdfPCell cellValorTotal = new PdfPCell(new Phrase(String.format("R$ %.2f", valorTotal), new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            cellValorTotal.setBackgroundColor(corFundoTotal);
            cellValorTotal.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellValorTotal.setPadding(8f);
            tabela.addCell(cellValorTotal);

            documento.add(tabela);
            documento.add(new Paragraph("\n"));

            // 6. CONDIÇÕES COMERCIAIS E PRAZOS
            documento.add(new Paragraph("CONDIÇÕES COMERCIAIS E PRAZOS", fonteSubtitulo));
            documento.add(new Paragraph("1. Validade da Proposta: 10 (dez) dias corridos a partir desta data.", fonteNormal));
            documento.add(new Paragraph("2. Prazo de Execução: 30 dias úteis após a aprovação do projeto e aprovação do material.", fonteNormal));
            documento.add(new Paragraph("3. Garantia: 1 ano contra defeitos de fabricação (materiais e mão de obra).\n\n", fonteNormal));

            // 7. CONDIÇÕES DE PAGAMENTO (Cálculo dinâmico de 50%)
            double valorMetade = valorTotal / 2;
            String textoMetade = String.format("R$ %.2f", valorMetade);

            documento.add(new Paragraph("CONDIÇÕES DE PAGAMENTO", fonteSubtitulo));

            // Usando Chunk para colocar apenas a palavra "ENTRADA" em negrito
            Paragraph linhaEntrada = new Paragraph();
            linhaEntrada.add(new Chunk("• ENTRADA: ", fonteNegrito));
            linhaEntrada.add(new Chunk("Sinal de 50% (" + textoMetade + ") na aprovação do projeto.", fonteNormal));
            documento.add(linhaEntrada);

            Paragraph linhaRestante = new Paragraph();
            linhaRestante.add(new Chunk("• RESTANTE: ", fonteNegrito));
            linhaRestante.add(new Chunk("50% (" + textoMetade + ") no ato da entrega do serviço.", fonteNormal));
            documento.add(linhaRestante);

            Paragraph linhaParcelamento = new Paragraph();
            linhaParcelamento.add(new Chunk("• OPÇÕES DE PARCELAMENTO: ", fonteNegrito));
            linhaParcelamento.add(new Chunk("Até 10x no cartão (taxas aplicáveis).\n\n\n", fonteNormal));
            documento.add(linhaParcelamento);

            // 8. ASSINATURA E RODAPÉ
            Paragraph assinatura = new Paragraph("Cordialmente,\n\n___________________________________\nPORTELA MARCENARIA\n\n", fonteNormal);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            documento.add(assinatura);

            Paragraph selo = new Paragraph("SELO DE QUALIDADE E COMPROMISSO PORTELA", new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.GRAY));
            selo.setAlignment(Element.ALIGN_CENTER);
            documento.add(selo);

            System.out.println("✅ SUCESSO: Proposta Comercial gerada com o nome '" + nomeArquivo + "'");

        } catch (Exception e) {
            System.out.println("Erro ao gerar o PDF: " + e.getMessage());
            e.printStackTrace();
        } finally {
            documento.close();
        }
    }
}