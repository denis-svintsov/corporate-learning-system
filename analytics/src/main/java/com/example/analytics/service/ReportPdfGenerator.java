package com.example.analytics.service;

import com.example.analytics.dto.AnalyticsDtos.CourseStatsDto;
import com.example.analytics.dto.AnalyticsDtos.DepartmentProgressDto;
import com.example.analytics.dto.AnalyticsDtos.ReportPayload;
import com.example.analytics.dto.AnalyticsDtos.UserEngagementDto;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class ReportPdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", new Locale("ru"));

    public byte[] generate(ReportPayload payload, String reportType, String generatedBy) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont regularBase = loadFont("/fonts/LiberationSans-Regular.ttf");
            BaseFont boldBase = loadFont("/fonts/LiberationSans-Bold.ttf");
            Font titleFont = new Font(boldBase, 20, Font.NORMAL, new Color(31, 55, 84));
            Font sectionFont = new Font(boldBase, 13, Font.NORMAL, new Color(31, 55, 84));
            Font bodyFont = new Font(regularBase, 10, Font.NORMAL, new Color(31, 41, 55));
            Font mutedFont = new Font(regularBase, 9, Font.NORMAL, new Color(107, 114, 128));
            Font headerFont = new Font(boldBase, 9, Font.NORMAL, Color.WHITE);

            Paragraph title = new Paragraph("Аналитический отчет по обучению", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);

            String generatedAt = payload.overview() == null || payload.overview().generatedAt() == null
                    ? "-"
                    : payload.overview().generatedAt().format(DATE_FORMATTER);
            Paragraph meta = new Paragraph(
                    "Тип отчета: " + reportTypeLabel(reportType) + "\n"
                            + "Дата формирования: " + generatedAt + "\n"
                            + "Автор: " + safe(generatedBy, "-"),
                    mutedFont
            );
            meta.setSpacingAfter(16);
            document.add(meta);

            if (payload.overview() != null) {
                document.add(section("Сводка", sectionFont));
                PdfPTable overview = table(4);
                addHeader(overview, headerFont, "Сотрудники", "Активные", "Курсы", "Средний прогресс");
                addRow(overview, bodyFont,
                        String.valueOf(payload.overview().users()),
                        String.valueOf(payload.overview().activeUsers()),
                        String.valueOf(payload.overview().courses()),
                        percent(payload.overview().averageProgress()));
                document.add(overview);
            }

            document.add(section("Подразделения", sectionFont));
            PdfPTable departments = table(4);
            addHeader(departments, headerFont, "Подразделение", "Сотрудники", "Активные", "Завершение");
            payload.departmentProgress().stream().limit(12).forEach(row -> addDepartmentRow(departments, bodyFont, row));
            document.add(departments);

            document.add(section("Курсы", sectionFont));
            PdfPTable courses = table(4);
            addHeader(courses, headerFont, "Курс", "Назначения", "Завершения", "Прогресс");
            payload.courseStats().stream().limit(12).forEach(row -> addCourseRow(courses, bodyFont, row));
            document.add(courses);

            document.add(section("Сотрудники", sectionFont));
            PdfPTable users = table(4);
            addHeader(users, headerFont, "Сотрудник", "Подразделение", "Курсы", "Прогресс");
            payload.userEngagement().stream().limit(15).forEach(row -> addUserRow(users, bodyFont, row));
            document.add(users);

            Paragraph note = new Paragraph(
                    "Отчет сформирован автоматически. В таблицы включены первые строки аналитической выборки для демонстрации ключевых показателей.",
                    mutedFont
            );
            note.setSpacingBefore(14);
            document.add(note);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate analytics PDF report", ex);
        }
    }

    private Paragraph section(String title, Font font) {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(12);
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private PdfPTable table(int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8);
        return table;
    }

    private void addHeader(PdfPTable table, Font font, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, font));
            cell.setBackgroundColor(new Color(31, 55, 84));
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addRow(PdfPTable table, Font font, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(safe(value, "-"), font));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addDepartmentRow(PdfPTable table, Font font, DepartmentProgressDto row) {
        addRow(table, font, row.departmentName(), String.valueOf(row.totalUsers()), String.valueOf(row.activeUsers()), percent(row.completionRate()));
    }

    private void addCourseRow(PdfPTable table, Font font, CourseStatsDto row) {
        addRow(table, font, row.courseTitle(), String.valueOf(row.totalEnrollments()), String.valueOf(row.completions()), percent(row.averageProgress()));
    }

    private void addUserRow(PdfPTable table, Font font, UserEngagementDto row) {
        addRow(table, font, row.userName(), row.departmentName(), String.valueOf(row.assignedCourses()), percent(row.averageProgress()));
    }

    private BaseFont loadFont(String resourcePath) throws Exception {
        try (InputStream inputStream = ReportPdfGenerator.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Font resource not found: " + resourcePath);
            }
            return BaseFont.createFont(resourcePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, inputStream.readAllBytes(), null);
        }
    }

    private String percent(double value) {
        return Math.round(value * 10.0) / 10.0 + "%";
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String reportTypeLabel(String reportType) {
        if ("dashboard-overview".equals(reportType)) {
            return "Сводный отчет";
        }
        return safe(reportType, "Сводный отчет");
    }
}
