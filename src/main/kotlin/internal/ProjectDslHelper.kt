package com.spicymemes.artifactory.internal

import net.minecraftforge.gradle.userdev.*
import net.minecraftforge.gradle.userdev.tasks.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.*
import org.gradle.kotlin.dsl.*

abstract class ProjectDslHelper {

    protected val Project.sourceSets: SourceSetContainer
        get() = the()

    protected val Project.fg: DependencyManagementExtension
        get() = the()

    protected val Project.reobf: NamedDomainObjectContainer<RenameJarInPlace>
        get() = the()

    protected val SourceSetContainer.main: NamedDomainObjectProvider<SourceSet>
        get() = named("main")

    protected val SourceSetContainer.api: NamedDomainObjectProvider<SourceSet>
        get() = named("api")

    protected val SourceSetContainer.test: NamedDomainObjectProvider<SourceSet>
        get() = named("test")

    protected val SourceSetContainer.generated: NamedDomainObjectProvider<SourceSet>
        get() = named("generated")

    protected val SourceSetContainer.nonTestSets: NamedDomainObjectSet<SourceSet>
        get() = matching { it.name != "test" }

    protected val ConfigurationContainer.compileClasspath: NamedDomainObjectProvider<Configuration>
        get() = named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)

    protected val ConfigurationContainer.apiCompileClasspath: NamedDomainObjectProvider<Configuration>
        get() = named("apiCompileClasspath")

    protected val TaskContainer.processResources: TaskProvider<Copy>
        get() = named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, Copy::class.java)

    protected val TaskContainer.jar: TaskProvider<Jar>
        get() = named(JavaPlugin.JAR_TASK_NAME, Jar::class.java)

    protected val TaskContainer.sourcesJar: TaskProvider<Jar>
        get() = named("sourcesJar", Jar::class.java)

    protected val TaskContainer.apiJar: TaskProvider<Jar>
        get() = named("apiJar", Jar::class.java)

    protected val TaskContainer.apiSourcesJar: TaskProvider<Jar>
        get() = named("apiSourcesJar", Jar::class.java)

    protected val TaskContainer.assemble: TaskProvider<Task>
        get() = named("assemble")
}