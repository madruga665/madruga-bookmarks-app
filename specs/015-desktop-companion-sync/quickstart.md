# Quickstart Validation Guide: Local Wi-Fi Companion Sync

**Feature**: `015-desktop-companion-sync`
**Date**: 2026-08-30

## 1. Prerequisites

- Android 8.0+ (API 26+) device or emulator.
- KDE Plasma 6 Desktop running `madruga665-bookmarks-desktop` Plasmoid or Python Mock Companion Server on the same Wi-Fi network / subnet.

---

## 2. Automated Unit & Integration Tests

Execute the unit test suite covering discovery parsing, pairing handshakes, Room soft deletions, and Last-Write-Wins conflict reconciliation:

```bash
# Run all unit tests including sync engine tests
./gradlew testDebugUnitTest
```

---

## 3. End-to-End Verification Scenarios

### Scenario 1: Peer Discovery & Pairing Handshake
1. Launch the KDE Plasmoid or Mock Companion Server (`mock_companion_server.py` on port 43888/43889).
2. Open the mobile app and navigate to **Settings > KDE Desktop Sync**.
3. Verify that the desktop instance appears under **Discovered Devices** within 3 seconds.
4. Tap **Pair**, enter the 6-digit verification code, and confirm.
5. Verify that the desktop moves to **Paired Devices** and status transitions to `SYNCED`.

### Scenario 2: Bidirectional Collections & Bookmarks Sync
1. On mobile, create a new collection "Dev Tools" with color `BLUE` and save a new bookmark `https://github.com`.
2. Observe the bookmark and collection immediately replicating to the desktop Plasmoid.
3. On desktop, create a bookmark `https://kotlinlang.org` in "Dev Tools".
4. On mobile, observe that `https://kotlinlang.org` appears under "Dev Tools".

### Scenario 3: Offline Deletion & Conflict Resolution
1. Disconnect mobile from Wi-Fi.
2. Delete a bookmark on mobile (marked with `is_deleted = true`).
3. Reconnect mobile to Wi-Fi and tap **Sync Now** (or wait for auto-sync).
4. Verify that the bookmark is deleted on desktop without resurrecting on mobile.
