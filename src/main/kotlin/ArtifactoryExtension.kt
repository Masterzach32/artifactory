package com.spicymemes.artifactory

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.*

open class ArtifactoryExtension(
    private val project: Project,
    private val sourceSets: SourceSetContainer,
    private val apiSourceSet: SourceSet,
    private val apiImplementation: NamedDomainObjectProvider<Configuration>,
    private val apiRuntimeOnly: NamedDomainObjectProvider<Configuration>
) {

    private var isSetupFor: String? = null

    var archivesBaseName: String by project.extensions.getByType<BasePluginExtension>().archivesName

    private fun checkIsSetup(name: String) {
        if (isSetupFor != null)
            error("Cannot setup module \"$name\": It has already been configured as \"$isSetupFor\".")
        else
            isSetupFor = name
    }

    fun common() {
        checkIsSetup("common")
        if(!project.plugins.hasPlugin("fabric-loom"))
            error("Project ${project.name} of module type \"common\" needs to have the fabric-loom plugin applied.")

        apiImplementation {
            extendsFrom(project.configurations["compileClasspath"])
        }
    }

    fun fabric(commonProject: Project) {
        checkIsSetup("fabric")
        if(!project.plugins.hasPlugin("fabric-loom"))
            error("Project ${project.name} of module type \"fabric\" needs to have the fabric-loom plugin applied.")

        val commonSourceSets = commonProject.extensions.getByType<SourceSetContainer>()
        apiSourceSet.apply {
            commonSourceSets["api"].also { commonApi ->
                compileClasspath += commonApi.output
                runtimeClasspath += commonApi.output
            }
        }
        sourceSets["main"].apply {
            commonSourceSets.filter { it.name != "test" }.forEach { sourceSet ->
                compileClasspath += sourceSet.output
                runtimeClasspath += sourceSet.output
            }
        }

        apiImplementation {
            extendsFrom(project.configurations["compileClasspath"])
        }

        project.tasks.named<ProcessResources>("processResources") {
            duplicatesStrategy = DuplicatesStrategy.FAIL
            inputs.property("version", project.version)
            filesMatching("META-INF/mods.toml") { expand("version" to project.version) }
            from(commonSourceSets["main"].resources)
        }
    }

    fun fabric(commonProject: ProjectDependency) = fabric(commonProject.dependencyProject)

    fun fabric() = fabric(project.rootProject.subprojects.first { it.name == "common" })

    fun forge(commonProject: Project) {
        checkIsSetup("forge")
        if(!project.plugins.hasPlugin("net.minecraftforge.gradle"))
            error("Project ${project.name} of module type \"forge\" needs to have the net.minecraftforge.gradle plugin applied.")

        val commonSourceSets = commonProject.extensions.getByType<SourceSetContainer>()
        apiSourceSet.apply {
            commonSourceSets["api"].also { commonApi ->
                compileClasspath += commonApi.output
            }
        }
        sourceSets["main"].apply {
            commonSourceSets.filter { it.name != "test" }.forEach { sourceSet ->
                compileClasspath += sourceSet.output
            }
        }

        apiImplementation {
            extendsFrom(project.configurations["implementation"])
        }
        apiRuntimeOnly {
            extendsFrom(project.configurations["runtimeOnly"])
        }

        project.repositories {
            maven("https://maven.minecraftforge.net")
        }
    }

    fun forge(commonProject: ProjectDependency) = forge(commonProject.dependencyProject)

    fun forge() = forge(project.rootProject.subprojects.first { it.name == "common" })
}
