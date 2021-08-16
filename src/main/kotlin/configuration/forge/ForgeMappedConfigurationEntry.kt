package com.spicymemes.artifactory.configuration.forge

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.internal.component.external.descriptor.*

data class ForgeMappedConfigurationEntry(
    val sourceConfigurationName: String,
    val targetConfigurationName: String,
    val mavenScope: MavenScope? = null
) {

    val mappedConfigurationName: String = "${sourceConfigurationName}Mapped"

    fun source(project: Project) = project.configurations.named(sourceConfigurationName)
    fun mapped(project: Project) = project.configurations.named(mappedConfigurationName)
    fun target(project: Project) = project.configurations.named(targetConfigurationName)

    companion object {
        val api = ForgeMappedConfigurationEntry("modApi", JavaPlugin.API_CONFIGURATION_NAME, MavenScope.Compile)
        val implementation = ForgeMappedConfigurationEntry("modImplementation", JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, MavenScope.Runtime)
        val runtimeOnly = ForgeMappedConfigurationEntry("modRuntimeOnly", JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)
        val compileOnly = ForgeMappedConfigurationEntry("modCompileOnly", JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

        val allEntries = listOf(api, implementation, runtimeOnly, compileOnly)
    }
}
