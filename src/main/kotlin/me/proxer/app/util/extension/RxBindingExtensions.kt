@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.widget.TextView
import androidx.annotation.CheckResult
import io.reactivex.Observable
import me.proxer.app.util.rx.TextViewLinkClickObservable

@CheckResult
inline fun TextView.linkClicks(noinline handled: (String) -> Boolean = { true }): Observable<String> {
    return TextViewLinkClickObservable(this, handled)
}