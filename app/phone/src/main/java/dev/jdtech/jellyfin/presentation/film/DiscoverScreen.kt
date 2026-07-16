package dev.jdtech.jellyfin.presentation.film

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.discover.DiscoverEvent
import dev.jdtech.jellyfin.film.presentation.discover.DiscoverState
import dev.jdtech.jellyfin.film.presentation.discover.DiscoverViewModel
import dev.jdtech.jellyfin.models.SeerrMediaStatus
import dev.jdtech.jellyfin.models.SeerrMediaType
import dev.jdtech.jellyfin.models.SeerrSearchItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents

/**
 * Jellyseerr-backed "add new content" screen: search TMDB, file a request (Jellyseerr routes it
 * to Sonarr/Radarr server-side), and see recent requests with their availability. Only reachable
 * when Jellyseerr is configured (see `MediaState.showDiscoverTab`).
 */
@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        val message =
            when (event) {
                is DiscoverEvent.Requested ->
                    context.getString(CoreR.string.discover_requested_toast, event.title)
                is DiscoverEvent.Failed ->
                    context.getString(
                        CoreR.string.discover_request_failed_toast,
                        event.message ?: context.getString(CoreR.string.unknown_error),
                    )
            }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    DiscoverScreenLayout(
        state = state,
        onQueryChanged = viewModel::onQueryChanged,
        onRequest = viewModel::request,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverScreenLayout(
    state: DiscoverState,
    onQueryChanged: (String) -> Unit = {},
    onRequest: (SeerrSearchItem) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier =
            Modifier.fillMaxSize()
                .recalculateWindowInsets()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(CoreR.string.title_discover)) },
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                placeholder = { Text(text = stringResource(CoreR.string.discover_search_hint)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_search),
                        contentDescription = null,
                    )
                },
                singleLine = true,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.spacings.default,
                            vertical = MaterialTheme.spacings.small,
                        ),
            )

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacings.default),
                )
            }

            if (state.isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacings.default)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (state.query.isNotBlank()) {
                    if (state.results.isEmpty() && !state.isSearching && state.error == null) {
                        item {
                            Text(
                                text = stringResource(CoreR.string.discover_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(MaterialTheme.spacings.default),
                            )
                        }
                    }
                    items(items = state.results, key = { "result-${it.mediaType}-${it.tmdbId}" }) {
                        item ->
                        DiscoverResultRow(
                            item = item,
                            requestedThisSession = item.tmdbId in state.requestedTmdbIds,
                            onRequest = { onRequest(item) },
                        )
                    }
                } else {
                    if (state.recentRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(CoreR.string.discover_recent_requests),
                                style = MaterialTheme.typography.titleMedium,
                                modifier =
                                    Modifier.padding(
                                        horizontal = MaterialTheme.spacings.default,
                                        vertical = MaterialTheme.spacings.small,
                                    ),
                            )
                        }
                        items(items = state.recentRequests, key = { "request-${it.id}" }) { request
                            ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(
                                            horizontal = MaterialTheme.spacings.default,
                                            vertical = MaterialTheme.spacings.small,
                                        ),
                                horizontalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.default),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SeerrPoster(posterUrl = request.posterUrl)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = request.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = mediaTypeLabel(request.mediaType),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                StatusText(status = request.mediaStatus)
                            }
                        }
                    } else if (state.error == null) {
                        item {
                            Text(
                                text = stringResource(CoreR.string.discover_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(MaterialTheme.spacings.default),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverResultRow(
    item: SeerrSearchItem,
    requestedThisSession: Boolean,
    onRequest: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacings.default,
                    vertical = MaterialTheme.spacings.small,
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeerrPoster(posterUrl = item.posterUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    listOfNotNull(item.year?.toString(), mediaTypeLabel(item.mediaType))
                        .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        when {
            requestedThisSession ->
                Text(
                    text = stringResource(CoreR.string.discover_status_requested),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            item.status == SeerrMediaStatus.NOT_REQUESTED ->
                Button(onClick = onRequest) {
                    Text(text = stringResource(CoreR.string.discover_request))
                }
            else -> StatusText(status = item.status)
        }
    }
}

@Composable
private fun SeerrPoster(posterUrl: String?) {
    Box(
        modifier =
            Modifier.width(56.dp)
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(CoreR.drawable.ic_film),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun StatusText(status: SeerrMediaStatus) {
    val (textRes, color) =
        when (status) {
            SeerrMediaStatus.AVAILABLE ->
                CoreR.string.discover_status_available to MaterialTheme.colorScheme.primary
            SeerrMediaStatus.PARTIALLY_AVAILABLE ->
                CoreR.string.discover_status_partially_available to
                    MaterialTheme.colorScheme.primary
            SeerrMediaStatus.PROCESSING ->
                CoreR.string.discover_status_processing to
                    MaterialTheme.colorScheme.onSurfaceVariant
            SeerrMediaStatus.PENDING ->
                CoreR.string.discover_status_pending to MaterialTheme.colorScheme.onSurfaceVariant
            SeerrMediaStatus.NOT_REQUESTED ->
                CoreR.string.discover_status_requested to
                    MaterialTheme.colorScheme.onSurfaceVariant
        }
    Text(text = stringResource(textRes), style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
private fun mediaTypeLabel(mediaType: SeerrMediaType): String =
    stringResource(
        when (mediaType) {
            SeerrMediaType.MOVIE -> CoreR.string.discover_type_movie
            SeerrMediaType.TV -> CoreR.string.discover_type_show
        }
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@androidx.compose.ui.tooling.preview.Preview
private fun DiscoverScreenLayoutPreview() {
    FindroidTheme {
        DiscoverScreenLayout(
            state =
                DiscoverState(
                    query = "dune",
                    results =
                        listOf(
                            SeerrSearchItem(
                                tmdbId = 438631,
                                mediaType = SeerrMediaType.MOVIE,
                                title = "Dune",
                                year = 2021,
                                overview = null,
                                posterUrl = null,
                                status = SeerrMediaStatus.AVAILABLE,
                            ),
                            SeerrSearchItem(
                                tmdbId = 693134,
                                mediaType = SeerrMediaType.MOVIE,
                                title = "Dune: Part Two",
                                year = 2024,
                                overview = null,
                                posterUrl = null,
                                status = SeerrMediaStatus.NOT_REQUESTED,
                            ),
                        ),
                )
        )
    }
}
