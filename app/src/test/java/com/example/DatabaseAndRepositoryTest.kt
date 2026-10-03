package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.SongEntity
import com.example.data.repository.MediaRepository
import com.example.domain.models.SortDirection
import com.example.domain.models.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseAndRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: MediaRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MediaRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveSong() = runBlocking {
        val song = SongEntity(
            id = "song_1",
            title = "Acoustic Melody",
            artist = "Singer",
            filePath = "/data/music/melody.mp3",
            format = "mp3",
            quality = "320kbps",
            durationMs = 200000L,
            sourceUrl = "https://example.com/song1",
            isFavorite = false
        )
        database.songDao().insert(song)

        val retrieved = repository.getSongById("song_1")
        assertNotNull(retrieved)
        assertEquals("Acoustic Melody", retrieved?.title)
        assertEquals("Singer", retrieved?.artist)
    }

    @Test
    fun duplicateDetectionInDao() = runBlocking {
        val song = SongEntity(
            id = "song_dup",
            title = "Test Song",
            artist = "Artist",
            filePath = "/path/test.mp3",
            format = "mp3",
            quality = "320kbps",
            sourceUrl = "https://example.com/unique_url"
        )
        database.songDao().insert(song)

        val found = database.songDao().findExisting("https://example.com/unique_url", "mp3", "320kbps")
        assertNotNull(found)
        assertEquals("song_dup", found?.id)

        val notFound = database.songDao().findExisting("https://example.com/other_url", "mp3", "320kbps")
        assertNull(notFound)
    }

    @Test
    fun toggleFavoriteUpdatesSong() = runBlocking {
        val song = SongEntity(
            id = "song_fav",
            title = "Favorite Song",
            artist = "Artist",
            filePath = "/path/fav.mp3",
            format = "mp3",
            quality = "320kbps",
            sourceUrl = "https://example.com/fav",
            isFavorite = false
        )
        database.songDao().insert(song)

        repository.setFavorite("song_fav", true)
        val favSongs = repository.getFavoriteSongs().first()
        assertEquals(1, favSongs.size)
        assertEquals("song_fav", favSongs[0].id)
        assertTrue(favSongs[0].isFavorite)
    }

    @Test
    fun playlistCrudAndSongs() = runBlocking {
        val playlistId = repository.createPlaylist("My Chill Mix")
        assertTrue(playlistId > 0)

        val song = SongEntity(
            id = "song_pl",
            title = "Chill Beats",
            artist = "Beatmaker",
            filePath = "/path/chill.mp3",
            format = "mp3",
            quality = "320kbps",
            sourceUrl = "https://example.com/chill"
        )
        database.songDao().insert(song)

        repository.addSongToPlaylist(playlistId, "song_pl")

        val playlistSongs = repository.getSongsForPlaylist(playlistId).first()
        assertEquals(1, playlistSongs.size)
        assertEquals("Chill Beats", playlistSongs[0].title)

        // Remove from playlist
        repository.removeSongFromPlaylist(playlistId, "song_pl")
        val emptyList = repository.getSongsForPlaylist(playlistId).first()
        assertEquals(0, emptyList.size)

        // Song itself should still exist
        val songStillExists = repository.getSongById("song_pl")
        assertNotNull(songStillExists)
    }

    @Test
    fun sortOrderAscendingAndDescending() = runBlocking {
        val songA = SongEntity(
            id = "1",
            title = "Alpha",
            artist = "Artist",
            filePath = "/path/a.mp3",
            format = "mp3",
            quality = "320kbps",
            sourceUrl = "urlA"
        )
        val songZ = SongEntity(
            id = "2",
            title = "Zeta",
            artist = "Artist",
            filePath = "/path/z.mp3",
            format = "mp3",
            quality = "320kbps",
            sourceUrl = "urlZ"
        )
        database.songDao().insertAll(listOf(songA, songZ))

        val asc = repository.getSongs(SortOrder.NAME, SortDirection.ASC).first()
        assertEquals("Alpha", asc[0].title)
        assertEquals("Zeta", asc[1].title)

        val desc = repository.getSongs(SortOrder.NAME, SortDirection.DESC).first()
        assertEquals("Zeta", desc[0].title)
        assertEquals("Alpha", desc[1].title)
    }
}
