dependencies {
    api(project(":versions:base")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")
}
