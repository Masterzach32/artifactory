package com.spicymemes.artifactory

import org.gradle.api.tasks.*

internal val SourceSetContainer.modSets: List<SourceSet>
    get() = filter { it.name != "test" }
