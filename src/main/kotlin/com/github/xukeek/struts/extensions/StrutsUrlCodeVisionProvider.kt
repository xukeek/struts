package com.github.xukeek.struts.extensions;

import com.github.xukeek.struts.utils.StrutsActionUtil
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import java.awt.event.MouseEvent


class StrutsUrlCodeVisionProvider : CodeVisionProviderBase() {

    override val id: String
        get() = "com.github.xukeek.struts.action"
    override val name: String
        get() = CodeInsightBundle.message("com.github.xukeek.struts.action.url")
    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = emptyList()
    override val groupId: String
        get() = "com.github.xukeek.struts.action.url"

    override fun acceptsElement(element: PsiElement): Boolean {
        val method: PsiElement? = element.parent
        if (element is PsiIdentifier && method is PsiMethod) {
            return StrutsActionUtil.isStrutsMethod(method)
        }
        return false
    }

    override fun acceptsFile(file: PsiFile): Boolean {
        if (file.language != JavaLanguage.INSTANCE) return false
        if (file is PsiClass) {
            val psiModifierList: PsiModifierList? = file.modifierList
            if (psiModifierList != null) {
                val annotations = psiModifierList.annotations
                for (annotation in annotations) {
                    if (annotation.qualifiedName == "com.rayse.plugins.core.trace.Trace") {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        return "hello"
    }

    override fun handleClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        TODO("Not yet implemented")
    }
}
