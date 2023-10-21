package com.example.myapplication2

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
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

class MainActivity : AppCompatActivity() {

    private val mHandler = Handler(Looper.getMainLooper())
    private lateinit var mWebView: WebView
    private var isCmdProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndRequestPermissions()
        setupWebView()
    }

    /**웹뷰 실행 메서드*/
    private fun setupWebView() {
        mWebView = findViewById(R.id.webView)
        configureWebViewSettings()
        mWebView.webViewClient = WebViewClientClass()
        mWebView.webChromeClient = WebChromeClient()
        mWebView.addJavascriptInterface(NativeBridge(this, mHandler, mWebView), "NativeBridge")
        mWebView.loadUrl("https://www.yubinhome.com")
    }

    /**웹뷰 설정 메서드*/
    private fun configureWebViewSettings() {
        mWebView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            domStorageEnabled = true
            userAgentString = "Chrome/56.0.0.0 Mobile"
        }
    }

    /**뒤로 가기 버튼*/
    override fun onBackPressed() {
        if (mWebView.canGoBack()) mWebView.goBack()
        else super.onBackPressed()
    }

    /**권한 요청*/
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {}
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {}
            permissions.getOrDefault(Manifest.permission.CAMERA, false) -> {}
            else -> {}
        }
    }

    /**권한 요청 메서드*/
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>().apply {
            if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (!hasPermission(Manifest.permission.CAMERA)) add(Manifest.permission.CAMERA)
        }
        if (permissionsToRequest.isNotEmpty()) permissionRequest.launch(permissionsToRequest.toTypedArray())
    }

    /**권한 여부 확인*/
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**웹뷰 클라이언트*/
    private inner class WebViewClientClass : WebViewClient() {
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed()
        }
        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
            view?.loadUrl(url)
            return true
        }
    }

    /**네이티브 브릿지, 네이티브 기능 사용*/
    inner class NativeBridge(private val mActivity: Activity, private val mHandler: Handler, private val mWebView: WebView) {

        @JavascriptInterface
        fun callLocationPos(strCallbackFunc: String) {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                checkAndRequestPermissions()
                return
            }
            if (isCmdProcessing) return
            isCmdProcessing = true
            mHandler.post {
                fetchLocation(strCallbackFunc)
                isCmdProcessing = false
            }
        }

        private fun fetchLocation(callbackFunc: String) {
            try {
                val locMgr = mActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val criteria = Criteria().apply { accuracy = Criteria.ACCURACY_COARSE }
                val bestProvider = locMgr.getBestProvider(criteria, true) ?: return

                if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    throw SecurityException("Location permissions not granted.")
                }

                val loc: Location? = locMgr.getLastKnownLocation(bestProvider)
                loc?.let {
                    val script = "$callbackFunc('${it.latitude}','${it.longitude}')"
                    mWebView.loadUrl("javascript:$script")
                }
            } catch (e: SecurityException) {
                mWebView.loadUrl("javascript:alert('Permission for location was not granted.')")
            } catch (e: Exception) {
                mWebView.loadUrl("javascript:alert('위치확인오류')")
            }
        }

        @JavascriptInterface
        fun callCamera() {
            if (!hasPermission(Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(mActivity, arrayOf(Manifest.permission.CAMERA), 1)
                return
            }
            if (isCmdProcessing) return
            isCmdProcessing = true
            mHandler.post {
                launchCameraActivity()
                isCmdProcessing = false
            }
        }

        private fun launchCameraActivity() {
            try {
                if (!hasPermission(Manifest.permission.CAMERA)) {
                    throw SecurityException("Camera permission not granted.")
                }
                val intent = Intent(mActivity, UtilCamera::class.java)
                startActivity(intent)
            } catch (e: SecurityException) {
                mWebView.loadUrl("javascript:alert('Permission for camera was not granted.')")
            } catch (e: Exception) {
                Log.e("Error", e.toString())
            }
        }
    }
}
