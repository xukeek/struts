package com.github.xukeek.struts.services

import com.github.xukeek.struts.MyBundle
import com.github.xukeek.struts.wrappers.ActionConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

@Service(Service.Level.APP)
class MyApplicationService {

    init {
        println(MyBundle.message("applicationService"))
    }

    fun findActionAndOpenIt(actionUrl: String, callBack: (Project, ActionConfig) -> Any) {
        val openedProjects = ProjectManager.getInstance().openProjects
        for (i in openedProjects.size - 1 downTo 0) {
            val mappedProject = openedProjects[i]
            val projectService: MyProjectService = mappedProject.getService(MyProjectService::class.java)
            val moduleActionConfigs: List<ActionConfig> = projectService.getActionConfigs()
            for (moduleActionConfig in moduleActionConfigs) {
                if (moduleActionConfig.matchServlet(actionUrl)) {
                    callBack.invoke(mappedProject, moduleActionConfig)
                }
            }
        }
    }
}
