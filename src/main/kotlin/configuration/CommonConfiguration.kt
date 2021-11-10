package com.spicymemes.artifactory.configuration

import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask
import net.fabricmc.loom.util.Constants
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.*

class CommonConfiguration(project: Project) : BaseConfiguration(project) {

    init {
        configureProject {
            tasks.named("remapJar", RemapJarTask::class) {
                archiveVersion.set(archivesVersion)
            }
            val remapSourcesJar by tasks.existing(RemapSourcesJarTask::class)

            tasks.apiJar {
                archiveBaseName.set(apiArchivesBaseName)
                archiveClassifier.set("dev")
            }
            val remapApiJar by tasks.registering(RemapJarTask::class) {
                description = "Remaps the built project api jar to intermediary mappings."
                group = Constants.TaskGroup.FABRIC
                dependsOn(tasks.apiJar)
                archiveVersion.set(archivesVersion)
                archiveBaseName.set(apiArchivesBaseName)
                input.set(tasks.apiJar.flatMap { it.archiveFile })
                classpath(configurations.apiCompileClasspath.get())
            }
            project.afterEvaluate {
                tasks.apiSourcesJar {
                    val remapSourcesJar = remapSourcesJar.get()
                    dependsOn(remapSourcesJar)
                    archiveBaseName.set(apiArchivesBaseName)
                    from(zipTree(remapSourcesJar.output)) { include("**/api/**") }
                }
            }

            artifacts {
                add(configurations.modApiJars.name, remapApiJar)
                add(configurations.modApiJars.name, tasks.apiSourcesJar)
            }

            tasks.assemble {
                dependsOn(remapApiJar)
            }

            plugins.withType<MavenPublishPlugin> {
                configure<PublishingExtension> {
                    publications {
                        named<MavenPublication>("mod") {
                            artifactId = apiArchivesBaseName
                            from(components["mod"])
                        }
                    }
                }
            }
        }
    }
}
