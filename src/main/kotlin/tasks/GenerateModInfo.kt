package com.spicymemes.artifactory.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class GenerateModInfo : DefaultTask() {

    @get:Input
    abstract val modId: Property<String>

    @get:Input
    abstract val modName: Property<String>

    @get:Input
    abstract val modVersion: Property<String>

    @get:Input
    abstract val location: Property<String>

    @get:Input
    abstract val `package`: Property<String>

    @get:Input
    abstract val fileName: Property<String>

    init {
        group = "artifactory"
        project.findProperty("modId")?.toString()?.also(modId::convention)
        project.findProperty("modName")?.toString()?.also(modName::convention)
        modVersion.convention(project.version.toString())
        `package`.convention("artifactory.generated")
    }
}