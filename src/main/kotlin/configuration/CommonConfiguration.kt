package com.spicymemes.artifactory.configuration

import net.fabricmc.loom.task.*
import net.fabricmc.loom.util.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*

class CommonConfiguration(project: Project) : BaseConfiguration(project) {

    init {
        configureProject {
            val remapJar by tasks.existing(RemapJarTask::class) {
                archiveVersion.set(archivesVersion)
            }
            val remapSourcesJar by tasks.existing(RemapSourcesJarTask::class)

            val apiJar by tasks.existing(Jar::class) {
                archiveBaseName.set(apiArchivesBaseName)
                archiveClassifier.set("dev")
                from(sourceSets["api"].output)
            }
            val remapApiJar by tasks.registering(RemapJarTask::class) {
                description = "Remaps the built project api jar to intermediary mappings."
                group = Constants.TaskGroup.FABRIC
                dependsOn(apiJar)
                archiveVersion.set(archivesVersion)
                archiveBaseName.set(apiArchivesBaseName)
                addNestedDependencies.set(true)
                input.set(apiJar.flatMap { it.archiveFile })
            }
            val apiSourcesJar by tasks.existing(Jar::class)
            project.afterEvaluate {
                apiSourcesJar {
                    val remapSourcesJar = remapSourcesJar.get()
                    dependsOn(remapSourcesJar)
                    archiveBaseName.set(apiArchivesBaseName)
                    from(zipTree(remapSourcesJar.output)) { include("**/api/**") }
                }
            }

            tasks.named("assemble") {
                dependsOn(remapApiJar)
            }

            plugins.withType<MavenPublishPlugin> {
                configure<PublishingExtension> {
                    publications {
                        named<MavenPublication>("api") {
                            artifactId = apiArchivesBaseName
                            artifact(remapApiJar)
                            artifact(apiSourcesJar)
                        }

                        remove(getByName("mod"))
                    }
                }
            }
        }
    }
}
