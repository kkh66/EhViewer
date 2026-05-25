package com.ehviewer.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.ehviewer.core.database.model.DownloadEntity
import com.ehviewer.core.database.model.DownloadInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadsDao {
    // https://issuetracker.google.com/327583152
    @Suppress("ktlint:standard:annotation", RoomWarnings.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE)
    @Query("SELECT LABEL, COUNT(*) AS COUNT FROM DOWNLOADS LEFT JOIN DOWNLOAD_LABELS USING(LABEL) GROUP BY LABEL")
    fun countByLabel(): Flow<Map<@MapColumn("LABEL") String?, @MapColumn("COUNT") Int>>

    @Suppress("ktlint:standard:annotation", RoomWarnings.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE)
    @Query("SELECT ARTIST, COUNT(*) AS COUNT FROM DOWNLOADS LEFT JOIN DOWNLOAD_ARTISTS USING(GID) GROUP BY ARTIST ORDER BY COUNT DESC, ARTIST")
    fun countByArtist(): Flow<Map<@MapColumn("ARTIST") String?, @MapColumn("COUNT") Int>>

    @Query("SELECT * FROM DOWNLOADS ORDER BY TIME")
    suspend fun list(): List<DownloadEntity>

    @Transaction
    @Query("SELECT * FROM DOWNLOADS LEFT JOIN DOWNLOAD_DIRNAME USING(GID) ORDER BY TIME DESC")
    suspend fun joinList(): List<DownloadInfo>

    @Transaction
    @Query("SELECT * FROM DOWNLOADS LEFT JOIN DOWNLOAD_DIRNAME USING(GID) WHERE DOWNLOADS.GID = :gid")
    suspend fun joinOne(gid: Long): DownloadInfo?

    @Transaction
    @Query("SELECT * FROM DOWNLOADS LEFT JOIN DOWNLOAD_DIRNAME USING(GID) WHERE DOWNLOADS.GID IN (:gidList)")
    suspend fun joinList(gidList: LongArray): List<DownloadInfo>

    @Transaction
    @Query("SELECT * FROM DOWNLOADS LEFT JOIN DOWNLOAD_DIRNAME USING(GID) WHERE STATE IN (:states) ORDER BY TIME DESC")
    suspend fun joinListByState(states: IntArray): List<DownloadInfo>

    @Transaction
    @Query(
        """SELECT DOWNLOADS.*, DOWNLOAD_DIRNAME.DIRNAME FROM DOWNLOADS
        JOIN GALLERIES USING(GID)
        LEFT JOIN DOWNLOAD_DIRNAME USING(GID)
        WHERE (:state = -1 OR DOWNLOADS.STATE = :state)
        AND (
            (:mode = 0 AND (:labelAll OR (:label IS NULL AND DOWNLOADS.LABEL IS NULL) OR DOWNLOADS.LABEL = :label))
            OR (:mode = 1 AND (
                :labelAll
                OR (:label IS NULL AND NOT EXISTS(SELECT 1 FROM DOWNLOAD_ARTISTS WHERE DOWNLOAD_ARTISTS.GID = DOWNLOADS.GID))
                OR EXISTS(SELECT 1 FROM DOWNLOAD_ARTISTS WHERE DOWNLOAD_ARTISTS.GID = DOWNLOADS.GID AND DOWNLOAD_ARTISTS.ARTIST = :label)
            ))
        )
        ORDER BY
            CASE WHEN :groupByDownloadLabel THEN DOWNLOADS.LABEL END COLLATE NOCASE ASC,
            CASE WHEN :field = 0 AND :order = 0 THEN DOWNLOADS.GID END ASC,
            CASE WHEN :field = 0 AND :order = 1 THEN DOWNLOADS.GID END DESC,
            CASE WHEN :field = 1 AND :order = 0 AND :showJpnTitle THEN COALESCE(NULLIF(GALLERIES.TITLE_JPN, ''), GALLERIES.TITLE, '') END COLLATE NOCASE ASC,
            CASE WHEN :field = 1 AND :order = 1 AND :showJpnTitle THEN COALESCE(NULLIF(GALLERIES.TITLE_JPN, ''), GALLERIES.TITLE, '') END COLLATE NOCASE DESC,
            CASE WHEN :field = 1 AND :order = 0 AND NOT :showJpnTitle THEN COALESCE(NULLIF(GALLERIES.TITLE, ''), GALLERIES.TITLE_JPN, '') END COLLATE NOCASE ASC,
            CASE WHEN :field = 1 AND :order = 1 AND NOT :showJpnTitle THEN COALESCE(NULLIF(GALLERIES.TITLE, ''), GALLERIES.TITLE_JPN, '') END COLLATE NOCASE DESC,
            CASE WHEN :field = 2 AND :order = 0 THEN DOWNLOADS.TIME END ASC,
            CASE WHEN :field = 2 AND :order = 1 THEN DOWNLOADS.TIME END DESC,
            CASE WHEN :field = 4 AND :order = 0 THEN GALLERIES.PAGES END ASC,
            CASE WHEN :field = 4 AND :order = 1 THEN GALLERIES.PAGES END DESC,
            CASE WHEN :field = 4 THEN DOWNLOADS.TIME END DESC""",
    )
    fun joinListLazy(
        mode: Int,
        label: String?,
        labelAll: Boolean,
        state: Int,
        field: Int,
        order: Int,
        groupByDownloadLabel: Boolean,
        showJpnTitle: Boolean,
    ): PagingSource<Int, DownloadInfo>

    @Transaction
    @Query(
        """SELECT DOWNLOADS.*, DOWNLOAD_DIRNAME.DIRNAME FROM DOWNLOADS
        JOIN GALLERIES USING(GID)
        JOIN GALLERIES_FTS ON GALLERIES.rowid = GALLERIES_FTS.docid
        LEFT JOIN DOWNLOAD_DIRNAME USING(GID)
        WHERE GALLERIES_FTS MATCH :keyword
        AND (:state = -1 OR DOWNLOADS.STATE = :state)
        AND (
            (:mode = 0 AND (:labelAll OR (:label IS NULL AND DOWNLOADS.LABEL IS NULL) OR DOWNLOADS.LABEL = :label))
            OR (:mode = 1 AND (
                :labelAll
                OR (:label IS NULL AND NOT EXISTS(SELECT 1 FROM DOWNLOAD_ARTISTS WHERE DOWNLOAD_ARTISTS.GID = DOWNLOADS.GID))
                OR EXISTS(SELECT 1 FROM DOWNLOAD_ARTISTS WHERE DOWNLOAD_ARTISTS.GID = DOWNLOADS.GID AND DOWNLOAD_ARTISTS.ARTIST = :label)
            ))
        )
        ORDER BY
            CASE WHEN :groupByDownloadLabel THEN DOWNLOADS.LABEL END COLLATE NOCASE ASC,
            CASE WHEN :field = 0 AND :order = 0 THEN DOWNLOADS.GID END ASC,
            CASE WHEN :field = 0 AND :order = 1 THEN DOWNLOADS.GID END DESC,
            CASE WHEN :field = 1 AND :order = 0 AND :showJpnTitle THEN COALESCE(NULLIF(GALLERIES.TITLE_JPN, ''), GALLERIES.TITLE, '') END COLLATE NOCASE ASC,
            CASE WHEN :field = 1 AND :order = 1 AND :showJpnTitle THEN COALESCE(NULLIF(GALLERIES.TITLE_JPN, ''), GALLERIES.TITLE, '') END COLLATE NOCASE DESC,
            CASE WHEN :field = 1 AND :order = 0 AND NOT :showJpnTitle THEN COALESCE(NULLIF(GALLERIES.TITLE, ''), GALLERIES.TITLE_JPN, '') END COLLATE NOCASE ASC,
            CASE WHEN :field = 1 AND :order = 1 AND NOT :showJpnTitle THEN COALESCE(NULLIF(GALLERIES.TITLE, ''), GALLERIES.TITLE_JPN, '') END COLLATE NOCASE DESC,
            CASE WHEN :field = 2 AND :order = 0 THEN DOWNLOADS.TIME END ASC,
            CASE WHEN :field = 2 AND :order = 1 THEN DOWNLOADS.TIME END DESC,
            CASE WHEN :field = 4 AND :order = 0 THEN GALLERIES.PAGES END ASC,
            CASE WHEN :field = 4 AND :order = 1 THEN GALLERIES.PAGES END DESC,
            CASE WHEN :field = 4 THEN DOWNLOADS.TIME END DESC""",
    )
    fun searchListLazy(
        keyword: String,
        mode: Int,
        label: String?,
        labelAll: Boolean,
        state: Int,
        field: Int,
        order: Int,
        groupByDownloadLabel: Boolean,
        showJpnTitle: Boolean,
    ): PagingSource<Int, DownloadInfo>

    @Query("UPDATE DOWNLOADS SET STATE = 0 WHERE STATE IN (1, 2)")
    suspend fun resetRunningState()

    @Update
    suspend fun update(downloadInfo: List<DownloadEntity>)

    @Insert
    suspend fun insert(downloadInfo: List<DownloadEntity>)

    @Upsert
    suspend fun upsert(t: DownloadEntity)

    @Delete
    suspend fun delete(downloadInfo: DownloadEntity)

    @Delete
    suspend fun delete(downloadInfo: List<DownloadEntity>)
}
