package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.*
import com.spicymemes.artifactory.configuration.forge.*
import net.minecraftforge.gradle.userdev.*
import net.minecraftforge.gradle.userdev.tasks.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.publish.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.*
import java.io.*

class ForgeConfiguration(project: Project, commonProject: Project) : BaseConfiguration(project) {

    private val commonSourceSets = commonProject.the<SourceSetContainer>()

    init {
        beforeConfiguration {
            archivesBaseName += "-forge"
        }

        configureProject {
            sourceSets["api"].apply {
                commonSourceSets["api"].also { commonApi ->
                    compileClasspath += commonApi.output
                }
            }
            sourceSets["main"].apply {
                commonSourceSets.filter { it.name != "test" }.forEach { sourceSet ->
                    compileClasspath += sourceSet.output
                }
                resources.srcDir("src/generated/resources")
            }

            val fg = the<DependencyManagementExtension>()
            ForgeMappedConfigurationEntry.allEntries.forEach {
                configurations.register(it.sourceConfigurationName) {
                    description = "Configuration to hold obfuscated dependencies for the ${it.targetConfigurationName} configuration."
                    isCanBeResolved = false
                    dependencies.all { project.dependencies.add(it.mappedConfigurationName, fg.deobf(this)) }
                }
                val mappedConfiguration = configurations.register(it.mappedConfigurationName) {
                    description = "Configuration to hold deobfuscated dependencies for the ${it.targetConfigurationName} configuration."
                    isCanBeResolved = false
                    isTransitive = false
                }
                configurations.named(it.targetConfigurationName) {
                    extendsFrom(mappedConfiguration.get())
                }
            }

            configurations.named("apiImplementation") {
                extendsFrom(project.configurations["implementation"])
            }
            configurations.named("apiRuntimeOnly") {
                extendsFrom(project.configurations["runtimeOnly"])
            }

            project.tasks.named<ProcessResources>("processResources") {
                duplicatesStrategy = DuplicatesStrategy.FAIL
                inputs.property("version", project.version)
                filesMatching("META-INF/mods.toml") { expand("version" to project.version) }
                from(commonSourceSets["main"].resources)
            }

            val jar by project.tasks.existing(Jar::class) {
                commonSourceSets.modSets.forEach {
                    from(it.output)
                }
                finalizedBy("reobfJar")
            }

            val sourcesJar by project.tasks.existing(Jar::class) {
                commonSourceSets.modSets.forEach {
                    from(it.allSource)
                }
            }

            val deobfJar by project.tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveClassifier.set("deobf")
                commonSourceSets.modSets.forEach {
                    from(it.output)
                }
                sourceSets.modSets.forEach {
                    from(it.output)
                }
            }

            val apiJar by project.tasks.existing(Jar::class) {
                archiveBaseName.set(apiArchivesBaseName)
                from(commonSourceSets["api"].output)
                from(sourceSets["api"].output)
                finalizedBy("reobfApiJar")
            }

            val apiSourcesJar by project.tasks.existing(Jar::class) {
                archiveBaseName.set(apiArchivesBaseName)
                from(commonSourceSets["api"].allSource)
                from(sourceSets["api"].allSource)
            }

            val apiDeobfJar by project.tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveBaseName.set(apiArchivesBaseName)
                archiveClassifier.set("deobf")
                from(commonSourceSets["api"].output)
                from(sourceSets["api"].output)
            }

            tasks.named("assemble") {
                dependsOn(deobfJar, apiDeobfJar)
            }

            extensions.configure<NamedDomainObjectContainer<RenameJarInPlace>>("reobf") {
                create("apiJar") {
                    classpath.from(sourceSets["api"].compileClasspath)
                }
                create("jar") {
                    classpath.from(sourceSets["main"].compileClasspath)
                }
            }

            plugins.withType<MavenPublishPlugin> {
                configure<PublishingExtension> {
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

        afterConfiguration {
            extensions.create<ForgeExtension>("forge", this)

            repositories {
                maven("https://maven.minecraftforge.net")
            }
        }
    }

    open class ForgeExtension(private val project: Project) {

        fun applyForgeMissingLibsTempfix() {
            project.the<UserDevExtension>().runs.all {
                lazyToken("minecraft_classpath") {
                    project.configurations["runtimeClasspath"]
                        .copyRecursive { !(it.group == "net.minecraftforge" && it.name == "forge") }
                        .resolve()
                        .joinToString(File.pathSeparator) { it.absolutePath }
                }
            }
        }

        fun applyInvalidModuleNameFix() {
            project.configurations["runtimeOnly"].apply {
                setExtendsFrom(extendsFrom.filter { it.name != ForgeMappedConfigurationEntry.runtimeOnly.mappedConfigurationName })
            }

            val modRuntimeModFiles by project.configurations.registering {
                extendsFrom(project.configurations[ForgeMappedConfigurationEntry.runtimeOnly.mappedConfigurationName])
                isCanBeConsumed = false
            }

            project.afterEvaluate {
                gradle.projectsEvaluated {
                    the<UserDevExtension>().runs.all {
                        val copyRuntimeMods = tasks.register("copy${name.capitalize()}RuntimeMods", Copy::class) {
                            val modsDir = "$workingDirectory/mods"
                            doFirst {
                                file(modsDir).deleteRecursively()
                                mkdir(modsDir)
                            }
                            from(modRuntimeModFiles.get().resolve())
                            into(modsDir)
                        }
                        tasks.named("prepareRun${name.capitalize()}") {
                            finalizedBy(copyRuntimeMods)
                        }
                    }
                }
            }
        }
    }
}