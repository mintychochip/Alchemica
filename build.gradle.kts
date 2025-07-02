plugins {
    java
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.aincraft"
version = "1.1"

allprojects {
    group = "org.aincraft"
    version = "1.1"

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21)) // ✅ force Java 21 everywhere
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21) // ✅ generates Java 21-compatible bytecode
    }
}

subprojects {
    apply(plugin = "java")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(project(":versions:v1_17_R1"))
    implementation(project(":versions:v1_21_R1"))
}

tasks {
    javadoc {
        options.encoding = "UTF-8"
    }

    named<xyz.jpenilla.runpaper.task.RunServer>("runServer") {
        minecraftVersion("1.21")
    }

    named<ProcessResources>("processResources") {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveFileName.set("${project.name}-${project.version}.jar")
        destinationDirectory.set(file("C:\\Users\\justi\\Desktop\\paper\\plugins"))
    }
}
