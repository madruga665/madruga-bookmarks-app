package com.madruga665.bookmarks.data.remote.sync

import com.madruga665.bookmarks.data.remote.sync.dto.PairRequestDto
import com.madruga665.bookmarks.data.remote.sync.dto.PairResponseDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeRequestDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncHttpClient @Inject constructor(
    private val okHttpClient: OkHttpClient = createDefaultOkHttpClient()
) {

    /**
     * Executes the pairing handshake with a remote peer (desktop Plasmoid).
     * Sends POST /api/v1/sync/pair with the 6-digit verification code and initiator details.
     */
    suspend fun pair(
        hostAddress: String,
        httpPort: Int,
        initiatorDeviceId: String,
        initiatorName: String,
        verificationCode: String
    ): Result<PairResponseDto> = withContext(Dispatchers.IO) {
        val url = "http://$hostAddress:$httpPort/api/v1/sync/pair"
        val requestPayload = PairRequestDto(
            initiatorDeviceId = initiatorDeviceId,
            initiatorName = initiatorName,
            verificationCode = verificationCode
        )

        val requestBody = requestPayload.toJson().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IOException("Empty response from peer (HTTP ${response.code})")
                    )
                }

                try {
                    val pairResponse = PairResponseDto.fromJson(bodyString)
                    Result.success(pairResponse)
                } catch (e: Exception) {
                    Result.failure(IOException("Failed to parse pairing response: ${e.message}", e))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exchanges delta synchronization data with a paired peer.
     * Sends POST /api/v1/sync/exchange with Bearer auth and changed collections/bookmarks.
     */
    suspend fun exchange(
        hostAddress: String,
        httpPort: Int,
        authToken: String,
        payload: SyncExchangeRequestDto
    ): Result<SyncExchangeResponseDto> = withContext(Dispatchers.IO) {
        val url = "http://$hostAddress:$httpPort/api/v1/sync/exchange"
        val requestBody = payload.toJson().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IOException("Empty response from peer (HTTP ${response.code})")
                    )
                }

                try {
                    val exchangeResponse = SyncExchangeResponseDto.fromJson(bodyString)
                    Result.success(exchangeResponse)
                } catch (e: Exception) {
                    Result.failure(IOException("Failed to parse exchange response: ${e.message}", e))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()
        }
    }
}
