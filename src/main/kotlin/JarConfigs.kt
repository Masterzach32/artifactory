package com.spicymemes.artifactory

import net.fabricmc.loom.task.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.jvm.tasks.Jar

internal fun AbstractCopyTask.fromSources(sourceSet: List<SourceSet>) =
    from(*sourceSet.map { it.allSource }.toTypedArray())

internal fun AbstractCopyTask.fromOutputs(sourceSet: List<SourceSet>) =
    from(*sourceSet.map { it.output }.toTypedArray())

internal fun Jar.jarConfig(archiveVersion: String) {
    group = "build"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    this.archiveVersion.set(archiveVersion)
}

internal fun RemapJarTask.jarConfig(archiveVersion: String) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    this.archiveVersion.set(archiveVersion)
}

internal fun RemapSourcesJarTask.jarConfig(sourcesTask: AbstractArchiveTask) {
    setInput(sourcesTask.archiveFile)
    setOutput(sourcesTask.archiveFile)
    dependsOn(sourcesTask)
}
