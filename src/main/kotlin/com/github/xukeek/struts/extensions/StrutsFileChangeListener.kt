package com.github.xukeek.struts.extensions

import com.github.xukeek.struts.services.MyProjectService
import com.github.xukeek.struts.utils.StrutsXmlUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class StrutsFileChangeListener : AsyncFileListener {

    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        return StrutsFileChangeApplier(events)
    }

    inner class StrutsFileChangeApplier(private val events: MutableList<out VFileEvent>) :
        AsyncFileListener.ChangeApplier {
        override fun afterVfsChange() {
            for (event in events) {
                if (event.isFromSave && event.file != null) {
                    val file = event.file
                    if (file != null && file.name == StrutsXmlUtil.FILE_NAME) {
                        val openedProjects = ProjectManager.getInstance().openProjects
                        for (project in openedProjects) {
                            if (ProjectFileIndex.getInstance(project).isInContent(file)) {
                                Notifications.Bus.notify(
                                    Notification(
                                        "intellij-struts",
                                        "Struts file changed",
                                        event.path,
                                        NotificationType.INFORMATION
                                    )
                                )
                                project.getService(MyProjectService::class.java).reloadFile(file)
                            }
                        }
                    }
                }
            }
        }
    }
}