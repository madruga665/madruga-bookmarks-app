package com.madruga665.bookmarks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices_table WHERE is_paired = 1")
    fun getAllPairedDevices(): Flow<List<PairedDeviceEntity>>

    @Query("SELECT * FROM paired_devices_table WHERE is_paired = 1")
    suspend fun getAllPairedDevicesList(): List<PairedDeviceEntity>

    @Query("SELECT * FROM paired_devices_table WHERE device_id = :deviceId")
    fun getPairedDeviceById(deviceId: String): Flow<PairedDeviceEntity?>

    @Query("SELECT * FROM paired_devices_table WHERE device_id = :deviceId")
    suspend fun getPairedDeviceByIdDirect(deviceId: String): PairedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairedDevice(device: PairedDeviceEntity)

    @Update
    suspend fun updatePairedDevice(device: PairedDeviceEntity)

    @Query("UPDATE paired_devices_table SET last_sync_timestamp = :timestamp WHERE device_id = :deviceId")
    suspend fun updateLastSyncTimestamp(deviceId: String, timestamp: Long)

    @Query("UPDATE paired_devices_table SET host_address = :hostAddress, http_port = :httpPort WHERE device_id = :deviceId")
    suspend fun updateDeviceAddress(deviceId: String, hostAddress: String, httpPort: Int)

    @Query("DELETE FROM paired_devices_table WHERE device_id = :deviceId")
    suspend fun deletePairedDevice(deviceId: String)

    @Query("UPDATE paired_devices_table SET is_paired = 0 WHERE device_id = :deviceId")
    suspend fun unpairDevice(deviceId: String)
}
