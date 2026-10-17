/*
 * Copyright (c) 2019-2025 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Build Info
val isCi = (System.getenv("CI") ?: "false") == "true"
val runJei = project.getProperties().getOrDefault("jei", "false").toString().toBoolean()
val runEmi = project.getProperties().getOrDefault("emi", "false").toString().toBoolean()
val runRei = project.getProperties().getOrDefault("rei", !runJei && !runEmi).toString().toBoolean()
val runSodium = project.getProperties().getOrDefault("sodium", !isCi).toString().toBoolean()

// Minecraft, Mappings, Loader Versions
val minecraftVersion         = project.property("minecraft.version").toString()
val loaderVersion            = project.property("loader.version").toString()
val yarnBuild                = project.property("yarn.build").toString()

// Mod Info
val modVersion               = project.property("mod.version").toString()
val modName                  = project.property("mod.name").toString()
val modGroup                 = project.property("mod.group").toString()

// Dependency Versions
val fabricVersion            = project.property("fabric.version").toString()
val clothConfigVersion       = project.property("cloth.config.version").toString()
val modMenuVersion           = project.property("modmenu.version").toString()
val dynamicdimensionsVersion = project.property("dynamicdimensions.version").toString()
val machineLibVersion        = project.property("machinelib.version").toString()
val reiVersion               = project.property("rei.version").toString()
val jeiVersion               = project.property("jei.version").toString()
val emiVersion               = project.property("emi.version").toString()
val badpacketsVersion        = project.property("badpackets.version").toString()
val wthitVersion             = project.property("wthit.version").toString()
val architecturyVersion      = project.property("architectury.version").toString()
val appleskinVersion         = project.property("appleskin.version").toString()
val objVersion               = project.property("obj.version").toString()
val sodiumVersion            = project.property("sodium.version").toString()
val reesesSodiumOptionsVersion = project.property("reeses.sodium.options.version").toString()
val sodiumExtraVersion       = project.property("sodium.extra.version").toString()
val lithiumVersion           = project.property("lithium.version").toString()
val ferriteCoreVersion       = project.property("ferritecore.version").toString()
val immediatelyFastVersion   = project.property("immediatelyfast.version").toString()

plugins {
    java
    `maven-publish`
    id("fabric-loom") version("1.13.6")
    id("com.diffplug.spotless") version("7.0.4")
    id("org.ajoberstar.grgit") version("5.3.2")
    id("dev.galacticraft.mojarn") version("0.6.1+19")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        resources {
            srcDir("src/main/generated")
            exclude(".cache/")
        }
    }
}

group = modGroup
version = buildString {
    append(modVersion)
    val env = System.getenv()
    if (env.containsKey("PRE_RELEASE") && env["PRE_RELEASE"] == "true") {
        append("-pre")
    }
    append('+')
    if (env.containsKey("GITHUB_RUN_NUMBER")) {
        append(env["GITHUB_RUN_NUMBER"])
    } else {
        val grgit = extensions.findByType<org.ajoberstar.grgit.Grgit>()
        if (grgit?.head() != null) {
            append(grgit.head().id.substring(0, 8))
            if (!grgit.status().isClean) {
                append("-dirty")
            }
        } else {
            append("unknown")
        }
    }
}
println("Galacticraft: $version")
base.archivesName.set(modName)

loom {
    accessWidenerPath.set(project.file("src/main/resources/galacticraft.accesswidener"))

    @Suppress("UnstableApiUsage")
    mixin {
        useLegacyMixinAp = true
        add(sourceSets.main.get(), "galacticraft.refmap.json")
        add(sourceSets.test.get(), "galacticraft-test.refmap.json")
    }

    runs {
        getByName("client") {
            name("Minecraft Client")
            source(sourceSets.test.get())
            property("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
        }

        getByName("server") {
            name("Minecraft Server")
            source(sourceSets.test.get())
        }

        register("datagen") {
            client()
            name("Data Generation")
            runDir("build/datagen")
            property("fabric-api.datagen")
            property("fabric-api.datagen.modid", "galacticraft")
            property("fabric-api.datagen.output-dir", project.file("src/main/generated").toString())
            property("fabric-api.datagen.strict-validation", "false")
        }

        register("gametest") {
            server()
            name("Game Test")
            source(sourceSets.test.get())
            property("fabric-api.gametest")
            property("fabric-api.gametest.report-file", "${project.layout.buildDirectory.get().file("junit.xml")}")
        }

        afterEvaluate {
            val mixinJarFile = configurations.runtimeClasspath.get().incoming.artifactView {
                componentFilter {
                    it is ModuleComponentIdentifier
                            && it.group == "net.fabricmc"
                            && it.module == "sponge-mixin"
                }
            }.files.first()

            configureEach {
                vmArg("-javaagent:$mixinJarFile")

                property("mixin.hotSwap", "true")
                property("mixin.debug.export", "true")
            }
        }
    }
}

repositories {
    mavenLocal()
    maven("http://5.161.199.182/repository/maven-releases/") {
        isAllowInsecureProtocol = true
        // https://maven.galacticraft.net/repository/maven-releases
        content {
            includeGroup("dev.galacticraft")
        }
    }
    maven("https://mvn.devos.one/snapshots/") {
        content {
            includeGroup("io.github.fabricators_of_create.Porting-Lib")
        }
    }
    maven("https://maven.shedaniel.me/") {
        content {
            includeGroup("me.shedaniel.cloth.api")
            includeGroup("me.shedaniel.cloth")
            includeGroup("me.shedaniel")
            includeGroup("dev.architectury")
        }
    }
    maven("https://maven.modmuss50.me/") {
        content {
            includeGroup("teamreborn")
        }
    }
    maven("https://maven.terraformersmc.com/releases/") {
        content {
            includeGroup("com.terraformersmc")
        }
    }
    maven("https://repo.sleeping.town") {
        content {
            includeGroup("dev.emi")
        }
    }
    maven("https://maven.bai.lol") {
        content {
            includeGroup("mcp.mobius.waila")
            includeGroup("lol.bai")
        }
    }
    maven("https://maven.blamejared.com/") {
        content {
            includeGroup("mezz.jei")
        }
    }
    maven("https://maven.ryanliptak.com/") {
        content {
            includeGroup("squeek.appleskin")
        }
    }
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
}

configurations {
    val core = create("core")
    val compat = create("compat")

    modImplementation {
        extendsFrom(core)
        extendsFrom(compat)
    }

    include {
        extendsFrom(core)
    }
}

dependencies {
    // Minecraft, Mappings, Loader
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(if (!isCi && yarnBuild.isNotEmpty()) {
        mojarn.mappings("net.fabricmc:yarn:$minecraftVersion+build.$yarnBuild:v2")
    } else {
        loom.officialMojangMappings()
    })
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    // Mandatory Dependencies (Included with Jar-In-Jar)
    include(implementation("de.javagl:obj:$objVersion")) {}

    "core"("dev.galacticraft:dynamicdimensions-fabric:$dynamicdimensionsVersion")
    "core"("dev.galacticraft:MachineLib:$machineLibVersion")
    "core"("lol.bai:badpackets:fabric-$badpacketsVersion")

    // Optional Dependencies
    "compat"("com.terraformersmc:modmenu:$modMenuVersion")
    "compat"("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")
    "compat"("mcp.mobius.waila:wthit:fabric-$wthitVersion")
    "compat"("dev.architectury:architectury-fabric:$architecturyVersion") // required for REI fluid support
    "compat"("squeek.appleskin:appleskin-fabric:$appleskinVersion")

    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:$reiVersion")
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-fabric:$reiVersion")
    if (runRei) {
        modLocalRuntime("me.shedaniel:RoughlyEnoughItems-fabric:$reiVersion")
    }

    modCompileOnly("mezz.jei:jei-$minecraftVersion-fabric-api:$jeiVersion")
    if (runJei) {
        modLocalRuntime("mezz.jei:jei-$minecraftVersion-fabric:$jeiVersion")
    }

	modCompileOnly("dev.emi:emi-fabric:$emiVersion:api")
    if (runEmi) {
	    modLocalRuntime("dev.emi:emi-fabric:$emiVersion")
    }

    if (runSodium) {
        modLocalRuntime("maven.modrinth:sodium:$sodiumVersion")
        modLocalRuntime("maven.modrinth:reeses-sodium-options:$reesesSodiumOptionsVersion")
        modLocalRuntime("maven.modrinth:sodium-extra:$sodiumExtraVersion")
        modLocalRuntime("maven.modrinth:lithium:$lithiumVersion")
        modLocalRuntime("maven.modrinth:ferrite-core:$ferriteCoreVersion")
        modLocalRuntime("maven.modrinth:immediatelyfast:$immediatelyFastVersion")
    }

    testImplementation("net.fabricmc:fabric-loader-junit:$loaderVersion")
}

tasks.processResources {
    val properties = mapOf(
        "version" to project.version,
        "fabricVersion" to project.property("fabric.version"),
        "minecraftVersion" to project.property("minecraft.version"),
    )
    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
    }

    // Minify json resources (https://stackoverflow.com/a/41029113)
    doLast {
        val jsonSlurper = groovy.json.JsonSlurper().setType(groovy.json.JsonParserType.LAX)
        fileTree(mapOf("dir" to outputs.files.asPath, "includes" to listOf("**/*.json", "**/*.mcmeta"))).forEach {
            groovy.json.JsonOutput.toJson(jsonSlurper.parse(it))
        }
    }
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${modName}"}
    }

    manifest {
        attributes(
            "Specification-Title" to modName,
            "Specification-Vendor" to "Team Galacticraft",
            "Specification-Version" to modVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to "${project.version}",
            "Implementation-Vendor" to "Team Galacticraft",
            "Implementation-Timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            "Maven-Artifact" to "${project.group}:${modName}:${project.version}",
        )
    }
}

val checkApiBoundaries by tasks.registering {
    group = "verification"
    description = "Verifies that dev.galacticraft.api contains zero imports from dev.galacticraft.mod."
    val apiDir = file("src/main/java/dev/galacticraft/api")
    inputs.dir(apiDir)
    doLast {
        val violations = mutableListOf<String>()
        apiDir.walkTopDown().filter { it.isFile && it.extension == "java" }.forEach { javaFile ->
            javaFile.useLines { lines ->
                lines.forEachIndexed { idx, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("import dev.galacticraft.mod.") || trimmed.startsWith("import static dev.galacticraft.mod.")) {
                        violations.add("${javaFile.relativeTo(projectDir)}:${idx + 1}: $trimmed")
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("API boundary violation(s) — dev.galacticraft.api must not import dev.galacticraft.mod:\n" + violations.joinToString("\n"))
        }
    }
}

val apiJar by tasks.registering(Jar::class) {
    archiveBaseName.set("${modName}-api")
    from(sourceSets.main.get().output) {
        include("dev/galacticraft/api/**")
        include("dev/galacticraft/impl/**")
    }
    from("LICENSE") {
        rename { "${it}_${modName}-api" }
    }
}

val apiSourcesJar by tasks.registering(Jar::class) {
    archiveBaseName.set("${modName}-api")
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource) {
        include("dev/galacticraft/api/**")
        include("dev/galacticraft/impl/**")
    }
}

tasks.check {
    dependsOn(checkApiBoundaries)
}

tasks.test {
    useJUnitPlatform()
    workingDir = project.file("run")
    dependsOn(tasks.getByName("runGametest"))
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX

    java {
        licenseHeader(processLicenseHeader(rootProject.file("LICENSE")))
        leadingTabsToSpaces()
        trimTrailingWhitespace()
        removeUnusedImports()
        importOrder("", "java", "\\#")
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = modGroup
            artifactId = modName
            version = project.version.toString()

            from(components["java"])

            pom {
                organization {
                    name.set("Team Galacticraft")
                    url.set("https://github.com/TeamGalacticraft")
                }

                scm {
                    url.set("https://github.com/TeamGalacticraft/Galacticraft")
                    connection.set("scm:git:git://github.com/TeamGalacticraft/Galacticraft.git")
                    developerConnection.set("scm:git:git@github.com:TeamGalacticraft/Galacticraft.git")
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/TeamGalacticraft/Galacticraft/issues")
                }

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/TeamGalacticraft/Galacticraft/blob/main/LICENSE")
                    }
                }
            }
        }

        register("mavenApi", MavenPublication::class) {
            groupId = modGroup
            artifactId = "${modName}-api"
            version = project.version.toString()

            artifact(apiJar)
            artifact(apiSourcesJar)

            pom {
                name.set("${modName} API")
                organization {
                    name.set("Team Galacticraft")
                    url.set("https://github.com/TeamGalacticraft")
                }
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/TeamGalacticraft/Galacticraft/blob/main/LICENSE")
                    }
                }
            }
        }
    }

    repositories {
        if (System.getenv().containsKey("NEXUS_REPOSITORY_URL")) {
            maven(System.getenv("NEXUS_REPOSITORY_URL")!!) {
                credentials {
                    username = System.getenv("NEXUS_USER")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "java")

    group = "$modGroup.addons"
    version = rootProject.version

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    repositories {
        mavenLocal()
        rootProject.repositories.forEach { add(it) }
    }

    dependencies {
        compileOnly(rootProject)
    }

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
        options.release.set(21)
        dependsOn(rootProject.tasks.compileJava)
    }
}

fun processLicenseHeader(license: File): String {
    val text = license.readText()
    return "/*\n * " + text.substring(text.indexOf("Copyright"))
        .replace("\n", "\n * ")
        .replace("* \n", "*\n")
        .trim() + "/\n\n"
}
