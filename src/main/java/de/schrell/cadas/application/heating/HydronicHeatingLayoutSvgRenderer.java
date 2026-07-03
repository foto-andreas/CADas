package de.schrell.cadas.application.heating;

import de.schrell.cadas.application.floor.FloorOpeningGeometryService;
import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService.PlacedSurfaceTile;
import de.schrell.cadas.application.layers.WallSurfaceSideService;
import de.schrell.cadas.application.view.WallPlanOutlineService;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanPolygonSupport;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Rendert Heizkreis-Layouts als SVG-Vorschau.
 */
final class HydronicHeatingLayoutSvgRenderer {

    private static final double EPSILON = 0.001;
    private static final String WALL_FILL = "#d5d1c8";
    private static final String WALL_STROKE = "#6b655d";
    private static final String ROOM_FILL = "#fffdf9";
    private static final String ROOM_STROKE = "#202020";
    private static final String VARIOTHERM_PATTERN_ID = "variotherm-rinne";
    private static final FloorOpeningGeometryService FLOOR_OPENING_GEOMETRY_SERVICE = new FloorOpeningGeometryService();
    private static final SurfaceRectangleTileLayoutService SURFACE_RECTANGLE_TILE_LAYOUT_SERVICE = new SurfaceRectangleTileLayoutService();
    private static final WallSurfaceSideService WALL_SURFACE_SIDE_SERVICE = new WallSurfaceSideService();
    private static final WallPlanOutlineService WALL_PLAN_OUTLINE_SERVICE = new WallPlanOutlineService();

    private HydronicHeatingLayoutSvgRenderer() {
    }

    static String render(
            Level level,
            Room room,
            HydronicHeating heating,
            List<FloorOpening> floorOpenings,
            List<HeatingExclusionArea> heatingExclusionAreas,
            List<HydronicHeatingLayoutService.CircuitLayout> circuits
    ) {
        List<Wall> roomWalls = level == null
                ? List.of()
                : level.walls().stream()
                .filter(wall -> WALL_SURFACE_SIDE_SERVICE.hasInteriorSide(level, wall, room.id()))
                .toList();
        List<PlanPoint> svgPoints = new ArrayList<>(room.outline());
        roomWalls.forEach(wall -> svgPoints.addAll(WALL_PLAN_OUTLINE_SERVICE.outline(wall)));
        heating.zones().forEach(zone -> svgPoints.addAll(zone.outline()));
        circuits.forEach(circuit -> svgPoints.addAll(circuit.pipePath()));
        Bounds bounds = bounds(svgPoints);
        double padding = Math.max(heating.pipeSpacing().toMillimeters(), 100.0);
        double minX = bounds.minX() - padding;
        double minY = bounds.minY() - padding;
        double width = bounds.width() + padding * 2.0;
        double height = bounds.height() + padding * 2.0;
        CoordinateTransform transform = new CoordinateTransform(minX, minY);
        List<PlacedSurfaceTile> variothermTiles = variothermTiles(level, room);
        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0.000 0.000 %.3f %.3f\">\n",
                width, height));
        if (variothermTiles.isEmpty()) {
            appendVariothermPatternDefinition(svg, transform);
        } else {
            appendVariothermPatternDefinitions(svg, variothermTiles, transform);
        }
        if (!roomWalls.isEmpty()) {
            svg.append("<g id=\"waende\" fill=\"").append(WALL_FILL).append("\" stroke=\"").append(WALL_STROKE)
                    .append("\" stroke-width=\"8\">\n");
            for (Wall wall : roomWalls) {
                svg.append("<polygon points=\"").append(pointsAttribute(WALL_PLAN_OUTLINE_SERVICE.outline(wall), transform)).append("\"/>\n");
            }
            svg.append("</g>\n");
        }
        svg.append("<g id=\"raum\" fill=\"").append(ROOM_FILL).append("\" stroke=\"").append(ROOM_STROKE).append("\" stroke-width=\"10\">\n");
        svg.append("<polygon points=\"").append(pointsAttribute(room.outline(), transform)).append("\"/>\n</g>\n");
        svg.append("<g id=\"sperrflaechen\" fill=\"#f8dcd8\" stroke=\"#aa2d23\" stroke-width=\"5\">\n");
        appendOpenings(svg, room, floorOpenings, transform);
        appendExclusionAreas(svg, room, heatingExclusionAreas, transform);
        svg.append("</g>\n");
        svg.append("<g id=\"variotherm-rinnen\" stroke=\"none\">\n");
        if (variothermTiles.isEmpty()) {
            appendFallbackVariothermGrooves(svg, room, transform);
        } else {
            appendVariothermPanelGrooves(svg, variothermTiles, transform);
        }
        svg.append("</g>\n");
        svg.append("<g id=\"heizbereiche\" fill=\"rgba(255,255,255,0.1)\" stroke=\"#315f8f\" stroke-width=\"5\" stroke-dasharray=\"35 20\">\n");
        for (HeatingZone zone : heating.zones()) {
            svg.append("<polygon points=\"").append(pointsAttribute(zone.outline(), transform)).append("\"/>\n");
        }
        svg.append("</g>\n");
        svg.append(String.format(Locale.US,
                "<g id=\"heizrohre\" fill=\"none\" stroke-width=\"%.3f\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\n",
                heating.pipeDiameter().toMillimeters()));
        int circuitIndex = 0;
        for (HydronicHeatingLayoutService.CircuitLayout circuit : circuits) {
            svg.append(pathElement("vorlauf", circuit.fieldSupplyPath().reversed(), "#d33b32", false, transform));
            svg.append(pathElement("ruecklauf", circuit.fieldReturnPath(), "#1f62d0", true, transform));
            svg.append(portCircle(circuit.supplyPort(), "#d33b32", "V" + (circuitIndex + 1), transform));
            svg.append(portCircle(circuit.returnPort(), "#1f62d0", "R" + (circuitIndex + 1), transform));
            circuitIndex++;
        }
        svg.append("</g>\n</svg>\n");
        return svg.toString();
    }

    private static void appendOpenings(StringBuilder svg, Room room, List<FloorOpening> floorOpenings, CoordinateTransform transform) {
        for (FloorOpening opening : floorOpenings) {
            if (!opening.roomId().equals(room.id())) {
                continue;
            }
            if (opening.shape() == FloorOpeningShape.CIRCLE) {
                svg.append(String.format(Locale.US,
                        "<circle cx=\"%.3f\" cy=\"%.3f\" r=\"%.3f\"/>\n",
                        transform.x(opening.center().xMillimeters()), transform.y(opening.center().yMillimeters()),
                        opening.width().toMillimeters() / 2.0));
            } else {
                svg.append("<polygon points=\"").append(pointsAttribute(rectangle(
                        opening.minXMillimeters(), opening.minYMillimeters(),
                        opening.maxXMillimeters(), opening.maxYMillimeters()
                ), transform)).append("\"/>\n");
            }
        }
    }

    private static void appendExclusionAreas(
            StringBuilder svg,
            Room room,
            List<HeatingExclusionArea> heatingExclusionAreas,
            CoordinateTransform transform
    ) {
        for (HeatingExclusionArea area : heatingExclusionAreas) {
            if (!area.roomId().equals(room.id())) {
                continue;
            }
            svg.append("<polygon points=\"").append(pointsAttribute(rectangle(
                    area.minXMillimeters(), area.minYMillimeters(),
                    area.maxXMillimeters(), area.maxYMillimeters()
            ), transform)).append("\"/>\n");
        }
    }

    private static void appendVariothermPatternDefinition(StringBuilder svg, CoordinateTransform transform) {
        double pitch = SurfaceCoveringPresetService.VARIOTHERM_GROOVE_PITCH_MILLIMETERS;
        double patternOffsetX = positiveModulo(-transform.minX(), pitch);
        double patternOffsetY = positiveModulo(-transform.minY(), pitch);
        svg.append("<defs>");
        appendVariothermPattern(svg, VARIOTHERM_PATTERN_ID, patternOffsetX, patternOffsetY);
        svg.append("</defs>\n");
    }

    private static void appendVariothermPatternDefinitions(StringBuilder svg, List<PlacedSurfaceTile> tiles, CoordinateTransform transform) {
        svg.append("<defs>");
        for (int index = 0; index < tiles.size(); index++) {
            PlacedSurfaceTile tile = tiles.get(index);
            appendVariothermPattern(svg, variothermPatternId(index), transform.x(tile.fullX()), transform.y(tile.fullY()));
        }
        svg.append("</defs>\n");
    }

    private static void appendVariothermPattern(StringBuilder svg, String patternId, double x, double y) {
        double pitch = SurfaceCoveringPresetService.VARIOTHERM_GROOVE_PITCH_MILLIMETERS;
        double radius = (pitch - SurfaceCoveringPresetService.VARIOTHERM_PIPE_DIAMETER_MILLIMETERS) / 2.0;
        svg.append(String.format(Locale.US,
                "<pattern id=\"%s\" patternUnits=\"userSpaceOnUse\" x=\"%.3f\" y=\"%.3f\" width=\"%.3f\" height=\"%.3f\">",
                patternId,
                x,
                y,
                pitch,
                pitch
        ));
        svg.append(String.format(Locale.US,
                "<circle cx=\"%.3f\" cy=\"%.3f\" r=\"%.3f\" fill=\"none\" stroke=\"#9aa6ad\" stroke-width=\"2\"/></pattern>",
                pitch / 2.0,
                pitch / 2.0,
                radius
        ));
    }

    private static void appendFallbackVariothermGrooves(StringBuilder svg, Room room, CoordinateTransform transform) {
        svg.append("<polygon fill=\"url(#").append(VARIOTHERM_PATTERN_ID).append(")\" points=\"")
                .append(pointsAttribute(room.outline(), transform))
                .append("\"/>\n");
    }

    private static void appendVariothermPanelGrooves(StringBuilder svg, List<PlacedSurfaceTile> tiles, CoordinateTransform transform) {
        for (int index = 0; index < tiles.size(); index++) {
            PlacedSurfaceTile tile = tiles.get(index);
            svg.append(String.format(Locale.US,
                    "<rect x=\"%.3f\" y=\"%.3f\" width=\"%.3f\" height=\"%.3f\" fill=\"url(#%s)\"/>\n",
                    transform.x(tile.x()),
                    transform.y(tile.y()),
                    tile.width(),
                    tile.height(),
                    variothermPatternId(index)
            ));
        }
    }

    private static String variothermPatternId(int index) {
        return VARIOTHERM_PATTERN_ID + "-" + index;
    }

    private static List<PlacedSurfaceTile> variothermTiles(Level level, Room room) {
        Optional<SurfaceLayer> layer = variothermLayer(level, room);
        if (layer.isEmpty()) {
            return List.of();
        }
        return SURFACE_RECTANGLE_TILE_LAYOUT_SERVICE.tilesForRectangles(
                FLOOR_OPENING_GEOMETRY_SERVICE.floorRectangles(level, room),
                layer.orElseThrow()
        );
    }

    private static Optional<SurfaceLayer> variothermLayer(Level level, Room room) {
        if (level == null) {
            return Optional.empty();
        }
        SurfaceLayerStack stack = level.findSurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        if (stack == null) {
            return Optional.empty();
        }
        return stack.layers().stream()
                .filter(layer -> layer.visible() && layer.thickness().toMillimeters() > EPSILON)
                .filter(SurfaceCoveringPresetService::isVariothermDryPanelLayer)
                .findFirst();
    }

    private static String pointsAttribute(List<PlanPoint> points, CoordinateTransform transform) {
        StringBuilder attribute = new StringBuilder();
        for (PlanPoint point : points) {
            if (!attribute.isEmpty()) {
                attribute.append(' ');
            }
            attribute.append(String.format(Locale.US, "%.3f,%.3f",
                    transform.x(point.xMillimeters()),
                    transform.y(point.yMillimeters())));
        }
        return attribute.toString();
    }

    private static String pathElement(
            String cssClass,
            List<PlanPoint> path,
            String color,
            boolean dashed,
            CoordinateTransform transform
    ) {
        if (path.size() < 2) {
            return "";
        }
        return String.format(Locale.US,
                "<path class=\"%s\" d=\"%s\" stroke=\"%s\"%s/>\n",
                cssClass,
                svgPath(path, transform),
                color,
                dashed ? " stroke-dasharray=\"60 35\"" : "");
    }

    private static String svgPath(List<PlanPoint> path, CoordinateTransform transform) {
        StringBuilder d = new StringBuilder();
        for (int index = 0; index < path.size(); index++) {
            PlanPoint point = path.get(index);
            d.append(index == 0 ? "M " : " L ");
            d.append(String.format(Locale.US, "%.3f %.3f",
                    transform.x(point.xMillimeters()),
                    transform.y(point.yMillimeters())));
        }
        return d.toString();
    }

    private static String portCircle(PlanPoint point, String color, String label, CoordinateTransform transform) {
        return String.format(Locale.US,
                "<circle cx=\"%.3f\" cy=\"%.3f\" r=\"38\" fill=\"#fff\" stroke=\"%s\" stroke-width=\"8\"/>"
                        + "<text x=\"%.3f\" y=\"%.3f\" fill=\"%s\">%s</text>\n",
                transform.x(point.xMillimeters()), transform.y(point.yMillimeters()), color,
                transform.x(point.xMillimeters()), transform.y(point.yMillimeters()) + 30.0, color, label);
    }

    private static Bounds bounds(List<PlanPoint> polygon) {
        return new Bounds(
                polygon.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
        );
    }

    private static List<PlanPoint> rectangle(double minX, double minY, double maxX, double maxY) {
        return List.of(
                new PlanPoint(minX, minY),
                new PlanPoint(maxX, minY),
                new PlanPoint(maxX, maxY),
                new PlanPoint(minX, maxY)
        );
    }

    private static double snapUp(double coordinate, double pitch) {
        return Math.ceil((coordinate - EPSILON) / pitch) * pitch;
    }

    private static double positiveModulo(double value, double modulo) {
        double remainder = value % modulo;
        return remainder < 0.0 ? remainder + modulo : remainder;
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) {
        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }
    }

    private record CoordinateTransform(double minX, double minY) {
        private double x(double value) {
            return value - minX;
        }

        private double y(double value) {
            return value - minY;
        }
    }
}
