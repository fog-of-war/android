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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest

class MainActivity : AppCompatActivity() {

    private val mHandler = Handler(Looper.getMainLooper())
    private lateinit var mWebView: WebView
    private var bCmdProcess = false

    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
            }
            else -> {
                // No location access granted.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestLocationPermissions()

        setupWebView()
    }

    /**웹뷰 실행메서드*/
    private fun setupWebView() {
        mWebView = findViewById(R.id.webView)

        mWebView.settings.apply {
            javaScriptEnabled = true   // 웹뷰 자바스크립트 허용
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            domStorageEnabled = true
            userAgentString = "Chrome/56.0.0.0 Mobile" // User Agent 설정
        }

        mWebView.webViewClient = WebViewClientClass()
        mWebView.webChromeClient = WebChromeClient()

        mWebView.addJavascriptInterface(NativeBridge(this, mHandler, mWebView), "NativeBridge")
        mWebView.loadUrl("https://www.yubinhome.com")
    }

    /**뒤로가기*/
    override fun onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**위치 인증 요청 함수*/
    private fun checkAndRequestLocationPermissions() {
        if (!hasLocationPermissions()) {
            locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
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
    inner class NativeBridge(private val mActivity: Activity, private val mHandler: Handler, private val mWebView: WebView) {

        @JavascriptInterface
        fun callLocationPos(strCallbackFunc: String) {
            if (!hasLocationPermissions()) {
                checkAndRequestLocationPermissions()
                return
            }

            Log.i("CALL NativeBridge callLocationPos", "START")
            if (bCmdProcess) return

            bCmdProcess = true
            mHandler.post {
                try {
                    val locMgr = mActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val criteria = Criteria().apply {
                        accuracy = Criteria.ACCURACY_COARSE
                    }
                    val bestProv = locMgr.getBestProvider(criteria, true) ?: return@post

                    if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // Handle lack of location permissions here, if needed
                        return@post
                    }

                    val loc: Location? = locMgr.getLastKnownLocation(bestProv)

                    loc?.let {
                        val strJavascript = "$strCallbackFunc('${it.latitude}','${it.longitude}')"
                        mWebView.loadUrl("javascript:$strJavascript")
                    }
                } catch (exLoc: Exception) {
                    val strJavascript = "alert('위치확인오류')"
                    mWebView.loadUrl("javascript:$strJavascript")
                }
                bCmdProcess = false
            }
        }
    }
}
