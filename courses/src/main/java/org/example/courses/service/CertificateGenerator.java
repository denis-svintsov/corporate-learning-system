package org.example.courses.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class CertificateGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru"));

    public GeneratedCertificate generate(String recipientName, String courseTitle) {
        OffsetDateTime issuedAt = OffsetDateTime.now();
        String certificateNumber = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        byte[] pdf = generatePdfBytes(recipientName, courseTitle, certificateNumber, issuedAt);
        String hash = sha256Hex(pdf);
        return new GeneratedCertificate(pdf, hash, issuedAt, certificateNumber);
    }

    private byte[] generatePdfBytes(
            String recipientName,
            String courseTitle,
            String certificateNumber,
            OffsetDateTime issuedAt
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 54, 54, 42, 42);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            BaseFont regularFont = loadFont("/fonts/LiberationSans-Regular.ttf");
            BaseFont boldFont = loadFont("/fonts/LiberationSans-Bold.ttf");
            Font brandFont = new Font(boldFont, 18, Font.NORMAL, new Color(31, 55, 84));
            Font titleFont = new Font(boldFont, 36, Font.NORMAL, new Color(31, 55, 84));
            Font subtitleFont = new Font(regularFont, 15, Font.NORMAL, new Color(88, 99, 115));
            Font recipientFont = new Font(boldFont, 28, Font.NORMAL, new Color(24, 35, 49));
            Font courseFont = new Font(boldFont, 20, Font.NORMAL, new Color(31, 55, 84));
            Font bodyFont = new Font(regularFont, 12, Font.NORMAL, new Color(75, 85, 99));
            Font smallFont = new Font(regularFont, 9, Font.NORMAL, new Color(107, 114, 128));

            drawFrame(writer);

            Paragraph brand = centered("Корпоративная платформа обучения", brandFont);
            brand.setSpacingAfter(18);
            document.add(brand);

            Paragraph title = centered("СЕРТИФИКАТ", titleFont);
            title.setSpacingAfter(6);
            document.add(title);

            Paragraph subtitle = centered("подтверждает успешное завершение обучения", subtitleFont);
            subtitle.setSpacingAfter(26);
            document.add(subtitle);

            Paragraph recipient = centered(safe(recipientName, "Сотрудник"), recipientFont);
            recipient.setSpacingAfter(20);
            document.add(recipient);

            Paragraph courseLabel = centered("по курсу", bodyFont);
            courseLabel.setSpacingAfter(8);
            document.add(courseLabel);

            Paragraph course = centered("«" + safe(courseTitle, "Учебный курс") + "»", courseFont);
            course.setSpacingAfter(28);
            document.add(course);

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(72);
            meta.setWidths(new float[]{1, 1});
            meta.setSpacingBefore(6);
            meta.addCell(metaCell("Дата выдачи", issuedAt.format(DATE_FORMATTER), bodyFont));
            meta.addCell(metaCell("Номер сертификата", certificateNumber, bodyFont));
            document.add(meta);

            Paragraph note = centered(
                    "Документ сформирован автоматически в корпоративной системе обучения.",
                    smallFont
            );
            note.setSpacingBefore(26);
            document.add(note);

            Paragraph hashHint = centered("Контрольная сумма хранится в системе для проверки подлинности.", smallFont);
            document.add(hashHint);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF", e);
        }
    }

    private void drawFrame(PdfWriter writer) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = writer.getPageSize();
        canvas.saveState();
        canvas.setColorStroke(new Color(31, 55, 84));
        canvas.setLineWidth(2.2f);
        canvas.rectangle(28, 28, page.getWidth() - 56, page.getHeight() - 56);
        canvas.stroke();
        canvas.setColorStroke(new Color(107, 160, 180));
        canvas.setLineWidth(0.8f);
        canvas.rectangle(38, 38, page.getWidth() - 76, page.getHeight() - 76);
        canvas.stroke();
        canvas.restoreState();
    }

    private Paragraph centered(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private PdfPCell metaCell(String label, String value, Font bodyFont) {
        Font labelFont = new Font(bodyFont.getBaseFont(), 9, Font.NORMAL, new Color(107, 114, 128));
        Font valueFont = new Font(bodyFont.getBaseFont(), 13, Font.NORMAL, new Color(31, 55, 84));
        Paragraph content = new Paragraph();
        content.setAlignment(Element.ALIGN_CENTER);
        content.add(new Phrase(label + "\n", labelFont));
        content.add(new Phrase(value, valueFont));
        PdfPCell cell = new PdfPCell(content);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private BaseFont loadFont(String resourcePath) {
        try (InputStream inputStream = CertificateGenerator.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Font resource not found: " + resourcePath);
            }
            return BaseFont.createFont(
                    resourcePath,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    inputStream.readAllBytes(),
                    null
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load certificate font: " + resourcePath, ex);
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash certificate", e);
        }
    }

    public record GeneratedCertificate(
            byte[] pdfBytes,
            String hash,
            OffsetDateTime issueDate,
            String certificateNumber
    ) {
    }
}
