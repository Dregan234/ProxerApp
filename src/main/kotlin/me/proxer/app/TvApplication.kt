package me.proxer.app

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TvApplication : Application() {

    companion object {
        const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"
        const val GENERIC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)
        FlavorInitializer.initialize(this)

        startKoin {
            androidContext(this@TvApplication)
            modules(koinModules)
        }
    }
}
