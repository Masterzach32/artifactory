package com.spicymemes.artifactory

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.*
import org.gradle.kotlin.dsl.*

class ArtifactoryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class)
        val sourceSets = project.extensions.getByType<SourceSetContainer>()

        val apiSourceSet = sourceSets.create("api")
        sourceSets["main"].apply {
            compileClasspath += apiSourceSet.output
            runtimeClasspath += apiSourceSet.output
        }
        sourceSets["test"].apply {
            compileClasspath += apiSourceSet.output
            runtimeClasspath += apiSourceSet.output
        }

        val configurations = project.configurations
        val apiImplementation = configurations.named(apiSourceSet.implementationConfigurationName)
        val apiRuntimeOnly = configurations.named(apiSourceSet.runtimeOnlyConfigurationName)

        val artifactoryExtension = project.extensions.create<ArtifactoryExtension>(
            "artifactory",
            project,
            sourceSets,
            apiSourceSet,
            apiImplementation,
            apiRuntimeOnly
        )

//        val sourcesJar by project.tasks.registering(Jar::class) {
//            archiveBaseName.set("")
//            archiveClassifier.set("sources")
//            //from(sourceSets.main.get().allSource)
//        }
//
//        val modJar by project.tasks.registering(Jar::class) {
//            archiveBaseName.set("")
//            archiveClassifier.set("obf")
//            //from(sourceSets.main.get().output)
//            //manifest()
//            finalizedBy("reobfJar")
//        }
    }
}
