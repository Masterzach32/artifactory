package com.spicymemes.artifactory

import com.spicymemes.artifactory.configuration.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.internal.JavaConfigurationVariantMapping
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

class ArtifactoryPlugin @Inject constructor(@Inject private val factory: SoftwareComponentFactory) : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class)

        project.registerConfigurations()

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

    private fun Project.registerConfigurations() {
        val modRuntimeElements by project.configurations.creating {
            isCanBeConsumed = true
            isCanBeResolved = false
            description = "Contains the mod jars from this project."
        }

        val modApiElements by project.configurations.creating {
            isCanBeConsumed = true
            isCanBeResolved = false
            description = "Contains the mod api jars from this project."
            attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, "jar")
        }

        registerSoftwareComponents(modRuntimeElements, modApiElements)
    }

    private fun Project.registerSoftwareComponents(modJarsConfiguration: Configuration, modApiJarsConfiguration: Configuration) {
        val modComponent = factory.adhoc("mod")

        modComponent.addVariantsFromConfiguration(modApiJarsConfiguration, JavaConfigurationVariantMapping("compile", false))
        modComponent.addVariantsFromConfiguration(modJarsConfiguration, JavaConfigurationVariantMapping("runtime", false))

        components.add(modComponent)
    }
}
