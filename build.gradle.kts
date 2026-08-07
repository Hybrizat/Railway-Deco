plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.28-beta"
}

version = "1.0.0"
group = "hybrizat"

base {
    archivesName = "rail_deco"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

neoForge {
    // NeoForge for Minecraft 1.21.1
    version = "21.1.206"

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
            programArgument("--nogui")
        }
    }

    mods {
        create("rail_deco") {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(mapOf("version" to project.version))
    }
}
