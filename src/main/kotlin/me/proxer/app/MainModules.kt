package me.proxer.app

import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.WorkManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.rubengees.rxbus.RxBus
import com.squareup.moshi.Moshi
import me.proxer.app.TvApplication.Companion.USER_AGENT
import me.proxer.app.auth.ProxerLoginTokenManager
import me.proxer.app.util.Mp4UploadTrustManagerWorkaround
import me.proxer.app.util.Validators
import me.proxer.app.util.data.HawkMoshiParser
import me.proxer.app.util.data.InstantJsonAdapter
import me.proxer.app.util.data.LocalDataInitializer
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.http.CacheInterceptor
import me.proxer.app.util.http.ConnectionCloseInterceptor
import me.proxer.app.util.http.ConnectivityInterceptor
import me.proxer.app.util.http.HttpsUpgradeInterceptor
import me.proxer.app.util.http.TaggedSocketFactory
import me.proxer.app.util.http.UserAgentInterceptor
import me.proxer.app.util.logging.HttpTimberLogger
import me.proxer.library.LoginTokenManager
import me.proxer.library.ProxerApi
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.platform.Platform
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.threeten.bp.Instant
import java.io.File
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private const val DEFAULT_PREFERENCES = "defaultPreferences"
private const val STORAGE_PREFERENCES = "storagePreferences"

private const val DEFAULT_RX_PREFERENCES = "defaultRxPreferences"
private const val STORAGE_RX_PREFERENCES = "storageRxPreferences"

private const val STORAGE_PREFERENCES_NAME = "me.proxer.encrypted_preferences"

private const val HTTP_CACHE_SIZE = 1_024L * 1_024L * 10L
private const val HTTP_CACHE_NAME = "http"

private const val API_TOKEN_HEADER = "proxer-api-token"

private val headersToRedact = listOf("proxer-api-key", "set-cookie")

private val applicationModules = module {
    single { androidContext().packageManager }

    single(named(DEFAULT_PREFERENCES)) { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
    single(named(STORAGE_PREFERENCES)) {
        val masterKey = MasterKey.Builder(androidContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            androidContext(),
            STORAGE_PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    single(named(DEFAULT_RX_PREFERENCES)) { RxSharedPreferences.create(get(named(DEFAULT_PREFERENCES))) }
    single(named(STORAGE_RX_PREFERENCES)) { RxSharedPreferences.create(get(named(STORAGE_PREFERENCES))) }

    single { PreferenceHelper(get(), get(named(DEFAULT_RX_PREFERENCES)), get(named(DEFAULT_PREFERENCES))) }
    single { StorageHelper(get(), get(named(STORAGE_RX_PREFERENCES)), get(named(STORAGE_PREFERENCES)), get()) }

    single { RxBus() }

    single {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }

        trustManagerFactory.trustManagers.filterIsInstance(X509TrustManager::class.java).first()
    }

    single { WorkManager.getInstance(androidContext()) }

    single {
        val preferenceHelper = get<PreferenceHelper>()

        val loggingInterceptor = when {
            BuildConfig.LOG -> HttpLoggingInterceptor(HttpTimberLogger()).apply {
                level = preferenceHelper.httpLogLevel

                headersToRedact.forEach { redactHeader(it) }

                if (preferenceHelper.shouldRedactToken) {
                    redactHeader(API_TOKEN_HEADER)
                }
            }
            else -> null
        }

        val trustManager = Mp4UploadTrustManagerWorkaround.create()

        OkHttpClient.Builder()
            .sslSocketFactory(Platform.get().newSslSocketFactory(trustManager), trustManager)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .socketFactory(TaggedSocketFactory())
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addNetworkInterceptor(CacheInterceptor())
            .addInterceptor(ConnectivityInterceptor(get()))
            .addInterceptor(HttpsUpgradeInterceptor())
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(ConnectionCloseInterceptor())
            .addInterceptor(BrotliInterceptor)
            .cache(Cache(File(androidContext().cacheDir, HTTP_CACHE_NAME), HTTP_CACHE_SIZE))
            .apply {
                if (loggingInterceptor != null) {
                    if (preferenceHelper.shouldLogHttpVerbose) {
                        addNetworkInterceptor(loggingInterceptor)
                    } else {
                        addInterceptor(loggingInterceptor)
                    }
                }
            }
            .build()
    }

    single {
        Moshi.Builder()
            .add(Instant::class.java, InstantJsonAdapter())
            .build()
    }

    single {
        ProxerApi.Builder(BuildConfig.PROXER_API_KEY)
            .enableRateLimitProtection()
            .loginTokenManager(get())
            .userAgent(USER_AGENT)
            .client(get())
            .moshi(get())
            .build()
    }

    single { Validators(get(), get()) }

    single { HawkMoshiParser(get()) }

    single {
        LocalDataInitializer(
            androidContext(),
            get(),
            get(named(DEFAULT_PREFERENCES)),
            get(named(STORAGE_PREFERENCES))
        )
    }

    single<LoginTokenManager> { ProxerLoginTokenManager(get()) }
}

val koinModules = listOf(applicationModules)
