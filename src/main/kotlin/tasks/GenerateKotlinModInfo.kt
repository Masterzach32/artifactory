package com.spicymemes.artifactory.tasks

import com.squareup.kotlinpoet.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*

abstract class GenerateKotlinModInfo : GenerateModInfo() {

    init {
        location.convention(project.the<SourceSetContainer>()["generated"].java.srcDirs.first().path)
        fileName.convention("ModInfo.kt")
    }

    @TaskAction
    fun generate() {
        val packagePath = `package`.get().replace(".", "/")
        project.mkdir("${location.get()}/$packagePath")

        val file = FileSpec.builder(`package`.get(), fileName.get())
            .addProperty(
                PropertySpec.builder("MOD_ID", String::class, KModifier.CONST)
                    .initializer("%S", modId.get())
                    .build()
            )
            .addProperty(
                PropertySpec.builder("MOD_NAME", String::class, KModifier.CONST)
                    .initializer("%S", modName.get())
                    .build()
            )
            .addProperty(
                PropertySpec.builder("MOD_VERSION", String::class, KModifier.CONST)
                    .initializer("%S", modVersion.get())
                    .build()
            )
            .build()

        project.file("${location.get()}/$packagePath/${fileName.get()}").writeText(file.toString())
    }
}
