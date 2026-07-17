package dev.jdtech.jellyfin.api.pvr

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SeerrApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: SeerrApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = SeerrApi(baseUrl = server.url("/").toString(), apiKey = "test-api-key")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getTvSeason returns episode details from the season endpoint`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "id": 1,
                    "name": "Season 2",
                    "seasonNumber": 2,
                    "episodes": [
                        {
                            "id": 33,
                            "name": "A New Episode",
                            "seasonNumber": 2,
                            "episodeNumber": 4,
                            "airDate": "2026-08-01",
                            "overview": "Episode overview",
                            "stillPath": "/still.jpg"
                        }
                    ]
                }
                """
                    .trimIndent()
            )
        )

        val season = api.getTvSeason(tmdbId = 123, seasonNumber = 2)

        assertEquals(2, season.seasonNumber)
        assertEquals(1, season.episodes.size)
        assertEquals("A New Episode", season.episodes.single().name)
        assertEquals(4, season.episodes.single().episodeNumber)
        val request = server.takeRequest()
        assertEquals("test-api-key", request.getHeader("X-Api-Key"))
        assertTrue(request.path.orEmpty().endsWith("/api/v1/tv/123/season/2"))
    }
}
