package com.github.xukeek.struts.extensions

import com.github.xukeek.struts.services.MyApplicationService
import com.github.xukeek.struts.utils.StrutsActionUtil
import com.google.gson.stream.JsonWriter
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.apache.commons.lang.StringUtils
import org.jetbrains.ide.RestService
import java.io.IOException


class ChromeRestService : RestService() {

    override fun execute(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): String? {
        val parameters = urlDecoder.parameters()
        val parameterAction = parameters["action"]
        val parameterType = parameters["type"]
        var actionUrl = ""
        var actionType = "action"
        if (parameterAction != null && parameterAction.size == 1) {
            actionUrl = getRequestURIFromURL(parameterAction[0])
        }
        if (parameterType != null && parameterType.size == 1) {
            actionType = parameterType[0]
        }
        Notifications.Bus.notify(
            Notification(
                "intellij-struts",
                "Open link..........",
                actionUrl,
                NotificationType.INFORMATION
            )
        )
        if (StringUtils.isNotEmpty(actionUrl)) {
            findActionAndOpenFile(actionUrl, actionType, request, context)
        }
        return null
    }

    override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
        return true;
    }

    override fun getServiceName(): String {
        return "struts"
    }

    override fun isSupported(request: FullHttpRequest): Boolean {
        return request.uri().contains("gmp") && request.uri().contains("action");
    }

    @Throws(IOException::class)
    private fun findActionAndOpenFile(
        uri: String,
        type: String,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ) {
        val application = ApplicationManager.getApplication()
        val strutsService: MyApplicationService = application.getService(MyApplicationService::class.java)
        application.executeOnPooledThread {
            application.runReadAction {
                strutsService.findActionAndOpenIt(uri) { project, a ->
                    val byteOut = BufferExposingByteArrayOutputStream()
                    val actionClassFile = JavaPsiFacade.getInstance(project)
                        .findClass(a.className, GlobalSearchScope.projectScope(project))
                    if (actionClassFile != null) {
                        val methodName = getRequestMethodFromURI(uri);
                        val m = actionClassFile.methods.first { m -> m.name == methodName }
                        if (m != null) {
                            if (type == "action" || type == "actionTemplate") {
                                var lineNumber = 0
                                val documentManager = PsiDocumentManager.getInstance(project)
                                val document: Document? = documentManager.getDocument(actionClassFile.containingFile)
                                if (document != null) {
                                    lineNumber = document.getLineNumber(m.textOffset)
                                }
                                ApplicationManager.getApplication().invokeLater(Runnable {
                                    if (actionClassFile.containingFile?.virtualFile != null) {
                                        OpenFileDescriptor(
                                            project,
                                            actionClassFile.containingFile?.virtualFile!!,
                                            lineNumber,
                                            1
                                        ).navigate(true);
                                    }
                                })
                            }
                            if (type == "template" || type == "actionTemplate") {
                                if (StrutsActionUtil.isStrutsMethod(m)) {
                                    val module = ModuleUtil.findModuleForPsiElement(m)
                                    if (module != null) {
                                        val aboutFiles = StrutsActionUtil.getAllFreemarkerFilesAboutThisMethod(m)
                                        if (aboutFiles.isNotEmpty()) {
                                            for (aboutFile in aboutFiles) {
                                                ApplicationManager.getApplication().invokeLater(Runnable {
                                                    if (aboutFile.containingFile?.virtualFile != null) {
                                                        OpenFileDescriptor(
                                                            project,
                                                            aboutFile.containingFile.virtualFile,
                                                            0,
                                                            1
                                                        ).navigate(true);
                                                    }
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        val writer: JsonWriter = createJsonWriter(byteOut)
                        writer.beginObject()
                        writer.name("info").value(actionClassFile.containingFile?.virtualFile?.path)
                        writer.endObject()
                        writer.close()
                        send(byteOut, request, context)
                    }
                }
            }
        }
    }

    private fun getRequestURIFromURL(url: String): String {
        val begin = url.indexOf("/action")
        val end = url.indexOf("?")
        if (begin > 0 && end < 0) {
            return url.substring(begin)
        } else if (begin > 0 && end > 0) {
            return url.substring(begin, end)
        }
        return url
    }

    private fun getRequestMethodFromURI(uri: String): String {
        val methodIndex = uri.indexOf("_")
        return if (methodIndex < 0) {
            "execute"
        } else {
            uri.substring(methodIndex + 1)
        }
    }
}
