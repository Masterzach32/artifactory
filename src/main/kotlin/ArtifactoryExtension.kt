package com.spicymemes.artifactory

import com.spicymemes.artifactory.configuration.*
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

open class ArtifactoryExtension(
    private val project: Project,
    configuration: AbstractModLoaderConfiguration
) {

    private var _configuration: AbstractModLoaderConfiguration = configuration
    val configuration: AbstractModLoaderConfiguration
        get() = _configuration

    /**
     * Setup this project as a common module. Requires that the `fabric-loom` plugin be applied.
     */
    fun common() {
        checkIsSetup("common")
        if (!project.plugins.hasPlugin("fabric-loom"))
            error("Project ${project.name} of target type \"common\" needs to have the fabric-loom plugin applied.")

        _configuration = CommonConfiguration(project)
        configuration.configure()
    }

    /**
     * Setup this project as a fabric module. Requires that the `fabric-loom` plugin be applied.
     */
    fun fabric(commonProject: Project) {
        checkIsSetup("fabric")
        if (!project.plugins.hasPlugin("fabric-loom"))
            error("Project ${project.name} of target type \"fabric\" needs to have the fabric-loom plugin applied.")

        _configuration = FabricConfiguration(project, commonProject)
        configuration.configure()
    }

    /**
     * Setup this project as a forge module. Requires that the `net.minecraftforge.gradle` plugin be applied.
     */
    fun forge(commonProject: Project) {
        checkIsSetup("forge")
        if (!project.plugins.hasPlugin("net.minecraftforge.gradle"))
            error("Project ${project.name} of target type \"forge\" needs to have the net.minecraftforge.gradle plugin applied.")

        _configuration = ForgeConfiguration(project, commonProject)
        configuration.configure()
    }

    /**
     * Setup this project as a fabric module. Requires that the `fabric-loom` plugin be applied.
     */
    fun fabric(commonProject: ProjectDependency) = fabric(commonProject.dependencyProject)

    /**
     * Setup this project as a fabric module. Requires that the `fabric-loom` plugin be applied.
     */
    fun fabric() = fabric(project.rootProject.subprojects.first { it.name == "common" })

    /**
     * Setup this project as a forge module. Requires that the `net.minecraftforge.gradle` plugin be applied.
     */
    fun forge(commonProject: ProjectDependency) = forge(commonProject.dependencyProject)

    /**
     * Setup this project as a forge module. Requires that the `net.minecraftforge.gradle` plugin be applied.
     */
    fun forge() = forge(project.rootProject.subprojects.first { it.name == "common" })

    private fun checkIsSetup(name: String) {
        if (configuration !is UnknownConfiguration)
            error("Cannot configure artifactory project \"$name\": This project has already been configured.")
    }
}
