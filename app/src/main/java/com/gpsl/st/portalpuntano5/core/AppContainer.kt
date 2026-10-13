package com.gpsl.st.portalpuntano5.core

import android.content.Context
import com.gpsl.st.portalpuntano5.BuildConfig
import com.gpsl.st.portalpuntano5.data.local.SessionStore
import com.gpsl.st.portalpuntano5.data.remote.NetworkModule
import com.gpsl.st.portalpuntano5.data.remote.OfficialPortalDataSource
import com.gpsl.st.portalpuntano5.data.repository.AuthRepositoryImpl
import com.gpsl.st.portalpuntano5.data.repository.PortalRepositoryImpl
import com.gpsl.st.portalpuntano5.domain.repository.AuthRepository
import com.gpsl.st.portalpuntano5.domain.repository.PortalRepository

/** Inyección manual de dependencias. */
class AppContainer(context: Context) {
    val session = SessionStore(context)
    private val network = NetworkModule(session)
    val officialPortal = OfficialPortalDataSource(session)
    val auth: AuthRepository = AuthRepositoryImpl(network.portal, session, BuildConfig.DEMO_MODE)
    val portal: PortalRepository = PortalRepositoryImpl(network.portal, session, BuildConfig.DEMO_MODE)
}