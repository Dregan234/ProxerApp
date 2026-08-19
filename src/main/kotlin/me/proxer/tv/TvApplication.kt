package me.proxer.tv

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TvApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TvApplication)
            modules(me.proxer.app.koinModules)
        }
    }
}
