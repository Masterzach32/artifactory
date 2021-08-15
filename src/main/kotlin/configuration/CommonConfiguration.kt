package com.spicymemes.artifactory.configuration

import net.fabricmc.loom.task.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*

class CommonConfiguration(project: Project) : AbstractModLoaderConfiguration(project) {

    override fun Project.configureConfigurations() {
        configurations["apiImplementation"].extendsFrom(configurations["compileClasspath"])
    }

    override fun Project.configureTasks() {
        val remapJar by tasks.existing(RemapJarTask::class) {
            archiveVersion.set(archivesVersion)
        }
        val remapSourcesJar by tasks.existing(RemapSourcesJarTask::class)

        val apiJar by tasks.existing(Jar::class)
        val apiSourcesJar by tasks.existing(Jar::class)
        project.afterEvaluate {
            apiJar {
                val remapJar = remapJar.get()
                dependsOn(remapJar)
                archiveBaseName.set(apiArchivesBaseName)
                from(zipTree(remapJar.archiveFile)) {
                    include("**/api/**")
                }
            }

            apiSourcesJar {
                val remapSourcesJar = remapSourcesJar.get()
                dependsOn(remapSourcesJar)
                archiveBaseName.set(apiArchivesBaseName)
                from(zipTree(remapSourcesJar.output)) {
                    include("**/api/**")
                }
            }
        }

        plugins.withType<MavenPublishPlugin> {
            configure<PublishingExtension> {
                publications {
                    named<MavenPublication>("api") {
                        artifactId = apiArchivesBaseName
                        artifact(apiJar)
                        artifact(apiSourcesJar)
                    }

                    remove(getByName("mod"))
                }
            }
        }
    }
}
