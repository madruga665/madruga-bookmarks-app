# Data Model: Save Button Loading State & Threads Metadata Extractor

## 1. Entities & Value Objects

### LinkMetadata
Immutable value object representing extracted web link metadata.

| Field | Type | Description | Threads Example |
|-------|------|-------------|-----------------|
| `title` | `String?` | Extracted or fallback title | `"Mark Zuckerberg (@zuck) on Threads"` |
| `faviconUrl` | `String?` | Favicon URI | `"https://www.google.com/s2/favicons?domain=threads.net&sz=128"` |
| `thumbnailUrl` | `String?` | Attached media or preview image | `"https://scontent...cdninstagram.com/..."` |
| `sourcePlatform` | `String?` | Platform identifier badge | `"@Threads"` |
| `description` | `String?` | Post body text or author bio | `"Building open source AI and future of connection..."` |

### SaveBookmarkModalUiState
Immutable UI state for the save modal bottom sheet.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `isVisible` | `Boolean` | `false` | Whether modal sheet is visible |
| `targetUrl` | `String` | `""` | Validated target link to save |
| `availableCollections` | `List<CollectionEntity>` | `emptyList()` | List of selectable user collections |
| `selectedCollectionId` | `String` | `"col_unsorted"` | Currently active collection ID |
| `isPinned` | `Boolean` | `false` | Pinned flag status |
| `isCreatingFolder` | `Boolean` | `false` | Inline folder creation toggle |
| `isSaving` | `Boolean` | `false` | **Active save operation in progress** |
| `error` | `String?` | `null` | Error message if save fails |
| `tags` | `List<String>` | `emptyList()` | Attached tag list |
| `tagInput` | `String` | `""` | Current tag input text |
| `existingTags` | `List<String>` | `emptyList()` | Autocomplete tag suggestions |

### BookmarkEntity (Room Database)
Persisted record stored in SQLite Room Database.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `TEXT` | PRIMARY KEY | Unique UUID identifier |
| `url` | `TEXT` | NOT NULL | Cleaned target URL |
| `title` | `TEXT` | NOT NULL | Extracted or fallback title |
| `description` | `TEXT` | NULLABLE | Post content / summary |
| `faviconUrl` | `TEXT` | NULLABLE | Source favicon URL |
| `thumbnailUrl` | `TEXT` | NULLABLE | Media preview URL |
| `sourcePlatform` | `TEXT` | NOT NULL | Extracted platform name (`@Threads`, `@X`, etc.) |
| `collectionId` | `TEXT` | NOT NULL | Parent collection folder ID |
| `notes` | `TEXT` | NULLABLE | User notes |
| `tags` | `TEXT` | NOT NULL | Comma-separated tag list |
| `isPinned` | `INTEGER` | NOT NULL | 0 for false, 1 for true |
| `createdAt` | `INTEGER` | NOT NULL | Epoch millis timestamp |
| `updatedAt` | `INTEGER` | NOT NULL | Epoch millis timestamp |
| `syncStatus` | `TEXT` | NOT NULL | `"PENDING_SYNC"` or `"SYNCED"` |

---

## 2. State Lifecycle & Transitions

```
[ User taps "Save to Folder" ]
              │
              ▼
    isSaving = true ────► UI: Button disabled, Text -> CircularProgressIndicator
              │
    ┌─────────┴─────────┐
    ▼                   ▼
[ Network Success ]   [ Error / Timeout ]
    │                   │
    ▼                   ▼
Room DB Insert        isSaving = false
isSaving = false      error = "Failed to save bookmark"
isVisible = false     UI: Button re-enabled, error shown
Modal Dismissed
```
