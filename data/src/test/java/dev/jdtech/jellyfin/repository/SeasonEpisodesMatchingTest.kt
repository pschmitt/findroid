package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.pvr.SonarrEpisodeDto
import java.time.LocalDate
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SeasonEpisodesMatchingTest {

    private lateinit var originalDefaultTimeZone: TimeZone

    @Before
    fun setUp() {
        originalDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalDefaultTimeZone)
    }

    @Test
    fun `returns only episodes of the requested season missing from the known set`() {
        val episodes =
            listOf(
                SonarrEpisodeDto(id = 1, seasonNumber = 1, episodeNumber = 1, title = "Pilot"),
                SonarrEpisodeDto(id = 2, seasonNumber = 1, episodeNumber = 2, title = "Episode Two"),
                SonarrEpisodeDto(id = 3, seasonNumber = 2, episodeNumber = 1, title = "Other Season"),
            )

        val result = matchUpcomingEpisodes(episodes, seasonNumber = 1, knownEpisodeNumbers = setOf(1))

        assertEquals(1, result.size)
        assertEquals(2, result[0].episodeNumber)
        assertEquals("Episode Two", result[0].title)
    }

    @Test
    fun `is sorted by episode number regardless of input order`() {
        val episodes =
            listOf(
                SonarrEpisodeDto(id = 1, seasonNumber = 1, episodeNumber = 3),
                SonarrEpisodeDto(id = 2, seasonNumber = 1, episodeNumber = 1),
                SonarrEpisodeDto(id = 3, seasonNumber = 1, episodeNumber = 2),
            )

        val result = matchUpcomingEpisodes(episodes, seasonNumber = 1, knownEpisodeNumbers = emptySet())

        assertEquals(listOf(1, 2, 3), result.map { it.episodeNumber })
    }

    @Test
    fun `blank title becomes null rather than an empty string`() {
        val episodes = listOf(SonarrEpisodeDto(id = 1, seasonNumber = 1, episodeNumber = 1, title = ""))

        val result = matchUpcomingEpisodes(episodes, seasonNumber = 1, knownEpisodeNumbers = emptySet())

        assertEquals(null, result.single().title)
    }

    @Test
    fun `parses airDateUtc into a LocalDate`() {
        val episodes =
            listOf(
                SonarrEpisodeDto(
                    id = 1,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    airDateUtc = "2024-07-24T01:00:00Z",
                )
            )

        val result = matchUpcomingEpisodes(episodes, seasonNumber = 1, knownEpisodeNumbers = emptySet())

        assertEquals(LocalDate.of(2024, 7, 24), result.single().airDate)
    }

    @Test
    fun `carries hasFile and monitored through unchanged`() {
        val episodes =
            listOf(
                SonarrEpisodeDto(
                    id = 1,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    hasFile = true,
                    monitored = false,
                )
            )

        val result = matchUpcomingEpisodes(episodes, seasonNumber = 1, knownEpisodeNumbers = emptySet())

        assertTrue(result.single().hasFile)
        assertEquals(false, result.single().monitored)
    }

    @Test
    fun `returns empty list when everything is already known`() {
        val episodes =
            listOf(
                SonarrEpisodeDto(id = 1, seasonNumber = 1, episodeNumber = 1),
                SonarrEpisodeDto(id = 2, seasonNumber = 1, episodeNumber = 2),
            )

        val result = matchUpcomingEpisodes(episodes, seasonNumber = 1, knownEpisodeNumbers = setOf(1, 2))

        assertTrue(result.isEmpty())
    }
}
