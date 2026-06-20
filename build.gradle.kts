import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile

plugins {
    application
    jacoco
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "de.schrell"
version = "1.3.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

javafx {
    version = "25"
    modules("javafx.controls", "javafx.swing", "javafx.web")
}

application {
    mainModule = "de.schrell.cadas"
    mainClass = "de.schrell.cadas.CadLauncher"
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=javafx.graphics,ALL-UNNAMED",
        // PDFBox ist als requires static deklariert; commons-logging wird von PDFBox
        // zur Laufzeit benötigt. Beide müssen dem Laufzeit-Layer hinzugefügt werden.
        "--add-modules=org.apache.pdfbox,org.apache.commons.logging"
    )
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = project.version
    }
}

abstract class GenerateThirdPartyLicensesTask : DefaultTask() {

    @get:Classpath
    abstract val artifactFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val markdown = buildString {
            appendLine("# Drittanbieter-Lizenzen")
            appendLine()
            appendLine("Diese Liste wird beim Build automatisch aus allen Laufzeitabhängigkeiten erzeugt.")
            appendLine()
            artifactFiles.files
                .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
                .sortedBy { it.name.lowercase() }
                .forEach { artifact ->
                    appendLine("## ${artifact.name}")
                    appendLine()
                    val licenseEntries = ZipFile(artifact).use { archive ->
                        archive.entries().asSequence()
                            .filterNot { it.isDirectory }
                            .filter { isLicenseFile(it.name) }
                            .sortedBy { it.name }
                            .map { entry -> entry.name to archive.getInputStream(entry).bufferedReader().use { it.readText() } }
                            .toList()
                    }
                    if (licenseEntries.isEmpty()) {
                        appendLine("Im Artefakt ist keine Lizenzdatei eingebettet.")
                        appendLine()
                    } else {
                        licenseEntries.forEach { (path, content) ->
                            appendLine("### $path")
                            appendLine()
                            content.trim().lineSequence().forEach { line -> appendLine("    $line") }
                            appendLine()
                        }
                    }
                }
        }
        val target = outputFile.get().asFile.toPath()
        Files.createDirectories(target.parent)
        Files.writeString(target, markdown)
    }

    private fun isLicenseFile(path: String): Boolean {
        val fileName = path.substringAfterLast('/').uppercase()
        return fileName == "LICENSE"
                || fileName.startsWith("LICENSE.")
                || fileName == "NOTICE"
                || fileName.startsWith("NOTICE.")
                || fileName == "COPYING"
                || fileName.startsWith("COPYING.")
    }
}

val generateThirdPartyLicenses by tasks.registering(GenerateThirdPartyLicensesTask::class) {
    artifactFiles.from(configurations.runtimeClasspath)
    outputFile.set(layout.buildDirectory.file("generated/licenses/drittanbieter-lizenzen.md"))
}

tasks.processResources {
    dependsOn(generateThirdPartyLicenses)
    from(generateThirdPartyLicenses.flatMap { it.outputFile }) {
        into("docs")
    }
}

dependencies {
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.24.0")
    implementation("org.apache.pdfbox:pdfbox:3.0.7")
    implementation("commons-logging:commons-logging:1.3.5")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Tests laufen ohne interaktiven Anwender; blockierende Dialoge würden die Automation aufhalten.
    systemProperty("cadas.automation.enabled", "true")
    // Unterdrückt die Native-Access-Warnung des JavaFX-Glass-Nativelibrary-Laders,
    // der im Test-Klassenpfad als unbenanntes Modul läuft.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf(
                "de.schrell.cadas.domain.*",
                "de.schrell.cadas.application.drawing",
                "de.schrell.cadas.application.exchange",
                "de.schrell.cadas.application.layers",
                "de.schrell.cadas.application.objects",
                "de.schrell.cadas.application.parts",
                "de.schrell.cadas.application.reports",
                "de.schrell.cadas.application.room",
                "de.schrell.cadas.application.view",
                "de.schrell.cadas.infrastructure.dxf"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

val macOsPackagingSupported = providers.systemProperty("os.name")
    .map { it.startsWith("Mac", ignoreCase = true) }
    .orElse(false)

val jpackageExecutable = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(25)
}.get().metadata.installationPath.asFile.resolve("bin/jpackage").absolutePath
val jlinkExecutable = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(25)
}.get().metadata.installationPath.asFile.resolve("bin/jlink").absolutePath

val installLibDirectory = layout.buildDirectory.dir("install/CADas/lib").get().asFile
val appImageOutputDirectory = layout.buildDirectory.dir("installer/macos/app-image").get().asFile
val dmgOutputDirectory = layout.buildDirectory.dir("installer/macos/dmg").get().asFile
// Laufzeitimage mit nur echten Modulen (JDK + JavaFX + modulare Drittabhängigkeiten), per jlink erzeugt.
val runtimeImageDirectory = layout.buildDirectory.dir("installer/macos/runtime-image").get().asFile
// Bereinigter Modulpfad für jlink: nur proper modules (echte Java-Module), sodass jlink keine
// automatic modules vorfindet.
val jlinkModulePath = layout.buildDirectory.dir("installer/macos/module-path").get().asFile

// JARs ohne eigenes module-info.class dürfen nicht auf den jlink-Modulpfad.
val automaticModuleJars = listOf(
    "commons-logging-1.3.5.jar",
    "fontbox-3.0.7.jar",
    "pdfbox-3.0.7.jar",
    "pdfbox-io-3.0.7.jar"
)
// JavaFX-Plugin legt jdk-jsobject als automatic module ins lib-Verzeichnis, obwohl das JDK
// das proper module jdk.jsobject bereits mitbringt. Es wird ebenfalls auf den Classpath gelegt.
val redundantJdkJars = listOf("jdk-jsobject-25.jar")
// Alle JARs, die als Classpath (nicht als Modul) ins App-Image gelangen.
val classpathJars = automaticModuleJars + redundantJdkJars

fun normalizedAppVersion(): String {
    val numericParts = version.toString()
        .substringBefore('-')
        .split('.')
        .mapNotNull { it.toIntOrNull() }
        .take(3)
        .toMutableList()
    while (numericParts.size < 3) {
        numericParts += 0
    }
    if (numericParts.firstOrNull() == null || numericParts.first() <= 0) {
        numericParts[0] = 1
    }
    return numericParts.joinToString(".")
}

fun artifactFileVersion(): String {
    return version.toString()
}

fun snapshotAppImageName(): String {
    return if (artifactFileVersion().contains('-')) "CADas-${artifactFileVersion()}.app" else "CADas.app"
}

abstract class PrepareModulePathTask : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:Input
    abstract val excludedJarNames: ListProperty<String>

    @get:OutputDirectory
    abstract val targetDirectory: DirectoryProperty

    @TaskAction
    fun prepare() {
        val target = targetDirectory.get().asFile
        target.deleteRecursively()
        target.mkdirs()
        val excluded = excludedJarNames.get().toSet()
        sourceDirectory.get().asFile
            .listFiles { file -> file.isFile && file.extension == "jar" && file.name !in excluded }
            ?.forEach { file -> file.copyTo(target.resolve(file.name)) }
    }
}

abstract class JlinkRuntimeImageTask : DefaultTask() {

    @get:InputDirectory
    abstract val modulePath: DirectoryProperty

    @get:OutputDirectory
    abstract val outputImage: DirectoryProperty

    @get:Input
    abstract val addModules: ListProperty<String>

    @get:Input
    abstract val jlinkToolExecutable: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun runJlink() {
        val outputDir = outputImage.get().asFile
        outputDir.deleteRecursively()
        execOperations.exec {
            executable = jlinkToolExecutable.get()
            args = listOf(
                "--output", outputDir.absolutePath,
                "--module-path", modulePath.get().asFile.absolutePath,
                "--add-modules", addModules.get().joinToString(","),
                "--strip-native-commands",
                "--no-header-files",
                "--no-man-pages"
            )
        }
    }
}

abstract class RenamePackagedDirectoryTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

    @get:Input
    abstract val generatedDirectoryName: Property<String>

    @get:Input
    abstract val targetDirectoryName: Property<String>

    @TaskAction
    fun renamePackagedDirectory() {
        val directory = outputDirectory.asFile.get()
        val generatedDirectory = directory.resolve(generatedDirectoryName.get())
        if (!generatedDirectory.exists()) {
            return
        }
        val targetDirectory = directory.resolve(targetDirectoryName.get())
        if (generatedDirectory.name != targetDirectory.name) {
            targetDirectory.deleteRecursively()
            generatedDirectory.renameTo(targetDirectory)
        }
    }
}

val commonJpackageArguments = listOf(
    "--name", "CADas",
    "--app-version", normalizedAppVersion(),
    "--vendor", "Andreas",
    "--icon", file("src/main/resources/icons/CADas.icns").absolutePath,
    "--runtime-image", runtimeImageDirectory.absolutePath,
    // Vollständiges lib-Verzeichnis als Modulpfad fürs App-Image: proper modules und automatic modules.
    // jlink verwendet stattdessen den bereinigten jlinkModulePath, der nur proper modules enthält.
    "--module-path", installLibDirectory.absolutePath,
    "--module", "de.schrell.cadas/de.schrell.cadas.CadLauncher",
    "--java-options", "--enable-native-access=javafx.graphics,ALL-UNNAMED",
    "--java-options", "--add-modules=org.apache.pdfbox,org.apache.commons.logging"
)

val cleanMacOsAppImage by tasks.registering(Delete::class) {
    delete(appImageOutputDirectory.resolve("CADas.app"))
}

val cleanMacOsDmg by tasks.registering(Delete::class) {
    delete(dmgOutputDirectory)
}

// Bereinigter Modulpfad für jlink: kopiert nur proper modules (echte Java-Module) aus dem
// lib-Verzeichnis, sodass jlink keine automatic modules vorfindet.
val prepareJlinkModulePath by tasks.registering(PrepareModulePathTask::class) {
    sourceDirectory.set(installLibDirectory)
    excludedJarNames.set(classpathJars)
    targetDirectory.set(jlinkModulePath)
    mustRunAfter(tasks.installDist)
}

// Erzeugt das Laufzeitimage per jlink mit nur echten Modulen (JDK + JavaFX + commonmark).
// Die automatischen Module liegen später auf dem App-Modulpfad und werden per --add-modules
// in den Laufzeit-Layer aufgenommen.
val jlinkRuntimeImage by tasks.registering(JlinkRuntimeImageTask::class) {
    group = "distribution"
    description = "Erzeugt das Java-Laufzeitimage für die macOS-Paketierung per jlink."
    enabled = macOsPackagingSupported.get()
    dependsOn(tasks.installDist, prepareJlinkModulePath)
    modulePath.set(jlinkModulePath)
    outputImage.set(runtimeImageDirectory)
    addModules.set(
        listOf(
            "de.schrell.cadas",
            "javafx.controls",
            "javafx.swing",
            "javafx.web",
            "org.commonmark",
            "org.commonmark.ext.gfm.tables"
        )
    )
    jlinkToolExecutable.set(jlinkExecutable)
}

val renameMacOsAppImage by tasks.registering(RenamePackagedDirectoryTask::class) {
    outputDirectory.set(appImageOutputDirectory)
    generatedDirectoryName.set("CADas.app")
    targetDirectoryName.set(snapshotAppImageName())
}

tasks.register<Exec>("packageMacOsAppImage") {
    group = "distribution"
    description = "Erstellt ein macOS-App-Image für CADas."
    enabled = macOsPackagingSupported.get()
    dependsOn(jlinkRuntimeImage, cleanMacOsAppImage)
    inputs.dir(installLibDirectory)
    outputs.dir(appImageOutputDirectory)
    executable = jpackageExecutable
    args = listOf(
        "--type", "app-image",
        "--dest", appImageOutputDirectory.absolutePath
    ) + commonJpackageArguments
    finalizedBy(renameMacOsAppImage)
}

// Vordefiniertes App-Image für die DMG-Erzeugung. jpackage legt den /Applications-Link
// nur dann automatisch ins DMG, wenn das DMG aus einem bestehenden App-Image gebaut wird.
val macOsAppImageForDmg = layout.buildDirectory.dir("installer/macos/app-image-for-dmg").get().asFile

val cleanMacOsAppImageForDmg by tasks.registering(Delete::class) {
    delete(macOsAppImageForDmg)
}

// Phase 1: App-Image in ein temporäres Verzeichnis erzeugen.
val prepareMacOsAppImageForDmg by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Erzeugt das App-Image als Vorlage für das DMG (intern)."
    enabled = macOsPackagingSupported.get()
    dependsOn(jlinkRuntimeImage, cleanMacOsAppImageForDmg)
    inputs.dir(installLibDirectory)
    outputs.dir(macOsAppImageForDmg)
    executable = jpackageExecutable
    args = listOf(
        "--type", "app-image",
        "--dest", macOsAppImageForDmg.absolutePath
    ) + commonJpackageArguments
}

// Phase 2: Beschreibbares DMG mit App und /Applications-Link per hdiutil erzeugen.
// jpackage legt bei modularen Apps mit eigenem Runtime-Image keinen /Applications-Link an;
// daher bauen wir das DMG selbst aus einem vorbereiteten Inhaltsverzeichnis.
val dmgStagingDirectory = layout.buildDirectory.dir("installer/macos/dmg-staging").get().asFile

val cleanMacOsDmgStaging by tasks.registering(Delete::class) {
    delete(dmgStagingDirectory)
}

val stageDmgContent by tasks.registering(DefaultTask::class) {
    group = "distribution"
    description = "Stagt App-Image und /Applications-Link für das DMG (intern)."
    dependsOn(prepareMacOsAppImageForDmg, cleanMacOsDmgStaging)
    doLast {
        dmgStagingDirectory.deleteRecursively()
        dmgStagingDirectory.mkdirs()
        // App-Image mit erhaltenen Dateirechten kopieren (cp -pR), damit die
        // ausführbare MacOS/CADas-Binärdatei ihr Execute-Bit behält.
        ProcessBuilder("cp", "-pR",
            macOsAppImageForDmg.resolve("CADas.app").absolutePath,
            dmgStagingDirectory.resolve("CADas.app").absolutePath)
            .redirectErrorStream(true).start().waitFor()
            .let { if (it != 0) error("Kopieren des App-Images fehlgeschlagen (Exit $it).") }
        // /Applications-Link anlegen.
        val link = dmgStagingDirectory.resolve("Applications")
        Files.createSymbolicLink(link.toPath(), Paths.get("/Applications"))
    }
}

// Phase 2c: DMG aus dem Staging-Verzeichnis per hdiutil erzeugen (UDZO, mit Applications-Link).
val buildMacOsDmgWithLink by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Erzeugt das finale DMG inklusive /Applications-Link per hdiutil (intern)."
    dependsOn(stageDmgContent, cleanMacOsDmg)
    // Bewusst ohne inputs.dir: das Staging-Verzeichnis enthält einen Symlink auf /Applications,
    // dem Gradle folgen würde (OoM-Loop). Die Abhängigkeit sichert die Reihenfolge.
    outputs.dir(dmgOutputDirectory)
    executable = "hdiutil"
    args = listOf(
        "create",
        "-volname", "CADas",
        "-fs", "HFS+",
        "-format", "UDZO",
        "-imagekey", "zlib-level=9",
        "-srcfolder", dmgStagingDirectory.absolutePath,
        dmgOutputDirectory.resolve("CADas-${artifactFileVersion()}.dmg").absolutePath
    )
}

// Phase 2 (öffentlich): DMG bauen und Applications-Link injizieren.
tasks.register<DefaultTask>("packageMacOsDmg") {
    group = "distribution"
    description = "Erstellt ein macOS-DMG-Installationspaket für CADas mit /Applications-Link."
    enabled = macOsPackagingSupported.get()
    dependsOn(buildMacOsDmgWithLink)
}

// Installiert CADas.app direkt in den macOS-Programme-Ordner (/Applications).
// Eine bestehende Installation wird vorher entfernt, danach das DMG gebaut
// und das neue App-Bundle kopiert.
tasks.register<Exec>("macosInstall") {
    group = "distribution"
    description = "Installiert CADas.app nach /Applications (überschreibt bestehende Version) und baut das DMG."
    enabled = macOsPackagingSupported.get()
    dependsOn("packageMacOsDmg")
    val appBundle = macOsAppImageForDmg.resolve("CADas.app")
    inputs.dir(appBundle)
    executable = "rm"
    args("-rf", "/Applications/CADas.app")
    doFirst {
        logger.lifecycle("Entferne bestehende CADas.app und installiere neue Version nach /Applications ...")
    }
    finalizedBy("macosInstallCopy")
}

tasks.register<Exec>("macosInstallCopy") {
    group = "distribution"
    description = "Kopiert das fertige CADas.app-Bundle nach /Applications (interner Teil von macosInstall)."
    enabled = macOsPackagingSupported.get()
    dependsOn(prepareMacOsAppImageForDmg)
    val appBundle = macOsAppImageForDmg.resolve("CADas.app")
    inputs.dir(appBundle)
    executable = "cp"
    args("-R", appBundle.absolutePath, "/Applications/")
}

tasks.register<Exec>("runMitAutomatisierung") {
    group = "application"
    description = "Startet CADas mit lokalem HTTP-Automatisierungszugriff für manuelle und agentische Tests."
    dependsOn(tasks.installDist)
    executable = layout.buildDirectory.file("install/CADas/bin/CADas").get().asFile.absolutePath
    environment("JAVA_OPTS", "-Dcadas.automation.enabled=true")
}
