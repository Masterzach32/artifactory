package com.spicymemes.artifactory.configuration.forge

import org.gradle.api.plugins.*

data class ForgeMappedConfigurationEntry(
    val sourceConfigurationName: String,
    val targetConfigurationName: String,
) {

    val mappedConfigurationName: String = "${sourceConfigurationName}Mapped"

    companion object {
        val implementation = ForgeMappedConfigurationEntry("modImplementation", JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
        val runtimeOnly = ForgeMappedConfigurationEntry("modRuntimeOnly", JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)
        val compileOnly = ForgeMappedConfigurationEntry("modCompileOnly", JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

        val allEntries = listOf(implementation, runtimeOnly, compileOnly)
    }
}
