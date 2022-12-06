package com.github.xukeek.struts.wrappers

import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag


class ResultXmlTag(private val resultTag: XmlTag) {
    private val typeAttribute: XmlAttribute? = resultTag.getAttribute("type")

    fun valid(): Boolean {
        return typeAttribute != null && typeAttribute.value == RAYSE_TYPE && mappedFilePath.length > 7
    }

    val mappedFilePath: String
        get() = resultTag.value.trimmedText

    companion object {
        const val RAYSE_TYPE = "template"
    }

}


