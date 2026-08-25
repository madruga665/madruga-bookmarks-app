# Tasks: Save Button Loading State & Threads Metadata Extractor

**Input**: Design documents from [`specs/014-save-loading-threads-metadata/`](./)

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Include exact file paths in descriptions

---

## Phase 1: Setup & Baseline Verification

**Purpose**: Baseline verification of project build and test suite before changes.

- [X] T001 Verify baseline build and test execution via `./gradlew testDebugUnitTest`

---

## Phase 2: Foundational (Shared Platform Routing)

**Purpose**: Shared platform identifier mapping for Threads across all stories.

- [X] T002 [P] Update domain platform formatting helper `formatDomainToPlatformName` to support Threads in `app/src/main/java/com/madruga665/bookmarks/data/remote/LinkMetadataExtractor.kt`

**Checkpoint**: Shared platform foundation ready for Threads extraction and UI presentation.

---

## Phase 3: User Story 1 - Loading Feedback on Bookmark Save Action (Priority: P1) 🎯 MVP

**Goal**: Provide immediate visual spinner feedback on the save button during bookmark persistence, disabling interactions and preventing double taps.

**Independent Test**: Trigger save bookmark in `SaveBookmarkBottomSheet`, verify spinner renders with test tag `"tag_save_bookmark_loading_spinner"`, button is disabled, and re-entrance calls are ignored.

### Tests for User Story 1

- [X] T003 [P] [US1] Add unit tests for `SaveBookmarkViewModel` `isSaving` lifecycle and re-entrance protection in `app/src/test/java/com/madruga665/bookmarks/ui/savemodal/SaveBookmarkViewModelTest.kt`

### Implementation for User Story 1

- [X] T004 [US1] Add re-entrance guard and ensure robust `isSaving` state handling in `app/src/main/java/com/madruga665/bookmarks/ui/savemodal/SaveBookmarkViewModel.kt`
- [X] T005 [US1] Update `SaveBookmarkBottomSheet.kt` save button to show centered `CircularProgressIndicator` with test tag `"tag_save_bookmark_loading_spinner"`, disable clicks when `isSaving == true`, and maintain fixed height in `app/src/main/java/com/madruga665/bookmarks/ui/savemodal/SaveBookmarkBottomSheet.kt`

**Checkpoint**: User Story 1 fully functional and testable independently.

---

## Phase 4: User Story 2 - Threads Post & Profile Metadata Extraction (Priority: P1) 🎯 MVP

**Goal**: Extract rich metadata for `threads.net` links (posts, profiles, shortlinks) including title format `Author (@user) on Threads`, caption in description, attached thumbnail, and `@Threads` badge.

**Independent Test**: Pass Threads post and profile URLs to `LinkMetadataExtractor.extractMetadata`, verify title, description, favicon, and `@Threads` source platform.

### Tests for User Story 2

- [X] T006 [P] [US2] Add unit tests for Threads post and profile metadata extraction in `app/src/test/java/com/madruga665/bookmarks/data/remote/LinkMetadataExtractorTest.kt`

### Implementation for User Story 2

- [X] T007 [US2] Implement `extractThreadsMetadata` and route `threads.net` domain in `app/src/main/java/com/madruga665/bookmarks/data/remote/LinkMetadataExtractor.kt`

**Checkpoint**: User Stories 1 AND 2 functional and testable independently.

---

## Phase 5: User Story 3 - Resilient Fallback for Login-Gated or Unreachable Threads Content (Priority: P2)

**Goal**: Provide clean structured fallback titles (`@username on Threads` or `Threads Post`) and `@Threads` badges when Threads endpoints return login walls, bot blocks, or network errors.

**Independent Test**: Pass login-gated or offline Threads URLs to `extractMetadata`, verify structural fallback returns valid title and `@Threads` badge in under 100ms.

### Tests for User Story 3

- [X] T008 [P] [US3] Add unit tests for Threads login-wall and offline structural URL parsing fallback in `app/src/test/java/com/madruga665/bookmarks/data/remote/LinkMetadataExtractorTest.kt`

### Implementation for User Story 3

- [X] T009 [US3] Implement structural fallback parsing for login-gated and offline Threads URLs in `app/src/main/java/com/madruga665/bookmarks/data/remote/LinkMetadataExtractor.kt`

**Checkpoint**: All user stories functional and resilient.

---

## Phase 6: Polish & Cross-Cutting Verification

**Purpose**: String resources, accessibility, and end-to-end verification across the test suite.

- [X] T010 [P] Verify string resources and accessibility content descriptions for save button loading state in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-pt-rBR/strings.xml`
- [X] T011 Execute full test suite and static checks via `./gradlew testDebugUnitTest check` per `specs/014-save-loading-threads-metadata/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 - Unblocks US2 and US3.
- **User Story 1 (Phase 3)**: Independent of Threads extraction - can start immediately after Phase 1.
- **User Story 2 (Phase 4)**: Depends on Phase 2 - Implements primary Threads extraction.
- **User Story 3 (Phase 5)**: Depends on Phase 4 - Implements fallback resilience.
- **Polish (Phase 6)**: Depends on all user stories being implemented.

### Parallel Opportunities

- T002, T003, T006, T008, T010 can be developed in parallel across test and UI/remote layers.
- User Story 1 (UI & ViewModel) and User Story 2 (Remote Extractor) can be implemented in parallel.

---

## Implementation Strategy

### MVP Scope (User Stories 1 & 2)
1. Complete Phase 1 (Baseline Verification).
2. Complete Phase 3 (Save Button Loading State & Re-entrance Protection).
3. Complete Phase 2 + Phase 4 (Threads Extractor & Platform Mapping).
4. Run Unit Tests to validate MVP.
5. Complete Phase 5 & Phase 6 (Fallback & Polish).
