package de.schrell.cadas.application.reports;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Exportiert die Räume- und Materialauswertung als gut lesbares PDF ohne eingebettete SVG-Ansichten.
 */
public final class SurfaceMaterialReportPdfService {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_MARGIN = 42.0f;
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float TITLE_FONT_SIZE = 16.0f;
    private static final float SECTION_FONT_SIZE = 13.0f;
    private static final float SUBSECTION_FONT_SIZE = 11.0f;
    private static final float BODY_FONT_SIZE = 9.0f;

    public void export(SurfaceMaterialListService.SurfaceMaterialReport report, Path targetFile) throws IOException {
        Objects.requireNonNull(report, "report darf nicht null sein.");
        Objects.requireNonNull(targetFile, "targetFile darf nicht null sein.");
        Path exportPath = targetFile.toAbsolutePath().normalize();
        Path parent = exportPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (PDDocument document = new PDDocument()) {
            try (PdfWriter writer = new PdfWriter(document)) {
                writer.title("Räume und Materialien - " + report.projectName());
                appendRooms(writer, report.rooms());
                appendMaterialSummary(writer, report.materials());
                appendHeatingPlans(writer, report.heatingPlans());
                appendHeatingElements(writer, report.heatingElements());
                appendMaterialDetails(writer, report.materials());
                appendRoomComplexities(writer, report.roomComplexities());
            }
            saveAtomically(document, exportPath);
        }
    }

    private void appendRooms(PdfWriter writer, List<SurfaceMaterialListService.RoomSummary> rooms) throws IOException {
        writer.section("Räume und Mietflächen nach WoFlV");
        if (rooms.isEmpty()) {
            writer.paragraph("Keine Räume vorhanden.");
            return;
        }
        LinkedHashMap<String, List<SurfaceMaterialListService.RoomSummary>> roomsByLevel = rooms.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        SurfaceMaterialListService.RoomSummary::levelName,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        for (var entry : roomsByLevel.entrySet()) {
            writer.subsection(entry.getKey());
            for (SurfaceMaterialListService.RoomSummary room : entry.getValue()) {
                writer.paragraph(formatRoom(room));
            }
        }
        writer.paragraph("Mietflächen nach WoFlV: ab 2 m volle Anrechnung, zwischen 1 m und 2 m halbe Anrechnung, darunter keine Anrechnung.");
    }

    private void appendMaterialSummary(PdfWriter writer, List<SurfaceMaterialListService.MaterialSummary> materials) throws IOException {
        writer.section("Zusammenfassung");
        if (materials.isEmpty()) {
            writer.paragraph("Keine sichtbaren Beläge vorhanden.");
            return;
        }
        for (SurfaceMaterialListService.MaterialSummary material : materials) {
            writer.paragraph(material.name()
                    + ": Fläche " + decimal(material.coveredAreaSquareMeters(), 2) + " m²"
                    + " | Stückzahl " + material.requiredPieces()
                    + " | Materialfläche " + decimal(material.requiredMaterialAreaSquareMeters(), 2) + " m²"
                    + " | Schnitte " + material.cutCount()
                    + " | Komplexität " + decimal(material.complexityScore(), 1));
        }
    }

    private void appendHeatingPlans(PdfWriter writer, List<SurfaceMaterialListService.HeatingPlanSummary> heatingPlans) throws IOException {
        writer.section("Flächenheizungen");
        if (heatingPlans.isEmpty()) {
            writer.paragraph("Keine Flächenheizungen vorhanden.");
            return;
        }
        for (SurfaceMaterialListService.HeatingPlanSummary plan : heatingPlans) {
            writer.paragraph(plan.levelName() + " / " + plan.roomName()
                    + " / " + plan.surfacePosition()
                    + " / " + plan.zoneName()
                    + ": Verlegung " + plan.layoutPattern()
                    + " | Heizfläche " + decimal(plan.areaSquareMeters(), 2) + " m²"
                    + " | HKL " + decimal(plan.pipeLengthMeters(), 1) + " m"
                    + " | Maximum " + decimal(plan.maximumPipeLengthMeters(), 1) + " m"
                    + " | " + decimal(plan.heatOutputWattsPerSquareMeter(), 1) + " W/m²"
                    + " | Leistung " + decimal(plan.heatOutputWatts(), 0) + " W"
                    + " | Raum gesamt " + decimal(plan.roomTotalHeatOutputWatts(), 0) + " W");
        }
    }

    private void appendHeatingElements(PdfWriter writer, List<SurfaceMaterialListService.HeatingElementSummary> heatingElements) throws IOException {
        writer.section("Heizelemente");
        if (heatingElements.isEmpty()) {
            writer.paragraph("Keine Heizelemente vorhanden.");
            return;
        }
        for (SurfaceMaterialListService.HeatingElementSummary element : heatingElements) {
            writer.paragraph(element.levelName() + " / " + element.roomName()
                    + " / " + element.objectName()
                    + ": Typ " + element.objectType()
                    + " | Heizart " + element.heatingType()
                    + " | Leistung " + decimal(element.heatOutputWatts(), 0) + " W");
        }
    }

    private void appendMaterialDetails(PdfWriter writer, List<SurfaceMaterialListService.MaterialSummary> materials) throws IOException {
        writer.section("Beläge");
        if (materials.isEmpty()) {
            writer.paragraph("Keine sichtbaren Beläge vorhanden.");
            return;
        }
        for (SurfaceMaterialListService.MaterialSummary material : materials) {
            writer.subsection(material.name());
            writer.paragraph("Beschreibung: " + material.description());
            writer.paragraph("Werte: " + material.values());
            writer.paragraph("Belegte Fläche: " + decimal(material.coveredAreaSquareMeters(), 2) + " m²");
            writer.paragraph("Benötigte Stückzahl: " + material.requiredPieces()
                    + " Stück | Materialfläche " + decimal(material.requiredMaterialAreaSquareMeters(), 2) + " m²");
            writer.paragraph("Vollstücke: " + material.fullPieces()
                    + " | Zuschnitte: " + material.cutPieces()
                    + " | notwendige Schnitte: " + material.cutCount()
                    + " | Komplexität " + decimal(material.complexityScore(), 1));
            appendRestPieces(writer, material.restPieces());
            for (SurfaceMaterialListService.MaterialRoomEntry entry : material.roomEntries()) {
                writer.paragraph("  " + entry.levelName() + " / " + entry.roomName() + " / " + entry.surfaceDescription()
                        + ": Fläche " + decimal(entry.coveredAreaSquareMeters(), 2) + " m²"
                        + " | Stückzahl " + entry.requiredPieces()
                        + " | Materialfläche " + decimal(entry.requiredMaterialAreaSquareMeters(), 2) + " m²"
                        + " | Schnitte " + entry.cutCount()
                        + " | Komplexität " + decimal(entry.complexityScore(), 1));
            }
        }
    }

    private void appendRestPieces(PdfWriter writer, List<SurfaceMaterialListService.RestPieceSummary> restPieces) throws IOException {
        if (restPieces.isEmpty()) {
            writer.paragraph("Reststücke: keine");
            return;
        }
        writer.paragraph("Reststücke: " + restPieces.stream().mapToInt(SurfaceMaterialListService.RestPieceSummary::count).sum() + " Stück");
        for (SurfaceMaterialListService.RestPieceSummary restPiece : restPieces) {
            writer.paragraph("  " + restPiece.count() + " x "
                    + decimal(restPiece.widthMillimeters() / 10.0, 1) + " cm x "
                    + decimal(restPiece.heightMillimeters() / 10.0, 1) + " cm"
                    + " | Gesamtfläche " + decimal(restPiece.totalAreaSquareMeters(), 2) + " m²");
        }
    }

    private void appendRoomComplexities(PdfWriter writer, List<SurfaceMaterialListService.RoomComplexitySummary> roomComplexities) throws IOException {
        writer.section("Komplexität pro Raum und Fläche");
        if (roomComplexities.isEmpty()) {
            writer.paragraph("Keine komplexitätsrelevanten Flächen vorhanden.");
            return;
        }
        for (SurfaceMaterialListService.RoomComplexitySummary room : roomComplexities) {
            writer.paragraph(room.levelName() + " / " + room.roomName() + " / " + room.surfaceDescription()
                    + ": Fläche " + decimal(room.coveredAreaSquareMeters(), 2) + " m²"
                    + " | Stückzahl " + room.requiredPieces()
                    + " | Schnitte " + room.cutCount()
                    + " | Komplexität " + decimal(room.complexityScore(), 1));
        }
    }

    private String formatRoom(SurfaceMaterialListService.RoomSummary room) {
        StringBuilder line = new StringBuilder();
        line.append(room.roomName())
                .append(": ")
                .append(decimal(room.widthMillimeters() / 1000.0, 2))
                .append(" x ")
                .append(decimal(room.depthMillimeters() / 1000.0, 2))
                .append(" m")
                .append(" | Höhe ")
                .append(decimal(room.minimumHeightMillimeters() / 1000.0, 2));
        if (Math.abs(room.maximumHeightMillimeters() - room.minimumHeightMillimeters()) > 0.001) {
            line.append("-").append(decimal(room.maximumHeightMillimeters() / 1000.0, 2));
        }
        line.append(" m")
                .append(" | Grundfläche ").append(decimal(room.areaSquareMeters(), 2)).append(" m²")
                .append(" | Mietfläche ").append(decimal(room.residentialAreaSquareMeters(), 2)).append(" m²")
                .append(" | Volumen ").append(decimal(room.volumeCubicMeters(), 2)).append(" m³")
                .append(" | FBH ").append(decimal(room.floorHeatingWatts(), 0)).append(" W")
                .append(" | DH ").append(decimal(room.ceilingHeatingWatts(), 0)).append(" W")
                .append(" | Fläche ").append(decimal(room.additionalSurfaceHeatingWatts(), 0)).append(" W")
                .append(" | Heizelemente ").append(decimal(room.heatingElementWatts(), 0)).append(" W")
                .append(" | Gesamt ").append(decimal(room.totalHeatOutputWatts(), 0)).append(" W");
        return line.toString();
    }

    private void saveAtomically(PDDocument document, Path targetFile) throws IOException {
        Path parent = targetFile.getParent();
        Path tempFile = Files.createTempFile(parent, tempFilePrefix(targetFile), ".pdf.tmp");
        try {
            document.save(tempFile.toFile());
            try {
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String tempFilePrefix(Path targetFile) {
        String fileName = targetFile.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        String prefix = fileName.length() < 3 ? "pdf_" + fileName : fileName;
        return prefix.length() < 3 ? "pdf" : prefix;
    }

    private static String decimal(double value, int decimals) {
        return String.format(Locale.GERMAN, "%." + decimals + "f", value);
    }

    private static final class PdfWriter implements AutoCloseable {

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void title(String text) throws IOException {
            writeWrappedLine(normalize(text), FONT_BOLD, TITLE_FONT_SIZE, 10.0f);
        }

        private void section(String text) throws IOException {
            gap(8.0f);
            writeWrappedLine(normalize(text), FONT_BOLD, SECTION_FONT_SIZE, 6.0f);
        }

        private void subsection(String text) throws IOException {
            gap(4.0f);
            writeWrappedLine(normalize(text), FONT_BOLD, SUBSECTION_FONT_SIZE, 3.0f);
        }

        private void paragraph(String text) throws IOException {
            for (String rawLine : normalize(text).split("\n")) {
                if (rawLine.isBlank()) {
                    gap(3.0f);
                    continue;
                }
                for (String wrappedLine : wrap(rawLine, FONT, BODY_FONT_SIZE)) {
                    writeLine(wrappedLine, FONT, BODY_FONT_SIZE);
                }
                gap(2.0f);
            }
        }

        private void writeWrappedLine(String text, PDType1Font font, float fontSize, float afterGap) throws IOException {
            for (String line : wrap(text, font, fontSize)) {
                writeLine(line, font, fontSize);
            }
            gap(afterGap);
        }

        private void writeLine(String text, PDType1Font font, float fontSize) throws IOException {
            ensureSpace(fontSize * 1.45f);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(PAGE_MARGIN, y);
            contentStream.showText(text);
            contentStream.endText();
            y -= fontSize * 1.35f;
        }

        private void gap(float points) throws IOException {
            ensureSpace(points + 2.0f);
            y -= points;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight >= PAGE_MARGIN) {
                return;
            }
            newPage();
        }

        private void newPage() throws IOException {
            closePage();
            page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = PAGE_SIZE.getHeight() - PAGE_MARGIN;
        }

        private List<String> wrap(String text, PDType1Font font, float fontSize) throws IOException {
            List<String> lines = new java.util.ArrayList<>();
            float maxWidth = PAGE_SIZE.getWidth() - PAGE_MARGIN * 2.0f;
            String[] words = text.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(candidate) / 1000.0f * fontSize;
                if (width <= maxWidth || current.isEmpty()) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines.isEmpty() ? List.of("") : List.copyOf(lines);
        }

        @Override
        public void close() throws IOException {
            closePage();
        }

        private void closePage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }
    }

    private static String normalize(String text) {
        return text
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u00a0', ' ');
    }
}
