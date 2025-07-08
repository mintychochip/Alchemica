dependencies {
    api(project(":versions:base")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}
