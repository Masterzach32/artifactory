package com.spicymemes.artifactory

import net.minecraftforge.gradle.common.util.*

var RunConfig.forgeLoggingMarkers: List<String>
    get() = properties["forge.logging.markers"]?.split(",") ?: emptyList()
    set(value) {
        properties["forge.logging.markers"] = value.joinToString(separator = ",")
    }
