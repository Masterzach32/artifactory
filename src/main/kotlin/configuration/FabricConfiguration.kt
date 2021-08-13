package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.*

class FabricConfiguration(project: Project, commonProject: Project) : AbstractModLoaderConfiguration(project) {

    private val commonSourceSets = commonProject.the<SourceSetContainer>()

    override fun Project.beforeConfiguration() {
        archivesBaseName += "-fabric"
    }

    override fun Project.configureSourceSets() {
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
    }

    override fun Project.configureConfigurations() {
        configurations["apiImplementation"].extendsFrom(configurations["compileClasspath"])
    }

    override fun Project.configureTasks() {
        tasks.named<ProcessResources>("processResources") {
            duplicatesStrategy = DuplicatesStrategy.FAIL
            inputs.property("version", project.version)
            filesMatching("fabric.mod.json") { expand("version" to project.version) }
            from(commonSourceSets["main"].resources)
        }

        val jar by tasks.existing(Jar::class) {
            fromOutputs(commonSourceSets.modSets)
        }

        val sourcesJar by tasks.existing(Jar::class) {
            fromSources(commonSourceSets.modSets)
        }

        val apiJar by tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
            from(commonSourceSets["api"].output)
        }

        val apiSourcesJar by tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
            from(commonSourceSets["api"].allSource)
        }

        val remapJar by tasks.existing
        val remapSourcesJar by tasks.existing
        plugins.withType<MavenPublishPlugin> {
            configure<PublishingExtension> {
                publications {
                    named<MavenPublication>("mod") {
                        artifactId = archivesBaseName
                        artifact(remapJar) {
                            builtBy(remapJar)
                        }
                        artifact(sourcesJar) {
                            builtBy(remapSourcesJar)
                        }
                    }
                    named<MavenPublication>("api") {
                        artifactId = apiArchivesBaseName
                        artifact(apiJar) {
                            builtBy(remapJar)
                        }
                        artifact(apiSourcesJar) {
                            builtBy(remapSourcesJar)
                        }
                    }
                }
            }
        }
    }
}
