@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import java.util.Date

inline fun Instant.toLocalDateTime(): LocalDateTime = atZone(ZoneId.systemDefault()).toLocalDateTime()
inline fun Instant.toLocalDate(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()
inline fun Instant.toDate() = Date(toEpochMilli())

inline fun Date.toLocalDateTimeBP(): LocalDateTime = toInstantBP().toLocalDateTime()
inline fun Date.toInstantBP(): Instant = Instant.ofEpochMilli(time)