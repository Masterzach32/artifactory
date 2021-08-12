package com.spicymemes.artifactory.tasks

import org.gradle.api.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*

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
    abstract val fileName: Property<String>

    init {
        group = "artifactory"
        description = "Generates the ModInfo.kt source file."
        project.findProperty("modId")?.toString()?.also(modId::convention)
        project.findProperty("modName")?.toString()?.also(modName::convention)
        modVersion.set(project.version.toString())
        location.convention("")
        fileName.convention("ModInfo.kt")
    }

    @TaskAction
    fun generate() {
        val packagePath = location.get().replace(".", "/")
        project.mkdir("src/main/generated/$packagePath")
        project.file("src/main/generated/$packagePath/${fileName.get()}").writeText("""
            package ${location.get()}
            
            const val MOD_ID = "${modId.get()}"
            const val MOD_NAME = "${modName.get()}"
            const val MOD_VERSION = "${modVersion.get()}"
        """.trimIndent() + "\n")
    }
}
