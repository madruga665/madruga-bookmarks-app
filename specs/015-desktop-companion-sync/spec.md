# Feature Specification: Local Wi-Fi Sync with KDE Plasmoid Desktop

**Feature Branch**: `015-desktop-companion-sync`

**Created**: 2026-08-30

**Status**: Draft

**Input**: User description: "sincronizar collections e bookmarks com o app kde plasmoid deskto, converse com o agent da sessão madruga665-bookmarks-desktop para entender como sera feita a sincronização"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Local Wi-Fi Device Discovery & Secure Pairing (Priority: P1) 🎯 MVP

When the user opens the mobile application on the same local Wi-Fi network as their KDE Plasmoid desktop widget, the app automatically discovers the nearby desktop companion (or broadcasts its own presence). The user can view available desktop instances, initiate a pairing handshake with a 6-digit verification code, and confirm authorization so both devices securely exchange synchronization tokens.

**Why this priority**: Core prerequisite for peer-to-peer communication between mobile and desktop without third-party cloud servers.

**Independent Test**: Can be tested independently by launching the mobile app and desktop Plasmoid on the same local network subnet, selecting the detected desktop device from the sync settings, entering the verification code, and confirming that both devices enter the `PAIRED` state with stored security credentials.

**Acceptance Scenarios**:

1. **Given** both mobile app and desktop Plasmoid are connected to the same Wi-Fi network, **When** the user navigates to Sync Settings, **Then** available desktop companion devices appear in a discovered devices list.
2. **Given** an unparied desktop companion is selected, **When** the user taps "Pair Device", **Then** a pairing verification prompt is displayed displaying matching verification codes on both screens.
3. **Given** matching verification codes, **When** the user confirms pairing on mobile, **Then** authorization succeeds, the desktop is added to "Paired Devices", and an initial synchronization cycle is scheduled.
4. **Given** an invalid or rejected code, **When** pairing times out or is rejected, **Then** a clear error message is shown and the device remains unpaired.

---

### User Story 2 - Bidirectional Synchronization of Collections & Bookmarks (Priority: P1) 🎯 MVP

Once paired, whenever the user creates, edits, moves, pins, or deletes bookmarks or collections on the mobile app (or on the desktop Plasmoid), the changes are synchronized bidirectionally over the local network. Sync triggers automatically on network connection, on data changes, or via manual sync request.

**Why this priority**: Primary value proposition enabling seamless parity between mobile bookmarks and KDE desktop workflow.

**Independent Test**: Can be tested by creating a new collection and bookmark on mobile and observing them appear in the desktop Plasmoid, then creating a bookmark in the desktop Plasmoid and confirming it syncs back into the mobile Room database.

**Acceptance Scenarios**:

1. **Given** a paired desktop companion on the local network, **When** a new bookmark or collection is saved on mobile, **Then** the updated payload is transmitted to the desktop companion.
2. **Given** updates made on the desktop companion, **When** the mobile receives the sync exchange, **Then** new collections and bookmarks are inserted/updated in the local mobile repository and UI reflects the changes instantly.
3. **Given** tags or pin status modified on either device, **When** sync completes, **Then** tags and pin states are unified on both ends.

---

### User Story 3 - Offline Resiliency & Deterministic Conflict Resolution (Priority: P2)

When devices are disconnected from the local network (e.g. mobile on cellular or outside home Wi-Fi), the user can continue creating, editing, and deleting bookmarks and collections offline without interruption. When the devices reconnect to the same Wi-Fi network, an incremental delta synchronization reconciles all offline changes using deterministic Last-Write-Wins and deletion tombstones.

**Why this priority**: Guarantees data integrity and smooth offline experience without data loss or resurrected deleted items.

**Independent Test**: Can be tested by modifying a bookmark title on mobile while offline, modifying the same bookmark's notes on desktop while offline, then reconnecting both to Wi-Fi and verifying that the final state deterministically merges both updates or preserves the latest modification timestamp.

**Acceptance Scenarios**:

1. **Given** no local network connection with the desktop companion, **When** the user adds or edits bookmarks on mobile, **Then** changes persist locally in offline storage and are marked pending synchronization.
2. **Given** an item deleted on mobile while offline, **When** reconnecting with the desktop companion, **Then** a deletion tombstone is sent ensuring the item is also removed on the desktop without being re-downloaded.
3. **Given** conflicting modifications to the same collection/bookmark on both devices, **When** synchronization occurs, **Then** the record with the most recent timestamp prevails without duplicating records.

---

### User Story 4 - Sync Status Indicator & Management in Mobile UI (Priority: P2)

Users can see real-time synchronization feedback throughout the app (e.g. status badge in top bar or Settings) indicating whether the app is currently Synced, Syncing, Disconnected, or experiencing an Error. In Settings, the user can review connected devices, view last sync timestamps, trigger a manual "Sync Now", or unpair a desktop device.

**Why this priority**: Gives users visual confidence, transparency, and manual control over their peer synchronization.

**Independent Test**: Can be tested by opening the Settings screen, checking the sync status and timestamp, tapping "Sync Now" to verify forced immediate sync, and unpairing a device to verify that synchronization ceases.

**Acceptance Scenarios**:

1. **Given** the app top bar or settings screen, **When** viewing the sync indicator, **Then** the current status is clearly rendered using Neobrutalist design tokens (e.g., green for Synced, yellow for Syncing, gray for Offline/Idle).
2. **Given** the Sync Settings screen, **When** the user taps "Sync Now", **Then** the app immediately attempts to connect to paired desktop devices and completes a delta sync cycle.
3. **Given** a paired device in Settings, **When** the user taps "Unpair Device" and confirms, **Then** the device credentials are removed and future sync exchanges with that device are rejected.

---

### Edge Cases

- **Network Change / Subnet Isolation**: When switching from Wi-Fi to cellular or between different Wi-Fi networks (e.g. guest networks with AP isolation), the discovery engine gracefully handles unreachable peers and pauses sync until a viable local network is restored.
- **Concurrent Deletions and Updates**: If a bookmark is deleted on mobile while updated on desktop, the latest timestamp determines if the tombstone deletes the item or if a recent edit reinstates it.
- **Malformed or Incomplete Sync Payload**: If network transmission is interrupted midway, atomic transactions ensure no partial or corrupt state is saved in the local database.
- **Large Bookmark Datasets**: When synchronizing hundreds of bookmarks at once, sync executes asynchronously in background coroutines without causing UI frame drops or blocking user interactions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST implement local network peer discovery over Wi-Fi (via UDP beacon broadcasting / mDNS) to detect companion KDE Plasmoid desktop instances.
- **FR-002**: The system MUST implement a secure pairing handshake with temporary verification codes and persistent authentication tokens.
- **FR-003**: The system MUST support bidirectional delta synchronization of Collections (id, name, colorAccent, iconKey, parentId, timestamps, deletion status).
- **FR-004**: The system MUST support bidirectional delta synchronization of Bookmarks (id, url, title, description, faviconUrl, thumbnailUrl, sourcePlatform, collectionId, notes, tags, isPinned, timestamps, deletion status).
- **FR-005**: The system MUST implement deterministic conflict resolution based on modification timestamps (Last-Write-Wins).
- **FR-006**: The system MUST maintain soft-deletion tombstones via `is_deleted: Boolean` on `CollectionEntity` and `BookmarkEntity` to propagate deletions cleanly across devices and prevent resurrecting deleted records.
- **FR-007**: The system MUST persist all data locally in the Room database so that all collections and bookmarks remain fully accessible when offline.
- **FR-008**: The system MUST automatically trigger an incremental sync exchange upon reconnecting to the paired desktop's Wi-Fi network, upon local data creation/edits, and when the user taps "Sync Now".
- **FR-009**: The UI MUST provide a Sync Management screen under Settings allowing users to discover nearby devices, manage paired devices, initiate manual pairing, and trigger manual "Sync Now".
- **FR-010**: The UI MUST display real-time sync status indicators (Synced, Syncing, Disconnected, Error) conforming to Neobrutalist design tokens.
- **FR-011**: The system MUST allow users to unpair and revoke tokens for paired desktop devices.

### Key Entities

- **Collection**: Folder categorization entity.
  - Attributes: `id` (String UUID), `name` (String), `linkCount` (Int), `subcollectionCount` (Int), `parentId` (String?), `iconKey` (String), `colorAccent` (String), `createdAt` (Long), `updatedAt` (Long), `isDeleted` (Boolean).
- **Bookmark**: Saved web link entity.
  - Attributes: `id` (String UUID), `url` (String), `title` (String?), `description` (String?), `faviconUrl` (String?), `thumbnailUrl` (String?), `sourcePlatform` (String?), `collectionId` (String), `notes` (String?), `tags` (String), `isPinned` (Boolean), `createdAt` (Long), `updatedAt` (Long), `syncStatus` (String), `isDeleted` (Boolean).
- **PairedCompanionDevice**: Record of an authorized KDE Plasmoid desktop device.
  - Attributes: `deviceId` (String UUID), `deviceName` (String), `hostAddress` (String), `httpPort` (Int), `authToken` (String), `lastSyncTimestamp` (Long), `isPaired` (Boolean).
- **SyncExchangePackage**: Data contract exchanged over HTTP between mobile and desktop.
  - Attributes: `deviceId` (String), `lastSyncTimestamp` (Long), `collections` (List<Collection>), `bookmarks` (List<Bookmark>).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Nearby desktop Plasmoid discovery over local Wi-Fi completes and displays on screen in under 3 seconds.
- **SC-002**: Incremental delta synchronization of up to 500 bookmarks and 20 collections between mobile and desktop completes in under 2 seconds.
- **SC-003**: 100% offline availability: all previously synced collections and bookmarks remain fully searchable and browsable without network connectivity.
- **SC-004**: Zero data loss during simultaneous edits: deterministic timestamp conflict resolution prevents duplicate items and reconciles 100% of concurrent changes.
- **SC-005**: 100% of UI components in the Sync settings and status badges strictly follow the project's Neobrutalism design system tokens (solid borders, hard drop shadows, high contrast).

## Assumptions

- Both the Android mobile app (`madruga665-bookmarks-app`) and KDE Plasmoid (`madruga665-bookmarks-desktop`) communicate on the same local Wi-Fi / LAN subnet.
- The KDE Plasmoid hosts the local HTTP server endpoint, while the Android mobile app acts as the HTTP client that sends discovery beacons, executes pairing handshakes, and pushes/pulls data exchanges.
- Sync is triggered automatically on Wi-Fi connection, on local data mutations (create/edit/delete), and on manual request ("Sync Now").
- The shared peer-to-peer sync protocol (`bookmarks-sync-v1`) defined in `specs/001-plasmoid-home-sync/contracts/sync-protocol.json` in the desktop project is the authoritative contract for discovery, pairing, and bidirectional exchange.
- Soft-deletion tombstones (`is_deleted: Boolean` column) are retained across Room entities and synced payloads to prevent re-creating deleted entities.
