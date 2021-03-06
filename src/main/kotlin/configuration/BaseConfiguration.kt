package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.jarConfig
import com.spicymemes.artifactory.tasks.GenerateJavaModInfo
import com.spicymemes.artifactory.tasks.GenerateKotlinModInfo
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

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

//            sourceSets.main {
//                resources.srcDir("src/generated/resources")
//            }

            configurations.apiCompileClasspath {
                extendsFrom(configurations.compileClasspath.get())
            }

            generatedSourceSet {
                tasks.named(compileJavaTaskName) {
                    dependsOn(tasks.withType(GenerateJavaModInfo::class))
                }
                if (project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
                    plugins.withType(KotlinPluginWrapper::class) {
                        tasks.named("compileGeneratedKotlin") {
                            dependsOn(tasks.withType(GenerateKotlinModInfo::class))
                        }
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
