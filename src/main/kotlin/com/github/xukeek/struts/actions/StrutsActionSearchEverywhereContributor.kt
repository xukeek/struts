package com.github.xukeek.struts.actions

import com.github.xukeek.struts.services.MyProjectService
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor
import com.intellij.ide.actions.searcheverywhere.RecentFilesSEContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.codeStyle.MinusculeMatcher
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.NotNull
import java.util.stream.Collectors


class StrutsActionSearchEverywhereContributor(event: @NotNull AnActionEvent) : RecentFilesSEContributor(event) {
    override fun getSearchProviderId(): String {
        return StrutsActionSearchEverywhereContributor::class.java.simpleName
    }

    override fun getGroupName(): String {
        return "Struts Action"
    }

    override fun getSortWeight(): Int {
        return 80;
    }

    override fun getElementPriority(element: Any, searchPattern: String): Int {
        return super.getElementPriority(element, searchPattern) + 6
    }

    override fun isShownInSeparateTab(): Boolean {
        return true;
    }

    override fun fetchWeightedElements(
        pattern: String,
        progressIndicator: ProgressIndicator,
        consumer: Processor<in FoundItemDescriptor<Any>>
    ) {
        if (myProject == null) return

        val projectService: MyProjectService = myProject.getService(MyProjectService::class.java)

        val searchString = filterControlSymbols(pattern)
        val matchUrl =
            if (searchString.contains("_")) searchString.substring(0, searchString.indexOf("_")) else searchString
        val matcher = createMatcher(matchUrl)
        val opened = FileEditorManager.getInstance(myProject).selectedFiles

        val res: MutableList<FoundItemDescriptor<Any>> = ArrayList()
        ProgressIndicatorUtils.yieldToPendingWriteActions()

        ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(
            {
                var configs = projectService.getAllActionConfigs().stream()
                if (!StringUtil.isEmptyOrSpaces(searchString)) {
                    configs = configs.filter { c -> matcher.matches(c.getServletFullPath()) }
                }
                res.addAll(configs
                    .filter { c -> !opened.contains(c.xmlFile.virtualFile) && c.xmlFile.virtualFile.isValid }
                    .flatMap { c -> c.getActionMethod(myProject, searchString).stream() }
                    .map { m -> FoundItemDescriptor<Any>(m, matcher.matchingDegree(m.name)) }
                    .collect(Collectors.toList())
                )
                ContainerUtil.process(res, consumer)
            }, progressIndicator
        )
    }

    private fun createMatcher(searchString: String): MinusculeMatcher {
        val builder = NameUtil.buildMatcher("*$searchString")
        return builder.build()
    }

    class Factory : SearchEverywhereContributorFactory<Any> {
        override fun createContributor(initEvent: AnActionEvent): SearchEverywhereContributor<Any> {
            return StrutsActionSearchEverywhereContributor(initEvent)
        }
    }
}