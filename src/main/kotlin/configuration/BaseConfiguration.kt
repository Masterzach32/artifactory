package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*

abstract class BaseConfiguration(project: Project) : AbstractModLoaderConfiguration(project) {

    init {
        beforeConfiguration {
            archivesBaseName = findProperty("archivesBaseName")?.toString()
                ?: findProperty("archivesName")?.toString()
                    ?: findProperty("modId")?.toString()
                    ?: name
        }

        configureProject {
            val apiSourceSet = sourceSets.create("api")
            sourceSets["main"].apply {
                compileClasspath += apiSourceSet.output
                runtimeClasspath += apiSourceSet.output
            }
            sourceSets["test"].apply {
                compileClasspath += apiSourceSet.output
                runtimeClasspath += apiSourceSet.output
            }

            tasks.named("jar", Jar::class) {
                jarConfig(archivesVersion)
                from(sourceSets["api"].output)
            }

            val sourcesJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveClassifier.set("sources")
                sourceSets.modSets.forEach {
                    from(it.allSource)
                }
            }

            val apiJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
            }

            val apiSourcesJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveClassifier.set("sources")
            }

            tasks.named("assemble") {
                dependsOn(sourcesJar, apiJar, apiSourcesJar)
            }

            plugins.withType<MavenPublishPlugin> {
                configure<PublishingExtension> {
                    publications {
                        register<MavenPublication>("mod") {
                            version = archivesVersion
                        }
                        register<MavenPublication>("api") {
                            version = archivesVersion
                        }
                    }
                }
            }
        }
    }
}
