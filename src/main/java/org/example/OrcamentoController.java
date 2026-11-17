package org.example;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orcamentos")
@CrossOrigin(origins = "*")
public class OrcamentoController {

    // Estrutura para enviar o histórico para o painel
    public static class OrcamentoSalvo {
        public int id;
        public String cliente;
        public String servicosResumo;
        public String valorTotal;
        public String data;

        public OrcamentoSalvo(int id, String cliente, String servicosResumo, String valorTotal, String data) {
            this.id = id;
            this.cliente = cliente;
            this.servicosResumo = servicosResumo;
            this.valorTotal = valorTotal;
            this.data = data;
        }
    }

    @PostMapping("/gerar-pdf")
    public ResponseEntity<byte[]> gerarPdfOrcamento(
            @RequestParam("cliente") String cliente,
            @RequestParam("servicos") List<String> servicos,
            @RequestParam("valores") List<String> valores,
            @RequestParam("valorTotal") String valorTotal) {

        try {
            // 1. SALVAR NO BANCO DE DADOS PRIMEIRO

            String resumoServicos = String.join(", ", servicos);

            String sql = "INSERT INTO orcamentos (cliente, servicos_resumo, valor_total) VALUES (?, ?, ?)";
            try (Connection conn = Banco.conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cliente);
                stmt.setString(2, resumoServicos);
                stmt.setString(3, valorTotal);
                stmt.executeUpdate();
            } catch (Exception dbErro) {
                System.out.println("Erro ao salvar histórico: " + dbErro.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            // 2. GERAR O PDF
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, saida);
            document.open();

            try {
                Image logo = Image.getInstance("logo.png");
                logo.scaleToFit(500, 200);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            } catch (Exception e) {}

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA, 16, BaseColor.BLACK);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
            Font fontVermelha = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.RED);
            Font fontNegrito = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);

            document.add(new Paragraph("\n"));
            Paragraph pOrcamento = new Paragraph("ORÇAMENTO", fontTitulo);
            pOrcamento.setAlignment(Element.ALIGN_CENTER);
            document.add(pOrcamento);

            Paragraph pCliente = new Paragraph(cliente, fontTitulo);
            pCliente.setAlignment(Element.ALIGN_CENTER);
            document.add(pCliente);

            document.add(new Paragraph("\n"));
            Paragraph pEmpresa = new Paragraph("Contratada: Portela Marcenaria inscrito no CNPJ 27.665.88/0001-85, localizada na Estr. Francisco da Cruz Nunes, 2254 - Itaipu - Niterói - RJ", fontNormal);
            pEmpresa.setAlignment(Element.ALIGN_CENTER);
            document.add(pEmpresa);
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1.5f});

            for (int i = 0; i < servicos.size(); i++) {
                PdfPCell celulaServico = new PdfPCell(new Phrase(servicos.get(i), fontNormal));
                celulaServico.setPadding(6);
                table.addCell(celulaServico);
                PdfPCell celulaValor = new PdfPCell(new Phrase("R$ " + valores.get(i), fontNormal));
                celulaValor.setPadding(6);
                table.addCell(celulaValor);
            }

            PdfPCell cellValorLabel = new PdfPCell(new Phrase("VALOR", fontVermelha));
            cellValorLabel.setPadding(6);
            table.addCell(cellValorLabel);
            PdfPCell cellValorTotal = new PdfPCell(new Phrase("R$ " + valorTotal, fontNegrito));
            cellValorTotal.setPadding(6);
            table.addCell(cellValorTotal);

            PdfPCell cellPagamentoLabel = new PdfPCell(new Phrase("FORMA DE PAGAMENTO", fontVermelha));
            cellPagamentoLabel.setPadding(6);
            table.addCell(cellPagamentoLabel);
            PdfPCell cellPagamentoDesc = new PdfPCell(new Phrase("SINAL DE 50% RESTANTE NA ENTREGA OU 10 X NO CARTÃO.", fontVermelha));
            cellPagamentoDesc.setPadding(6);
            table.addCell(cellPagamentoDesc);
            document.add(table);

            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("CLIENTE " + cliente, fontNormal));
            document.add(new Paragraph("\n"));
            Chunk chunkValidade = new Chunk("ORÇAMENTO VÁLIDO ATÉ 10 DIAS CORRIDO.", fontVermelha);
            chunkValidade.setBackground(BaseColor.YELLOW);
            document.add(new Paragraph(chunkValidade));
            document.add(new Paragraph("\n"));
            document.add(new Paragraph(" SINAL DE 50% E RESTANTE NO ALTO DA ENTREGA.", fontVermelha));
            document.add(new Paragraph("\n"));
            document.add(new Paragraph(" ; Portela's Marcenaria", fontNormal));

            document.close();

            byte[] pdfBytes = saida.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Orcamento_" + cliente.replaceAll(" ", "_") + ".pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> baixarOrcamentoPdf(@PathVariable("id") int id) {
        String sql = "SELECT cliente, servicos_resumo, valor_total FROM orcamentos WHERE id = ?";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String cliente = rs.getString("cliente");
                String servicosResumo = rs.getString("servicos_resumo");
                String valorTotal = rs.getString("valor_total");

                Document document = new Document(PageSize.A4, 40, 40, 40, 40);
                ByteArrayOutputStream saida = new ByteArrayOutputStream();
                PdfWriter.getInstance(document, saida);
                document.open();

                try {
                    Image logo = Image.getInstance("logo.png");
                    logo.scaleToFit(500, 200);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                } catch (Exception e) {}

                Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA, 16, BaseColor.BLACK);
                Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
                Font fontVermelha = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.RED);
                Font fontNegrito = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);

                document.add(new Paragraph("\n"));
                Paragraph pOrcamento = new Paragraph("ORÇAMENTO #" + id, fontTitulo);
                pOrcamento.setAlignment(Element.ALIGN_CENTER);
                document.add(pOrcamento);

                Paragraph pCliente = new Paragraph(cliente, fontTitulo);
                pCliente.setAlignment(Element.ALIGN_CENTER);
                document.add(pCliente);

                document.add(new Paragraph("\n"));
                Paragraph pEmpresa = new Paragraph("Contratada: Portela Marcenaria inscrito no CNPJ 27.665.88/0001-85, localizada na Estr. Francisco da Cruz Nunes, 2254 - Itaipu - Niterói - RJ", fontNormal);
                pEmpresa.setAlignment(Element.ALIGN_CENTER);
                document.add(pEmpresa);
                document.add(new Paragraph("\n"));

                PdfPTable table = new PdfPTable(2);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{3f, 1.5f});

                PdfPCell celulaServico = new PdfPCell(new Phrase(servicosResumo, fontNormal));
                celulaServico.setPadding(6);
                table.addCell(celulaServico);
                PdfPCell celulaValor = new PdfPCell(new Phrase("R$ " + valorTotal, fontNormal));
                celulaValor.setPadding(6);
                table.addCell(celulaValor);

                PdfPCell cellValorLabel = new PdfPCell(new Phrase("VALOR TOTAL", fontVermelha));
                cellValorLabel.setPadding(6);
                table.addCell(cellValorLabel);
                PdfPCell cellValorTotal = new PdfPCell(new Phrase("R$ " + valorTotal, fontNegrito));
                cellValorTotal.setPadding(6);
                table.addCell(cellValorTotal);

                PdfPCell cellPagamentoLabel = new PdfPCell(new Phrase("FORMA DE PAGAMENTO", fontVermelha));
                cellPagamentoLabel.setPadding(6);
                table.addCell(cellPagamentoLabel);
                PdfPCell cellPagamentoDesc = new PdfPCell(new Phrase("SINAL DE 50% RESTANTE NA ENTREGA OU 10 X NO CARTÃO.", fontVermelha));
                cellPagamentoDesc.setPadding(6);
                table.addCell(cellPagamentoDesc);
                document.add(table);

                document.add(new Paragraph("\n\n"));
                document.add(new Paragraph("CLIENTE " + cliente, fontNormal));
                document.add(new Paragraph("\n"));
                Chunk chunkValidade = new Chunk("ORÇAMENTO VÁLIDO ATÉ 10 DIAS CORRIDO.", fontVermelha);
                chunkValidade.setBackground(BaseColor.YELLOW);
                document.add(new Paragraph(chunkValidade));
                document.add(new Paragraph("\n"));
                document.add(new Paragraph(" SINAL DE 50% E RESTANTE NO ALTO DA ENTREGA.", fontVermelha));
                document.add(new Paragraph("\n"));
                document.add(new Paragraph(" ; Portela's Marcenaria", fontNormal));

                document.close();

                byte[] pdfBytes = saida.toByteArray();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "Orcamento_" + id + "_" + cliente.replaceAll(" ", "_") + ".pdf");
                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/listar")
    public ResponseEntity<List<OrcamentoSalvo>> listarOrcamentos() {
        List<OrcamentoSalvo> lista = new ArrayList<>();
        String sql = "SELECT id, cliente, servicos_resumo, valor_total, DATE_FORMAT(data_geracao, '%d/%m/%Y %H:%i') as data_formatada FROM orcamentos ORDER BY id DESC";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new OrcamentoSalvo(
                        rs.getInt("id"),
                        rs.getString("cliente"),
                        rs.getString("servicos_resumo"),
                        rs.getString("valor_total"),
                        rs.getString("data_formatada")
                ));
            }
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/excluir/{id}")
    public ResponseEntity<String> excluirOrcamento(@PathVariable("id") int id) {
        String sql = "DELETE FROM orcamentos WHERE id = ?";

        try (Connection conn = Banco.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            return ResponseEntity.ok("Orçamento excluído com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao excluir orçamento: " + e.getMessage());
        }
    }
}