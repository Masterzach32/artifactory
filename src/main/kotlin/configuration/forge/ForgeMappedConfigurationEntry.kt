package com.spicymemes.artifactory.configuration.forge

import org.gradle.api.plugins.JavaPlugin

data class ForgeMappedConfigurationEntry(
    val targetConfigurationName: String,
) {

    val sourceConfigurationName: String = "obf${targetConfigurationName.capitalize()}"
    val mappedConfigurationName: String = "mapped${targetConfigurationName.capitalize()}"

    companion object {
        val implementation = ForgeMappedConfigurationEntry(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
        val runtimeOnly = ForgeMappedConfigurationEntry(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)
        val compileOnly = ForgeMappedConfigurationEntry(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

        val allEntries = listOf(implementation, runtimeOnly, compileOnly)
    }
}
