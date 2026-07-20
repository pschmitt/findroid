package dev.jdtech.jellyfin.film.presentation.homelayout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.pvr.PvrConfiguration
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.HomeSectionKeys
import dev.jdtech.jellyfin.utils.homeSectionOrderFromString
import dev.jdtech.jellyfin.utils.homeSectionOrderToString
import dev.jdtech.jellyfin.utils.resolveHomeSectionOrder
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class HomeLayoutSettingsViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val pvrConfiguration: PvrConfiguration,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeLayoutSettingsState())
    val state = _state.asStateFlow()

    /**
     * Every known (key, label) pair regardless of hidden status - populated once by [load] (it's
     * the only part of this screen that needs a repository round trip, for library names), then
     * reused by [hide]/[restore] to recompute the visible/hidden split without refetching.
     */
    private var cachedLabels: LinkedHashMap<String, UiText> = LinkedHashMap()

    fun load() {
        viewModelScope.launch {
            _state.emit(_state.value.copy(isLoading = true))

            val labels = LinkedHashMap<String, UiText>()

            if (appPreferences.getValue(appPreferences.homeSuggestions)) {
                labels[HomeSectionKeys.SUGGESTIONS] =
                    UiText.StringResource(FilmR.string.home_section_suggestions)
            }
            if (appPreferences.getValue(appPreferences.homeContinueWatching)) {
                labels[HomeSectionKeys.CONTINUE_WATCHING] =
                    UiText.StringResource(FilmR.string.continue_watching)
            }
            if (appPreferences.getValue(appPreferences.homeNextUp)) {
                labels[HomeSectionKeys.NEXT_UP] = UiText.StringResource(FilmR.string.next_up)
            }
            if (appPreferences.getValue(appPreferences.homeLatest)) {
                try {
                    repository
                        .getUserViews()
                        .filter { view ->
                            CollectionType.fromString(view.collectionType?.serialName) in
                                CollectionType.supported
                        }
                        .forEach { view ->
                            val id = view.id
                            labels[HomeSectionKeys.view(id)] =
                                UiText.StringResource(
                                    FilmR.string.latest_library,
                                    view.name.orEmpty(),
                                )
                        }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load library views for home layout settings")
                }
            }
            if (
                appPreferences.getValue(appPreferences.homeDiscover) &&
                    pvrConfiguration.isSeerrConfigured()
            ) {
                labels[HomeSectionKeys.discover(FilmR.string.home_discover_trending)] =
                    UiText.StringResource(FilmR.string.home_discover_trending)
                labels[HomeSectionKeys.discover(FilmR.string.home_discover_popular_movies)] =
                    UiText.StringResource(FilmR.string.home_discover_popular_movies)
                labels[HomeSectionKeys.discover(FilmR.string.home_discover_popular_shows)] =
                    UiText.StringResource(FilmR.string.home_discover_popular_shows)
            }
            labels[HomeSectionKeys.ACTIVE_DOWNLOADS] =
                UiText.StringResource(CoreR.string.pvr_queue_section_title)

            cachedLabels = labels
            recomputeRows()
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onAction(action: HomeLayoutSettingsAction) {
        when (action) {
            is HomeLayoutSettingsAction.OnMoveUp -> move(action.index, action.index - 1)
            is HomeLayoutSettingsAction.OnMoveDown -> move(action.index, action.index + 1)
            is HomeLayoutSettingsAction.OnHide -> hide(action.key)
            is HomeLayoutSettingsAction.OnRestore -> restore(action.key)
        }
    }

    private fun move(from: Int, to: Int) {
        val rows = _state.value.rows
        if (from !in rows.indices || to !in rows.indices) return

        val reordered = rows.toMutableList()
        val item = reordered.removeAt(from)
        reordered.add(to, item)

        _state.value = _state.value.copy(rows = reordered)
        appPreferences.setValue(
            appPreferences.homeSectionOrder,
            homeSectionOrderToString(reordered.map { it.key }),
        )
    }

    private fun hide(key: String) {
        val hidden = currentHiddenKeys().toMutableList()
        if (key !in hidden) hidden.add(key)
        appPreferences.setValue(appPreferences.homeHiddenSections, homeSectionOrderToString(hidden))
        recomputeRows()
    }

    private fun restore(key: String) {
        val hidden = currentHiddenKeys().toMutableList()
        hidden.remove(key)
        appPreferences.setValue(appPreferences.homeHiddenSections, homeSectionOrderToString(hidden))
        recomputeRows()
    }

    private fun currentHiddenKeys(): List<String> =
        homeSectionOrderFromString(appPreferences.getValue(appPreferences.homeHiddenSections))

    private fun recomputeRows() {
        val hidden = currentHiddenKeys().toSet()
        val natural = cachedLabels.keys.toList()

        val visibleNatural = natural.filterNot { it in hidden }
        val persisted =
            homeSectionOrderFromString(appPreferences.getValue(appPreferences.homeSectionOrder))
        val order = resolveHomeSectionOrder(visibleNatural, persisted)
        val rows = order.mapNotNull { key -> cachedLabels[key]?.let { HomeLayoutRow(key, it) } }

        val hiddenRows =
            natural.filter { it in hidden }.mapNotNull { key ->
                cachedLabels[key]?.let { HomeLayoutRow(key, it) }
            }

        _state.value = _state.value.copy(rows = rows, hiddenRows = hiddenRows)
    }
}
