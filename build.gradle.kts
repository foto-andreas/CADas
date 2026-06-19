import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

plugins {
    application
    jacoco
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "de.andreas"
version = "1.1.0"

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
    mainModule = "de.andreas.cadas"
    mainClass = "de.andreas.cadas.CadLauncher"
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics,ALL-UNNAMED")
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
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
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

abstract class RenamePackagedFileTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

    @get:Input
    abstract val expectedExtension: Property<String>

    @get:Input
    abstract val targetFileName: Property<String>

    @TaskAction
    fun renamePackagedFile() {
        val directory = outputDirectory.asFile.get()
        val generatedArtifact = directory.listFiles()
            ?.filter { file -> file.isFile && file.extension.equals(expectedExtension.get(), ignoreCase = true) }
            ?.maxByOrNull(File::lastModified)
            ?: return
        val targetFile = directory.resolve(targetFileName.get())
        if (generatedArtifact.name != targetFile.name) {
            targetFile.delete()
            generatedArtifact.renameTo(targetFile)
        }
    }
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
    "--module", "de.andreas.cadas/de.andreas.cadas.CadLauncher",
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
            "de.andreas.cadas",
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

val renameMacOsDmg by tasks.registering(RenamePackagedFileTask::class) {
    outputDirectory.set(dmgOutputDirectory)
    expectedExtension.set("dmg")
    targetFileName.set("CADas-${artifactFileVersion()}.dmg")
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

tasks.register<Exec>("packageMacOsDmg") {
    group = "distribution"
    description = "Erstellt ein macOS-DMG-Installationspaket für CADas."
    enabled = macOsPackagingSupported.get()
    dependsOn(jlinkRuntimeImage, cleanMacOsDmg)
    inputs.dir(installLibDirectory)
    outputs.dir(dmgOutputDirectory)
    executable = jpackageExecutable
    args = listOf(
        "--type", "dmg",
        "--dest", dmgOutputDirectory.absolutePath
    ) + commonJpackageArguments
    finalizedBy(renameMacOsDmg)
}

tasks.register<Exec>("runMitAutomatisierung") {
    group = "application"
    description = "Startet CADas mit lokalem HTTP-Automatisierungszugriff für manuelle und agentische Tests."
    dependsOn(tasks.installDist)
    executable = layout.buildDirectory.file("install/CADas/bin/CADas").get().asFile.absolutePath
    environment("JAVA_OPTS", "-Dcadas.automation.enabled=true")
}
