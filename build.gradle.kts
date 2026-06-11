plugins {
    application
    jacoco
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "de.andreas"
version = "0.1.0-SNAPSHOT"

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
    modules("javafx.controls")
}

application {
    mainModule = "de.andreas.cadas"
    mainClass = "de.andreas.cadas.CadLauncher"
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics,ALL-UNNAMED")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
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
}

tasks.register<Exec>("runMitAutomatisierung") {
    group = "application"
    description = "Startet CADas mit lokalem HTTP-Automatisierungszugriff für manuelle und agentische Tests."
    dependsOn(tasks.installDist)
    executable = layout.buildDirectory.file("install/CADas/bin/CADas").get().asFile.absolutePath
    environment("JAVA_OPTS", "-Dcadas.automation.enabled=true")
}
