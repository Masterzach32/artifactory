package com.spicymemes.artifactory

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

internal fun Jar.jarConfig(archiveVersion: String) {
    group = "build"
    duplicatesStrategy = DuplicatesStrategy.FAIL
    this.archiveVersion.set(archiveVersion)
}
