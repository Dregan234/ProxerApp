package me.proxer.tv

import android.content.Context
import me.proxer.app.util.data.StorageHelper

object TvDataMigration {

    private const val STATE_PREFERENCES = "proxer_tv_state"
    private const val DATA_VERSION = "data_version"
    private const val CURRENT_VERSION = 1

    fun resetLegacyDataIfNeeded(context: Context, storage: StorageHelper) {
        val state = context.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)
        if (state.getInt(DATA_VERSION, 0) < CURRENT_VERSION) {
            storage.reset()
            state.edit().putInt(DATA_VERSION, CURRENT_VERSION).apply()
        }
    }
}
