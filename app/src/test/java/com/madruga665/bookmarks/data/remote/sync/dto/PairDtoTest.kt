package com.madruga665.bookmarks.data.remote.sync.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairDtoTest {

    @Test
    fun discoveryBeaconPayload_serializationAndDeserialization() {
        val payload = DiscoveryBeaconPayload(
            protocol = "bookmarks-sync-v1",
            deviceId = "dev-123",
            deviceName = "Pixel 8 Pro",
            httpPort = 43889,
            deviceType = "mobile"
        )

        val jsonStr = payload.toJson()
        val parsed = DiscoveryBeaconPayload.fromJson(jsonStr)

        assertEquals("bookmarks-sync-v1", parsed.protocol)
        assertEquals("dev-123", parsed.deviceId)
        assertEquals("Pixel 8 Pro", parsed.deviceName)
        assertEquals(43889, parsed.httpPort)
        assertEquals("mobile", parsed.deviceType)
    }

    @Test
    fun pairRequestDto_serializationAndDeserialization() {
        val request = PairRequestDto(
            initiatorDeviceId = "phone-456",
            initiatorName = "User Phone",
            verificationCode = "123456"
        )

        val jsonStr = request.toJson()
        val parsed = PairRequestDto.fromJson(jsonStr)

        assertEquals("phone-456", parsed.initiatorDeviceId)
        assertEquals("User Phone", parsed.initiatorName)
        assertEquals("123456", parsed.verificationCode)
    }

    @Test
    fun pairResponseDto_serializationAndDeserialization() {
        val response = PairResponseDto(
            status = "PAIRED_SUCCESS",
            authToken = "sec_token_987",
            responderDeviceId = "desk-789",
            responderName = "Desktop KDE"
        )

        val jsonStr = response.toJson()
        val parsed = PairResponseDto.fromJson(jsonStr)

        assertEquals("PAIRED_SUCCESS", parsed.status)
        assertEquals("sec_token_987", parsed.authToken)
        assertEquals("desk-789", parsed.responderDeviceId)
        assertEquals("Desktop KDE", parsed.responderName)
    }
}
