plugins {
    kotlin("jvm") version "1.5.21"
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.15.0"
    id("net.researchgate.release") version "2.8.1"
}

group = "com.spicymemes"

val isRelease = !version.toString().endsWith("-SNAPSHOT")
val isSnapshot = !isRelease

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://maven.fabricmc.net")
}

dependencies {
    implementation(gradleKotlinDsl())
//    implementation("net.minecraftforge.gradle:ForgeGradle:5.1.+")
//    implementation("fabric-loom:fabric-loom.gradle.plugin:0.9.+")
}

gradlePlugin {
    plugins {
        create(project.name) {
            id = "com.spicymemes.artifactory"
            displayName = "Minecraft Artifactory"
            description = "Helps configure multi-target minecraft mods. Supports fabric and forge out of the box."
            implementationClass = "com.spicymemes.artifactory.ArtifactoryPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/SummerModVenture"
    vcsUrl = "https://github.com/SummerModVenture/artifactory"
    tags = listOf("minecraft")
}

tasks.publishPlugins {
    onlyIf { isRelease }
}

publishing {
    repositories {
        if (isSnapshot) {
            val mavenUsername: String? by project
            val mavenPassword: String? by project
            if (mavenUsername != null && mavenPassword != null) {
                maven {
                    name = "Snapshots"
                    url = uri("https://maven.masterzach32.net/artifactory/gradle-plugins/")
                    credentials {
                        username = mavenUsername
                        password = mavenPassword
                    }
                }
            }
        }
    }
}

release {
    preTagCommitMessage = "Release version"
    tagCommitMessage = "Release version"
    newVersionCommitMessage = "Next development version"
}