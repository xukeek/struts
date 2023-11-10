package com.github.xukeek.struts.extensions

import com.github.xukeek.struts.utils.StrutsActionUtil
import com.github.xukeek.struts.utils.StrutsActionUtil.getAllFreemarkerFilesAboutThisMethod
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class LineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
            element: PsiElement,
            result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        element.project
        val method: PsiElement? = element.parent
        if (element is PsiIdentifier && method is PsiMethod) {
            if (StrutsActionUtil.isStrutsMethod(method)) {
                val module = ModuleUtil.findModuleForPsiElement(element)
                if (module != null) {
                    val aboutFiles = getAllFreemarkerFilesAboutThisMethod(method)
                    if (aboutFiles.isNotEmpty()) {
                        val builder = NavigationGutterIconBuilder.create(StrutsActionUtil.STRUTS_ICON)
                                .setTargets(*aboutFiles).setTooltipText("Navigate to a simple property")
                        result.add(builder.createLineMarkerInfo(element))
                    }
                }
            }
        }
    }
}
