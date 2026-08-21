package me.proxer.app

import android.os.Bundle
import androidx.activity.compose.setContent
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.ProxerApi
import me.proxer.tv.ProxerTvApp
import me.proxer.tv.TvDataMigration
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    private val api by inject<ProxerApi>()
    private val storage by inject<StorageHelper>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TvDataMigration.resetLegacyDataIfNeeded(this, storage)

        setContent {
            ProxerTvApp(
                api = api,
                storage = storage,
                initialRoute = intent.data?.pathSegments?.let { segments ->
                    when (segments.firstOrNull()) {
                        "info" -> segments.getOrNull(1)?.let { "info/${encode(it)}/0" }
                        "watch" -> segments.getOrNull(1)?.let { id ->
                            "watch/${encode(id)}/${segments.getOrNull(2)?.toIntOrNull() ?: 1}/ENGLISH_SUB"
                        }
                        else -> null
                    }
                }
            )
        }
    }

    private fun encode(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
}
