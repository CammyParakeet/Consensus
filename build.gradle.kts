import net.minecrell.pluginyml.paper.PaperPluginDescription
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    `java-library`
    id("io.freefair.lombok") version "8.11"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"

    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "8.3.5"
    id("net.kyori.indra.git") version "3.1.3"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.triumphteam.dev/snapshots")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // Menus
    implementation("dev.triumphteam:triumph-gui-paper:4.0.0-SNAPSHOT")

    // Commands
    implementation("org.incendo:cloud-paper:2.0.0-beta.10")
    implementation("org.incendo:cloud-annotations:2.0.0")

    // DI
    implementation("com.google.inject:guice:7.0.0")
    implementation("com.google.inject.extensions:guice-assistedinject:7.0.0")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    // PAPI
    //compileOnly("me.clip:placeholderapi:2.11.6")

    // Storage
    implementation("org.jdbi:jdbi3-core:3.49.5")
    implementation("org.jdbi:jdbi3-sqlobject:3.49.5")
    implementation("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.46.0.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.6")
    }

    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs = listOf("-parameters")
    }
}

tasks.named<RunServer>("runServer") {
    systemProperty("com.mojang.eula.agree", "true")
}

configure<PaperPluginDescription> {
    name = "Consensus"

    apiVersion = "1.21"
    version = "Git-${indraGit.commit()?.name?.take(7) ?: "unknown"}"
    //version = "1.0.0"

    main = "com.glance.codex.platform.paper.ConsensusPlugin"

//    serverDependencies {
//        create("PlaceholderAPI") {
//            required = false
//            load = PaperPluginDescription.RelativeLoadOrder.AFTER
//        }
//    }
}