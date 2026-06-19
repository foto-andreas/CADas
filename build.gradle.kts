import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

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

val installLibDirectory = layout.buildDirectory.dir("install/CADas/lib").get().asFile
val appImageOutputDirectory = layout.buildDirectory.dir("installer/macos/app-image").get().asFile
val dmgOutputDirectory = layout.buildDirectory.dir("installer/macos/dmg").get().asFile

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
    "--module-path", installLibDirectory.absolutePath,
    "--module", "de.andreas.cadas/de.andreas.cadas.CadLauncher",
    "--java-options", "--enable-native-access=javafx.graphics,ALL-UNNAMED"
)

val cleanMacOsAppImage by tasks.registering(Delete::class) {
    delete(appImageOutputDirectory.resolve("CADas.app"))
}

val cleanMacOsDmg by tasks.registering(Delete::class) {
    delete(dmgOutputDirectory)
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
    dependsOn(tasks.installDist, cleanMacOsAppImage)
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
    dependsOn(tasks.installDist, cleanMacOsDmg)
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
