# Artifactory

[![GitHub workflow status](https://img.shields.io/github/workflow/status/Masterzach32/artifactory/Java%20CI/master?style=for-the-badge)]()

Artifactory is a gradle plugin to assist in developing Minecraft mods that can target different modloaders. Currently,
Fabric and Forge are the only two modloaders supported. This plugin is currently in early development, expect the API 
to remain unstable until a 1.0 release. (This means breaking changes can happen any time!)

You can find the latest version on the [gradle plugin portal](https://plugins.gradle.org/plugin/com.github.masterzach32.artifactory).
Snapshot versions are available in the snapshots repository: https://maven.masterzach32.net/artifactory/gradle-plugins/

To accomplish this, your mod should be divided into three sub-projects: `common`, `fabric`, and `forge`. Then in each
`build.gradle.kts` file, apply the plugin after each modloader's respective plugin. Artifactory then automatically sets 
up your `fabric` and `forge` projects to depend on the `common` project's source, and automatically configures the 
resources, jar tasks, and publications. If you don't want to follow this project layout, you can specify each projects'
target like below:

`/common/build.gradle.kts`:
```kotlin
plugins {
    id("fabric-loom")
    id("com.github.masterzach32.artifactory")
}

artifactory.common()
```

`/fabric/build.gradle.kts`:
```kotlin
plugins {
    id("fabric-loom")
    id("com.github.masterzach32.artifactory")
}

artifactory.fabric()
```

`/forge/build.gradle.kts`:
```kotlin
plugins {
    id("net.minecraftforge.gradle")
    id("com.github.masterzach32.artifactory")
}

artifactory.forge()
```

An example of a project using Artifactory: https://github.com/SummerModVenture/SpicyCore
