dependencies {
    api(project(":api")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    api("org.spigotmc:spigot-api:1.21.7-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
}
