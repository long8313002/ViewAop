package com.mb.xmlaop

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.BaseVariantImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.*

class XMLPlugin : Plugin<Project> {

    private lateinit var mcImageProject: Project
    private lateinit var resetCacheFileMap: MutableMap<File, ByteArray>


    var isDebugTask = false
    var isContainAssembleTask = false

    override fun apply(project: Project) {

        mcImageProject = project
        resetCacheFileMap = HashMap()

        //check is library or application
        val hasAppPlugin = project.plugins.hasPlugin("com.android.application")
        val variants = if (hasAppPlugin) {
            (project.property("android") as AppExtension).applicationVariants
        } else {
            (project.property("android") as LibraryExtension).libraryVariants
        }

        project.gradle.taskGraph.whenReady {
            it.allTasks.forEach { task ->
                val taskName = task.name
                if (taskName.contains("assemble") || taskName.contains("resguard") || taskName.contains(
                        "bundle"
                    )
                ) {
                    if (taskName.toLowerCase().endsWith("debug") &&
                        taskName.toLowerCase().contains("debug")
                    ) {
                        isDebugTask = true
                    }
                    isContainAssembleTask = true
                    return@forEach
                }
            }
        }

        project.afterEvaluate {
            variants.all { variant ->

                variant as BaseVariantImpl


                val mergeResourcesTask = variant.mergeResourcesProvider.get()
                val mcXmlTask = project.task("McXml${variant.name.capitalize()}")

                mcXmlTask.doLast {


                    println("---- McXML Plugin Start ----")

                    val dir = variant.allRawAndroidResources.files

                    val start = System.currentTimeMillis()


                    for (channelDir: File in dir) {
                        testTransForm(channelDir)
                    }



                    println("---- McXML Plugin End ----, Total Time(ms) : ${System.currentTimeMillis() - start}")
                }

                //chmod task
                val chmodTaskName = "chmod${variant.name.capitalize()}"
                val chmodTask = project.task(chmodTaskName)
                chmodTask.doLast {
                }

                //inject task
                (project.tasks.findByName(chmodTask.name) as Task).dependsOn(
                    mergeResourcesTask.taskDependencies.getDependencies(
                        mergeResourcesTask
                    )
                )
                (project.tasks.findByName(mcXmlTask.name) as Task).dependsOn(
                    project.tasks.findByName(
                        chmodTask.name
                    ) as Task
                )
                mergeResourcesTask.dependsOn(project.tasks.findByName(mcXmlTask.name))


                mergeResourcesTask.doLast {

                    resetCacheFileMap.forEach {
                        writeByte(it.key, it.value)
                    }
                }

            }
        }

    }

    fun testTransForm(channelDir: File) {

        if (channelDir.isDirectory) {
            channelDir.listFiles()?.forEach {
                testTransForm(it)
            }
        } else if (channelDir.name.contains("activity_main")) {
            val byteArray = readFileToByte(channelDir)
            var content = String(byteArray)
            resetCacheFileMap.put(channelDir, byteArray)
            content = content.replace("Button", "TextView")
            println("zzzzz==>$content")
            writeString(channelDir, content)
        }
    }

    private fun readFileToByte(file:File): ByteArray {
        val input =  BufferedInputStream( FileInputStream(file))
        val out =  ByteArrayOutputStream(1024)
        val temp =  ByteArray(1024){0}
        while(true){
            val size = input.read(temp)
            if(size == -1){
                break
            }
            out.write(temp, 0, size)
        }
        input.close()
        return out.toByteArray()
    }



    private fun writeString(file: File, content: String) {
        val write = BufferedWriter(FileWriter(file))
        write.write(content)
        write.close()
    }

    private fun writeByte(file: File, content: ByteArray) {
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(content)
        fileOutputStream.close()
    }

}