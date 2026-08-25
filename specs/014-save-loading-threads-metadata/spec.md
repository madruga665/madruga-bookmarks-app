# Feature Specification: Save Button Loading State & Threads Metadata Extractor

**Feature Branch**: `014-save-loading-threads-metadata`

**Created**: 2026-08-25

**Status**: Draft (Aligned via Grill-Me)

**Input**: User description: "adicionar estado de loading nos botão de add novo link, e add link metadata extractor para threads"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Loading Feedback on Bookmark Save Action (Priority: P1) 🎯 MVP

When saving a bookmark from the Save Bookmark modal, the user receives immediate visual feedback. The primary save button replaces its text label with a centered Neobrutalist progress indicator (spinner), maintains its fixed layout dimensions to avoid shifts, and enters a disabled state to prevent duplicate submissions or concurrent database writes while remote metadata extraction and persistence are in flight.

**Why this priority**: Remote metadata extraction over mobile or slow networks can take 1–3 seconds. Visual spinner feedback and click disabling eliminate accidental multi-saves, double database records, and user uncertainty.

**Independent Test**: Can be tested by opening the save modal with any URL, tapping the save button under simulated network delay, and verifying the button disables, renders a centered spinner, and dismisses smoothly upon completion.

**Acceptance Scenarios**:

1. **Given** the save bookmark modal is open with a valid URL, **When** the user taps the save button, **Then** the button text is replaced by a centered progress indicator, click interactions are disabled, and duplicate submissions are blocked.
2. **Given** the save operation is in progress, **When** metadata extraction and local database insertion succeed, **Then** the loading state concludes, the modal automatically dismisses, and a success confirmation toast is shown.
3. **Given** the save operation is in progress, **When** an error occurs during saving, **Then** the button exits the loading state, re-enables interaction, restores the button label, and displays an appropriate error message without closing the modal.

---

### User Story 2 - Threads Post & Profile Metadata Extraction (Priority: P1) 🎯 MVP

When a user captures or saves a Threads URL (e.g., `https://www.threads.net/@zuck/post/Cx...` or `https://www.threads.net/@zuck`), the system automatically routes to a dedicated Threads extractor. It extracts rich metadata matching the X/Twitter pattern: `title` formatted as `Author (@user) on Threads` (or `@user on Threads`), `description` populated with the post text / caption, post media thumbnail captured if attached, Threads platform favicon, and the source platform badge set to `@Threads`.

**Why this priority**: Enhances bookmarks fidelity with rich author details, media thumbnails, and consistent `@Threads` platform branding.

**Independent Test**: Can be tested by passing a Threads post URL to the metadata extractor and verifying that `sourcePlatform` is `@Threads`, `title` is `Author (@user) on Threads`, `description` contains the post caption, `thumbnailUrl` is extracted if present, and `faviconUrl` points to the platform icon.

**Acceptance Scenarios**:

1. **Given** a valid Threads post URL (`https://www.threads.net/@user/post/123` or `https://threads.net/t/123`), **When** metadata is extracted, **Then** the title is formatted as `Author (@user) on Threads`, description contains the post caption/text, platform is tagged as `@Threads`, and thumbnail image is captured if present.
2. **Given** a Threads user profile URL (`https://www.threads.net/@user`), **When** metadata is extracted, **Then** the title is formatted as `@user on Threads` (or profile display name) and the platform is tagged as `@Threads`.
3. **Given** any Threads URL format, **When** platform formatting helper is queried for `threads.net`, **Then** it returns `@Threads`.

---

### User Story 3 - Resilient Fallback for Login-Gated or Unreachable Threads Content (Priority: P2)

When a Threads URL is private, gated by a login wall, or unreachable due to offline/network failure, the extractor gracefully falls back to structured URL parsing. It derives the username or post identifier directly from the URL path so the user still gets a clean title (e.g., `@user on Threads`) and the `@Threads` platform tag rather than a generic or broken title.

**Why this priority**: Ensures offline resiliency and consistent bookmark presentation even when external social media endpoints block bot requests.

**Independent Test**: Can be tested by extracting metadata for a simulated login-wall response or unreachable Threads URL, verifying that fallback values like `@username on Threads` and `@Threads` platform badge are returned.

**Acceptance Scenarios**:

1. **Given** a Threads URL that returns a login redirect or 403/429 response, **When** metadata extraction runs, **Then** the system parses the username from the URL path, formats a clean fallback title (e.g., `@user on Threads`), sets `@Threads` as source platform, and uses the default platform favicon.

---

### Edge Cases

- **Rapid Multiple Taps**: User rapidly taps the save button multiple times; only the first tap executes the save flow, while subsequent taps are blocked by the disabled/loading state.
- **Malformed or Root Threads URLs**: URLs like `threads.net/` without username or path fall back gracefully to title `Threads` with `@Threads` platform badge.
- **Threads Short Links and Query Parameters**: URLs containing tracking parameters (e.g., `?xmt=...` or `?ssp=...`) or shortened path formats (`threads.net/t/XYZ`) are properly parsed without parameter pollution in titles.
- **Theme Contrast**: The loading spinner inside the Neobrutalist yellow save button renders in black/dark on-surface color for maximum contrast in both Light and Dark themes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The bookmark save button in `SaveBookmarkBottomSheet` MUST reflect the active saving state (`isSaving`) by displaying a centered loading spinner (`CircularProgressIndicator`) and disabling user click events while saving is in progress.
- **FR-002**: The save button MUST maintain its neobrutalist border, corner radius, and height while displaying the loading spinner, preventing layout jumps.
- **FR-003**: The save button MUST be disabled during loading to prevent duplicate save requests and concurrent database insertions.
- **FR-004**: If bookmark saving fails, the save button MUST return to its interactive state, hide the loading indicator, and display the error message to the user.
- **FR-005**: The system MUST detect domain `threads.net` and route metadata extraction to a dedicated Threads extractor strategy in `LinkMetadataExtractor`.
- **FR-006**: The Threads metadata extractor MUST attempt extraction via public endpoints and OpenGraph meta tag scraping using social bot User-Agent headers.
- **FR-007**: The Threads extractor MUST extract the author handle/display name, post text/caption as description, attached media thumbnail URL, and default platform favicon (`https://www.google.com/s2/favicons?domain=threads.net&sz=128`).
- **FR-008**: The Threads extractor MUST assign `@Threads` as the `sourcePlatform` badge.
- **FR-009**: The Threads extractor MUST detect login walls or bot blocks and fall back to structural URL parsing to generate a clean title (e.g., `@username on Threads` or `Threads Post`).
- **FR-010**: Domain formatting helper `formatDomainToPlatformName` MUST map `threads.net` and `www.threads.net` to `@Threads`.
- **FR-011**: All metadata extraction MUST run asynchronously on background I/O dispatchers without blocking the UI thread.
- **FR-012**: Quick Save Bar on the home screen immediately opens the Save Bookmark modal where full loading feedback is presented upon confirmation.

### Key Entities

- **LinkMetadata**: Data structure holding extracted bookmark metadata (`title`, `faviconUrl`, `thumbnailUrl`, `sourcePlatform`, `description`).
- **SaveBookmarkModalUiState**: Presentation state for the save bottom sheet including `isSaving`, `isVisible`, `targetUrl`, `selectedCollectionId`, `isPinned`, `tags`, and `error`.
- **BookmarkEntity**: Persisted bookmark record containing metadata, destination collection ID, pin state, tags, timestamps, and sync status.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Save button immediately displays centered spinner feedback within 50ms of user tap, preventing duplicate submissions (0 duplicate bookmark records created during stress tapping).
- **SC-002**: 100% of valid Threads URLs (`threads.net`) are classified with the `@Threads` source platform badge.
- **SC-003**: Extracted Threads bookmarks successfully populate non-empty titles and favicon URLs across 100% of public post and profile links tested.
- **SC-004**: In unreachable or gated network conditions, Threads fallback metadata resolves in under 100ms with structural title generation.

## Assumptions

- Users capture Threads URLs copied directly from web browsers or the Threads mobile app share sheet.
- Default favicon for Threads can be reliably sourced via Google S2 Favicon service (`https://www.google.com/s2/favicons?domain=threads.net&sz=128`).
- The Neobrutalist save button replaces the label text with a 20dp `CircularProgressIndicator` tinted in dark/border color when `isSaving == true`.
