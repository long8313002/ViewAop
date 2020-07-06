package com.mb.xmlaop

import org.gradle.api.Project

class XmlTransform(project: Project) {


    companion object {
        private val xmlMap = mutableMapOf<String, String>()
    }

    init {
        val properties = LinkedProperties()
        properties.load(project.rootProject.file("mapping.properties").inputStream())
        var start = false
        properties.keys.forEach {
            if (it == "StartXML") {
                start = true
            } else if (it == "EndXML") {
                start = false
                return@forEach
            }

            if (start && it != "StartXML") {
                xmlMap[it.toString()] = properties[it].toString()
            }
        }
    }


    fun transform(xmlContent: String): String {

        var content = xmlContent

        xmlMap.forEach {
            content = content.replace("</${it.key}", "</${it.value}")
            content = content.replace("<${it.key}", "<${it.value}")

            println("<${it.key} "+ "     <${it.value} ")
        }

        return content

    }
}