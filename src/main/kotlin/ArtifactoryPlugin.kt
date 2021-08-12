package com.spicymemes.artifactory

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.plugins.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*

class ArtifactoryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class)
        val sourceSets = project.extensions.getByType<SourceSetContainer>()

        val apiSourceSet = sourceSets.create("api")
        sourceSets["main"].apply {
            compileClasspath += apiSourceSet.output
            runtimeClasspath += apiSourceSet.output
        }
        sourceSets["test"].apply {
            compileClasspath += apiSourceSet.output
            runtimeClasspath += apiSourceSet.output
        }

        if (project.name == "forge") {
            project.configurations.register("library").also { library ->
                project.configurations.named("implementation") {
                    extendsFrom(library.get())
                }
            }
        }

        val archivesBaseName: String = project.findProperty("archivesBaseName")?.toString()
            ?: project.findProperty("archivesName")?.toString()
            ?: project.findProperty("modId")?.toString()
            ?: project.name
        project.the<BasePluginExtension>().archivesName.set(archivesBaseName)

        val mcVersion: String? = project.the<VersionCatalog>().findVersion("minecraft").orElse(null)?.requiredVersion
            ?: project.findProperty("mcVersion")?.toString()
            ?: project.findProperty("minecraftVersion")?.toString()
        val archivesVersion: String = if (mcVersion != null)
            "$mcVersion-${project.version}"
        else
            "${project.version}"

        project.extensions.create<ArtifactoryExtension>(
            "artifactory",
            project,
            archivesVersion,
        )

        val jar by project.tasks.existing(Jar::class) {
            jarConfig(archivesVersion)
            from(sourceSets["api"].output)
        }

        val sourcesJar by project.tasks.registering(Jar::class) {
            jarConfig(archivesVersion)
            archiveClassifier.set("sources")
            fromSources(sourceSets.modSets)
        }

        val apiJar by project.tasks.registering(Jar::class) {
            jarConfig(archivesVersion)
            from(sourceSets["api"].output)
        }

        val apiSourcesJar by project.tasks.registering(Jar::class) {
            jarConfig(archivesVersion)
            archiveClassifier.set("sources")
            from(sourceSets["api"].allSource)
        }

        project.tasks.named("assemble") {
            dependsOn(sourcesJar, apiJar, apiSourcesJar)
        }

        project.plugins.withType<MavenPublishPlugin> {
            project.configure<PublishingExtension> {
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
