package com.gpsl.st.portalpuntano5

import android.app.Application
import com.gpsl.st.portalpuntano5.core.AppContainer

class PortalPuntanoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
