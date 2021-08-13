package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.*
import net.fabricmc.loom.task.*
import net.fabricmc.loom.util.*
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
        val apiJar by tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
        }
        val apiSourcesJar by tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
        }

        val remapJar by tasks.existing(RemapJarTask::class) {
            jarConfig(archivesVersion)
        }
        val remapSourcesJar by tasks.existing(RemapSourcesJarTask::class) {
            jarConfig(apiSourcesJar.get())
        }
        val remapApiJar by tasks.registering(RemapJarTask::class) {
            jarConfig(archivesVersion)
            dependsOn(apiJar)
            group = Constants.TaskGroup.FABRIC
            input.set(apiJar.flatMap { it.archiveFile })
            addNestedDependencies.set(true)
        }
        val remapApiSourcesJar by tasks.registering(RemapSourcesJarTask::class) {
            jarConfig(apiSourcesJar.get())
            dependsOn(apiSourcesJar)
            group = Constants.TaskGroup.FABRIC
        }

        project.tasks.named("assemble") {
            dependsOn(remapApiJar, remapApiSourcesJar)
        }

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
