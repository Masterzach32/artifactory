package com.spicymemes.artifactory

import org.gradle.api.file.*
import org.gradle.jvm.tasks.*

internal fun Jar.jarConfig(archiveVersion: String) {
    group = "build"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    this.archiveVersion.set(archiveVersion)
}
