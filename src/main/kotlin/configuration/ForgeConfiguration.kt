package com.spicymemes.artifactory.configuration

import com.spicymemes.artifactory.configuration.forge.ForgeMappedConfigurationEntry
import com.spicymemes.artifactory.jarConfig
import net.minecraftforge.gradle.userdev.UserDevExtension
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import java.io.File

class ForgeConfiguration(project: Project, commonProject: Project) : BaseConfiguration(project) {

    init {
        beforeConfiguration {
            archivesBaseName += "-forge"
        }

        configureProject {
            sourceSets.api {
                commonProject.sourceSets["api"].also { commonApi ->
                    compileClasspath += commonApi.output
                }
            }
            sourceSets.main {
                commonProject.sourceSets.nonTestSets.forEach { sourceSet ->
                    compileClasspath += sourceSet.output
                }
            }

            ForgeMappedConfigurationEntry.allEntries.forEach {
                configurations.register(it.sourceConfigurationName) {
                    description = "Configuration to hold obfuscated dependencies for the " +
                        "${it.targetConfigurationName} configuration."
                    isCanBeResolved = false
                    isCanBeConsumed = false
                    isTransitive = false
                    dependencies.all { project.dependencies.add(it.mappedConfigurationName, fg.deobf(this)) }
                }
                configurations.register(it.oldSourceConfigurationName) {
                    description =
                        "Configuration to hold obfuscated dependencies for the " +
                            "${it.targetConfigurationName} configuration."
                    isCanBeResolved = false
                    isCanBeConsumed = false
                    isTransitive = false
                    dependencies.all {
                        logger.warn("The configuration ${it.oldSourceConfigurationName} will be removed in " +
                            "artifactory 0.5. use ${it.sourceConfigurationName} instead.")
                        project.dependencies.add(it.mappedConfigurationName, fg.deobf(this))
                    }
                }
                val mappedConfiguration = configurations.register(it.mappedConfigurationName) {
                    description =
                        "Configuration to hold deobfuscated dependencies for the " +
                            "${it.targetConfigurationName} configuration."
                    isCanBeResolved = false
                    isCanBeConsumed = false
                    isTransitive = false
                }
                configurations.named(it.targetConfigurationName) {
                    extendsFrom(mappedConfiguration.get())
                }
            }

            tasks.processResources {
                duplicatesStrategy = DuplicatesStrategy.FAIL
                inputs.property("version", project.version)
                filesMatching("META-INF/mods.toml") { expand("version" to project.version) }
//                from(commonProject.sourceSets["main"].resources)
            }

            tasks.jar {
                commonProject.sourceSets.nonTestSets.forEach {
                    from(it.output)
                }
                finalizedBy("reobfJar")
            }

            tasks.sourcesJar {
                commonProject.sourceSets.nonTestSets.forEach {
                    from(it.allSource)
                }
            }

            val deobfJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveClassifier.set("deobf")
                commonProject.sourceSets.nonTestSets.forEach {
                    from(it.output)
                }
                sourceSets.nonTestSets.forEach {
                    from(it.output)
                }
            }

            tasks.apiJar {
                archiveBaseName.set(apiArchivesBaseName)
                from(commonProject.sourceSets["api"].output)
                finalizedBy("reobfApiJar")
            }

            tasks.apiSourcesJar {
                archiveBaseName.set(apiArchivesBaseName)
                from(commonProject.sourceSets["api"].allSource)
                from(sourceSets["api"].allSource)
            }

            val apiDeobfJar by tasks.registering(Jar::class) {
                jarConfig(archivesVersion)
                archiveBaseName.set(apiArchivesBaseName)
                archiveClassifier.set("deobf")
                from(commonProject.sourceSets["api"].output)
                from(sourceSets["api"].output)
            }

            tasks.assemble {
                dependsOn(deobfJar, apiDeobfJar)
            }

            reobf {
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
                            artifact(tasks.jar)
                            artifact(tasks.sourcesJar)
                            artifact(deobfJar)
                        }
                        named<MavenPublication>("api") {
                            artifactId = apiArchivesBaseName
                            artifact(tasks.apiJar)
                            artifact(tasks.apiSourcesJar)
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
                    project.configurations[JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME]
                        .copyRecursive { !(it.group == "net.minecraftforge" && it.name == "forge") }
                        .resolve()
                        .joinToString(File.pathSeparator) { it.absolutePath }
                }
            }
        }

        fun applyInvalidModuleNameFix() {
            project.configurations.named(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME) {
                setExtendsFrom(extendsFrom.filter { it.name != ForgeMappedConfigurationEntry.runtimeOnly.mappedConfigurationName })
            }

            val modRuntimeModFiles by project.configurations.registering {
                extendsFrom(project.configurations[ForgeMappedConfigurationEntry.runtimeOnly.mappedConfigurationName])
                isCanBeConsumed = false
            }

            project.afterEvaluate {
                project.gradle.projectsEvaluated {
                    the<UserDevExtension>().runs.all {
                        tasks.named("prepareRun${name.capitalize()}") {
                            doLast {
                                val modsDir = "$workingDirectory/mods"
                                file(modsDir).deleteRecursively()
                                mkdir(modsDir)
                                copy {
                                    from(modRuntimeModFiles.get())
                                    into(modsDir)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}