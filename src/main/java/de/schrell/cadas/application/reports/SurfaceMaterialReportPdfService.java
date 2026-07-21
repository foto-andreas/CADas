package de.schrell.cadas.application.reports;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final float LEVEL_PLAN_MAX_HEIGHT = 220.0f;

    public record ExportAssets(
            Map<String, BufferedImage> levelPlanImages,
            Map<String, BufferedImage> materialLevelImages,
            Map<String, BufferedImage> heatingLevelImages
    ) {
        public ExportAssets {
            levelPlanImages = Map.copyOf(levelPlanImages);
            materialLevelImages = Map.copyOf(materialLevelImages);
            heatingLevelImages = Map.copyOf(heatingLevelImages);
        }

        public static ExportAssets empty() {
            return new ExportAssets(Map.of(), Map.of(), Map.of());
        }
    }

    public void export(SurfaceMaterialListService.SurfaceMaterialReport report, Path targetFile) throws IOException {
        export(report, targetFile, ExportAssets.empty());
    }

    public void export(
            SurfaceMaterialListService.SurfaceMaterialReport report,
            Path targetFile,
            ExportAssets exportAssets
    ) throws IOException {
        Objects.requireNonNull(report, "report darf nicht null sein.");
        Objects.requireNonNull(targetFile, "targetFile darf nicht null sein.");
        Objects.requireNonNull(exportAssets, "exportAssets darf nicht null sein.");
        Path exportPath = targetFile.toAbsolutePath().normalize();
        Path parent = exportPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (PDDocument document = new PDDocument()) {
            try (PdfWriter writer = new PdfWriter(document)) {
                writer.title("Räume und Materialien - " + report.projectName());
                appendRooms(writer, report.rooms(), Map.of());
                appendHeatingLoads(writer, report.heatingLoads());
                appendRasterOverviewAndMaterialPages(writer, report, exportAssets);
                appendMaterialSummary(writer, report.materials());
                appendHeatingPlans(writer, report.heatingPlans(), exportAssets);
                appendHeatingElements(writer, report.heatingElements());
                appendMaterialDetails(writer, report.materials());
                appendRoomComplexities(writer, report.roomComplexities());
            }
            saveAtomically(document, exportPath);
        }
    }

    private void appendRooms(
            PdfWriter writer,
            List<SurfaceMaterialListService.RoomSummary> rooms,
            Map<String, BufferedImage> levelPlanImages
    ) throws IOException {
        writer.section("Räume und Wohnflächen nach WoFlV");
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
                7.9f,
                List.of(
                        new TableColumn("Raum", 1.1f, TableAlignment.LEFT),
                        new TableColumn("Maße", 0.95f, TableAlignment.LEFT),
                        new TableColumn("Lichte Höhe", 0.8f, TableAlignment.RIGHT),
                        new TableColumn("Grundfläche", 0.7f, TableAlignment.RIGHT),
                        new TableColumn("Innenumfang", 0.8f, TableAlignment.RIGHT),
                        new TableColumn("Wohnfläche", 0.7f, TableAlignment.RIGHT),
                        new TableColumn("Volumen", 0.7f, TableAlignment.RIGHT),
                        new TableColumn("FBH", 0.45f, TableAlignment.RIGHT),
                        new TableColumn("DH", 0.45f, TableAlignment.RIGHT),
                        new TableColumn("Fläche", 0.55f, TableAlignment.RIGHT),
                        new TableColumn("Heizelemente", 0.8f, TableAlignment.RIGHT),
                        new TableColumn("Gesamtwärme", 0.9f, TableAlignment.RIGHT),
                        new TableColumn("Heizlast", 0.75f, TableAlignment.RIGHT),
                        new TableColumn("Überschuss", 0.75f, TableAlignment.RIGHT)
                )
        );
        for (Map.Entry<String, List<SurfaceMaterialListService.RoomSummary>> entry : roomsByLevel.entrySet()) {
            writer.subsection(entry.getKey());
            BufferedImage levelPlanImage = levelPlanImages.get(entry.getKey());
            if (levelPlanImage != null) {
                writer.imageBlock("2D-Ansicht " + entry.getKey(), levelPlanImage, LEVEL_PLAN_MAX_HEIGHT);
            }
            List<List<String>> rows = new ArrayList<>();
            for (SurfaceMaterialListService.RoomSummary room : entry.getValue()) {
                rows.add(List.of(
                        room.roomName(),
                        decimal(room.widthMillimeters() / 1000.0, 2) + " x " + decimal(room.depthMillimeters() / 1000.0, 2) + " m",
                        roomHeightText(room),
                        decimal(room.areaSquareMeters(), 2) + " m²",
                        decimal(room.innerPerimeterMillimeters() / 1000.0, 2) + " m",
                        decimal(room.residentialAreaSquareMeters(), 2) + " m²",
                        decimal(room.volumeCubicMeters(), 2) + " m³",
                        decimal(room.floorHeatingWatts(), 0) + " W",
                        decimal(room.ceilingHeatingWatts(), 0) + " W",
                        decimal(room.additionalSurfaceHeatingWatts(), 0) + " W",
                        decimal(room.heatingElementWatts(), 0) + " W",
                        decimal(room.totalHeatOutputWatts(), 0) + " W",
                        decimal(room.heatLoadWatts(), 0) + " W",
                        decimal(room.heatingSurplusWatts(), 0) + " W"
                ));
            }
            writer.table(roomTable, rows);
        }
        writer.paragraph("Wohnflächen nach WoFlV: ab 2 m volle Anrechnung, zwischen 1 m und 2 m halbe Anrechnung, darunter keine Anrechnung. Außenbalkone werden regulär zu 25 % angerechnet.");
    }

    private void appendHeatingLoads(
            PdfWriter writer,
            List<SurfaceMaterialListService.HeatingLoadSummary> heatingLoads
    ) throws IOException {
        writer.section("Heizlast");
        if (heatingLoads.isEmpty()) {
            writer.paragraph("Keine Räume vorhanden.");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (SurfaceMaterialListService.HeatingLoadSummary summary : heatingLoads) {
            rows.add(List.of(
                    summary.levelName() + " / " + summary.roomName(),
                    decimal(summary.heatLoadWatts(), 0) + " W",
                    summary.heatings(),
                    decimal(summary.heatOutputWatts(), 0) + " W",
                    decimal(summary.surplusWatts(), 0) + " W"
            ));
        }
        rows.add(List.of(
                "Summe",
                decimal(heatingLoads.stream().mapToDouble(SurfaceMaterialListService.HeatingLoadSummary::heatLoadWatts).sum(), 0) + " W",
                "",
                decimal(heatingLoads.stream().mapToDouble(SurfaceMaterialListService.HeatingLoadSummary::heatOutputWatts).sum(), 0) + " W",
                decimal(heatingLoads.stream().mapToDouble(SurfaceMaterialListService.HeatingLoadSummary::surplusWatts).sum(), 0) + " W"
        ));
        writer.table(
                new TableDefinition(
                        BODY_FONT_SIZE,
                        List.of(
                                new TableColumn("Raum", 1.25f, TableAlignment.LEFT),
                                new TableColumn("Heizlast", 0.65f, TableAlignment.RIGHT),
                                new TableColumn("Heizungen", 2.7f, TableAlignment.LEFT),
                                new TableColumn("Leistung", 0.65f, TableAlignment.RIGHT),
                                new TableColumn("Überschuss/Fehlbetrag", 0.9f, TableAlignment.RIGHT)
                        )
                ),
                rows
        );
    }

    private void appendRasterOverviewAndMaterialPages(
            PdfWriter writer,
            SurfaceMaterialListService.SurfaceMaterialReport report,
            ExportAssets exportAssets
    ) throws IOException {
        LinkedHashSet<String> levelNames = new LinkedHashSet<>();
        report.rooms().stream().map(SurfaceMaterialListService.RoomSummary::levelName).forEach(levelNames::add);
        levelNames.addAll(exportAssets.levelPlanImages().keySet());
        if (levelNames.isEmpty()) {
            return;
        }
        for (String levelName : levelNames) {
            BufferedImage levelPlanImage = exportAssets.levelPlanImages().get(levelName);
            if (levelPlanImage != null) {
                writer.imagePage("Etagenübersicht " + levelName, levelPlanImage);
            }
            for (SurfaceMaterialListService.MaterialSummary material : report.materials()) {
                if (material.surfaceType() != de.schrell.cadas.domain.model.SurfaceType.FLOOR) {
                    continue;
                }
                BufferedImage materialImage = exportAssets.materialLevelImages().get(materialLevelImageKey(material, levelName));
                if (materialImage != null) {
                    writer.imagePage("Belag " + levelName + " / " + material.name(), materialImage);
                }
            }
        }
    }

    private List<String> heatingSurfacePositions(
            List<SurfaceMaterialListService.HeatingPlanSummary> heatingPlans,
            ExportAssets exportAssets,
            String levelName
    ) {
        LinkedHashSet<String> surfacePositions = new LinkedHashSet<>();
        heatingPlans.stream()
                .filter(plan -> plan.levelName().equals(levelName))
                .map(SurfaceMaterialListService.HeatingPlanSummary::surfacePosition)
                .filter(surfacePosition -> exportAssets.heatingLevelImages().containsKey(heatingLevelImageKey(levelName, surfacePosition)))
                .forEach(surfacePositions::add);
        exportAssets.heatingLevelImages().keySet().stream()
                .filter(key -> heatingLevelName(key).equals(levelName))
                .map(SurfaceMaterialReportPdfService::heatingSurfacePosition)
                .filter(surfacePosition -> !surfacePosition.isBlank())
                .forEach(surfacePositions::add);
        return List.copyOf(surfacePositions);
    }

    private void appendMaterialSummary(PdfWriter writer, List<SurfaceMaterialListService.MaterialSummary> materials) throws IOException {
        writer.section("Zusammenfassung");
        if (materials.isEmpty()) {
            writer.paragraph("Keine Beläge vorhanden.");
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
        writer.gap(6.0f);
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

    private void appendHeatingPlans(
            PdfWriter writer,
            List<SurfaceMaterialListService.HeatingPlanSummary> heatingPlans,
            ExportAssets exportAssets
    ) throws IOException {
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
        appendRasterHeatingPlanPages(writer, heatingPlans, exportAssets);
    }

    private void appendRasterHeatingPlanPages(
            PdfWriter writer,
            List<SurfaceMaterialListService.HeatingPlanSummary> heatingPlans,
            ExportAssets exportAssets
    ) throws IOException {
        LinkedHashSet<String> levelNames = new LinkedHashSet<>();
        heatingPlans.stream().map(SurfaceMaterialListService.HeatingPlanSummary::levelName).forEach(levelNames::add);
        exportAssets.heatingLevelImages().keySet().stream()
                .map(SurfaceMaterialReportPdfService::heatingLevelName)
                .forEach(levelNames::add);
        if (levelNames.isEmpty()) {
            return;
        }
        for (String levelName : levelNames) {
            for (String surfacePosition : heatingSurfacePositions(heatingPlans, exportAssets, levelName)) {
                BufferedImage heatingImage = exportAssets.heatingLevelImages().get(heatingLevelImageKey(levelName, surfacePosition));
                if (heatingImage != null) {
                    writer.imagePage("Heizflächen " + surfacePosition + " - " + levelName, heatingImage);
                }
            }
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
            writer.paragraph("Keine Beläge vorhanden.");
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
        TableDefinition levelSummaryTable = new TableDefinition(
                BODY_FONT_SIZE,
                List.of(
                        new TableColumn("Etage", 1.8f, TableAlignment.LEFT),
                        new TableColumn("Fläche", 0.7f, TableAlignment.RIGHT),
                        new TableColumn("Stückzahl", 0.7f, TableAlignment.RIGHT),
                        new TableColumn("Materialfläche", 0.85f, TableAlignment.RIGHT),
                        new TableColumn("Schnitte", 0.65f, TableAlignment.RIGHT),
                        new TableColumn("Komplexität", 0.75f, TableAlignment.RIGHT)
                )
        );
        TableDefinition propertyUsageTable = new TableDefinition(
                BODY_FONT_SIZE,
                List.of(
                        new TableColumn("Name", 0.9f, TableAlignment.LEFT),
                        new TableColumn("Eigenschaften", 3.7f, TableAlignment.LEFT),
                        new TableColumn("Geschoss + Raum", 1.4f, TableAlignment.LEFT)
                )
        );
        for (SurfaceMaterialListService.MaterialSummary material : materials) {
            writer.gap(8.0f);
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
            if (!material.propertyUsages().isEmpty()) {
                writer.caption("Abweichende Eigenschaften");
                writer.table(
                        propertyUsageTable,
                        material.propertyUsages().stream()
                                .map(usage -> List.of(
                                        usage.name(),
                                        usage.properties(),
                                        usage.location()
                                ))
                                .toList()
                );
            }
            appendMaterialLevelSummaries(writer, levelSummaryTable, material.roomEntries());
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

    private void appendMaterialLevelSummaries(
            PdfWriter writer,
            TableDefinition table,
            List<SurfaceMaterialListService.MaterialRoomEntry> roomEntries
    ) throws IOException {
        writer.caption("Summen pro Etage");
        writer.table(
                table,
                roomEntries.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                SurfaceMaterialListService.MaterialRoomEntry::levelName,
                                LinkedHashMap::new,
                                java.util.stream.Collectors.toList()
                        ))
                        .entrySet()
                        .stream()
                        .map(entry -> materialLevelSummaryRow(entry.getKey(), entry.getValue()))
                        .toList()
        );
    }

    private List<String> materialLevelSummaryRow(
            String levelName,
            List<SurfaceMaterialListService.MaterialRoomEntry> entries
    ) {
        int placedPieces = entries.stream().mapToInt(entry -> entry.fullPieces() + entry.cutPieces()).sum();
        int cutCount = entries.stream().mapToInt(SurfaceMaterialListService.MaterialRoomEntry::cutCount).sum();
        return List.of(
                levelName,
                decimal(entries.stream().mapToDouble(SurfaceMaterialListService.MaterialRoomEntry::coveredAreaSquareMeters).sum(), 2) + " m²",
                Integer.toString(entries.stream().mapToInt(SurfaceMaterialListService.MaterialRoomEntry::requiredPieces).sum()),
                decimal(entries.stream().mapToDouble(SurfaceMaterialListService.MaterialRoomEntry::requiredMaterialAreaSquareMeters).sum(), 2) + " m²",
                Integer.toString(cutCount),
                decimal(SurfaceMaterialListService.complexity(placedPieces, cutCount,
                        entries.stream().mapToDouble(SurfaceMaterialListService.MaterialRoomEntry::cutPenaltySum).sum()), 1)
        );
    }

    public static String materialLevelImageKey(SurfaceMaterialListService.MaterialSummary material, String levelName) {
        return material.lookupKey() + "\u0000" + levelName;
    }

    public static String heatingLevelImageKey(String levelName, String surfacePosition) {
        return levelName + "\u0000" + surfacePosition;
    }

    private static String heatingLevelName(String heatingLevelImageKey) {
        int separatorIndex = heatingLevelImageKey.indexOf('\u0000');
        return separatorIndex < 0 ? heatingLevelImageKey : heatingLevelImageKey.substring(0, separatorIndex);
    }

    private static String heatingSurfacePosition(String heatingLevelImageKey) {
        int separatorIndex = heatingLevelImageKey.indexOf('\u0000');
        return separatorIndex < 0 ? "" : heatingLevelImageKey.substring(separatorIndex + 1);
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
            material.labeledValues().entrySet().stream()
                    .filter(entry -> "Dicke".equals(entry.getKey()) || "Format".equals(entry.getKey()))
                    .forEach(entry -> valuesByLabel.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(entry.getValue()));
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
        if (!Double.isFinite(room.minimumHeightMillimeters())) {
            return "-";
        }
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

    private static final class PdfWriter implements AutoCloseable {

        private final PDDocument document;
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
            gap(14.0f);
            writeWrappedText(normalize(text), FONT_BOLD, SECTION_FONT_SIZE, PAGE_MARGIN, availableWidth(), 5.0f);
        }

        private void subsection(String text) throws IOException {
            gap(8.0f);
            writeWrappedText(normalize(text), FONT_BOLD, SUBSECTION_FONT_SIZE, PAGE_MARGIN, availableWidth(), 3.0f);
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

        private void imageBlock(String title, BufferedImage image, float maximumHeight) throws IOException {
            float frameWidth = availableWidth();
            float innerWidth = frameWidth - SVG_INNER_PADDING * 2.0f;
            float scale = Math.min(innerWidth / Math.max(1.0f, image.getWidth()), maximumHeight / Math.max(1.0f, image.getHeight()));
            float imageWidth = (float) (image.getWidth() * scale);
            float imageHeight = (float) (image.getHeight() * scale);
            float blockHeight = estimateWrappedHeight(normalize(title), FONT_BOLD, SUBSECTION_FONT_SIZE, availableWidth())
                    + 4.0f
                    + imageHeight
                    + SVG_INNER_PADDING * 2.0f
                    + 6.0f;
            if (!hasSpace(blockHeight)) {
                newPage();
            }
            writeWrappedText(normalize(title), FONT_BOLD, SUBSECTION_FONT_SIZE, PAGE_MARGIN, availableWidth(), 3.0f);
            float top = y;
            float frameHeight = imageHeight + SVG_INNER_PADDING * 2.0f;
            canvas.rectangle(PAGE_MARGIN, top - frameHeight, frameWidth, frameHeight, 0.65f, SVG_FRAME_STROKE, SVG_FRAME_FILL);
            float imageX = PAGE_MARGIN + SVG_INNER_PADDING + (innerWidth - imageWidth) / 2.0f;
            canvas.image(image, imageX, top - SVG_INNER_PADDING - imageHeight, imageWidth, imageHeight);
            y = top - frameHeight - 6.0f;
        }

        private void imagePage(String title, BufferedImage image) throws IOException {
            newPage();
            writeWrappedText(normalize(title), FONT_BOLD, SECTION_FONT_SIZE, PAGE_MARGIN, availableWidth(), 6.0f);
            float frameWidth = availableWidth();
            float frameHeight = Math.max(80.0f, y - PAGE_MARGIN);
            float innerPadding = 4.0f;
            float innerWidth = frameWidth - innerPadding * 2.0f;
            float innerHeight = frameHeight - innerPadding * 2.0f;
            float scale = Math.min(innerWidth / Math.max(1.0f, image.getWidth()), innerHeight / Math.max(1.0f, image.getHeight()));
            float imageWidth = (float) (image.getWidth() * scale);
            float imageHeight = (float) (image.getHeight() * scale);
            float frameBottom = PAGE_MARGIN;
            canvas.rectangle(PAGE_MARGIN, frameBottom, frameWidth, frameHeight, 0.65f, SVG_FRAME_STROKE, SVG_FRAME_FILL);
            float imageX = PAGE_MARGIN + innerPadding + (innerWidth - imageWidth) / 2.0f;
            float imageY = frameBottom + innerPadding + (innerHeight - imageHeight) / 2.0f;
            canvas.image(image, imageX, imageY, imageWidth, imageHeight);
            y = PAGE_MARGIN;
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

        private final PDDocument document;
        private final PDPageContentStream stream;

        private PageCanvas(PDDocument document, PDPage page) throws IOException {
            this.document = document;
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
            if (fill == null && stroke == null) {
                return;
            }
            stream.saveGraphicsState();
            stream.addRect((float) x, (float) y, (float) width, (float) height);
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

        private void image(BufferedImage image, double x, double y, double width, double height) throws IOException {
            PDImageXObject imageObject = LosslessFactory.createFromImage(document, image);
            stream.drawImage(imageObject, (float) x, (float) y, (float) width, (float) height);
        }

        @Override
        public void close() throws IOException {
            stream.close();
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
