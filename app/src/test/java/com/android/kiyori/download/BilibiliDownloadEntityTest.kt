package com.android.kiyori.download

import org.junit.Assert.assertEquals
import org.junit.Test

class BilibiliDownloadEntityTest {

    @Test
    fun entityRoundTrip_preservesResumeMetadataAndFragments() {
        val item = DownloadItem(
            id = "download-1",
            title = "Episode 1",
            url = "bilibili://ep100",
            status = "paused",
            progress = 42,
            filePath = "/tmp/output.mp4",
            errorMessage = "network interrupted",
            mediaType = MediaType.Bangumi,
            totalSize = 300L,
            downloadedSize = 120L,
            fragments = listOf(
                DownloadFragmentState(
                    type = "video",
                    url = "https://example.com/video.m4s",
                    size = 200L,
                    downloadedSize = 80L,
                    status = "downloading"
                ),
                DownloadFragmentState(
                    type = "audio",
                    url = "https://example.com/audio.m4s",
                    size = 100L,
                    downloadedSize = 40L,
                    status = "pending"
                )
            ),
            aid = "av123",
            cid = "456",
            epId = "100",
            seasonId = "200",
            createdAt = 1_000L,
            updatedAt = 2_000L
        )

        val restored = item.toBilibiliDownloadEntity().toDownloadItem()

        assertEquals(item, restored)
    }
}
