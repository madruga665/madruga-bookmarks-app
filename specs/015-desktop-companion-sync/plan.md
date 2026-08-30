# Implementation Plan: Local Wi-Fi Sync with KDE Plasmoid Desktop

**Branch**: `015-desktop-companion-sync` | **Date**: 2026-08-30 | **Spec**: [specs/015-desktop-companion-sync/spec.md](spec.md)

**Input**: Feature specification from `specs/015-desktop-companion-sync/spec.md`

## Summary

Implement local-first, peer-to-peer Wi-Fi synchronization in the Android mobile application to synchronize collections and bookmarks bidirectionally with the companion KDE Plasma 6 desktop Plasmoid (`madruga665-bookmarks-desktop`). The feature includes:
1. **Network Discovery & Pairing**: Background UDP broadcast beaconing (port 43888) for automatic desktop peer discovery and secure 6-digit verification handshake (`/api/v1/sync/pair`).
2. **Bidirectional Delta Sync & Conflict Resolution**: Incremental delta payload exchange (`/api/v1/sync/exchange`) over HTTP with Bearer token authorization, soft-deletion tombstones (`is_deleted`), and deterministic Last-Write-Wins conflict resolution.
3. **Room Database Evolution**: Adding `is_deleted` columns to `collections_table` and `bookmarks_table`, plus a new `paired_devices_table` (or Preferences) with version migration.
4. **Neobrutalist Sync UI**: Real-time sync status indicator badge in the Home TopBar, and a full Sync Settings screen in Settings for discovering, pairing, unpairing, and manually synchronizing with desktop companions.

## Technical Context

**Language/Version**: Kotlin 2.2.10 (JVM Target 17)  
**Primary Dependencies**: Jetpack Compose (BOM 2026.08.00), Material 3, Hilt 2.60.1, Room Database 2.8.4, OkHttp/HttpURLConnection, Kotlinx Coroutines & Flow 1.11.0  
**Storage**: Android Room Database (SQLite) with schema migration to version 5 + DataStore Preferences  
**Testing**: JUnit 4, MockK 1.14.11, kotlinx-coroutines-test 1.11.0  
**Target Platform**: Android 8.0+ (API level 26+)  
**Project Type**: Native Android Application Module (`app`)  
**Performance Goals**: UDP peer discovery <3s; Wi-Fi sync <2s for 500 items; Local database queries <50ms  
**Constraints**: 100% offline-capable; Zero external cloud dependency; Strict Neobrutalism design system tokens; Shared protocol contract parity with `madruga665-bookmarks-desktop` (`bookmarks-sync-v1`)  
**Scale/Scope**: Support up to 10,000 bookmarks, 200 collections, and multiple paired desktop companions  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked post Phase 1 design.*

| Principle / Gate | Compliance Assessment | Status |
|---|---|:---:|
| **I. API-First & Cross-Platform Sync** | Shared peer-to-peer contract (`bookmarks-sync-v1`) standardized via JSON schema, enabling exact schema and entity parity between Android and KDE desktop. | ✅ PASS |
| **II. Frictionless Capture & OS Share Target** | Local and share-captured links automatically queue for sync without blocking user workflow. | ✅ PASS |
| **III. Flexible Folder Organization** | Collections and hierarchical taxonomy sync seamlessly with folder tab colors, link counts, and icons. | ✅ PASS |
| **IV. Dedicated Search & Instant Discovery** | All synced records reside in local Room DB, ensuring offline and low-latency search (<200ms). | ✅ PASS |
| **V. UI Consistency & Offline Resiliency** | Full offline support with soft-delete tombstones (`is_deleted`) and Last-Write-Wins reconciliation. Sync status indicators in Home and Settings follow Neobrutalism tokens. | ✅ PASS |

## Project Structure

### Documentation (this feature)

```text
specs/015-desktop-companion-sync/
├── plan.md              # This implementation plan
├── research.md          # Technical research and architecture decisions
├── data-model.md        # Schema definitions, entities, and reconciliation algorithm
├── quickstart.md        # Run and validation guide
├── contracts/
│   ├── sync-protocol.json     # P2P Wi-Fi Sync JSON Schema contract
│   └── mobile-sync-api.md     # Kotlin component interfaces & DTO contracts
└── checklists/
    └── requirements.md  # Quality validation checklist
```

### Source Code (repository root)

```text
app/src/main/java/com/madruga665/bookmarks/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt               # Room database definition with v5 migration
│   │   ├── Entities.kt                  # Added is_deleted to CollectionEntity, BookmarkEntity; Added PairedDeviceEntity
│   │   ├── CollectionDao.kt             # Query updates filtering is_deleted = 0 and delta sync queries
│   │   ├── BookmarkDao.kt               # Query updates filtering is_deleted = 0 and delta sync queries
│   │   └── PairedDeviceDao.kt           # DAO for paired companion devices
│   ├── remote/
│   │   ├── sync/
│   │   │   ├── PeerDiscoveryManager.kt  # UDP broadcast discovery on port 43888
│   │   │   ├── SyncHttpClient.kt        # HTTP client for /pair and /exchange
│   │   │   └── dto/
│   │   │       ├── SyncDto.kt           # JSON DTOs matching bookmarks-sync-v1 schema
│   │   │       └── PairDto.kt           # Handshake DTOs
│   └── repository/
│       ├── BookmarkRepository.kt        # Soft deletion and delta extraction methods
│       ├── CollectionRepository.kt      # Soft deletion and delta extraction methods
│       └── SyncRepository.kt            # Sync coordination, reconciliation, and status state
├── di/
│   └── SyncModule.kt                    # Hilt injection bindings for discovery, HTTP, and sync repository
└── ui/
    ├── components/
    │   └── SyncStatusBadge.kt           # Neobrutalist sync status badge (Home TopBar)
    └── settings/
        └── sync/
            ├── SyncSettingsScreen.kt    # Discovered devices, paired devices, manual sync trigger
            ├── SyncSettingsUiState.kt   # UI state for sync management screen
            ├── SyncSettingsViewModel.kt # ViewModel managing discovery, pairing, and sync
            └── components/
                ├── DiscoveredDeviceCard.kt
                ├── PairedDeviceCard.kt
                └── PairVerificationDialog.kt

app/src/test/java/com/madruga665/bookmarks/
├── data/
│   └── repository/
│       └── SyncRepositoryTest.kt        # Unit tests for Last-Write-Wins and delta reconciliation
└── remote/
    └── sync/
        ├── PeerDiscoveryTest.kt         # UDP beacon payload tests
        └── SyncHttpClientTest.kt        # Handshake and exchange network tests
```

**Structure Decision**: Clean Architecture with Domain-Driven separation. Network and discovery protocols reside in `data/remote/sync/`, persistence changes in `data/local/`, business coordination in `data/repository/SyncRepository.kt`, and UI components in `ui/settings/sync/` adhering strictly to Neobrutalism design tokens.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| *None* | Architecture strictly complies with all Constitution principles without unnecessary layers. | N/A |
