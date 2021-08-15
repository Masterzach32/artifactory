package com.spicymemes.artifactory.configuration

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*

abstract class AbstractModLoaderConfiguration(protected val project: Project) {

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

    protected val sourceSets = project.the<SourceSetContainer>()

    protected val apiImplementation: NamedDomainObjectProvider<Configuration> by project.configurations.existing
    protected val apiRuntimeOnly: NamedDomainObjectProvider<Configuration> by project.configurations.existing

    fun configure() {
        project.beforeConfiguration()

        project.configureSourceSets()
        project.configureConfigurations()
        project.configureTasks()
        project.configureArtifacts()

        project.afterConfiguration()
    }

    protected open fun Project.beforeConfiguration() {}

    protected open fun Project.configureSourceSets() {}

    protected open fun Project.configureConfigurations() {}

    protected open fun Project.configureTasks() {}

    protected open fun Project.configureArtifacts() {}

    protected open fun Project.afterConfiguration() {}
}
