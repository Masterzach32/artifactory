package com.spicymemes.artifactory

import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.*

internal fun AbstractCopyTask.fromSources(sourceSet: List<SourceSet>) =
    from(*sourceSet.map { it.allSource }.toTypedArray())

internal fun AbstractCopyTask.fromOutputs(sourceSet: List<SourceSet>) =
    from(*sourceSet.map { it.output }.toTypedArray())

internal fun Jar.jarConfig(archiveVersion: String) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    this.archiveVersion.set(archiveVersion)
}
