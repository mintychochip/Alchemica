plugins {
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}
dependencies {
    implementation(project(":api"))
    implementation("org.aincraft:utilities-common:2026.08.27")
    implementation("org.aincraft:utilities-db-sql:2026.08.27")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation(project(":versions:base")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    implementation(project(":versions:v1_8")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    implementation(project(":versions:v1_12_R1")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    implementation(project(":versions:v1_13_R0")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("com.google.guava:guava:33.3.1-jre")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.7.0")
}


// Allow resolving paper-api/mockbukkit and Utilities SQL (JVM 25) on compile/test classpaths.
listOf("compileClasspath", "testCompileClasspath", "testRuntimeClasspath").forEach { configName ->
    configurations.named(configName) {
        attributes {
            attribute(
                org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                25
            )
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-Xmx512m")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(25)
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}

tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(25)
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Multi-Release"] = "true"
    }
    dependsOn(":versions:paper:jar")
    from(zipTree(project(":versions:paper").tasks.named("jar").get().outputs.files.singleFile)) {
        into("META-INF/versions/17")
    }
}

tasks {
    named<ProcessResources>("processResources") {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    named<xyz.jpenilla.runpaper.task.RunServer>("runServer") {
        val toolchains = project.extensions.getByType<JavaToolchainService>()
        javaLauncher.set(
            toolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        )
        minecraftVersion("1.21.11")
    }


    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveFileName.set("${rootProject.name}-${project.version}.jar")
    }
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set("Snapshot")
        id.set("Alchemica")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            paper {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                val versions: List<String> = (rootProject.property("paperVersions") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
                dependencies {

                }
            }

        }
    }
}
