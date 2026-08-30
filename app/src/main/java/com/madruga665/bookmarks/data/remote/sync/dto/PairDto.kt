package com.madruga665.bookmarks.data.remote.sync.dto

import org.json.JSONObject

/**
 * Payload broadcast over UDP (port 43888) to announce presence or discover peers.
 */
data class DiscoveryBeaconPayload(
    val protocol: String = "bookmarks-sync-v1",
    val deviceId: String,
    val deviceName: String,
    val httpPort: Int = 43889,
    val deviceType: String = "mobile"
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("protocol", protocol)
        put("deviceId", deviceId)
        put("deviceName", deviceName)
        put("httpPort", httpPort)
        put("deviceType", deviceType)
    }

    fun toJson(): String = toJSONObject().toString()

    companion object {
        fun fromJson(jsonStr: String): DiscoveryBeaconPayload =
            fromJson(JSONObject(jsonStr))

        fun fromJson(json: JSONObject): DiscoveryBeaconPayload =
            DiscoveryBeaconPayload(
                protocol = json.optString("protocol", ""),
                deviceId = json.getString("deviceId"),
                deviceName = json.getString("deviceName"),
                httpPort = json.optInt("httpPort", 43889),
                deviceType = json.optString("deviceType", "mobile")
            )
    }
}

/**
 * Request payload sent to POST /api/v1/sync/pair during manual pairing handshake.
 */
data class PairRequestDto(
    val initiatorDeviceId: String,
    val initiatorName: String,
    val verificationCode: String
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("initiatorDeviceId", initiatorDeviceId)
        put("initiatorName", initiatorName)
        put("verificationCode", verificationCode)
    }

    fun toJson(): String = toJSONObject().toString()

    companion object {
        fun fromJson(jsonStr: String): PairRequestDto =
            fromJson(JSONObject(jsonStr))

        fun fromJson(json: JSONObject): PairRequestDto =
            PairRequestDto(
                initiatorDeviceId = json.getString("initiatorDeviceId"),
                initiatorName = json.getString("initiatorName"),
                verificationCode = json.getString("verificationCode")
            )
    }
}

/**
 * Response received from POST /api/v1/sync/pair.
 */
data class PairResponseDto(
    val status: String,
    val authToken: String,
    val responderDeviceId: String,
    val responderName: String
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("status", status)
        put("authToken", authToken)
        put("responderDeviceId", responderDeviceId)
        put("responderName", responderName)
    }

    fun toJson(): String = toJSONObject().toString()

    companion object {
        fun fromJson(jsonStr: String): PairResponseDto =
            fromJson(JSONObject(jsonStr))

        fun fromJson(json: JSONObject): PairResponseDto =
            PairResponseDto(
                status = json.getString("status"),
                authToken = json.optString("authToken", ""),
                responderDeviceId = json.optString("responderDeviceId", ""),
                responderName = json.optString("responderName", "")
            )
    }
}
