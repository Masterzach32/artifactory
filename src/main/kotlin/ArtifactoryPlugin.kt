package com.spicymemes.artifactory

import com.spicymemes.artifactory.configuration.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.kotlin.dsl.*

class ArtifactoryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class)

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
