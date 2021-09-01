package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.*
import com.spicymemes.artifactory.tasks.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

abstract class BaseConfiguration(project: Project) : AbstractModLoaderConfiguration(project) {

    init {
        beforeConfiguration {
            archivesBaseName = findProperty("archivesBaseName")?.toString()
                ?: findProperty("archivesName")?.toString()
                    ?: findProperty("modId")?.toString()
                    ?: name
        }

        configureProject {
            val apiSourceSet = sourceSets.register("api")
            val generatedSourceSet = sourceSets.register("generated") {
                java {
                    setSrcDirs(listOf("$buildDir/generated-sources/kotlin", "$buildDir/generated-sources/java"))
                }
                resources {
                    setSrcDirs(listOf("$buildDir/generated-resources"))
                }
            }

            listOf(apiSourceSet, generatedSourceSet).forEach { sourceSet ->
                sourceSets.matching { it.name == "main" || it.name == "test" }.all {
                    compileClasspath += sourceSet.get().output
                    runtimeClasspath += sourceSet.get().output
                }
            }

            sourceSets.main {
                resources.srcDir("src/generated/resources")
            }

            configurations.apiCompileClasspath {
                extendsFrom(configurations.compileClasspath.get())
            }

            tasks.named(generatedSourceSet.get().compileJavaTaskName) {
                dependsOn(tasks.withType(GenerateJavaModInfo::class))
            }

            if (project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
                plugins.withType(KotlinPluginWrapper::class) {
                    tasks.named("compileGeneratedKotlin") {
                        dependsOn(tasks.withType(GenerateKotlinModInfo::class))
                    }
                }
            }

            tasks.jar {
                jarConfig(archivesVersion)
                // because the jar task already includes the main SourceSet
                sourceSets.nonTestSets.matching { it.name != "main" }.forEach {
                    from(it.output)
                }
            }

            val sourcesJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveClassifier.set("sources")
                sourceSets.nonTestSets.forEach {
                    from(it.allSource)
                }
            }

            val apiJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                from(sourceSets["api"].output)
            }

            val apiSourcesJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveClassifier.set("sources")
            }

            tasks.assemble {
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
