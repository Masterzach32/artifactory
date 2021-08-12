package com.spicymemes.artifactory

import net.minecraftforge.gradle.userdev.*
import net.minecraftforge.gradle.userdev.tasks.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.*
import java.io.*

open class ArtifactoryExtension(
    private val project: Project,
    private val archivesVersion: String
) {

    private var isSetupFor: String? = null

    private val sourceSets = project.the<SourceSetContainer>()

    private val apiImplementation: NamedDomainObjectProvider<Configuration> by project.configurations.existing
    private val apiRuntimeOnly: NamedDomainObjectProvider<Configuration> by project.configurations.existing

    var fullModVersion: String = project.version.toString()

    var archivesBaseName: String by project.extensions.getByType<BasePluginExtension>().archivesName
        private set
    val apiArchivesBaseName: String
        get() = "$archivesBaseName-api"

    /**
     * Setup this project as a common module. Requires that the `fabric-loom` plugin be applied.
     */
    fun common() {
        checkIsSetup("common")
        if(!project.plugins.hasPlugin("fabric-loom"))
            error("Project ${project.name} of module type \"common\" needs to have the fabric-loom plugin applied.")

        apiImplementation {
            extendsFrom(project.configurations["compileClasspath"])
        }

        val apiJar by project.tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
        }
        val apiSourcesJar by project.tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
        }
        val remapJar by project.tasks.existing
        val remapSourcesJar by project.tasks.existing

        project.plugins.withType<MavenPublishPlugin> {
            project.configure<PublishingExtension> {
                publications {
                    named<MavenPublication>("api") {
                        artifactId = apiArchivesBaseName
                        artifact(apiJar) {
                            builtBy(remapJar)
                        }
                        artifact(apiSourcesJar) {
                            builtBy(remapSourcesJar)
                        }
                    }

                    removeIf { it.name == "mod" }
                }
            }
        }
    }

    /**
     * Setup this project as a fabric module. Requires that the `fabric-loom` plugin be applied.
     */
    fun fabric(commonProject: Project) {
        checkIsSetup("fabric")
        if(!project.plugins.hasPlugin("fabric-loom"))
            error("Project ${project.name} of module type \"fabric\" needs to have the fabric-loom plugin applied.")

        archivesBaseName += "-fabric"

        val commonSourceSets = commonProject.the<SourceSetContainer>()
        sourceSets["api"].apply {
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
            filesMatching("fabric.mod.json") { expand("version" to project.version) }
            from(commonSourceSets["main"].resources)
        }

        val jar by project.tasks.existing(Jar::class) {
            fromOutputs(commonSourceSets.modSets)
        }

        val sourcesJar by project.tasks.existing(Jar::class) {
            fromSources(commonSourceSets.modSets)
        }

        val apiJar by project.tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
            from(commonSourceSets["api"].output)
        }

        val apiSourcesJar by project.tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
            from(commonSourceSets["api"].allSource)
        }

        val remapJar by project.tasks.existing
        val remapSourcesJar by project.tasks.existing
        project.plugins.withType<MavenPublishPlugin> {
            project.configure<PublishingExtension> {
                publications {
                    named<MavenPublication>("mod") {
                        artifactId = archivesBaseName
                        artifact(jar) {
                            builtBy(remapJar)
                        }
                        artifact(sourcesJar) {
                            builtBy(remapSourcesJar)
                        }
                    }
                    named<MavenPublication>("api") {
                        artifactId = apiArchivesBaseName
                        artifact(apiJar) {
                            builtBy(remapJar)
                        }
                        artifact(apiSourcesJar) {
                            builtBy(remapSourcesJar)
                        }
                    }
                }
            }
        }
    }

    /**
     * Setup this project as a forge module. Requires that the `net.minecraftforge.gradle` plugin be applied.
     */
    fun forge(commonProject: Project) {
        checkIsSetup("forge")
        if(!project.plugins.hasPlugin("net.minecraftforge.gradle"))
            error("Project ${project.name} of module type \"forge\" needs to have the net.minecraftforge.gradle plugin applied.")

        archivesBaseName += "-forge"

        val commonSourceSets = commonProject.the<SourceSetContainer>()
        sourceSets["api"].apply {
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

        project.tasks.named<ProcessResources>("processResources") {
            duplicatesStrategy = DuplicatesStrategy.FAIL
            inputs.property("version", project.version)
            filesMatching("META-INF/mods.toml") { expand("version" to project.version) }
            from(commonSourceSets["main"].resources)
        }

        val jar by project.tasks.existing(Jar::class) {
            fromOutputs(commonSourceSets.modSets)
            finalizedBy("reobfJar")
        }

        val sourcesJar by project.tasks.existing(Jar::class) {
            fromSources(commonSourceSets.modSets)
        }

        val deobfJar by project.tasks.registering(Jar::class) {
            jarConfig(archivesVersion)
            archiveClassifier.set("deobf")
            fromOutputs(commonSourceSets.modSets)
            fromOutputs(sourceSets.modSets)
        }

        val apiJar by project.tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
            from(commonSourceSets["api"].output)
            finalizedBy("reobfApiJar")
        }

        val apiSourcesJar by project.tasks.existing(Jar::class) {
            archiveBaseName.set(apiArchivesBaseName)
            from(commonSourceSets["api"].allSource)
        }

        val apiDeobfJar by project.tasks.registering(Jar::class) {
            jarConfig(archivesVersion)
            archiveBaseName.set(apiArchivesBaseName)
            archiveClassifier.set("deobf")
            from(commonSourceSets["api"].output)
            from(sourceSets["api"].output)
        }

        project.tasks.named("assemble") {
            dependsOn(deobfJar, apiDeobfJar)
        }

        project.extensions.configure<NamedDomainObjectContainer<RenameJarInPlace>>("reobf") {
            create("apiJar") {
                classpath.from(sourceSets["api"].compileClasspath)
            }
            create("jar") {
                classpath.from(sourceSets["main"].compileClasspath)
            }
        }

        project.tasks

        project.plugins.withType<MavenPublishPlugin> {
            project.configure<PublishingExtension> {
                publications {
                    named<MavenPublication>("mod") {
                        artifactId = archivesBaseName
                        artifact(jar)
                        artifact(sourcesJar)
                        artifact(deobfJar)
                    }
                    named<MavenPublication>("api") {
                        artifactId = apiArchivesBaseName
                        artifact(apiJar)
                        artifact(apiSourcesJar)
                        artifact(apiDeobfJar)
                    }
                }
            }
        }
    }

    fun fabric(commonProject: ProjectDependency) = fabric(commonProject.dependencyProject)

    fun fabric() = fabric(project.rootProject.subprojects.first { it.name == "common" })

    fun forge(commonProject: ProjectDependency) = forge(commonProject.dependencyProject)

    fun forge() = forge(project.rootProject.subprojects.first { it.name == "common" })

    fun applyForgeMissingLibsTempfix() {
        project.the<UserDevExtension>().runs.all {
            lazyToken("minecraft_classpath") {
                project.configurations["library"]
                    .copyRecursive()
                    .resolve()
                    .joinToString(File.pathSeparator) { it.absolutePath }
            }
        }
    }

    private fun checkIsSetup(name: String) {
        if (isSetupFor != null)
            error("Cannot configure module \"$name\": This project has already been configured as \"$isSetupFor\".")
        else
            isSetupFor = name
    }
}
