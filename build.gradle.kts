plugins {
    java
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

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
            toolchain.languageVersion.set(JavaLanguageVersion.of(17)) // ✅ force Java 21 everywhere
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(8)
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
}

tasks {
    javadoc {
        options.encoding = "UTF-8"
    }

    named<xyz.jpenilla.runpaper.task.RunServer>("runServer") {
        minecraftVersion("1.17")
    }
}
