plugins {
    id("net.fabricmc.fabric-loom")
    id("maven-publish")
}

version = "${property("mod.version")}+${sc.current.version}"
group = property("mod.group") as String

base.archivesName = property("mod.id") as String

val requiredJava = when {
    sc.current.parsed >= "26" -> 25
    sc.current.parsed >= "1.20.5" -> 21
    sc.current.parsed >= "1.18" -> 21
    sc.current.parsed >= "1.17" -> 21
    else -> 8
}
java.toolchain.languageVersion.set(JavaLanguageVersion.of(requiredJava))
java.withSourcesJar()

repositories {
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    testImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    testImplementation(sourceSets.main.get().output)
}

tasks {
    processResources {
        notCompatibleWithConfigurationCache("I don't know why...")
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("minecraft_version_range"))

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to project.property("mod.version"),
                    "minecraft" to project.property("minecraft_version_range")))
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("jar")
    }
}

// When building 26.x while shared src/ contains unusedN (switched from an older version),
// restore them to JDK 22+ unnamed _ in generated sources before compilation.
val restoreForBuild = tasks.register("restoreUnnamedVarsForBuild") {
    group = "stonecutter"
    description = "Restores unusedN identifiers back to JDK 22+ unnamed variable (_) in generated sources"
    notCompatibleWithConfigurationCache("transforms generated source files in place")
    @Suppress("OPT_IN_USAGE")
    dependsOn(sc.tasks.generate.values)
    doLast {
        val renamedPattern = Regex("""\bunused\d+\b""")
        val genDir = sc.tasks.generatedSourcesDir.get().asFile
        genDir.walkTopDown().filter { it.isFile && it.extension == "java" }.forEach { file ->
            val original = file.readText()
            val transformed = renamedPattern.replace(original, "_")
            if (transformed != original) file.writeText(transformed)
        }
    }
}
tasks.withType<JavaCompile>().configureEach { dependsOn(restoreForBuild) }

sourceSets {
    named("test") {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
}

loom {
    runs {
        register("gameTest") {
            server()
            name("Game Test")
            source(sourceSets.test.get())
            property("fabric-api.gametest")
            property("fabric-api.gametest.report-file", "${project.layout.buildDirectory.get()}/junit.xml")
        }
    }
}

// The only tests are Fabric server game tests (run via runGameTest); there are no JUnit tests,
// so disable the empty JUnit task to keep `build` clean.
tasks.named<Test>("test") { enabled = false }

apply(from = "../../stonecutter-swaps.gradle.kts")
@Suppress("UNCHECKED_CAST")
val swapMap = extra["swaps"] as Map<String, Map<String, String>>
stonecutter {
    replacements {
        for ((version, swaps) in swapMap) {
            string(sc.current.parsed >= version) {
                for ((from, to) in swaps) {
                    replace(from, to)
                }
            }
        }
    }
}
