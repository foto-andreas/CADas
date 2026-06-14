package de.andreas.cadas.application.dwg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ExternalDwgToDxfConverter implements DwgToDxfConverter {

    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    private final Tool tool;
    private final Duration timeout;

    public ExternalDwgToDxfConverter() {
        this(detect(System.getenv()), TIMEOUT);
    }

    ExternalDwgToDxfConverter(Tool tool) {
        this(tool, TIMEOUT);
    }

    ExternalDwgToDxfConverter(Tool tool, Duration timeout) {
        this.tool = tool;
        this.timeout = timeout;
    }

    public static ExternalDwgToDxfConverter fromEnvironment(Map<String, String> environment) {
        return new ExternalDwgToDxfConverter(detect(environment), TIMEOUT);
    }

    @Override
    public DwgConversionAvailability availability() {
        if (tool == null) {
            return DwgConversionAvailability.unavailable(
                    "Kein DWG-Konverter gefunden. Installiere zum Beispiel `brew install libredwg`, damit `dwg2dxf` verfügbar ist."
            );
        }
        return DwgConversionAvailability.available(tool.displayName(), tool.executable().toString());
    }

    @Override
    public DwgConversionResult convert(Path dwgFile, Path targetDxfFile) throws IOException, InterruptedException {
        if (tool == null) {
            throw new IOException(availability().message());
        }
        Files.createDirectories(targetDxfFile.toAbsolutePath().normalize().getParent());
        List<String> command = tool.command(dwgFile.toAbsolutePath().normalize(), targetDxfFile.toAbsolutePath().normalize());
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            String output = readProcessOutput(process).trim();
            String detail = output.isBlank() ? "" : ": " + output;
            throw new IOException("DWG-Konverter wurde nach " + timeout.toMillis() + " ms abgebrochen: " + tool.displayName() + detail);
        }
        String output = readProcessOutput(process);
        if (process.exitValue() != 0 || !Files.isRegularFile(targetDxfFile)) {
            throw new IOException("DWG-Konvertierung fehlgeschlagen mit " + tool.displayName() + ": " + output.trim());
        }
        return new DwgConversionResult(targetDxfFile, tool.displayName(), output.isBlank() ? List.of() : List.of(output.trim()));
    }

    private String readProcessOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private static Tool detect(Map<String, String> environment) {
        Optional<Tool> configuredTool = Optional.ofNullable(environment.get("CADAS_DWG_CONVERTER"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .flatMap(ExternalDwgToDxfConverter::toolForExecutable);
        if (configuredTool.isPresent()) {
            return configuredTool.get();
        }
        return firstExecutableInPath(environment, "dwg2dxf")
                .flatMap(ExternalDwgToDxfConverter::toolForExecutable)
                .or(() -> firstExecutableInPath(environment, "dwgread").flatMap(ExternalDwgToDxfConverter::toolForExecutable))
                .orElse(null);
    }

    private static Optional<Path> firstExecutableInPath(Map<String, String> environment, String executableName) {
        String pathValue = environment.getOrDefault("PATH", "");
        for (String pathEntry : pathValue.split(java.io.File.pathSeparator)) {
            if (pathEntry.isBlank()) {
                continue;
            }
            Path candidate = Path.of(pathEntry).resolve(executableName);
            if (Files.isExecutable(candidate)) {
                return Optional.of(candidate.toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }

    private static Optional<Tool> toolForExecutable(Path executable) {
        String fileName = executable.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.equals("dwg2dxf")) {
            return Optional.of(new Tool("LibreDWG dwg2dxf", executable.toAbsolutePath().normalize(), ToolMode.DWG2DXF));
        }
        if (fileName.equals("dwgread")) {
            return Optional.of(new Tool("LibreDWG dwgread", executable.toAbsolutePath().normalize(), ToolMode.DWGREAD));
        }
        return Optional.empty();
    }

    record Tool(String displayName, Path executable, ToolMode mode) {
        List<String> command(Path dwgFile, Path targetDxfFile) {
            List<String> command = new ArrayList<>();
            command.add(executable.toString());
            switch (mode) {
                case DWG2DXF -> {
                    command.add("--as");
                    command.add("r2000");
                    command.add("-y");
                    command.add("-o");
                    command.add(targetDxfFile.toString());
                    command.add(dwgFile.toString());
                }
                case DWGREAD -> {
                    command.add("-O");
                    command.add("DXF");
                    command.add("-o");
                    command.add(targetDxfFile.toString());
                    command.add(dwgFile.toString());
                }
            }
            return command;
        }
    }

    enum ToolMode {
        DWG2DXF,
        DWGREAD
    }
}
