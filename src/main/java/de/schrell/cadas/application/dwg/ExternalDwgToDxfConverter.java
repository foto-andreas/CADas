package de.schrell.cadas.application.dwg;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ExternalDwgToDxfConverter {

    private static final Duration TIMEOUT = Duration.ofSeconds(90);
    private static final int MAXIMUM_DIAGNOSTIC_OUTPUT_BYTES = 1_048_576;
    private static final List<Path> STANDARD_TOOL_DIRECTORIES = List.of(
            Path.of("/opt/homebrew/bin"),
            Path.of("/opt/homebrew/opt/libredwg/bin"),
            Path.of("/usr/local/bin"),
            Path.of("/usr/local/opt/libredwg/bin"),
            Path.of("/opt/local/bin")
    );

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
        return fromEnvironment(environment, STANDARD_TOOL_DIRECTORIES);
    }

    public static ExternalDwgToDxfConverter fromEnvironment(Map<String, String> environment, List<Path> standardToolDirectories) {
        return new ExternalDwgToDxfConverter(detect(environment, standardToolDirectories), TIMEOUT);
    }

    public DwgConversionAvailability availability() {
        if (tool == null) {
            return DwgConversionAvailability.unavailable(
                    "Kein DWG-Konverter gefunden. Installiere zum Beispiel `brew install libredwg`, damit `dwg2dxf` verfügbar ist."
            );
        }
        return DwgConversionAvailability.available(tool.displayName(), tool.executable().toString());
    }

    public DwgConversionResult convert(Path dwgFile, Path targetDxfFile) throws IOException, InterruptedException {
        if (tool == null) {
            throw new IOException(availability().message());
        }
        Files.createDirectories(targetDxfFile.toAbsolutePath().normalize().getParent());
        List<String> command = tool.command(dwgFile.toAbsolutePath().normalize(), targetDxfFile.toAbsolutePath().normalize());
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        FutureTask<String> outputTask = new FutureTask<>(() -> readProcessOutput(process.getInputStream()));
        Thread outputReader = Thread.ofVirtual().name("cadas-dwg-konverter-ausgabe").start(outputTask);
        try {
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                String output = completedOutput(outputTask).trim();
                String detail = output.isBlank() ? "" : ": " + output;
                throw new IOException("DWG-Konverter wurde nach " + timeout.toMillis() + " ms abgebrochen: " + tool.displayName() + detail);
            }
            String output = completedOutput(outputTask);
            if (process.exitValue() != 0 || !Files.isRegularFile(targetDxfFile)) {
                throw new IOException("DWG-Konvertierung fehlgeschlagen mit " + tool.displayName() + ": " + output.trim());
            }
            return new DwgConversionResult(targetDxfFile, tool.displayName(), output.isBlank() ? List.of() : List.of(output.trim()));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            outputTask.cancel(true);
            outputReader.interrupt();
        }
    }

    private String completedOutput(FutureTask<String> outputTask) throws InterruptedException {
        try {
            return outputTask.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException exception) {
            return "";
        }
    }

    /**
     * Leert die Prozess-Pipe vollständig, damit der Kindprozess nie an einem vollen Puffer blockiert. Für eine
     * Fehlermeldung wird nur das erste MiB aufbewahrt; weitere Bytes werden verworfen, aber weiterhin gelesen.
     */
    private String readProcessOutput(InputStream input) {
        try (input; ByteArrayOutputStream retainedOutput = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8_192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) >= 0) {
                int retainedBytes = Math.min(bytesRead, MAXIMUM_DIAGNOSTIC_OUTPUT_BYTES - retainedOutput.size());
                if (retainedBytes > 0) {
                    retainedOutput.write(buffer, 0, retainedBytes);
                }
            }
            return retainedOutput.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private static Tool detect(Map<String, String> environment) {
        return detect(environment, STANDARD_TOOL_DIRECTORIES);
    }

    private static Tool detect(Map<String, String> environment, List<Path> standardToolDirectories) {
        Optional<Tool> configuredTool = Optional.ofNullable(environment.get("CADAS_DWG_CONVERTER"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .flatMap(ExternalDwgToDxfConverter::toolForExecutable);
        if (configuredTool.isPresent()) {
            return configuredTool.get();
        }
        List<Path> searchDirectories = searchDirectories(environment, standardToolDirectories);
        return firstExecutable(searchDirectories, "dwg2dxf")
                .flatMap(ExternalDwgToDxfConverter::toolForExecutable)
                .or(() -> firstExecutable(searchDirectories, "dwgread").flatMap(ExternalDwgToDxfConverter::toolForExecutable))
                .orElse(null);
    }

    private static List<Path> searchDirectories(Map<String, String> environment, List<Path> standardToolDirectories) {
        LinkedHashSet<Path> directories = new LinkedHashSet<>();
        String pathValue = environment.getOrDefault("PATH", "");
        for (String pathEntry : pathValue.split(java.io.File.pathSeparator)) {
            if (!pathEntry.isBlank()) {
                directories.add(Path.of(pathEntry));
            }
        }
        Optional.ofNullable(environment.get("HOMEBREW_PREFIX"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .map(path -> path.resolve("bin"))
                .ifPresent(directories::add);
        directories.addAll(standardToolDirectories);
        return List.copyOf(directories);
    }

    private static Optional<Path> firstExecutable(List<Path> searchDirectories, String executableName) {
        for (Path directory : searchDirectories) {
            Path candidate = directory.resolve(executableName);
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
