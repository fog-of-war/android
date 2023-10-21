package com.example.myapplication2

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val mHandler = Handler(Looper.getMainLooper())
    lateinit var mWebView: WebView
    private var bCmdProcess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**웹뷰 부분*/
        mWebView = findViewById(R.id.webView)
        mWebView.settings.run {
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

        mWebView.webViewClient = WebViewClientClass()
        mWebView.webChromeClient = WebChromeClient()
        mWebView.loadUrl("https://www.yubinhome.com")

        mWebView.addJavascriptInterface(NativeBridge(this), "Android")
    }

    /**뒤로가기*/
    override fun onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**SSL 인증서 무시*/
    private inner class WebViewClientClass : WebViewClient() {
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed()
        }
        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
            view?.loadUrl(url)
            return true
        }
    }

    /**네이티브 브릿지*/
    inner class NativeBridge(private val mActivity: Activity) {

        @JavascriptInterface
        fun callLocationPos(strCallbackFunc: String) {
            if (ContextCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }
            if (ContextCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }

            Log.i("CALL NativeBridge callLocationPos", "START")
            if (bCmdProcess) return

            bCmdProcess = true
            mHandler.post {
                try {
                    val locMgr = mActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val criteria = Criteria()
                    criteria.accuracy = Criteria.ACCURACY_COARSE
                    val bestProv = locMgr.getBestProvider(criteria, true)
                    val result1 = ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    val result2 = ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)

                    if (result1 != PackageManager.PERMISSION_GRANTED && result2 != PackageManager.PERMISSION_GRANTED) {
                        return@post
                    }

                    val loc: Location = locMgr.getLastKnownLocation(bestProv.toString()) ?: return@post
                    val strJavascript = "$strCallbackFunc('${loc.latitude}','${loc.longitude}')"
                    Log.e("", strJavascript)
                    mWebView.loadUrl("javascript:$strJavascript")
                } catch (exLoc: Exception) {
                    val strJavascript = "alert('위치확인오류')"
                    mWebView.loadUrl("javascript:$strJavascript")
                }
                bCmdProcess = false
            }
        }
    }
}

/**크롬 맞춤탭 부분*/
//        val url = "https://www.yubinhome.com"
//        val intent = CustomTabsIntent.Builder()
//            .setUrlBarHidingEnabled(true)
//            .setShowTitle(true)
//            .build();
//
//
//        intent.launchUrl(this@MainActivity, Uri.parse(url)) // //        private fun openCustomTab(s: String) {
//
//     }
