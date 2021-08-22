package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.*
import com.spicymemes.artifactory.tasks.*
import org.gradle.api.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
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
                    setSrcDirs(listOf("$buildDir/generated-sources/resources"))
                }
            }

            listOf(apiSourceSet, generatedSourceSet).forEach { sourceSet ->
                sourceSets.matching { it.name == "main" || it.name == "test" }.all {
                    compileClasspath += sourceSet.get().output
                    runtimeClasspath += sourceSet.get().output
                }
            }

            configurations.named(sourceSets["api"].compileClasspathConfigurationName) {
                extendsFrom(project.configurations[sourceSets["main"].compileClasspathConfigurationName])
            }

            tasks.named(generatedSourceSet.get().compileJavaTaskName) {
                dependsOn(tasks.withType(GenerateJavaModInfo::class))
            }

            plugins.withType(KotlinPluginWrapper::class) {
                the<KotlinJvmProjectExtension>().target {  }
                tasks.named("compileGeneratedKotlin") {
                    dependsOn(tasks.withType(GenerateKotlinModInfo::class))
                }
            }

            tasks.named("jar", Jar::class) {
                jarConfig(archivesVersion)
                from(apiSourceSet.get().output)
                from(generatedSourceSet.get().output)
            }

            val sourcesJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveClassifier.set("sources")
                sourceSets.modSets.forEach {
                    from(it.allSource)
                }
                from(generatedSourceSet.get().allSource)
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
