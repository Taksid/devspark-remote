package com.profwht.dragonpazzle

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ConfigManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("dragonpazzle_prefs", Context.MODE_PRIVATE)
    private val CONFIG_URL = "https://config.pookieai.ink/config.json"
    private val CONFIG_CACHE_KEY = "cached_config"
    private val COUNTRY_CACHE_KEY = "cached_country"
    private val CACHE_TTL = 3600000L // 1 hour

    companion object {
        var cachedConfig: JSONObject? = null
    }

    fun fetchConfig(callback: (JSONObject?) -> Unit) {
        val cachedJson = sharedPreferences.getString(CONFIG_CACHE_KEY, null)
        val cacheTimestamp = sharedPreferences.getLong("${CONFIG_CACHE_KEY}_time", 0)

        if (cachedJson != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL) {
            cachedConfig = JSONObject(cachedJson)
            callback(cachedConfig)
            return
        }

        Thread {
            try {
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
                val request = Request.Builder().url(CONFIG_URL).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val configJson = response.body?.string() ?: "{}"
                    sharedPreferences.edit().apply {
                        putString(CONFIG_CACHE_KEY, configJson)
                        putLong("${CONFIG_CACHE_KEY}_time", System.currentTimeMillis())
                        apply()
                    }
                    cachedConfig = JSONObject(configJson)
                    callback(cachedConfig)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                callback(null)
            }
        }.start()
    }

    fun getUserCountry(callback: (String) -> Unit) {
        val cachedCountry = sharedPreferences.getString(COUNTRY_CACHE_KEY, null)
        val countryTimestamp = sharedPreferences.getLong("${COUNTRY_CACHE_KEY}_time", 0)

        if (cachedCountry != null && System.currentTimeMillis() - countryTimestamp < CACHE_TTL) {
            callback(cachedCountry)
            return
        }

        Thread {
            try {
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
                val request = Request.Builder().url("https://ipapi.co/json/").build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val country = json.optString("country_code", "US")
                    sharedPreferences.edit().apply {
                        putString(COUNTRY_CACHE_KEY, country)
                        putLong("${COUNTRY_CACHE_KEY}_time", System.currentTimeMillis())
                        apply()
                    }
                    callback(country)
                } else {
                    callback("US")
                }
            } catch (e: Exception) {
                callback("US")
            }
        }.start()
    }
}
