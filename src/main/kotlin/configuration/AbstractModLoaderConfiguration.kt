package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.internal.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.plugins.*
import org.gradle.kotlin.dsl.*

abstract class AbstractModLoaderConfiguration(protected val project: Project) : ProjectDslHelper() {

    var archivesBaseName: String by project.extensions.getByType<BasePluginExtension>().archivesName
    val apiArchivesBaseName: String
        get() = "$archivesBaseName-api"

    val mcVersion: String? = project.the<VersionCatalog>().findVersion("minecraft").orElse(null)?.requiredVersion
        ?: project.findProperty("mcVersion")?.toString()
        ?: project.findProperty("minecraftVersion")?.toString()

    val archivesVersion: String = project.rootProject.the<ExtraPropertiesExtension>().get("archivesVersion")?.toString() ?:
        if (mcVersion != null)
            "$mcVersion-${project.version}"
        else
            "${project.version}"

    private val beforeConfigurationListeners: MutableList<Action<Project>> = mutableListOf()
    private val configurationListeners: MutableList<Action<Project>> = mutableListOf()
    private val afterConfigurationListeners: MutableList<Action<Project>> = mutableListOf()
    private val allConfigurationListeners: List<Action<Project>>
        get() = listOf(beforeConfigurationListeners, configurationListeners, afterConfigurationListeners).flatten()

    fun configure() {
        allConfigurationListeners.forEach { it.execute(project) }
    }

    protected fun beforeConfiguration(action: Action<Project>) {
        beforeConfigurationListeners.add(action)
    }

    protected fun configureProject(action: Action<Project>) {
        configurationListeners.add(action)
    }

    protected fun afterConfiguration(action: Action<Project>) {
        afterConfigurationListeners.add(action)
    }
}
