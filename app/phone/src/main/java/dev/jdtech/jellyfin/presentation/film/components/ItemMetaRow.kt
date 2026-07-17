package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * The quiet one-line metadata strip under a detail page's header: date, runtime, age rating and
 * community rating joined by middle dots, e.g. "Jul 3, 2026 · 2h 8m · PG-13 · ★ 7.8".
 * [trailingContent] hosts non-text status companions (queue badge and the like). When
 * [onPlayedClick]/[onFavoriteClick] are wired up, compact watched/favorite icon toggles sit
 * right-aligned on the same line - state-flavored metadata lives with the metadata, keeping the
 * action row below for "heavier" actions.
 */
@Composable
fun ItemMetaRow(
    dateText: String? = null,
    runtimeTicks: Long = 0,
    officialRating: String? = null,
    communityRating: Float? = null,
    modifier: Modifier = Modifier,
    played: Boolean = false,
    favorite: Boolean = false,
    onPlayedClick: (() -> Unit)? = null,
    onFavoriteClick: (() -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val segments =
        listOfNotNull(
            dateText,
            runtimeTicks.takeIf { it > 0 }?.let { formatRuntime(it) },
            officialRating,
        )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (segments.isNotEmpty()) {
            Text(
                text = segments.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        communityRating?.let { rating ->
            if (segments.isNotEmpty()) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(CoreR.drawable.ic_star),
                contentDescription = null,
                tint = Color("#F2C94C".toColorInt()),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "%.1f".format(rating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailingContent()
        if (onPlayedClick != null || onFavoriteClick != null) {
            Spacer(Modifier.weight(1f))
            onPlayedClick?.let { playedClick ->
                MetaToggle(
                    iconRes = CoreR.drawable.ic_check,
                    checked = played,
                    onClick = playedClick,
                    contentDescription =
                        stringResource(
                            if (played) CoreR.string.unmark_as_played
                            else CoreR.string.mark_as_played
                        ),
                )
            }
            onFavoriteClick?.let { favoriteClick ->
                MetaToggle(
                    iconRes =
                        if (favorite) CoreR.drawable.ic_heart_filled else CoreR.drawable.ic_heart,
                    checked = favorite,
                    onClick = favoriteClick,
                    contentDescription =
                        stringResource(
                            if (favorite) CoreR.string.remove_from_favorites
                            else CoreR.string.add_to_favorites
                        ),
                )
            }
        }
    }
}

/**
 * A quiet, borderless icon toggle sized to sit on the meta line without shouting: subdued when
 * off, primary-tinted when on, with the standard 48dp touch target.
 */
@Composable
private fun MetaToggle(
    iconRes: Int,
    checked: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        colors =
            IconButtonDefaults.iconToggleButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContentColor = MaterialTheme.colorScheme.primary,
            ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** "2h 8m" for long items, plain "52m" under an hour - compact enough for the dot-joined strip. */
@Composable
private fun formatRuntime(runtimeTicks: Long): String {
    val totalMinutes = runtimeTicks / 600_000_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(CoreR.string.runtime_hours_minutes, hours, minutes)
    } else {
        stringResource(CoreR.string.runtime_minutes_short, minutes)
    }
}
