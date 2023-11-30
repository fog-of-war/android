package com.fow.fogofwar

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val mHandler = Handler(Looper.getMainLooper())
    private lateinit var mWebView: WebView
    private var isCmdProcessing = false
    companion object {
        private const val GALLERY_REQUEST_CODE = 1002
        private const val LOCATION_SETTINGS_REQUEST_CODE = 1003
        private const val FILE_CHOOSER_REQUEST_CODE = 1
    }
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraPhotoFile: File? = null // 카메라로 찍은 사진 파일

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupWebView()
        // checkLocationServicesAndRequestPermissions()
    }

    override fun onStart() {
        super.onStart()
        checkLocationServicesAndRequestPermissions()// 앱 실행 시 위치 서비스 확인
    }
    override fun onResume() {
        super.onResume()
        // checkLocationServicesAndRequestPermissions()// 앱 실행 시 위치 서비스 확인
    }

    /**앱을 켤때 마다 위치 기능 활성화 요청*/
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.DONUT)
    private fun checkLocationServicesAndRequestPermissions() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            // 위치 서비스가 비활성화되었을 때만 다이얼로그 표시
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.apply {
                setTitle("위치 서비스 활성화 필요")
                setMessage("계속 진행하려면 기기에서 위치 서비스를 활성화해주세요.")
                setPositiveButton("확인") { _, _ ->
                    // Open the location settings screen
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    try {
                        startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE)
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
                setNegativeButton("아니오") { _, _ ->
                    // Handle cancellation if needed
                }
                setCancelable(false)
            }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        } else {
            // 위치 서비스가 활성화되어 있으면 권한 확인 및 요청
            checkAndRequestPermissions()
        }
    }

    /**웹뷰 실행 메서드*/
    private fun setupWebView() {
        mWebView = findViewById(R.id.webView)
        configureWebViewSettings()
        // 기존 WebViewClient 설정
        mWebView.webViewClient = WebViewClientClass()
        // WebChromeClient 설정 부분
        mWebView.webChromeClient = object : WebChromeClient() {
            // For Lollipop 5.0+ Devices
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                mFilePathCallback = filePathCallback

                // 갤러리에서 이미지를 선택하기 위한 인텐트
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                // 카메라로 사진을 찍기 위한 인텐트
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                // 카메라 인텐트에 대한 파일 URI 생성
                val photoFile: File? = createImageFile() // createImageFile() 메소드 구현 필요
                if (photoFile != null) {
                    val photoURI = FileProvider.getUriForFile(
                        this@MainActivity,
                        "com.fow.fogofwar.fileprovider", // 여기에 파일 프로바이더의 권한을 넣으세요
                        photoFile
                    )
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }

                // 인텐트 선택기로 카메라와 갤러리 중 선택
                val chooserIntent = Intent.createChooser(galleryIntent, "Select Source")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

                startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE)
                return true
            }

            // 추가: onGeolocationPermissionsShowPrompt
            // 오버라이딩
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                callback?.invoke(origin, true, false)
            }
        }
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

    /**갤러리에서 이미지 불러오기*/
    fun fetchImageFromGallery(filePathCallback: ValueCallback<Array<Uri>>?) { // 그리고 이 함수도 추가
        mFilePathCallback = filePathCallback
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    /**갤러리에서 이미지 불러오기*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                mFilePathCallback?.onReceiveValue(arrayOf(uri))
            } ?: run {
                mFilePathCallback?.onReceiveValue(null)
            }
        } else if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            cameraPhotoFile?.let { file ->
                val uri = Uri.fromFile(file)
                mFilePathCallback?.onReceiveValue(arrayOf(uri))
            } ?: run {
                mFilePathCallback?.onReceiveValue(null)
            }
        }

        mFilePathCallback = null
    }

    /** 카메라 인텐트를 위한 임시 파일을 생성하는 메소드*/
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).also {
            cameraPhotoFile = it // 카메라로 찍은 사진 파일 저장
        }
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
            if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) { add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionRequest.launch(permissionsToRequest.toTypedArray())
        }
        // else {
        // 모든 권한이 이미 부여된 경우에도 checkLocationServices() 호출
        //   checkLocationServices()
        //}
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
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
            view?.loadUrl(url)
            return true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            view?.evaluateJavascript("""
            function requestLocationFromAndroidApp() {
                if (window.NativeBridge) {
                    NativeBridge.callLocationPos("handleLocationFromApp");
                }
            }

            function handleLocationFromApp(latitude, longitude) {
                console.log("From Android App: ", latitude, longitude);
            }
            """, null)
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
