package com.github.xukeek.struts.utils

import com.intellij.psi.PsiFile

class TemplateFile(file: PsiFile) : Comparable<TemplateFile> {
    val psiFile = file
    private val paths: Array<String> = file.virtualFile.path.split("views").toTypedArray()

    private var fileLocateModuleName: String = ""
    private val isInJar = !file.isWritable

    init {
        fileLocateModuleName = paths[0]
    }

    override fun compareTo(other: TemplateFile): Int {
        if (fileLocateModuleName.endsWith("resource")) {
            if (!other.fileLocateModuleName.endsWith("resource")) {
                return -1
            } else if (!this.isInJar && other.isInJar) {
                return -1
            } else if (this.isInJar && !other.isInJar) {
                return 1
            } else if (this.isInJar) {
                return 0
            }
        } else if (other.fileLocateModuleName.endsWith("resource")) {
            return 1
        }
        return 0
    }
}