package com.spicymemes.artifactory.configuration

import net.fabricmc.loom.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*

class CommonConfiguration(project: Project) : AbstractModLoaderConfiguration(project) {

    override fun Project.configureConfigurations() {
        configurations["apiImplementation"].extendsFrom(configurations["compileClasspath"])
    }

    override fun Project.configureTasks() {
        val apiJar by tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
        }
        val apiSourcesJar by tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
        }
        val remapJar by tasks.existing
        val remapSourcesJar by tasks.existing

        project.plugins.withType<MavenPublishPlugin> {
            project.configure<PublishingExtension> {
                publications {
                    named<MavenPublication>("api") {
                        artifactId = apiArchivesBaseName
                        artifact(apiJar) {
                            builtBy(remapJar)
                        }
                        artifact(apiSourcesJar) {
                            builtBy(remapSourcesJar)
                        }
                    }

                    remove(getByName("mod"))
                }
            }
        }
    }
}
