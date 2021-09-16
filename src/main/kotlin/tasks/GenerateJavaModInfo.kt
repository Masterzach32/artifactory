package com.spicymemes.artifactory.tasks

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import javax.lang.model.element.Modifier

abstract class GenerateJavaModInfo : GenerateModInfo() {

    init {
        srcDir.convention(project.the<SourceSetContainer>()["generated"].java.srcDirs.last().path)
        fileName.convention("ModInfo.java")
    }

    @TaskAction
    fun generate() {
        val packagePath = `package`.get().replace(".", "/")
        project.mkdir("${srcDir.get()}/$packagePath")

        val typeSpec = TypeSpec.classBuilder("ModInfo")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(
                FieldSpec.builder(String::class.java, "MOD_ID")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("\$S", modId.get())
                    .build()
            )
            .addField(
                FieldSpec.builder(String::class.java, "MOD_NAME")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("\$S", modName.get())
                    .build()
            )
            .addField(
                FieldSpec.builder(String::class.java, "MOD_VERSION")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("\$S", modVersion.get())
                    .build()
            )
            .build()

        val file = JavaFile.builder(`package`.get(), typeSpec).build()

        project.file("${srcDir.get()}/$packagePath/${fileName.get()}").writeText(file.toString())
    }
}
