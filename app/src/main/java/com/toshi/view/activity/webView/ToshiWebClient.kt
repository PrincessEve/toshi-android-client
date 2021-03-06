/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.view.activity.webView

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.toshi.R
import com.toshi.util.webView.WebViewCookieJar
import com.toshi.view.BaseApplication
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.net.UnknownHostException
import javax.net.ssl.SSLPeerUnverifiedException

class ToshiWebClient(
        private val context: Context,
        private val updateListener: () -> Unit
) : WebViewClient() {

    private val toshiManager by lazy { BaseApplication.get().toshiManager }
    private val httpClient by lazy { OkHttpClient.Builder().cookieJar(WebViewCookieJar()).build() }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (request == null || view == null) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            /*
            * In order to follow redirects properly, we return null in interceptRequest().
            * Doing this breaks the web3 injection on the resulting page, so we have to reload to
            * make sure web3 is available.
            * */

            if (request.isForMainFrame && request.isRedirect) {
                view.loadUrl(request.url.toString())
                return true
            }
        }
        // TODO: handle API < 24 where isMainFrame is not available
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return null
    }

    @TargetApi(21)
    override fun shouldInterceptRequest(view: WebView?, webRequest: WebResourceRequest?): WebResourceResponse? {
        if (webRequest?.method != "GET") return null
        updateListener()
        if (!webRequest.isForMainFrame) return null
        return interceptRequest(webRequest)
    }

    @TargetApi(21)
    private fun interceptRequest(webRequest: WebResourceRequest): WebResourceResponse? {
        val request = buildRequest(webRequest)
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.priorResponse()?.isRedirect == true) null
            else buildWebResponse(response)
        } catch (e: SSLPeerUnverifiedException) {
            null
        } catch (e: UnknownHostException) {
            null
        }
    }

    @TargetApi(21)
    private fun buildRequest(webRequest: WebResourceRequest): Request {
        val requestBuilder = Request.Builder()
                .get()
                .url(webRequest.url.toString())
        webRequest.requestHeaders.forEach {
            requestBuilder.addHeader(it.key, it.value)
        }
        return requestBuilder.build()
    }

    private fun buildWebResponse(response: Response): WebResourceResponse? {
        val body = response.body()?.string() ?: ""
        val script = loadSofaScript()
        val injectedBody = injectScripts(body, script)
        val byteStream = ByteArrayInputStream(injectedBody.toByteArray())
        val headerParser = HeaderParser()
        val contentType = headerParser.getContentTypeHeader(response)
        val charset = headerParser.getCharset(contentType)
        val mimeType = headerParser.getMimeType(contentType)
        return WebResourceResponse(mimeType, charset, byteStream)
    }

    private fun loadSofaScript(): String {
        val sb = StringBuilder()
        val rcpUrl = "window.SOFA = {" +
                "config: {accounts: [\"${getWallet().paymentAddress}\"]," +
                "rcpUrl: \"${context.getString(R.string.rcp_url)}\"}" +
                "};"
        sb.append(rcpUrl)
        val stream = context.resources.openRawResource(R.raw.sofa)
        val reader = BufferedReader(InputStreamReader(stream))
        val text: List<String> = reader.readLines()
        for (line in text) sb.append(line).append("\n")
        return "<script type=\"text/javascript\">$sb</script>"
    }

    private fun getWallet() = toshiManager.wallet.toBlocking().value()

    private fun injectScripts(body: String?, script: String): String {
        val safeBody = body ?: ""
        val position = getInjectionPosition(safeBody)
        if (position == -1) return safeBody
        val beforeTag = safeBody.substring(0, position)
        val afterTab = safeBody.substring(position)
        return beforeTag + script + afterTab
    }

    private fun getInjectionPosition(body: String): Int {
        val htmlTag = "<script"
        return body.toLowerCase().indexOf(htmlTag)
    }
}