import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.15.0"
    id("net.researchgate.release") version "2.8.1"
}

group = "com.github.masterzach32"

val isRelease = !version.toString().endsWith("-SNAPSHOT")
val isSnapshot = !isRelease

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://maven.fabricmc.net")
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("net.minecraftforge.gradle:ForgeGradle:5.1.+")
    compileOnly("fabric-loom:fabric-loom.gradle.plugin:0.9.+")
    implementation("com.squareup:kotlinpoet:1.9.0")
    implementation("com.squareup:javapoet:1.13.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

gradlePlugin {
    plugins {
        create(project.name) {
            id = "${project.group}.${project.name}"
            displayName = "Minecraft Artifactory"
            description = "Helps configure multi-target minecraft mods. Supports fabric and forge out of the box."
            implementationClass = "com.spicymemes.artifactory.ArtifactoryPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/Masterzach32/artifactory/"
    vcsUrl = "https://github.com/Masterzach32/artifactory/"
    tags = listOf("minecraft")
}

tasks.publishPlugins {
    onlyIf { isRelease }
}

publishing {
    repositories {
        val mavenUsername: String? by project
        val mavenPassword: String? by project
        if (mavenUsername != null && mavenPassword != null) {
            maven {
                if (isRelease) {
                    name = "Release"
                    url = uri("https://maven.masterzach32.net/artifactory/gradle-plugin-releases/")
                } else {
                    name = "Snapshot"
                    url = uri("https://maven.masterzach32.net/artifactory/gradle-plugin-snapshots/")
                }
                credentials {
                    username = mavenUsername
                    password = mavenPassword
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
