package com.madruga665.bookmarks.data.remote.sync

import com.madruga665.bookmarks.data.remote.sync.dto.DiscoveryBeaconPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeerDiscoveryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var manager: PeerDiscoveryManager

    @Before
    fun setUp() {
        manager = PeerDiscoveryManager(testDispatcher)
    }

    @Test
    fun beaconPayload_parseDesktopBeaconJson() {
        val desktopJson = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "desk-uuid-001",
                "deviceName": "Workstation KDE Plasma",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        val parsed = DiscoveryBeaconPayload.fromJson(desktopJson)

        assertEquals("bookmarks-sync-v1", parsed.protocol)
        assertEquals("desk-uuid-001", parsed.deviceId)
        assertEquals("Workstation KDE Plasma", parsed.deviceName)
        assertEquals(43889, parsed.httpPort)
        assertEquals("desktop", parsed.deviceType)
    }

    @Test
    fun beaconPayload_parseMobileBeaconJson() {
        val mobileJson = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "mob-uuid-002",
                "deviceName": "Pixel 8 Pro",
                "httpPort": 43889,
                "deviceType": "mobile"
            }
        """.trimIndent()

        val parsed = DiscoveryBeaconPayload.fromJson(mobileJson)

        assertEquals("bookmarks-sync-v1", parsed.protocol)
        assertEquals("mob-uuid-002", parsed.deviceId)
        assertEquals("Pixel 8 Pro", parsed.deviceName)
        assertEquals(43889, parsed.httpPort)
        assertEquals("mobile", parsed.deviceType)
    }

    @Test
    fun processIncomingBeacon_acceptsValidPeer() {
        val beaconJson = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "peer-desktop-1",
                "deviceName": "Living Room PC",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        val processed = manager.processIncomingBeacon(
            message = beaconJson,
            hostAddress = "192.168.1.50",
            myDeviceId = "my-phone-id",
            timestamp = 10_000L
        )

        assertTrue(processed)
        val peers = manager.discoveredPeers.value
        assertEquals(1, peers.size)
        val peer = peers.first()
        assertEquals("peer-desktop-1", peer.deviceId)
        assertEquals("Living Room PC", peer.deviceName)
        assertEquals("192.168.1.50", peer.hostAddress)
        assertEquals(43889, peer.httpPort)
        assertEquals("desktop", peer.deviceType)
        assertEquals(10_000L, peer.lastSeenTimestamp)
    }

    @Test
    fun processIncomingBeacon_ignoresNonMatchingProtocol() {
        val invalidProtocolJson = """
            {
                "protocol": "bookmarks-sync-v2",
                "deviceId": "peer-desktop-2",
                "deviceName": "Laptop KDE",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        val processed = manager.processIncomingBeacon(
            message = invalidProtocolJson,
            hostAddress = "192.168.1.51",
            myDeviceId = "my-phone-id",
            timestamp = 10_000L
        )

        assertFalse(processed)
        assertTrue(manager.discoveredPeers.value.isEmpty())
    }

    @Test
    fun processIncomingBeacon_ignoresSelfBeacon() {
        val selfJson = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "my-phone-id",
                "deviceName": "My Mobile Phone",
                "httpPort": 43889,
                "deviceType": "mobile"
            }
        """.trimIndent()

        val processed = manager.processIncomingBeacon(
            message = selfJson,
            hostAddress = "192.168.1.10",
            myDeviceId = "my-phone-id",
            timestamp = 10_000L
        )

        assertFalse(processed)
        assertTrue(manager.discoveredPeers.value.isEmpty())
    }

    @Test
    fun processIncomingBeacon_ignoresMalformedJson() {
        val malformed = "{ not a valid json"

        val processed = manager.processIncomingBeacon(
            message = malformed,
            hostAddress = "192.168.1.100",
            myDeviceId = "my-phone-id"
        )

        assertFalse(processed)
        assertTrue(manager.discoveredPeers.value.isEmpty())
    }

    @Test
    fun processIncomingBeacon_deduplicatesAndUpdatesTimestamps() {
        val beacon1 = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "peer-1",
                "deviceName": "Old Name",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        val beacon2Updated = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "peer-1",
                "deviceName": "New Name",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        manager.processIncomingBeacon(beacon1, "192.168.1.50", "my-phone-id", timestamp = 5_000L)
        assertEquals(1, manager.discoveredPeers.value.size)
        assertEquals("Old Name", manager.discoveredPeers.value[0].deviceName)
        assertEquals(5_000L, manager.discoveredPeers.value[0].lastSeenTimestamp)

        manager.processIncomingBeacon(beacon2Updated, "192.168.1.50", "my-phone-id", timestamp = 8_000L)
        assertEquals(1, manager.discoveredPeers.value.size)
        assertEquals("New Name", manager.discoveredPeers.value[0].deviceName)
        assertEquals(8_000L, manager.discoveredPeers.value[0].lastSeenTimestamp)
    }

    @Test
    fun processIncomingBeacon_tracksMultipleDistinctPeers() {
        val peer1Json = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "desk-1",
                "deviceName": "Desktop 1",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        val peer2Json = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "desk-2",
                "deviceName": "Desktop 2",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        manager.processIncomingBeacon(peer1Json, "192.168.1.50", "my-phone-id", timestamp = 1_000L)
        manager.processIncomingBeacon(peer2Json, "192.168.1.55", "my-phone-id", timestamp = 2_000L)

        val peers = manager.discoveredPeers.value
        assertEquals(2, peers.size)
        assertEquals("desk-1", peers[0].deviceId)
        assertEquals("desk-2", peers[1].deviceId)
    }

    @Test
    fun cleanupStalePeers_removesPeersOlderThan15Seconds() {
        val peer1Json = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "stale-peer",
                "deviceName": "Stale Desktop",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        val peer2Json = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "active-peer",
                "deviceName": "Active Desktop",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        // peer1 last seen at t=1000
        manager.processIncomingBeacon(peer1Json, "192.168.1.50", "my-phone-id", timestamp = 1_000L)
        // peer2 last seen at t=10000
        manager.processIncomingBeacon(peer2Json, "192.168.1.55", "my-phone-id", timestamp = 10_000L)

        assertEquals(2, manager.discoveredPeers.value.size)

        // At t=16001:
        // peer1 age = 16001 - 1000 = 15001ms (> 15000ms -> STALE)
        // peer2 age = 16001 - 10000 = 6001ms (<= 15000ms -> ACTIVE)
        manager.cleanupStalePeers(now = 16_001L)

        val remaining = manager.discoveredPeers.value
        assertEquals(1, remaining.size)
        assertEquals("active-peer", remaining[0].deviceId)

        // At t=26000:
        // peer2 age = 26000 - 10000 = 16000ms (> 15000ms -> STALE)
        manager.cleanupStalePeers(now = 26_000L)
        assertTrue(manager.discoveredPeers.value.isEmpty())
    }

    @Test
    fun clearPeers_clearsAllDiscoveredPeers() {
        val peerJson = """
            {
                "protocol": "bookmarks-sync-v1",
                "deviceId": "desk-1",
                "deviceName": "Desktop",
                "httpPort": 43889,
                "deviceType": "desktop"
            }
        """.trimIndent()

        manager.processIncomingBeacon(peerJson, "192.168.1.50", "my-phone-id", timestamp = 1_000L)
        assertEquals(1, manager.discoveredPeers.value.size)

        manager.clearPeers()
        assertTrue(manager.discoveredPeers.value.isEmpty())
    }

    @Test
    fun startAndStopDiscovery_lifecycle() = testScope.runTest {
        manager.startDiscovery("my-phone-id", "Pixel 8")
        manager.broadcastBeacon()
        manager.stopDiscovery()
    }
}
