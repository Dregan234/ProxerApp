package me.proxer.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.google.android.material.tabs.TabLayout
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem
import me.proxer.library.ProxerApi
import me.proxer.tv.ProxerTvApp
import me.proxer.tv.TvDataMigration
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    // Kept as a source-compatibility bridge while legacy fragments are removed.
    @Deprecated("Legacy fragments are no longer hosted by the TV activity")
    internal val tabs: TabLayout
        get() = error("Legacy fragments are not available in the TV application")

    companion object {
        private const val SECTION_EXTRA = "section"
        private const val SECTION_ACTION_PREFIX = "me.proxer.app.intent.action."

        fun navigateToSection(context: Context, section: DrawerItem) {
            context.startActivity(getSectionIntent(context, section))
        }

        fun getSectionIntent(context: Context, section: DrawerItem): Intent = Intent(context, MainActivity::class.java)
            .putExtra(SECTION_EXTRA, section)
            .setAction(SECTION_ACTION_PREFIX + section.name)
    }

    private val api by inject<ProxerApi>()
    private val storage by inject<StorageHelper>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TvDataMigration.resetLegacyDataIfNeeded(this, storage)

        setContent {
            ProxerTvApp(
                api = api,
                preferenceHelper = preferenceHelper,
                storage = storage,
                initialRoute = intent.data?.pathSegments?.let { segments ->
                    when (segments.firstOrNull()) {
                        "info" -> segments.getOrNull(1)?.let { "info/${encode(it)}" }
                        "watch" -> segments.getOrNull(1)?.let { id ->
                            "watch/${encode(id)}/${segments.getOrNull(2)?.toIntOrNull() ?: 1}"
                        }
                        else -> null
                    }
                }
            )
        }
    }

    private fun encode(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
}
