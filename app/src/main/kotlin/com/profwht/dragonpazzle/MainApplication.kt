package com.profwht.dragonpazzle

import android.app.Application
import com.appsflyer.AppsFlyerLib

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeAppsFlyer()
    }

    private fun initializeAppsFlyer() {
        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_KEY", null, this)
        AppsFlyerLib.getInstance().start(this)
    }
}
