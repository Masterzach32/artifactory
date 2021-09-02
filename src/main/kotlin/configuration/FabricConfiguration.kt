package com.spicymemes.artifactory.configuration

import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask
import net.fabricmc.loom.util.Constants
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.*

class FabricConfiguration(project: Project, commonProject: Project) : BaseConfiguration(project) {

    init {
        beforeConfiguration {
            archivesBaseName += "-fabric"
        }

        configureProject {
            sourceSets.api {
                commonProject.sourceSets["api"].also { commonApi ->
                    compileClasspath += commonApi.output
                    runtimeClasspath += commonApi.output
                }
            }
            sourceSets.main {
                commonProject.sourceSets.nonTestSets.forEach { sourceSet ->
                    compileClasspath += sourceSet.output
                    runtimeClasspath += sourceSet.output
                }
            }

            tasks.processResources {
                duplicatesStrategy = DuplicatesStrategy.FAIL
                inputs.property("version", project.version)
                filesMatching("fabric.mod.json") { expand("version" to project.version) }
//                from(commonProject.sourceSets["main"].resources)
            }

            tasks.jar {
                commonProject.sourceSets.nonTestSets.forEach {
                    from(it.output)
                }
            }
            tasks.sourcesJar {
                commonProject.sourceSets.nonTestSets.forEach {
                    from(it.allSource)
                }
            }

            val remapJar by tasks.existing(RemapJarTask::class) {
                archiveVersion.set(archivesVersion)
            }
            val remapSourcesJar by tasks.existing(RemapSourcesJarTask::class)

            tasks.apiJar {
                archiveBaseName.set(apiArchivesBaseName)
                archiveClassifier.set("dev")
                from(commonProject.sourceSets["api"].output)
            }
            val remapApiJar by tasks.registering(RemapJarTask::class) {
                description = "Remaps the built project api jar to intermediary mappings."
                group = Constants.TaskGroup.FABRIC
                dependsOn(tasks.apiJar)
                archiveVersion.set(archivesVersion)
                archiveBaseName.set(apiArchivesBaseName)
                addNestedDependencies.set(true)
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

            tasks.assemble {
                dependsOn(remapApiJar)
            }

            plugins.withType<MavenPublishPlugin> {
                configure<PublishingExtension> {
                    publications {
                        named<MavenPublication>("mod") {
                            artifactId = archivesBaseName
                            artifact(remapJar).builtBy(remapJar)
                            artifact(tasks.sourcesJar).builtBy(remapSourcesJar)
                        }
                        named<MavenPublication>("api") {
                            artifactId = apiArchivesBaseName
                            artifact(remapApiJar).builtBy(remapApiJar)
                            artifact(tasks.apiSourcesJar)
                        }
                    }
                }
            }
        }
    }
}
