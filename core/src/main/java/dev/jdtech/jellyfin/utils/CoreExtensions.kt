package dev.jdtech.jellyfin.utils

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.util.Base64
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.View
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemDto

fun BaseItemDto.toView(items: List<FindroidItem>): View {
    return View(
        id = id,
        name = name ?: "",
        items = items,
        type = CollectionType.fromString(collectionType?.serialName),
    )
}

fun Resources.dip(px: Int) = (px * displayMetrics.density).toInt()

fun Activity.restart() {
    val intent = Intent(this, this::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
}

/**
 * Unlike [restart], this kills the whole process rather than just recreating the Activity - only
 * an Activity restart isn't enough after a backup restore, since @Singleton-scoped Hilt
 * dependencies like JellyfinApi are constructed once from the current server/user at process
 * startup and never rebuilt for the lifetime of the process.
 */
fun Activity.restartProcess() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    Runtime.getRuntime().exit(0)
}

fun String.base64ToByteArray(): ByteArray {
    return Base64.decode(toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
}

fun ByteArray.toBase64Str(): String {
    return Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP)
}

// For episodes/seasons, `.name` alone ("Pilot", "Season 1") is ambiguous out of context - this
// adds the show (and, for episodes, season/episode number) so delete confirmations read
// unambiguously, e.g. "Breaking Bad • S1E1 • Pilot".
fun FindroidItem.displayNameWithContext(): String =
    when (this) {
        is FindroidEpisode -> "$seriesName • S${parentIndexNumber}E$indexNumber • $name"
        is FindroidSeason -> "$seriesName • $name"
        else -> name
    }

// [pattern] is the raw "pref_date_format" preference value ("system"/"iso"/"dmy"/"mdy") - see
// AppPreferences.dateFormat. Falls back to the locale-based system short date format for
// "system" and for any unrecognized value, so an old/blank preference never breaks formatting.
fun DateTime.format(pattern: String = "system"): String {
    val instant = this.toInstant(ZoneOffset.UTC)
    val date = Date.from(instant)
    return when (pattern) {
        "iso" -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        "dmy" -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        "mdy" -> SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date)
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }
}
