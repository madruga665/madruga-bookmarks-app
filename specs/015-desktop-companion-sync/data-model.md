# Data Model & Synchronization Architecture

**Feature**: `015-desktop-companion-sync`
**Date**: 2026-08-30

## 1. Entities & Schema

### 1.1 Collection Entity (`collections_table`)

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | Primary Key | UUID identifying the collection |
| `name` | `TEXT` | NOT NULL | Collection display name (1..40 chars) |
| `link_count` | `INTEGER` | NOT NULL, DEFAULT 0 | Cached count of active bookmarks |
| `subcollection_count` | `INTEGER` | NOT NULL, DEFAULT 0 | Reserved for nested hierarchy |
| `parent_id` | `TEXT` | NULLABLE | Parent collection ID |
| `icon_key` | `TEXT` | NOT NULL | Icon identifier (e.g. "folder", "star", "bookmark") |
| `color_accent` | `TEXT` | NOT NULL | Color token (e.g. "YELLOW", "PURPLE", "ORANGE") |
| `created_at` | `INTEGER` | NOT NULL | Epoch millis timestamp |
| `updated_at` | `INTEGER` | NOT NULL | Epoch millis of last modification |
| `is_deleted` | `INTEGER` | NOT NULL, DEFAULT 0 | Soft-deletion tombstone (0 = active, 1 = deleted) |

### 1.2 Bookmark Entity (`bookmarks_table`)

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | Primary Key | UUID identifying the bookmark |
| `url` | `TEXT` | NOT NULL | Web link URL |
| `title` | `TEXT` | NULLABLE | Page title |
| `description` | `TEXT` | NULLABLE | Web page meta description |
| `favicon_url` | `TEXT` | NULLABLE | Favicon URL |
| `thumbnail_url` | `TEXT` | NULLABLE | Web thumbnail image URL |
| `source_platform` | `TEXT` | NULLABLE | Domain/source badge text |
| `collection_id` | `TEXT` | NOT NULL, DEFAULT 'col_unsorted' | Foreign reference to collection |
| `notes` | `TEXT` | NULLABLE | User notes |
| `tags` | `TEXT` | NOT NULL, DEFAULT '' | Comma-separated tag strings |
| `is_pinned` | `INTEGER` | NOT NULL, DEFAULT 0 | Pinned bookmark flag |
| `created_at` | `INTEGER` | NOT NULL | Epoch millis timestamp |
| `updated_at` | `INTEGER` | NOT NULL | Epoch millis of last modification |
| `sync_status` | `TEXT` | NOT NULL, DEFAULT 'PENDING_SYNC' | Sync state (`SYNCED`, `PENDING_SYNC`) |
| `is_deleted` | `INTEGER` | NOT NULL, DEFAULT 0 | Soft-deletion tombstone (0 = active, 1 = deleted) |

### 1.3 Paired Device Entity / Preferences (`paired_devices_table`)

| Column / Key | Type | Description |
| :--- | :--- | :--- |
| `device_id` | `TEXT` (PK) | UUID of the paired KDE desktop |
| `device_name` | `TEXT` | Friendly name (e.g., "KDE Plasma Desktop") |
| `host_address` | `TEXT` | IP address on the local Wi-Fi network |
| `http_port` | `INTEGER` | Port number of the companion HTTP server (default 43889) |
| `auth_token` | `TEXT` | Secret Bearer token issued during pairing |
| `last_sync_timestamp`| `INTEGER` | Epoch millis of the last successful synchronization |
| `is_paired` | `INTEGER` | Whether device authorization is currently active |

---

## 2. Sync Exchange Protocol Models (JSON)

### 2.1 Pairing Handshake Request & Response
```json
// POST /api/v1/sync/pair
// Request
{
  "initiatorDeviceId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "initiatorName": "Pixel 8 Pro",
  "verificationCode": "482910"
}

// Response
{
  "status": "PAIRED_SUCCESS", // "PAIRED_SUCCESS" | "INVALID_CODE" | "REJECTED"
  "authToken": "sec_984f1a23e5bc87",
  "responderDeviceId": "11e8b7d7-8405-427a-bdc4-1dd97ed1d5df",
  "responderName": "Arch Linux Plasma 6"
}
```

### 2.2 Delta Exchange Request & Response
```json
// POST /api/v1/sync/exchange
// Headers: Authorization: Bearer sec_984f1a23e5bc87
// Request
{
  "deviceId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "lastSyncTimestamp": 1788062000000,
  "collections": [
    {
      "id": "col_ia",
      "name": "Inteligência Artificial",
      "parentId": null,
      "colorAccent": "PURPLE",
      "iconKey": "robot",
      "createdAt": 1788060000000,
      "updatedAt": 1788062100000,
      "isDeleted": false
    }
  ],
  "bookmarks": [
    {
      "id": "bm_12345",
      "url": "https://anthropic.com",
      "title": "Anthropic AI",
      "description": "AI safety and research",
      "faviconUrl": "https://anthropic.com/favicon.ico",
      "thumbnailUrl": null,
      "sourcePlatform": "anthropic.com",
      "collectionId": "col_ia",
      "notes": "Research articles",
      "tags": "ia,research",
      "isPinned": true,
      "createdAt": 1788060100000,
      "updatedAt": 1788062200000,
      "isDeleted": false
    }
  ]
}

// Response
{
  "status": "SUCCESS",
  "serverTimestamp": 1788062300000,
  "collections": [...],
  "bookmarks": [...]
}
```

---

## 3. Conflict Resolution Algorithm (Last-Write-Wins)

When receiving a remote record (Collection or Bookmark) with ID $K$:

1. **Check Local Record**: Query local Room database for item with ID $K$ (including soft-deleted records).
2. **If Local Record Does Not Exist**:
   - Insert incoming record directly into local database with `sync_status = 'SYNCED'`.
3. **If Local Record Exists**:
   - Compare timestamps: `incoming.updated_at` vs `local.updated_at`.
   - **Case A: `incoming.updated_at > local.updated_at`**:
     - Overwrite local fields with incoming values (including `is_deleted` flag).
     - Set `sync_status = 'SYNCED'`.
   - **Case B: `incoming.updated_at <= local.updated_at`**:
     - Keep local record as is (local modification was more recent).
4. **Update `last_sync_timestamp`**:
   - Record `serverTimestamp` as the new baseline timestamp for future delta queries.
