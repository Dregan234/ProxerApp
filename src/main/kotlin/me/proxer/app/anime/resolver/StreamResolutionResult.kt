package me.proxer.app.anime.resolver

import android.content.Context
import android.content.Intent
import android.net.Uri
import me.proxer.app.base.CustomTabsAware
import me.proxer.app.util.extension.addReferer
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
sealed class StreamResolutionResult {

    class Video(
        val url: HttpUrl,
        val mimeType: String,
        val referer: String? = null,
        val adTag: Uri? = null,
        val internalPlayerOnly: Boolean = false
    ) : StreamResolutionResult()

    class Link(val url: HttpUrl) : StreamResolutionResult() {

        fun show(customTabsAware: CustomTabsAware) {
            customTabsAware.showPage(url, skipCheck = true)
        }
    }

    class App(val uri: Uri) : StreamResolutionResult() {

        private val intent = Intent(Intent.ACTION_VIEW)
            .setData(uri)
            .addReferer()

        fun navigate(context: Context) {
            context.startActivity(intent)
        }
    }

    class Message(val message: CharSequence) : StreamResolutionResult()
}
