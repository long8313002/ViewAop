package com.mb.xmlaop

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.mb.xmlaop.utils.Tools
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.*

class XMLPlugin : Plugin<Project> {

    private lateinit var resetCacheFileMap: MutableMap<File, ByteArray>



    override fun apply(project: Project) {

        resetCacheFileMap = HashMap()

        //check is library or application
        val hasAppPlugin = project.plugins.hasPlugin("com.android.application")
        val variants = if (hasAppPlugin) {
            (project.property("android") as AppExtension).applicationVariants
        } else {
            (project.property("android") as LibraryExtension).libraryVariants
        }


        project.afterEvaluate {
            variants.all { variant ->

                variant as BaseVariantImpl


                val mergeResourcesTask = variant.mergeResourcesProvider.get()
                val mcXmlTask = project.task("McXml${variant.name.capitalize()}")

                mcXmlTask.doLast {

                    val dir = variant.allRawAndroidResources.files

                    for (channelDir: File in dir) {
                        transform(channelDir)
                    }
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

                    restoreXml()
                }

            }

            project.gradle.buildFinished{
                restoreXml()
            }
        }

    }

    private fun restoreXml(){
        val xmlNames = resetCacheFileMap.keys.iterator()
        while (xmlNames.hasNext()){

            val xmlFile = xmlNames.next()
            val xmlValue = resetCacheFileMap[xmlFile]

            xmlNames.remove()

            if(xmlValue !=null){
                writeByte(xmlFile, xmlValue)
            }
        }
        resetCacheFileMap.clear()
    }

    private fun transform(channelDir: File) {

        if (channelDir.isDirectory) {
            channelDir.listFiles()?.forEach {
                transform(it)
            }
        } else if (channelDir.name.contains("activity_main")) {
            val byteArray = readFileToByte(channelDir)
            var content = String(byteArray)
            resetCacheFileMap[channelDir] = byteArray
            content = content.replace("Button", "TextView")
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