package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.*
import net.fabricmc.loom.task.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.*

class FabricConfiguration(project: Project, commonProject: Project) : BaseConfiguration(project) {

    private val commonSourceSets = commonProject.the<SourceSetContainer>()

    init {
        beforeConfiguration {
            archivesBaseName += "fabric"
        }

        configureProject {
            sourceSets["api"].apply {
                commonSourceSets["api"].also { commonApi ->
                    compileClasspath += commonApi.output
                    runtimeClasspath += commonApi.output
                }
            }
            sourceSets["main"].apply {
                commonSourceSets.filter { it.name != "test" }.forEach { sourceSet ->
                    compileClasspath += sourceSet.output
                    runtimeClasspath += sourceSet.output
                }
            }

            configurations["apiImplementation"].extendsFrom(configurations["compileClasspath"])

            tasks.named<ProcessResources>("processResources") {
                duplicatesStrategy = DuplicatesStrategy.FAIL
                inputs.property("version", project.version)
                filesMatching("fabric.mod.json") { expand("version" to project.version) }
                from(commonSourceSets["main"].resources)
            }

            tasks.named("jar", Jar::class) {
                commonSourceSets.modSets.forEach {
                    from(it.output)
                }
            }
            val sourcesJar by tasks.existing(Jar::class) {
                commonSourceSets.modSets.forEach {
                    from(it.allSource)
                }
            }

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
                        named<MavenPublication>("mod") {
                            artifactId = archivesBaseName
                            artifact(remapJar)
                            artifact(sourcesJar) {
                                builtBy(remapSourcesJar)
                            }
                        }
                        named<MavenPublication>("api") {
                            artifactId = apiArchivesBaseName
                            artifact(apiJar)
                            artifact(apiSourcesJar)
                        }
                    }
                }
            }
        }
    }
}
