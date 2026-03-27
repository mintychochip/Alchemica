dependencies {
    api(project(":versions:base")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    // LegacyPotionProvider lives in v1_12_R1; compile-only so v1_8_PotionProvider can extend it.
    // Both modules end up in the same shadow jar at runtime, so the class is always present.
    compileOnly(project(":versions:v1_12_R1")) {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    // Compile against 1.12 spigot-api so that CauldronLevelChangeEvent is on the classpath
    // (required by the ICauldronProvider interface). Only 1.8-compatible APIs are used
    // in method bodies; getOldLevel/getNewLevel throw UnsupportedOperationException
    // since CauldronLevelChangeEvent does not fire on 1.8 servers.
    compileOnly("org.spigotmc:spigot-api:1.12-R0.1-SNAPSHOT")
}
