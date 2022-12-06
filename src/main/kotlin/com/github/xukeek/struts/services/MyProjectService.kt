package com.github.xukeek.struts.services

import com.github.xukeek.struts.MyBundle
import com.github.xukeek.struts.utils.StrutsXmlUtil
import com.github.xukeek.struts.wrappers.ActionConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile

class MyProjectService(private val project: Project) {
    private var fileCache: MutableMap<String, List<ActionConfig>> = mutableMapOf()

    init {
        println(MyBundle.message("projectService", project.name))
    }

    fun getActionConfigs(): List<ActionConfig> {
        val strutsFiles =
            FilenameIndex.getFilesByName(project, StrutsXmlUtil.FILE_NAME, GlobalSearchScope.allScope(project));
        for (strutsFile in strutsFiles) {
            if (!fileCache.containsKey(strutsFile.virtualFile.path)) {
                fileCache[strutsFile.virtualFile.path] = StrutsXmlUtil.buildConfigs(strutsFile as XmlFile)
            }
        }
        return fileCache.values.flatten()
    }

    fun getAllActionConfigs(): List<ActionConfig> {
        return fileCache.values.flatten()
    }

    fun reloadFile(file: VirtualFile) {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile != null) {
            fileCache[file.path] = StrutsXmlUtil.buildConfigs(psiFile as XmlFile)
        }
    }
}
