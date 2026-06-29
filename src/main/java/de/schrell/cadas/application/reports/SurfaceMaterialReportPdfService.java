package de.schrell.cadas.application.reports;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Exportiert die Räume- und Materialauswertung als tabellarisches PDF mit eingebetteten Heizplan-Grafiken.
 */
public final class SurfaceMaterialReportPdfService {

    private static final PDRectangle PAGE_SIZE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float PAGE_MARGIN = 30.0f;
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float TITLE_FONT_SIZE = 16.0f;
    private static final float SECTION_FONT_SIZE = 12.5f;
    private static final float SUBSECTION_FONT_SIZE = 10.5f;
    private static final float BODY_FONT_SIZE = 8.2f;
    private static final float TABLE_PADDING = 3.5f;
    private static final float TABLE_LINE_WIDTH = 0.55f;
    private static final Color TABLE_HEADER_FILL = new Color(232, 236, 240);
    private static final Color TABLE_BORDER = new Color(166, 171, 177);
    private static final Color TABLE_ZEBRA_FILL = new Color(249, 250, 251);
    private static final Color SVG_FRAME_FILL = new Color(252, 252, 250);
    private static final Color SVG_FRAME_STROKE = new Color(182, 186, 191);
    private static final float SVG_INNER_PADDING = 10.0f;
    private static final float SVG_MAX_HEIGHT = 240.0f;

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
        TableDefinition roomTable = new TableDefinition(
                7.3f,
                List.of(
                        new TableColumn("Raum", 1.3f, TableAlignment.LEFT),
                        new TableColumn("Maße", 1.0f, TableAlignment.LEFT),
                        new TableColumn("Lichte Höhe", 0.9f, TableAlignment.RIGHT),
                        new TableColumn("Grundfläche", 0.85f, TableAlignment.RIGHT),
                        new TableColumn("Mietfläche", 0.85f, TableAlignment.RIGHT),
                        new TableColumn("Volumen", 0.85f, TableAlignment.RIGHT),
                        new TableColumn("FBH", 0.6f, TableAlignment.RIGHT),
                        new TableColumn("DH", 0.6f, TableAlignment.RIGHT),
                        new TableColumn("Fläche", 0.7f, TableAlignment.RIGHT),
                        new TableColumn("Heizelemente", 0.95f, TableAlignment.RIGHT),
                        new TableColumn("Gesamtwärme", 0.9f, TableAlignment.RIGHT)
                )
        );
        for (Map.Entry<String, List<SurfaceMaterialListService.RoomSummary>> entry : roomsByLevel.entrySet()) {
            writer.subsection(entry.getKey());
            List<List<String>> rows = new ArrayList<>();
            for (SurfaceMaterialListService.RoomSummary room : entry.getValue()) {
                rows.add(List.of(
                        room.roomName(),
                        decimal(room.widthMillimeters() / 1000.0, 2) + " x " + decimal(room.depthMillimeters() / 1000.0, 2) + " m",
                        roomHeightText(room),
                        decimal(room.areaSquareMeters(), 2) + " m²",
                        decimal(room.residentialAreaSquareMeters(), 2) + " m²",
                        decimal(room.volumeCubicMeters(), 2) + " m³",
                        decimal(room.floorHeatingWatts(), 0) + " W",
                        decimal(room.ceilingHeatingWatts(), 0) + " W",
                        decimal(room.additionalSurfaceHeatingWatts(), 0) + " W",
                        decimal(room.heatingElementWatts(), 0) + " W",
                        decimal(room.totalHeatOutputWatts(), 0) + " W"
                ));
            }
            writer.table(roomTable, rows);
        }
        writer.paragraph("Mietflächen nach WoFlV: ab 2 m volle Anrechnung, zwischen 1 m und 2 m halbe Anrechnung, darunter keine Anrechnung.");
    }

    private void appendMaterialSummary(PdfWriter writer, List<SurfaceMaterialListService.MaterialSummary> materials) throws IOException {
        writer.section("Zusammenfassung");
        if (materials.isEmpty()) {
            writer.paragraph("Keine sichtbaren Beläge vorhanden.");
            return;
        }
        writer.table(
                new TableDefinition(
                        BODY_FONT_SIZE,
                        List.of(
                                new TableColumn("Belag", 1.4f, TableAlignment.LEFT),
                                new TableColumn("Fläche", 0.8f, TableAlignment.RIGHT),
                                new TableColumn("Stückzahl", 0.75f, TableAlignment.RIGHT),
                                new TableColumn("Materialfläche", 0.95f, TableAlignment.RIGHT),
                                new TableColumn("Schnitte", 0.7f, TableAlignment.RIGHT),
                                new TableColumn("Komplexität", 0.8f, TableAlignment.RIGHT)
                        )
                ),
                materials.stream()
                        .map(material -> List.of(
                                material.name(),
                                decimal(material.coveredAreaSquareMeters(), 2) + " m²",
                                Integer.toString(material.requiredPieces()),
                                decimal(material.requiredMaterialAreaSquareMeters(), 2) + " m²",
                                Integer.toString(material.cutCount()),
                                decimal(material.complexityScore(), 1)
                        ))
                        .toList()
        );
        appendMaterialSplitReasons(writer, materials);
    }

    private void appendMaterialSplitReasons(PdfWriter writer, List<SurfaceMaterialListService.MaterialSummary> materials) throws IOException {
        Map<String, List<SurfaceMaterialListService.MaterialSummary>> materialsByName = materials.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        SurfaceMaterialListService.MaterialSummary::name,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        List<Map.Entry<String, List<SurfaceMaterialListService.MaterialSummary>>> splitMaterials = materialsByName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .toList();
        if (splitMaterials.isEmpty()) {
            return;
        }
        writer.subsection("Trennmerkmale bei mehrfachen Materialnamen");
        writer.table(
                new TableDefinition(
                        BODY_FONT_SIZE,
                        List.of(
                                new TableColumn("Name", 1.0f, TableAlignment.LEFT),
                                new TableColumn("Trennmerkmale", 3.0f, TableAlignment.LEFT)
                        )
                ),
                splitMaterials.stream()
                        .map(entry -> List.of(
                                entry.getKey(),
                                String.join(", ", differingPropertyDescriptions(entry.getValue()))
                        ))
                        .toList()
        );
    }

    private void appendHeatingPlans(PdfWriter writer, List<SurfaceMaterialListService.HeatingPlanSummary> heatingPlans) throws IOException {
        writer.section("Flächenheizungen");
        if (heatingPlans.isEmpty()) {
            writer.paragraph("Keine Flächenheizungen vorhanden.");
            return;
        }
        writer.table(
                new TableDefinition(
                        7.1f,
                        List.of(
                                new TableColumn("Raum", 1.55f, TableAlignment.LEFT),
                                new TableColumn("Fläche", 0.65f, TableAlignment.LEFT),
                                new TableColumn("Verlegung", 0.75f, TableAlignment.LEFT),
                                new TableColumn("Heizkreis", 0.85f, TableAlignment.LEFT),
                                new TableColumn("Heizfläche", 0.7f, TableAlignment.RIGHT),
                                new TableColumn("HKL", 0.65f, TableAlignment.RIGHT),
                                new TableColumn("Maximum", 0.72f, TableAlignment.RIGHT),
                                new TableColumn("W/m²", 0.55f, TableAlignment.RIGHT),
                                new TableColumn("Leistung", 0.72f, TableAlignment.RIGHT),
                                new TableColumn("Raumwärme", 1.9f, TableAlignment.LEFT)
                        )
                ),
                heatingPlans.stream()
                        .map(plan -> List.of(
                                plan.levelName() + " / " + plan.roomName(),
                                plan.surfacePosition(),
                                plan.layoutPattern(),
                                plan.zoneName(),
                                heatingPlanMetric(plan, plan.areaSquareMeters(), 2, " m²"),
                                heatingPlanMetric(plan, plan.pipeLengthMeters(), 1, " m"),
                                heatingPlanMetric(plan, plan.maximumPipeLengthMeters(), 1, " m"),
                                heatingPlanMetric(plan, plan.heatOutputWattsPerSquareMeter(), 1, ""),
                                decimal(plan.heatOutputWatts(), 0) + " W",
                                String.format(
                                        Locale.GERMAN,
                                        "FBH %s W / DH %s W / Fläche %s W / Heizelemente %s W / Gesamt %s W",
                                        decimal(plan.roomFloorHeatOutputWatts(), 0),
                                        decimal(plan.roomCeilingHeatOutputWatts(), 0),
                                        decimal(plan.roomAdditionalSurfaceHeatOutputWatts(), 0),
                                        decimal(plan.roomHeatingElementWatts(), 0),
                                        decimal(plan.roomTotalHeatOutputWatts(), 0)
                                )
                        ))
                        .toList()
        );
        writer.subsection("Heizplan-Grafiken");
        Map<String, List<SurfaceMaterialListService.HeatingPlanSummary>> groupedPlans = heatingPlans.stream()
                .filter(plan -> !plan.objectBased())
                .collect(java.util.stream.Collectors.groupingBy(
                        plan -> plan.levelName() + "\u0000" + plan.roomName() + "\u0000" + plan.surfacePosition(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        for (List<SurfaceMaterialListService.HeatingPlanSummary> plans : groupedPlans.values()) {
            SurfaceMaterialListService.HeatingPlanSummary first = plans.getFirst();
            writer.svgBlock(
                    "Heizplan " + first.levelName() + " / " + first.roomName() + " / " + first.surfacePosition(),
                    first.svg()
            );
        }
    }

    private void appendHeatingElements(PdfWriter writer, List<SurfaceMaterialListService.HeatingElementSummary> heatingElements) throws IOException {
        writer.section("Heizelemente");
        if (heatingElements.isEmpty()) {
            writer.paragraph("Keine Heizelemente vorhanden.");
            return;
        }
        writer.table(
                new TableDefinition(
                        BODY_FONT_SIZE,
                        List.of(
                                new TableColumn("Raum", 1.4f, TableAlignment.LEFT),
                                new TableColumn("Objekt", 1.1f, TableAlignment.LEFT),
                                new TableColumn("Heizart", 0.95f, TableAlignment.LEFT),
                                new TableColumn("Leistung", 0.7f, TableAlignment.RIGHT),
                                new TableColumn("Raum FBH", 0.75f, TableAlignment.RIGHT),
                                new TableColumn("Raum DH", 0.75f, TableAlignment.RIGHT),
                                new TableColumn("Raum Fläche", 0.85f, TableAlignment.RIGHT),
                                new TableColumn("Raum Heizelemente", 1.0f, TableAlignment.RIGHT),
                                new TableColumn("Raum gesamt", 0.9f, TableAlignment.RIGHT)
                        )
                ),
                heatingElements.stream()
                        .map(element -> List.of(
                                element.levelName() + " / " + element.roomName(),
                                element.objectName(),
                                element.heatingType(),
                                decimal(element.heatOutputWatts(), 0) + " W",
                                decimal(element.roomFloorHeatOutputWatts(), 0) + " W",
                                decimal(element.roomCeilingHeatOutputWatts(), 0) + " W",
                                decimal(element.roomAdditionalSurfaceHeatOutputWatts(), 0) + " W",
                                decimal(element.roomHeatingElementWatts(), 0) + " W",
                                decimal(element.roomTotalHeatOutputWatts(), 0) + " W"
                        ))
                        .toList()
        );
    }

    private void appendMaterialDetails(PdfWriter writer, List<SurfaceMaterialListService.MaterialSummary> materials) throws IOException {
        writer.section("Beläge");
        if (materials.isEmpty()) {
            writer.paragraph("Keine sichtbaren Beläge vorhanden.");
            return;
        }
        TableDefinition factTable = new TableDefinition(
                BODY_FONT_SIZE,
                List.of(
                        new TableColumn("Merkmal", 0.9f, TableAlignment.LEFT),
                        new TableColumn("Wert", 2.3f, TableAlignment.LEFT)
                )
        );
        TableDefinition roomEntryTable = new TableDefinition(
                BODY_FONT_SIZE,
                List.of(
                        new TableColumn("Raum/Fläche", 1.8f, TableAlignment.LEFT),
                        new TableColumn("Fläche", 0.7f, TableAlignment.RIGHT),
                        new TableColumn("Stückzahl", 0.7f, TableAlignment.RIGHT),
                        new TableColumn("Materialfläche", 0.85f, TableAlignment.RIGHT),
                        new TableColumn("Schnitte", 0.65f, TableAlignment.RIGHT),
                        new TableColumn("Komplexität", 0.75f, TableAlignment.RIGHT)
                )
        );
        TableDefinition restPieceTable = new TableDefinition(
                BODY_FONT_SIZE,
                List.of(
                        new TableColumn("Anzahl", 0.6f, TableAlignment.RIGHT),
                        new TableColumn("Breite", 0.8f, TableAlignment.RIGHT),
                        new TableColumn("Höhe", 0.8f, TableAlignment.RIGHT),
                        new TableColumn("Gesamtfläche", 0.9f, TableAlignment.RIGHT)
                )
        );
        for (SurfaceMaterialListService.MaterialSummary material : materials) {
            writer.subsection(material.name());
            writer.table(
                    factTable,
                    List.of(
                            List.of("Beschreibung", material.description()),
                            List.of("Werte", material.values()),
                            List.of("Belegte Fläche", decimal(material.coveredAreaSquareMeters(), 2) + " m²"),
                            List.of("Benötigte Stückzahl", Integer.toString(material.requiredPieces()) + " Stück"),
                            List.of("Materialfläche", decimal(material.requiredMaterialAreaSquareMeters(), 2) + " m²"),
                            List.of("Vollstücke", Integer.toString(material.fullPieces())),
                            List.of("Zuschnitte", Integer.toString(material.cutPieces())),
                            List.of("Notwendige Schnitte", Integer.toString(material.cutCount())),
                            List.of("Komplexität", decimal(material.complexityScore(), 1))
                    )
            );
            writer.caption("Reststücke");
            if (material.restPieces().isEmpty()) {
                writer.paragraph("Keine Reststücke.");
            } else {
                writer.table(
                        restPieceTable,
                        material.restPieces().stream()
                                .map(restPiece -> List.of(
                                        Integer.toString(restPiece.count()),
                                        decimal(restPiece.widthMillimeters() / 10.0, 1) + " cm",
                                        decimal(restPiece.heightMillimeters() / 10.0, 1) + " cm",
                                        decimal(restPiece.totalAreaSquareMeters(), 2) + " m²"
                                ))
                                .toList()
                );
            }
            writer.caption("Belegte Flächen");
            writer.table(
                    roomEntryTable,
                    material.roomEntries().stream()
                            .map(entry -> List.of(
                                    entry.levelName() + " / " + entry.roomName() + " / " + entry.surfaceDescription(),
                                    decimal(entry.coveredAreaSquareMeters(), 2) + " m²",
                                    Integer.toString(entry.requiredPieces()),
                                    decimal(entry.requiredMaterialAreaSquareMeters(), 2) + " m²",
                                    Integer.toString(entry.cutCount()),
                                    decimal(entry.complexityScore(), 1)
                            ))
                            .toList()
            );
        }
    }

    private void appendRoomComplexities(PdfWriter writer, List<SurfaceMaterialListService.RoomComplexitySummary> roomComplexities) throws IOException {
        writer.section("Komplexität pro Raum und Fläche");
        if (roomComplexities.isEmpty()) {
            writer.paragraph("Keine komplexitätsrelevanten Flächen vorhanden.");
            return;
        }
        writer.table(
                new TableDefinition(
                        BODY_FONT_SIZE,
                        List.of(
                                new TableColumn("Raum/Fläche", 1.85f, TableAlignment.LEFT),
                                new TableColumn("Belegte Fläche", 0.85f, TableAlignment.RIGHT),
                                new TableColumn("Stückzahl", 0.75f, TableAlignment.RIGHT),
                                new TableColumn("Schnitte", 0.7f, TableAlignment.RIGHT),
                                new TableColumn("Komplexität", 0.8f, TableAlignment.RIGHT)
                        )
                ),
                roomComplexities.stream()
                        .map(room -> List.of(
                                room.levelName() + " / " + room.roomName() + " / " + room.surfaceDescription(),
                                decimal(room.coveredAreaSquareMeters(), 2) + " m²",
                                Integer.toString(room.requiredPieces()),
                                Integer.toString(room.cutCount()),
                                decimal(room.complexityScore(), 1)
                        ))
                        .toList()
        );
    }

    private List<String> differingPropertyDescriptions(List<SurfaceMaterialListService.MaterialSummary> materialsWithSameName) {
        List<String> descriptions = new ArrayList<>();
        LinkedHashMap<String, List<String>> valuesByLabel = new LinkedHashMap<>();
        for (SurfaceMaterialListService.MaterialSummary material : materialsWithSameName) {
            material.labeledValues().forEach((label, value) -> valuesByLabel.computeIfAbsent(label, ignored -> new ArrayList<>()).add(value));
        }
        for (Map.Entry<String, List<String>> entry : valuesByLabel.entrySet()) {
            List<String> distinctValues = entry.getValue().stream().distinct().toList();
            if (distinctValues.size() <= 1) {
                continue;
            }
            descriptions.add(entry.getKey() + " " + String.join(" | ", distinctValues));
        }
        return descriptions;
    }

    private String roomHeightText(SurfaceMaterialListService.RoomSummary room) {
        StringBuilder height = new StringBuilder(decimal(room.minimumHeightMillimeters() / 1000.0, 2));
        if (Math.abs(room.maximumHeightMillimeters() - room.minimumHeightMillimeters()) > 0.001) {
            height.append("-").append(decimal(room.maximumHeightMillimeters() / 1000.0, 2));
        }
        return height + " m";
    }

    private String heatingPlanMetric(SurfaceMaterialListService.HeatingPlanSummary plan, double value, int decimals, String unit) {
        if (plan.objectBased()) {
            return "-";
        }
        return decimal(value, decimals) + unit;
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

    private static String normalize(String text) {
        return OptionalText.of(text)
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u00a0', ' ')
                .trim();
    }

    private record TableDefinition(float fontSize, List<TableColumn> columns) {
    }

    private record TableColumn(String header, float weight, TableAlignment alignment) {
    }

    private enum TableAlignment {
        LEFT,
        CENTER,
        RIGHT
    }

    private record PreparedCell(TableColumn column, float width, List<String> lines) {
    }

    private record PreparedRow(List<PreparedCell> cells, float height) {
    }

    private record PdfPoint(double x, double y) {
    }

    private static final class PdfWriter implements AutoCloseable {

        private final PDDocument document;
        private final SvgRenderer svgRenderer = new SvgRenderer();
        private PageCanvas canvas;
        private float y;
        private String documentTitle;
        private int pageNumber;

        private PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void title(String text) throws IOException {
            documentTitle = normalize(text);
            writeWrappedText(documentTitle, FONT_BOLD, TITLE_FONT_SIZE, PAGE_MARGIN, availableWidth(), 8.0f);
        }

        private void section(String text) throws IOException {
            gap(8.0f);
            writeWrappedText(normalize(text), FONT_BOLD, SECTION_FONT_SIZE, PAGE_MARGIN, availableWidth(), 4.0f);
        }

        private void subsection(String text) throws IOException {
            gap(4.0f);
            writeWrappedText(normalize(text), FONT_BOLD, SUBSECTION_FONT_SIZE, PAGE_MARGIN, availableWidth(), 2.5f);
        }

        private void caption(String text) throws IOException {
            writeWrappedText(normalize(text), FONT_BOLD, BODY_FONT_SIZE, PAGE_MARGIN, availableWidth(), 1.5f);
        }

        private void paragraph(String text) throws IOException {
            for (String rawLine : normalize(text).split("\n")) {
                if (rawLine.isBlank()) {
                    gap(3.0f);
                    continue;
                }
                writeWrappedText(rawLine, FONT, BODY_FONT_SIZE, PAGE_MARGIN, availableWidth(), 2.5f);
            }
        }

        private void table(TableDefinition definition, List<List<String>> rows) throws IOException {
            PreparedRow header = prepareRow(definition, definition.columns().stream().map(TableColumn::header).toList(), FONT_BOLD);
            boolean headerPending = true;
            int rowIndex = 0;
            while (rowIndex < rows.size()) {
                PreparedRow row = prepareRow(definition, rows.get(rowIndex), FONT);
                float requiredHeight = row.height() + (headerPending ? header.height() : 0.0f);
                if (!hasSpace(requiredHeight)) {
                    newPage();
                    headerPending = true;
                    continue;
                }
                if (headerPending) {
                    drawRow(header, definition.fontSize(), true, false);
                    headerPending = false;
                }
                drawRow(row, definition.fontSize(), false, rowIndex % 2 == 1);
                rowIndex++;
            }
            gap(8.0f);
        }

        private void svgBlock(String title, String svgMarkup) throws IOException {
            SvgDocument svg = svgRenderer.parse(svgMarkup);
            float frameWidth = availableWidth();
            float innerWidth = frameWidth - SVG_INNER_PADDING * 2.0f;
            float svgHeight = svg.scaledHeight(innerWidth, SVG_MAX_HEIGHT);
            float blockHeight = estimateWrappedHeight(normalize(title), FONT_BOLD, SUBSECTION_FONT_SIZE, availableWidth())
                    + 4.0f
                    + svgHeight
                    + SVG_INNER_PADDING * 2.0f
                    + 6.0f;
            if (!hasSpace(blockHeight)) {
                newPage();
            }
            writeWrappedText(normalize(title), FONT_BOLD, SUBSECTION_FONT_SIZE, PAGE_MARGIN, availableWidth(), 3.0f);
            float top = y;
            float frameHeight = svgHeight + SVG_INNER_PADDING * 2.0f;
            canvas.rectangle(PAGE_MARGIN, top - frameHeight, frameWidth, frameHeight, 0.65f, SVG_FRAME_STROKE, SVG_FRAME_FILL);
            svgRenderer.render(
                    canvas,
                    svg,
                    PAGE_MARGIN + SVG_INNER_PADDING,
                    top - SVG_INNER_PADDING,
                    innerWidth,
                    svgHeight
            );
            y = top - frameHeight - 6.0f;
        }

        private PreparedRow prepareRow(TableDefinition definition, List<String> values, PDType1Font font) throws IOException {
            float totalWeight = 0.0f;
            for (TableColumn column : definition.columns()) {
                totalWeight += column.weight();
            }
            float xWidth = availableWidth();
            float rowHeight = 0.0f;
            List<PreparedCell> cells = new ArrayList<>();
            for (int index = 0; index < definition.columns().size(); index++) {
                TableColumn column = definition.columns().get(index);
                float width = xWidth * (column.weight() / totalWeight);
                String value = index < values.size() ? normalize(values.get(index)) : "";
                List<String> lines = wrap(value, font, definition.fontSize(), Math.max(20.0f, width - TABLE_PADDING * 2.0f));
                float cellHeight = Math.max(
                        definition.fontSize() * 1.45f + TABLE_PADDING * 2.0f,
                        lines.size() * definition.fontSize() * 1.25f + TABLE_PADDING * 2.0f
                );
                rowHeight = Math.max(rowHeight, cellHeight);
                cells.add(new PreparedCell(column, width, lines));
            }
            return new PreparedRow(List.copyOf(cells), rowHeight);
        }

        private void drawRow(PreparedRow row, float fontSize, boolean header, boolean zebra) throws IOException {
            float currentX = PAGE_MARGIN;
            float baselineStart = y - TABLE_PADDING - fontSize;
            float rowBottom = y - row.height();
            for (PreparedCell cell : row.cells()) {
                Color fill = header ? TABLE_HEADER_FILL : zebra ? TABLE_ZEBRA_FILL : Color.WHITE;
                canvas.rectangle(currentX, rowBottom, cell.width(), row.height(), TABLE_LINE_WIDTH, TABLE_BORDER, fill);
                for (int lineIndex = 0; lineIndex < cell.lines().size(); lineIndex++) {
                    String line = cell.lines().get(lineIndex);
                    float textWidth = textWidth(line, header ? FONT_BOLD : FONT, fontSize);
                    float textX = switch (cell.column().alignment()) {
                        case LEFT -> currentX + TABLE_PADDING;
                        case CENTER -> currentX + (cell.width() - textWidth) / 2.0f;
                        case RIGHT -> currentX + cell.width() - TABLE_PADDING - textWidth;
                    };
                    float textY = baselineStart - lineIndex * fontSize * 1.25f;
                    canvas.text(textX, textY, fontSize, line, Color.BLACK, header);
                }
                currentX += cell.width();
            }
            y = rowBottom;
        }

        private void writeWrappedText(String text, PDType1Font font, float fontSize, float x, float width, float afterGap) throws IOException {
            List<String> lines = wrap(text, font, fontSize, width);
            float requiredHeight = lines.size() * fontSize * 1.3f + afterGap + 2.0f;
            if (!hasSpace(requiredHeight)) {
                newPage();
            }
            for (String line : lines) {
                canvas.text(x, y, fontSize, line, Color.BLACK, font == FONT_BOLD);
                y -= fontSize * 1.3f;
            }
            y -= afterGap;
        }

        private float estimateWrappedHeight(String text, PDType1Font font, float fontSize, float width) throws IOException {
            return wrap(text, font, fontSize, width).size() * fontSize * 1.3f;
        }

        private List<String> wrap(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
            if (text.isBlank()) {
                return List.of("");
            }
            List<String> lines = new ArrayList<>();
            for (String paragraph : text.split("\n")) {
                if (paragraph.isBlank()) {
                    lines.add("");
                    continue;
                }
                StringBuilder current = new StringBuilder();
                for (String word : paragraph.split("\\s+")) {
                    if (word.isBlank()) {
                        continue;
                    }
                    String candidate = current.isEmpty() ? word : current + " " + word;
                    if (textWidth(candidate, font, fontSize) <= maxWidth || current.isEmpty()) {
                        current.setLength(0);
                        current.append(candidate);
                    } else {
                        lines.add(current.toString());
                        current.setLength(0);
                        current.append(word);
                    }
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
            }
            return lines.isEmpty() ? List.of("") : List.copyOf(lines);
        }

        private float textWidth(String text, PDType1Font font, float fontSize) throws IOException {
            return font.getStringWidth(text) / 1000.0f * fontSize;
        }

        private float availableWidth() {
            return PAGE_SIZE.getWidth() - PAGE_MARGIN * 2.0f;
        }

        private void gap(float points) throws IOException {
            if (!hasSpace(points + 2.0f)) {
                newPage();
                return;
            }
            y -= points;
        }

        private boolean hasSpace(float requiredHeight) {
            return y - requiredHeight >= PAGE_MARGIN;
        }

        private void newPage() throws IOException {
            closePage();
            pageNumber++;
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            canvas = new PageCanvas(document, page);
            y = PAGE_SIZE.getHeight() - PAGE_MARGIN;
            if (documentTitle != null && pageNumber > 1) {
                canvas.text(PAGE_MARGIN, y, 8.0f, documentTitle, new Color(80, 84, 89), false);
                canvas.text(PAGE_SIZE.getWidth() - PAGE_MARGIN - 38.0f, y, 8.0f, "Seite " + pageNumber, new Color(80, 84, 89), false);
                canvas.line(PAGE_MARGIN, y - 4.0f, PAGE_SIZE.getWidth() - PAGE_MARGIN, y - 4.0f, 0.45f, TABLE_BORDER, null);
                y -= 16.0f;
            }
        }

        @Override
        public void close() throws IOException {
            closePage();
        }

        private void closePage() throws IOException {
            if (canvas != null) {
                canvas.close();
                canvas = null;
            }
        }
    }

    private static final class PageCanvas implements AutoCloseable {

        private static final double CIRCLE_KAPPA = 0.552284749831;
        private final PDPageContentStream stream;

        private PageCanvas(PDDocument document, PDPage page) throws IOException {
            this.stream = new PDPageContentStream(document, page);
        }

        private void line(double x1, double y1, double x2, double y2, float width, Color color, float[] dashPattern) throws IOException {
            stream.saveGraphicsState();
            stream.setStrokingColor(color);
            stream.setLineWidth(width);
            if (dashPattern != null) {
                stream.setLineDashPattern(dashPattern, 0.0f);
            }
            stream.moveTo((float) x1, (float) y1);
            stream.lineTo((float) x2, (float) y2);
            stream.stroke();
            stream.restoreGraphicsState();
        }

        private void rectangle(double x, double y, double width, double height, float lineWidth, Color stroke, Color fill) throws IOException {
            stream.saveGraphicsState();
            stream.addRect((float) x, (float) y, (float) width, (float) height);
            if (fill == null && stroke == null) {
                stream.appendRawCommands("n\n");
                stream.restoreGraphicsState();
                return;
            }
            stream.setLineWidth(lineWidth);
            if (fill != null) {
                stream.setNonStrokingColor(fill);
                if (stroke != null) {
                    stream.setStrokingColor(stroke);
                    stream.fillAndStroke();
                } else {
                    stream.fill();
                }
            } else if (stroke != null) {
                stream.setStrokingColor(stroke);
                stream.stroke();
            }
            stream.restoreGraphicsState();
        }

        private void polygon(List<PdfPoint> points, float lineWidth, Color stroke, Color fill, float[] dashPattern) throws IOException {
            if (points.isEmpty()) {
                return;
            }
            stream.saveGraphicsState();
            beginPolygonPath(points);
            if (fill == null && stroke == null) {
                stream.appendRawCommands("n\n");
                stream.restoreGraphicsState();
                return;
            }
            stream.setLineWidth(lineWidth);
            if (dashPattern != null) {
                stream.setLineDashPattern(dashPattern, 0.0f);
            }
            if (fill != null) {
                stream.setNonStrokingColor(fill);
            }
            if (stroke != null) {
                stream.setStrokingColor(stroke);
            }
            if (fill != null && stroke != null) {
                stream.fillAndStroke();
            } else if (fill != null) {
                stream.fill();
            } else if (stroke != null) {
                stream.stroke();
            }
            stream.restoreGraphicsState();
        }

        private void polyline(List<PdfPoint> points, float lineWidth, Color color, float[] dashPattern) throws IOException {
            if (points.size() < 2) {
                return;
            }
            stream.saveGraphicsState();
            stream.setStrokingColor(color);
            stream.setLineWidth(lineWidth);
            if (dashPattern != null) {
                stream.setLineDashPattern(dashPattern, 0.0f);
            }
            stream.moveTo((float) points.getFirst().x(), (float) points.getFirst().y());
            for (int index = 1; index < points.size(); index++) {
                PdfPoint point = points.get(index);
                stream.lineTo((float) point.x(), (float) point.y());
            }
            stream.stroke();
            stream.restoreGraphicsState();
        }

        private void circle(double centerX, double centerY, double radius, float lineWidth, Color stroke, Color fill) throws IOException {
            stream.saveGraphicsState();
            appendCirclePath(centerX, centerY, radius);
            if (fill == null && stroke == null) {
                stream.appendRawCommands("n\n");
                stream.restoreGraphicsState();
                return;
            }
            stream.setLineWidth(lineWidth);
            if (fill != null) {
                stream.setNonStrokingColor(fill);
            }
            if (stroke != null) {
                stream.setStrokingColor(stroke);
            }
            if (fill != null && stroke != null) {
                stream.fillAndStroke();
            } else if (fill != null) {
                stream.fill();
            } else if (stroke != null) {
                stream.stroke();
            }
            stream.restoreGraphicsState();
        }

        private void clipPolygon(List<PdfPoint> points) throws IOException {
            if (points.isEmpty()) {
                return;
            }
            beginPolygonPath(points);
            stream.clip();
            stream.appendRawCommands("n\n");
        }

        private void text(double x, double y, float fontSize, String text, Color color, boolean bold) throws IOException {
            if (text == null || text.isBlank()) {
                return;
            }
            stream.beginText();
            stream.setFont(bold ? FONT_BOLD : FONT, fontSize);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset((float) x, (float) y);
            stream.showText(text);
            stream.endText();
        }

        private void save() throws IOException {
            stream.saveGraphicsState();
        }

        private void restore() throws IOException {
            stream.restoreGraphicsState();
        }

        private void beginPolygonPath(List<PdfPoint> points) throws IOException {
            stream.moveTo((float) points.getFirst().x(), (float) points.getFirst().y());
            for (int index = 1; index < points.size(); index++) {
                PdfPoint point = points.get(index);
                stream.lineTo((float) point.x(), (float) point.y());
            }
            stream.closePath();
        }

        private void appendCirclePath(double centerX, double centerY, double radius) throws IOException {
            double control = radius * CIRCLE_KAPPA;
            stream.moveTo((float) (centerX + radius), (float) centerY);
            stream.curveTo(
                    (float) (centerX + radius), (float) (centerY + control),
                    (float) (centerX + control), (float) (centerY + radius),
                    (float) centerX, (float) (centerY + radius)
            );
            stream.curveTo(
                    (float) (centerX - control), (float) (centerY + radius),
                    (float) (centerX - radius), (float) (centerY + control),
                    (float) (centerX - radius), (float) centerY
            );
            stream.curveTo(
                    (float) (centerX - radius), (float) (centerY - control),
                    (float) (centerX - control), (float) (centerY - radius),
                    (float) centerX, (float) (centerY - radius)
            );
            stream.curveTo(
                    (float) (centerX + control), (float) (centerY - radius),
                    (float) (centerX + radius), (float) (centerY - control),
                    (float) (centerX + radius), (float) centerY
            );
            stream.closePath();
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    private static final class SvgRenderer {

        private SvgDocument parse(String svgMarkup) throws IOException {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                factory.setNamespaceAware(false);
                Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(svgMarkup)));
                Element root = document.getDocumentElement();
                double[] viewBox = parseViewBox(root.getAttribute("viewBox"));
                return new SvgDocument(root, viewBox[0], viewBox[1], viewBox[2], viewBox[3], parsePatterns(root));
            } catch (SAXException | ParserConfigurationException exception) {
                throw new IOException("SVG konnte nicht gelesen werden.", exception);
            }
        }

        private void render(PageCanvas canvas, SvgDocument svg, double left, double top, double targetWidth, double targetHeight) throws IOException {
            SvgRenderContext context = new SvgRenderContext(left, top, Math.min(targetWidth / svg.width(), targetHeight / svg.height()), svg);
            renderChildren(canvas, svg.root(), SvgStyle.defaults(), context);
        }

        private void renderChildren(PageCanvas canvas, Element parent, SvgStyle inheritedStyle, SvgRenderContext context) throws IOException {
            NodeList children = parent.getChildNodes();
            for (int index = 0; index < children.getLength(); index++) {
                Node child = children.item(index);
                if (!(child instanceof Element element)) {
                    continue;
                }
                if ("defs".equals(element.getTagName())) {
                    continue;
                }
                SvgStyle style = mergeStyle(inheritedStyle, element);
                switch (element.getTagName()) {
                    case "g", "svg" -> renderChildren(canvas, element, style, context);
                    case "polygon" -> renderPolygon(canvas, element, style, context);
                    case "circle" -> renderCircle(canvas, element, style, context);
                    case "path" -> renderPath(canvas, element, style, context);
                    case "text" -> renderText(canvas, element, style, context);
                    default -> {
                    }
                }
            }
        }

        private void renderPolygon(PageCanvas canvas, Element element, SvgStyle style, SvgRenderContext context) throws IOException {
            List<PdfPoint> points = transformPoints(parsePoints(element.getAttribute("points")), context);
            if (points.isEmpty()) {
                return;
            }
            String patternId = patternId(style.fill());
            if (patternId != null) {
                renderPatternFill(canvas, points, context.document().patterns().get(patternId), context);
            }
            canvas.polygon(
                    points,
                    scaledStrokeWidth(style, context),
                    parseColor(style.stroke()),
                    patternId == null ? parseColor(style.fill()) : null,
                    parseDashArray(style.dashArray(), context.scale())
            );
        }

        private void renderPatternFill(PageCanvas canvas, List<PdfPoint> points, PatternDefinition pattern, SvgRenderContext context) throws IOException {
            if (pattern == null) {
                return;
            }
            canvas.save();
            canvas.clipPolygon(points);
            double maxX = context.document().minX() + context.document().width() + pattern.width();
            double maxY = context.document().minY() + context.document().height() + pattern.height();
            for (double y = context.document().minY() + pattern.circleCenterY(); y <= maxY; y += pattern.height()) {
                for (double x = context.document().minX() + pattern.circleCenterX(); x <= maxX; x += pattern.width()) {
                    canvas.circle(
                            context.x(x),
                            context.y(y),
                            pattern.circleRadius() * context.scale(),
                            Math.max(0.4f, (float) (pattern.strokeWidth() * context.scale())),
                            pattern.strokeColor(),
                            null
                    );
                }
            }
            canvas.restore();
        }

        private void renderCircle(PageCanvas canvas, Element element, SvgStyle style, SvgRenderContext context) throws IOException {
            double cx = parseDouble(element.getAttribute("cx"));
            double cy = parseDouble(element.getAttribute("cy"));
            double radius = parseDouble(element.getAttribute("r"));
            canvas.circle(
                    context.x(cx),
                    context.y(cy),
                    radius * context.scale(),
                    scaledStrokeWidth(style, context),
                    parseColor(style.stroke()),
                    parseColor(style.fill())
            );
        }

        private void renderPath(PageCanvas canvas, Element element, SvgStyle style, SvgRenderContext context) throws IOException {
            List<PdfPoint> points = parsePath(element.getAttribute("d")).stream()
                    .map(point -> new PdfPoint(context.x(point.x()), context.y(point.y())))
                    .toList();
            if (points.size() < 2) {
                return;
            }
            canvas.polyline(points, scaledStrokeWidth(style, context), parseColor(style.stroke()), parseDashArray(style.dashArray(), context.scale()));
        }

        private void renderText(PageCanvas canvas, Element element, SvgStyle style, SvgRenderContext context) throws IOException {
            String content = normalize(element.getTextContent());
            if (content.isBlank()) {
                return;
            }
            float fontSize = Math.max(6.0f, (float) (style.fontSize() * context.scale()));
            canvas.text(
                    context.x(parseDouble(element.getAttribute("x"))),
                    context.y(parseDouble(element.getAttribute("y"))),
                    fontSize,
                    content,
                    parseColor(style.fill()) == null ? Color.BLACK : parseColor(style.fill()),
                    true
            );
        }

        private SvgStyle mergeStyle(SvgStyle inherited, Element element) {
            return new SvgStyle(
                    attributeOrDefault(element, "fill", inherited.fill()),
                    attributeOrDefault(element, "stroke", inherited.stroke()),
                    parseDoubleOrDefault(element.getAttribute("stroke-width"), inherited.strokeWidth()),
                    attributeOrDefault(element, "stroke-dasharray", inherited.dashArray()),
                    parseDoubleOrDefault(element.getAttribute("font-size"), inherited.fontSize())
            );
        }

        private Map<String, PatternDefinition> parsePatterns(Element root) {
            LinkedHashMap<String, PatternDefinition> patterns = new LinkedHashMap<>();
            NodeList nodes = root.getElementsByTagName("pattern");
            for (int index = 0; index < nodes.getLength(); index++) {
                Node node = nodes.item(index);
                if (!(node instanceof Element patternElement)) {
                    continue;
                }
                String id = patternElement.getAttribute("id");
                Element circleElement = firstChildElement(patternElement, "circle");
                if (id.isBlank() || circleElement == null) {
                    continue;
                }
                patterns.put(id, new PatternDefinition(
                        parseDoubleOrDefault(patternElement.getAttribute("width"), 0.0),
                        parseDoubleOrDefault(patternElement.getAttribute("height"), 0.0),
                        parseDoubleOrDefault(circleElement.getAttribute("cx"), 0.0),
                        parseDoubleOrDefault(circleElement.getAttribute("cy"), 0.0),
                        parseDoubleOrDefault(circleElement.getAttribute("r"), 0.0),
                        parseDoubleOrDefault(circleElement.getAttribute("stroke-width"), 1.0),
                        parseColor(circleElement.getAttribute("stroke"))
                ));
            }
            return Map.copyOf(patterns);
        }

        private Element firstChildElement(Element parent, String tagName) {
            NodeList children = parent.getChildNodes();
            for (int index = 0; index < children.getLength(); index++) {
                Node child = children.item(index);
                if (child instanceof Element element && tagName.equals(element.getTagName())) {
                    return element;
                }
            }
            return null;
        }

        private List<PdfPoint> parsePoints(String rawPoints) {
            if (rawPoints == null || rawPoints.isBlank()) {
                return List.of();
            }
            List<PdfPoint> points = new ArrayList<>();
            for (String pair : rawPoints.trim().split("\\s+")) {
                String[] coordinates = pair.split(",");
                if (coordinates.length != 2) {
                    continue;
                }
                points.add(new PdfPoint(parseDouble(coordinates[0]), parseDouble(coordinates[1])));
            }
            return List.copyOf(points);
        }

        private List<PdfPoint> parsePath(String rawPath) {
            if (rawPath == null || rawPath.isBlank()) {
                return List.of();
            }
            List<String> tokens = Arrays.stream(rawPath.replace(",", " ").trim().split("\\s+"))
                    .filter(token -> !token.isBlank())
                    .toList();
            List<PdfPoint> points = new ArrayList<>();
            int index = 0;
            while (index < tokens.size()) {
                String token = tokens.get(index++);
                if (!"M".equalsIgnoreCase(token) && !"L".equalsIgnoreCase(token)) {
                    continue;
                }
                if (index + 1 >= tokens.size()) {
                    break;
                }
                points.add(new PdfPoint(parseDouble(tokens.get(index++)), parseDouble(tokens.get(index++))));
            }
            return List.copyOf(points);
        }

        private List<PdfPoint> transformPoints(List<PdfPoint> points, SvgRenderContext context) {
            return points.stream()
                    .map(point -> new PdfPoint(context.x(point.x()), context.y(point.y())))
                    .toList();
        }

        private float[] parseDashArray(String rawDashArray, double scale) {
            if (rawDashArray == null || rawDashArray.isBlank() || "none".equalsIgnoreCase(rawDashArray)) {
                return null;
            }
            String[] tokens = rawDashArray.trim().split("\\s+");
            float[] dashes = new float[tokens.length];
            for (int index = 0; index < tokens.length; index++) {
                dashes[index] = (float) (parseDouble(tokens[index]) * scale);
            }
            return dashes;
        }

        private String patternId(String fill) {
            if (fill == null || !fill.startsWith("url(#") || !fill.endsWith(")")) {
                return null;
            }
            return fill.substring(5, fill.length() - 1);
        }

        private float scaledStrokeWidth(SvgStyle style, SvgRenderContext context) {
            return Math.max(0.4f, (float) (style.strokeWidth() * context.scale()));
        }

        private static Color parseColor(String value) {
            if (value == null || value.isBlank() || "none".equalsIgnoreCase(value)) {
                return null;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("url(#")) {
                return null;
            }
            if (normalized.startsWith("rgba(") && normalized.endsWith(")")) {
                String[] parts = normalized.substring(5, normalized.length() - 1).split(",");
                if (parts.length != 4) {
                    return null;
                }
                double alpha = parseDouble(parts[3].trim());
                if (alpha <= 0.15) {
                    return null;
                }
                return new Color(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                );
            }
            if (normalized.startsWith("#")) {
                if (normalized.length() == 4) {
                    return new Color(
                            Integer.parseInt(normalized.substring(1, 2) + normalized.substring(1, 2), 16),
                            Integer.parseInt(normalized.substring(2, 3) + normalized.substring(2, 3), 16),
                            Integer.parseInt(normalized.substring(3, 4) + normalized.substring(3, 4), 16)
                    );
                }
                if (normalized.length() == 7) {
                    return new Color(Integer.parseInt(normalized.substring(1), 16));
                }
            }
            return switch (normalized) {
                case "black" -> Color.BLACK;
                case "white" -> Color.WHITE;
                default -> null;
            };
        }

        private static String attributeOrDefault(Element element, String attributeName, String fallback) {
            String value = element.getAttribute(attributeName);
            return value == null || value.isBlank() ? fallback : value;
        }

        private static double[] parseViewBox(String viewBox) {
            String[] tokens = viewBox.trim().split("\\s+");
            if (tokens.length != 4) {
                throw new IllegalArgumentException("Ungültige SVG-viewBox: " + viewBox);
            }
            return new double[]{
                    parseDouble(tokens[0]),
                    parseDouble(tokens[1]),
                    parseDouble(tokens[2]),
                    parseDouble(tokens[3])
            };
        }

        private static double parseDouble(String value) {
            return Double.parseDouble(value.trim());
        }

        private static double parseDoubleOrDefault(String value, double fallback) {
            return value == null || value.isBlank() ? fallback : parseDouble(value);
        }
    }

    private record SvgDocument(
            Element root,
            double minX,
            double minY,
            double width,
            double height,
            Map<String, PatternDefinition> patterns
    ) {
        private float scaledHeight(float targetWidth, float maximumHeight) {
            double scale = Math.min(targetWidth / width, maximumHeight / height);
            return (float) (height * scale);
        }
    }

    private record PatternDefinition(
            double width,
            double height,
            double circleCenterX,
            double circleCenterY,
            double circleRadius,
            double strokeWidth,
            Color strokeColor
    ) {
    }

    private record SvgStyle(String fill, String stroke, double strokeWidth, String dashArray, double fontSize) {
        private static SvgStyle defaults() {
            return new SvgStyle("black", "none", 1.0, null, 16.0);
        }
    }

    private record SvgRenderContext(double left, double top, double scale, SvgDocument document) {
        private double x(double value) {
            return left + (value - document.minX()) * scale;
        }

        private double y(double value) {
            return top - (value - document.minY()) * scale;
        }
    }

    private static final class OptionalText {

        private String value;

        private OptionalText(String value) {
            this.value = value == null ? "" : value;
        }

        private static OptionalText of(String value) {
            return new OptionalText(value);
        }

        private OptionalText replace(char search, char replacement) {
            value = value.replace(search, replacement);
            return this;
        }

        private String trim() {
            return value.trim();
        }
    }
}
