@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.util.Linkify
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import me.proxer.app.BuildConfig.APPLICATION_ID
import me.proxer.app.R
import me.proxer.app.settings.theme.ThemeVariant
import me.proxer.app.ui.LinkCheckDialog
import me.proxer.app.ui.WebViewActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.library.util.ProxerUrls.hasProxerHost
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.DefinitionParameters
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import java.util.regex.Pattern.quote

val MENTIONS_REGEX = Regex("@(?:.*?)(?:(?:(?! )(?!${quote(".")} )(?!${quote(".")}\n)(?!\n)).)*").toPattern()

inline fun <T> unsafeLazy(noinline initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

inline fun Fragment.dip(value: Int) = requireContext().dip(value)

inline fun CharSequence.linkify(web: Boolean = true, mentions: Boolean = true, vararg custom: Regex): Spannable {
    val spannable = this as? Spannable ?: SpannableString(this)

    if (web) LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
    if (mentions) LinkifyCompat.addLinks(spannable, MENTIONS_REGEX, null)

    custom.forEach {
        LinkifyCompat.addLinks(spannable, it.toPattern(), null)
    }

    return spannable
}

inline fun HttpUrl.androidUri(): Uri = Uri.parse(toString())

fun String.toPrefixedUrlOrNull(): HttpUrl? = when {
    this.startsWith("http://") || this.startsWith("https://") -> this.toHttpUrlOrNull()
    else -> when {
        this.startsWith("//") -> "http:$this"
        else -> "http://$this"
    }.toHttpUrlOrNull()
}

inline fun Intent.addReferer(): Intent {
    putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://$APPLICATION_ID"))

    return this
}

// TODO: https://github.com/InsertKoinIO/koin/issues/303
@Suppress("UNCHECKED_CAST")
inline fun unsafeParametersOf(vararg parameters: Any?): DefinitionParameters {
    return DefinitionParameters(parameters.toList())
}

fun CustomTabsHelperFragment.fallbackHandleLink(
    activity: FragmentActivity,
    url: HttpUrl,
    forceBrowser: Boolean = false,
    skipCheck: Boolean = false
) {
    if (forceBrowser) {
        openHttpPage(activity, url)
    } else {
        val nativePackages = Utils.getNativeAppPackage(activity, url)

        when (nativePackages.isEmpty()) {
            true -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // In Android 11+ we can not query the packages beforehand so we need to try and fallback if no
                    // activity can handle the url.
                    val intent = Intent(Intent.ACTION_VIEW, url.androidUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        activity.startActivity(intent.addReferer())
                    } catch (ignored: ActivityNotFoundException) {
                        fallbackHandleLink(activity, url, skipCheck)
                    }
                } else {
                    fallbackHandleLink(activity, url, skipCheck)
                }
            }
            false -> {
                val intent = when (nativePackages.contains(APPLICATION_ID)) {
                    true -> Intent(Intent.ACTION_VIEW, url.androidUri()).setPackage(APPLICATION_ID)
                    false -> Intent(Intent.ACTION_VIEW, url.androidUri()).apply {
                        if (!url.hasProxerHost) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                }

                activity.startActivity(intent.addReferer())
            }
        }
    }
}

private fun CustomTabsHelperFragment.fallbackHandleLink(
    activity: FragmentActivity,
    url: HttpUrl,
    skipCheck: Boolean = false
) {
    val preferenceHelper = activity.get<PreferenceHelper>()

    when (!skipCheck && !url.hasProxerHost && preferenceHelper.shouldCheckLinks) {
        true -> LinkCheckDialog.show(activity, url)
        false -> openHttpPage(activity, url)
    }
}

fun CustomTabsHelperFragment.openHttpPage(activity: Activity, url: HttpUrl) {
    val colorScheme = when (getKoin().get<PreferenceHelper>().themeContainer.variant) {
        ThemeVariant.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        ThemeVariant.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
        ThemeVariant.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
    }

    val colorSchemeParams = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(activity.resolveColor(R.attr.colorPrimary))
        .setSecondaryToolbarColor(activity.resolveColor(R.attr.colorPrimary))
        .setNavigationBarColor(activity.resolveColor(R.attr.colorPrimary))
        .build()

    CustomTabsIntent.Builder(session)
        .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, colorSchemeParams)
        .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, colorSchemeParams)
        .setColorScheme(colorScheme)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .setUrlBarHidingEnabled(true)
        .setShowTitle(true)
        .build()
        .let {
            it.intent.addReferer()

            CustomTabsHelperFragment.open(activity, it, url.androidUri()) { context, uri ->
                WebViewActivity.navigateTo(context, uri.toString())
            }
        }
}

inline fun <reified T> safeInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = lazy { GlobalContext.get().get(qualifier, parameters) }