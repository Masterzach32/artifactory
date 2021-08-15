package com.spicymemes.artifactory

import com.spicymemes.artifactory.configuration.*
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

        val mcVersion: String? = project.the<VersionCatalog>().findVersion("minecraft").orElse(null)?.requiredVersion
            ?: project.findProperty("mcVersion")?.toString()
            ?: project.findProperty("minecraftVersion")?.toString()
        val archivesVersion: String = project.rootProject.the<ExtraPropertiesExtension>().get("archivesVersion")?.toString() ?:
            if (mcVersion != null)
                "$mcVersion-${project.version}"
            else
                "${project.version}"

        val sourceSets = project.the<SourceSetContainer>()

        val apiSourceSet = sourceSets.create("api")
        sourceSets["main"].apply {
            compileClasspath += apiSourceSet.output
            runtimeClasspath += apiSourceSet.output
        }
        sourceSets["test"].apply {
            compileClasspath += apiSourceSet.output
            runtimeClasspath += apiSourceSet.output
        }

        val archivesBaseName: String = project.findProperty("archivesBaseName")?.toString()
            ?: project.findProperty("archivesName")?.toString()
            ?: project.findProperty("modId")?.toString()
            ?: project.name
        project.the<BasePluginExtension>().archivesName.set(archivesBaseName)

        val jar by project.tasks.existing(Jar::class) {
            jarConfig(archivesVersion)
            from(sourceSets["api"].output)
        }

        val sourcesJar by project.tasks.registering(Jar::class) {
            jarConfig(archivesVersion)
            archiveClassifier.set("sources")
            sourceSets.modSets.forEach {
                from(it.allSource)
            }
        }

        val apiJar by project.tasks.registering(Jar::class) {
            jarConfig(archivesVersion)
        }

        val apiSourcesJar by project.tasks.registering(Jar::class) {
            jarConfig(archivesVersion)
            archiveClassifier.set("sources")
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

        val commonProject: Project? = project.rootProject.subprojects.firstOrNull {
            it.name == "common" || it.the<ArtifactoryExtension>().configuration is CommonConfiguration
        }

        val projectConfig: AbstractModLoaderConfiguration = when (project.name) {
            "common" -> CommonConfiguration(project)
            "fabric" -> commonProject?.let { FabricConfiguration(project, it) }
                ?: error(
                    "Cannot setup named configuration for fabric project, because the common project " +
                        "configuration could not be located."
                )
            "forge" -> commonProject?.let { ForgeConfiguration(project, it) }
                ?: error(
                    "Cannot setup named configuration for forge project, because the common project " +
                        "configuration could not be located."
                )
            else -> UnknownConfiguration(project)
        }

        projectConfig.configure()

        project.extensions.create<ArtifactoryExtension>(
            "artifactory",
            project,
            projectConfig
        )
    }
}
