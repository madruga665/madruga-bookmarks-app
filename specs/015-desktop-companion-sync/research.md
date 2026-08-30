# Technical Research: Local Wi-Fi Sync with KDE Plasmoid Desktop

**Feature**: `015-desktop-companion-sync`
**Date**: 2026-08-30

## 1. Network Discovery & Transport Protocol

### Decision
Use local UDP broadcast on port `43888` for peer discovery and HTTP/1.1 REST client (`OkHttp` / `java.net.HttpURLConnection`) on port `43889` for handshake and delta synchronization.

### Rationale
- **UDP Broadcast on Port 43888**: Matches the KDE Plasmoid synchronization protocol (`bookmarks-sync-v1`) defined in `madruga665-bookmarks-desktop`. Allows instant zero-configuration peer discovery across the local subnet without requiring third-party cloud servers or router port forwarding.
- **HTTP Client**: The KDE Plasmoid hosts an embedded local HTTP server. Android app acts as the client, executing standard JSON requests (`/api/v1/sync/pair` and `/api/v1/sync/exchange`).
- **Standard Coroutine Concurrency**: Kotlin coroutines with `Dispatchers.IO` ensure non-blocking background discovery and data exchange without UI lag.

### Alternatives Considered
- **mDNS / NSD (Network Service Discovery)**: Considered, but Android NSD has historically suffered from OEM-specific packet filtering and service registration delays. UDP broadcast provides immediate, deterministic discovery compatible with KDE Connect patterns.
- **Raw WebSockets**: Overkill for periodic and event-driven delta sync batches; HTTP/1.1 REST over LAN achieves sub-100ms latency for payload exchanges with simpler state and reconnection semantics.

---

## 2. Persistence & Tombstone Deletion Strategy

### Decision
Implement soft deletions with `is_deleted: Boolean` column and timestamp tracking (`updated_at: Long`) across `collections_table` and `bookmarks_table` in Room database. Normal UI queries add `WHERE is_deleted = 0` (or `false`), while sync queries fetch all mutations where `updated_at > lastSyncTimestamp`.

### Rationale
- Hard deletions permanently remove rows, making it impossible to determine whether an entity was deleted or never existed on the remote peer, causing deleted entities to resurrect during sync.
- With `is_deleted = true` tombstones and `updated_at`, deletions are transmitted as normal entity updates.
- If an entity is deleted locally at `T1` and modified remotely at `T2` (where `T2 > T1`), Last-Write-Wins deterministically resolves the state.

### Alternatives Considered
- **Separate Tombstone Table**: Increases relational complexity and requires dual-table transactions for every delete operation. Soft delete on the primary entity table is cleaner and directly aligns with the desktop companion schema.

---

## 3. Pairing Security & Authentication

### Decision
Implement a 6-digit temporary verification code during initial pairing, followed by a persistent UUID Bearer `authToken` stored in DataStore / Room for all subsequent sync requests.

### Rationale
- Prevents rogue devices on public or shared Wi-Fi networks from syncing without user confirmation.
- The 6-digit code is displayed on both screens for visual confirmation before the desktop server issues the persistent `authToken`.
- Subsequent sync requests include `Authorization: Bearer <authToken>`.

---

## 4. UI Architecture & Neobrutalism Design Integration

### Decision
Integrate a dedicated Sync Management section in the Settings screen (`ui/settings/sync/`) and a lightweight `SyncStatusBadge` component for the Home TopBar.

### Rationale
- Preserves the project's Neobrutalism design system tokens (2px solid black borders, hard offset drop shadows `#000000`, vibrant status badges).
- Gives users immediate visual feedback (`SYNCED`, `SYNCING`, `DISCONNECTED`, `ERROR`) and access to manual sync triggers and device pairing.
