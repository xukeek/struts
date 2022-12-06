package com.github.xukeek.struts.wrappers

import com.github.xukeek.struts.utils.StrutsActionUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.util.containers.stream
import java.util.stream.Collectors

class ActionConfig(
    val xmlFile: XmlFile,
    private val packageNameSpace: String,
    private val name: String,
    val className: String
) {
    private val resultConfigs: MutableList<ResultConfig> = ArrayList()

    fun addResultConfig(resultConfig: ResultConfig) {
        resultConfigs.add(resultConfig)
    }

    fun getResultConfigs(): List<ResultConfig> {
        return resultConfigs
    }

    fun getServletFullPath(): String {
        return "$packageNameSpace/$name"
    }

    fun getActionMethod(project: Project, url: String): List<PsiMethod> {
        val result: MutableList<PsiMethod> = ArrayList()
        val methodName = if (url.contains("_")) url.substring(url.indexOf("_") + 1) else "execute"
        println("url: $url -> methodName: $methodName")
        val classFile = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project))
        if (classFile != null) {
            result.addAll(classFile.allMethods.stream().filter { m ->
                if (methodName == "execute") StrutsActionUtil.isStrutsMethod(m) else m.name == methodName
            }.collect(Collectors.toList()))
        }
        return result
    }


    fun matchServlet(url: String): Boolean {
        val servletPath = getServletFullPath()
        val pureUrl = if (url.contains("_")) url.substring(0, url.indexOf("_")) else url
        return if (name.contains("_")) {
            servletPath.substring(0, servletPath.indexOf("_")) == pureUrl
        } else {
            pureUrl == servletPath
        }
    }

    data class ResultConfig(val name: String, val type: String, val viewPath: String)
}

