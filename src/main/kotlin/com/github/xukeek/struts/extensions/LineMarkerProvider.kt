package com.github.xukeek.struts.extensions

import com.github.xukeek.struts.services.MyProjectService
import com.github.xukeek.struts.utils.StrutsActionUtil
import com.github.xukeek.struts.utils.StrutsXmlUtil
import com.github.xukeek.struts.wrappers.ActionConfig
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import java.util.*

class LineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val project = element.project
        val method: PsiElement? = element.parent
        if (element is PsiIdentifier && method is PsiMethod) {
            if (StrutsActionUtil.isStrutsMethod(method)) {
                val module = ModuleUtil.findModuleForPsiElement(element)
                if (module != null) {
                    val projectService: MyProjectService = project.getService(MyProjectService::class.java)
                    val moduleActionConfigs: List<ActionConfig> = projectService.getActionConfigs()
                    val aboutFiles: MutableList<PsiElement> = ArrayList()
                    val psiClass: PsiClass? = method.containingClass
                    if (psiClass != null) {
                        val allMethodReturnStr: Set<String> = StrutsActionUtil.summarizeAllReturns(method)
                        val allMethodReturnFilePaths =
                            moduleActionConfigs.filter { a -> a.className == psiClass.qualifiedName }
                                .flatMap { a ->
                                    a.getResultConfigs().filter { c -> allMethodReturnStr.contains(c.name) }
                                }
                                .map { c -> c.viewPath }
                        aboutFiles.addAll(StrutsXmlUtil.getActionViewFiles(project, allMethodReturnFilePaths))
                    }
                    val targets = aboutFiles.toTypedArray()
                    if (aboutFiles.size > 0) {
                        val builder =
                            NavigationGutterIconBuilder.create(StrutsActionUtil.STRUTS_ICON).setTargets(*targets)
                                .setTooltipText("Navigate to a simple property")
                        result.add(builder.createLineMarkerInfo(element))
                    }
                }
            }
        }
    }
}