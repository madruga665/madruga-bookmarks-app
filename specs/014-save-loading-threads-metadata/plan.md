# Implementation Plan: Save Button Loading State & Threads Metadata Extractor

**Branch**: `014-save-loading-threads-metadata` | **Date**: 2026-08-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from [`specs/014-save-loading-threads-metadata/spec.md`](./spec.md)

## Summary

Implement interactive loading feedback on the bookmark save confirmation button and add a dedicated Threads (`threads.net`) link metadata extractor to enrich captured Threads bookmarks with author details, post caption descriptions, attached thumbnails, and `@Threads` platform tagging.

## Technical Context

**Language/Version**: Kotlin 2.2.10 (JVM Target 17)

**Primary Dependencies**: Jetpack Compose (BOM 2024.10.01), Material 3, Hilt 2.60.1, Coroutines & Flow (1.11.0), Room 2.8.4, Jsoup 1.23.1

**Storage**: Room SQLite Database (`BookmarkEntity`, `CollectionEntity`) & DataStore Preferences

**Testing**: JUnit 4, MockK 1.14.11, kotlinx-coroutines-test 1.11.0

**Target Platform**: Android API 26+ (Native Android Client)

**Project Type**: Mobile Application (`app` module)

**Performance Goals**: Button loading feedback rendered in <50ms; metadata extraction timeout capped at 6000ms with fallback in <100ms

**Constraints**: High-contrast Neobrutalism visual design tokens, offline-capable fallback parsing, zero layout shift on loading transitions

**Scale/Scope**: Android Client UI (`SaveBookmarkBottomSheet`), Data Remote (`LinkMetadataExtractor`), and Unit Tests

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

- [x] **Principle I: API-First & Cross-Platform Sync**: `LinkMetadata` schema and `BookmarkEntity` remain aligned with backend sync protocol.
- [x] **Principle II: Frictionless Capture & OS Share Target Integration**: Loading state enhances capture UX without adding any extra steps or blocking user workflows.
- [x] **Principle III: Flexible Folder Organization via Share & App**: Save modal maintains full folder selection and creation capabilities while loading.
- [x] **Principle IV: Dedicated Search & Instant Discovery**: `@Threads` source platform badge and extracted descriptions are searchable.
- [x] **Principle V: Cross-Platform UI Consistency & Offline Resiliency**: Follows Neobrutalism design system tokens with high contrast borders and provides graceful offline/login-gated fallback parsing for Threads.

---

## Project Structure

### Documentation (this feature)

```text
specs/014-save-loading-threads-metadata/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan
├── research.md          # Technical research & decisions
├── data-model.md        # Data models & state lifecycle
├── quickstart.md        # Validation and run guide
├── contracts/
│   ├── save-modal-ui-contract.md
│   └── threads-extractor-contract.md
└── checklists/
    └── requirements.md
```

### Source Code Layout

```text
app/src/
├── main/java/com/madruga665/bookmarks/
│   ├── data/
│   │   ├── remote/
│   │   │   └── LinkMetadataExtractor.kt      # Add Threads extraction strategy & domain mapping
│   │   └── repository/
│   │       └── BookmarkRepository.kt         # Metadata extraction execution
│   └── ui/
│       ├── savemodal/
│       │   ├── SaveBookmarkBottomSheet.kt   # Add button loading spinner and disabled state
│       │   ├── SaveBookmarkViewModel.kt     # Add re-entrance protection on save
│       │   └── SaveBookmarkModalUiState.kt  # Ensure isSaving state binding
│       └── theme/
│           └── Color.kt                      # Neobrutalism color tokens
└── test/java/com/madruga665/bookmarks/
    ├── data/remote/
    │   └── LinkMetadataExtractorTest.kt     # Threads extraction & domain mapping tests
    └── ui/savemodal/
        └── SaveBookmarkViewModelTest.kt     # isSaving state & re-entrance tests
```

---

## Complexity Tracking

*No violations of Constitution or Architectural standards.*
