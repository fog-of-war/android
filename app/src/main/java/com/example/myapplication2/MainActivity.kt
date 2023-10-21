package com.example.myapplication2

import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**크롬 맞춤탭 부분*/
//        val url = "https://www.yubinhome.com"
//        val intent = CustomTabsIntent.Builder()
//            .setUrlBarHidingEnabled(true)
//            .setShowTitle(true)
//            .build();
//
//
//        intent.launchUrl(this@MainActivity, Uri.parse(url))


        val myWebView: WebView = findViewById(R.id.webView)

        myWebView.settings.run {
            // 웹뷰 자바스크립트 허용
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            domStorageEnabled = true
            // User Agent 설정
            userAgentString = "Chrome/56.0.0.0 Mobile"
        }

        myWebView.webViewClient = WebViewClientClass()
        myWebView.webChromeClient = WebChromeClient()
        myWebView.loadUrl("https://www.yubinhome.com")
    }

    private fun openCustomTab(s: String) {

    }

    override fun onBackPressed() { // 뒤로가기 기능 구현
        val myWebView: WebView = findViewById(R.id.webView)
        if (myWebView.canGoBack()) {
            myWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private inner class WebViewClientClass : WebViewClient() {
        // SSL 인증서 무시
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed()
        }

        // 페이지 내에서만 url 이동하게끔 만듬
        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
            view?.loadUrl(url)
            return true
        }
    }
}
