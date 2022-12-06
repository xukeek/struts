package com.github.xukeek.struts.extensions

import com.github.xukeek.struts.services.MyApplicationService
import com.google.gson.stream.JsonWriter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.OpenFileDescriptor
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
        var actionUrl = ""
        if (parameterAction != null && parameterAction.size == 1) {
            actionUrl = getRequestURIFromURL(parameterAction[0])
        }
        if (StringUtils.isNotEmpty(actionUrl)) {
            findActionAndOpenFile(actionUrl, request, context)
        }
        return null
    }

    override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
        return true;
    }

    override fun getServiceName(): String {
        return "struts"
    }

    @Throws(IOException::class)
    private fun findActionAndOpenFile(uri: String, request: FullHttpRequest, context: ChannelHandlerContext) {
        val application = ApplicationManager.getApplication();
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
                        var lineNumber = 0;
                        if (m != null) {
                            val documentManager = PsiDocumentManager.getInstance(project)
                            val document: Document? = documentManager.getDocument(actionClassFile.containingFile)
                            if (document != null) {
                                lineNumber = document.getLineNumber(m.textOffset)
                            }
                        }
                        ApplicationManager.getApplication().invokeLater(Runnable {
                            //OpenFileAction.openFile(actionClassFile.containingFile?.virtualFile?.path.toString(), project);
                            if (actionClassFile.containingFile?.virtualFile != null) {
                                OpenFileDescriptor(project, actionClassFile.containingFile?.virtualFile!!, lineNumber, 1).navigate(true);
                            }
                        })
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
            "execute";
        } else {
            uri.substring(methodIndex + 1)
        }
    }
}