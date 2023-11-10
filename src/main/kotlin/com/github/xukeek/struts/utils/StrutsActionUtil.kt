package com.github.xukeek.struts.utils

import com.github.xukeek.struts.services.MyProjectService
import com.github.xukeek.struts.wrappers.ActionConfig
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import java.util.*

object StrutsActionUtil {
    private val ANNOTATION_NAMES: MutableSet<String> = HashSet()
    val STRUTS_ICON = IconLoader.getIcon("/icons/action_small.svg", javaClass)

    init {
        ANNOTATION_NAMES.add("com.rayse.plugins.core.trace.Trace")
        ANNOTATION_NAMES.add("com.rayse.plugins.core.auth.IgnoreAuth")
        ANNOTATION_NAMES.add("com.rayse.plugins.auth.annotations.RightMethod")
    }

    private fun summarizeAllReturns(method: PsiMethod): Set<String> {
        val methodReturns: MutableSet<String> = HashSet()
        val returnStatements = PsiUtil.findReturnStatements(method)
        Arrays.stream(returnStatements).forEach { s: PsiReturnStatement ->
            val value = s.returnValue
            resolveLiteralFromExpression(methodReturns, value)
        }
        return methodReturns
    }

    private fun resolveLiteralFromExpression(literals: MutableSet<String>, expression: PsiExpression?) {
        if (expression != null) {
            if (expression is PsiLiteralExpression) {
                val v = expression.value
                if (v != null) {
                    literals.add(v.toString())
                }
            } else if (expression is PsiMethodCallExpression) {
                val valueCall = expression.resolveMethod()
                if (valueCall != null) {
                    literals.addAll(summarizeAllReturns(valueCall))
                }
            } else if (expression is PsiConditionalExpression) {
                if (expression.thenExpression != null) {
                    resolveLiteralFromExpression(literals, expression.thenExpression)
                }
                if (expression.elseExpression != null) {
                    resolveLiteralFromExpression(literals, expression.elseExpression)
                }
            } else if (expression is PsiReferenceExpression) {
                for (ref in expression.getReferences()) {
                    val field = ref.resolve()
                    if (field is PsiField) {
                        val fieldInitializer = field.initializer
                        resolveLiteralFromExpression(literals, fieldInitializer)
                    }
                }
            }
        }
    }

    fun isStrutsMethod(method: PsiMethod?): Boolean {
        return method != null && method.returnType != null &&
                method.returnType!!.equalsToText(CommonClassNames.JAVA_LANG_STRING) &&
                method.parameterList.parametersCount == 0 &&
                method.modifierList.annotations.isNotEmpty() &&
                Arrays.stream(method.modifierList.annotations)
                        .anyMatch { a: PsiAnnotation -> ANNOTATION_NAMES.contains(a.qualifiedName) }
    }

    fun getAllFreemarkerFilesAboutThisMethod(method: PsiMethod): Array<PsiElement> {
        val project = method.project
        val projectService: MyProjectService = project.getService(MyProjectService::class.java)
        val moduleActionConfigs: List<ActionConfig> = projectService.getActionConfigs()
        val aboutFiles: MutableList<PsiElement> = ArrayList()
        val psiClass: PsiClass? = method.containingClass
        if (psiClass != null) {
            val allMethodReturnStr: Set<String> = summarizeAllReturns(method)
            val allMethodReturnFilePaths =
                    moduleActionConfigs.filter { a -> a.className == psiClass.qualifiedName }
                            .flatMap { a ->
                                a.getResultConfigs().filter { c -> allMethodReturnStr.contains(c.name) }
                            }
                            .map { c -> c.viewPath }
            aboutFiles.addAll(StrutsXmlUtil.getActionViewFiles(project, allMethodReturnFilePaths))
        }
        return aboutFiles.toTypedArray()
    }
}
