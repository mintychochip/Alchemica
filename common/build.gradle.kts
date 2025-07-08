import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
dependencies {
    implementation(project(":api"))
    implementation(project(":versions:base")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    implementation(project(":versions:v1_12_R1")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT") // or lowest supported
    compileOnly("org.jetbrains:annotations:24.1.0")
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

//    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
//        archiveFileName.set("${project.name}-${project.version}.jar")
//        destinationDirectory.set(file("C:\\Users\\justi\\Desktop\\paper\\plugins"))
//    }
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set("Snapshot")
        id.set("Alchemica")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
           /* register(Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                val versions: List<String> = (property("paperVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }*/
        }
    }
}
