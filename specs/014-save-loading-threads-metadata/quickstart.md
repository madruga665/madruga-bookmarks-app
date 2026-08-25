# Quickstart Validation Guide: Save Button Loading & Threads Metadata Extractor

## Prerequisites
- Android SDK (API 34 / JVM 17)
- Gradle wrapper executable `./gradlew`

---

## 1. Automated Verification (Unit Tests)

Run all unit tests across the codebase, specifically targeting `LinkMetadataExtractorTest` and `SaveBookmarkViewModelTest`:

```bash
./gradlew testDebugUnitTest --tests "com.madruga665.bookmarks.data.remote.LinkMetadataExtractorTest"
./gradlew testDebugUnitTest --tests "com.madruga665.bookmarks.ui.savemodal.SaveBookmarkViewModelTest"
```

### Expected Test Results:
- `LinkMetadataExtractorTest.extractMetadata_handlesThreadsPostExtraction`: PASSED (verifies `@Threads` badge, title, and favicon).
- `LinkMetadataExtractorTest.extractMetadata_handlesThreadsProfileExtraction`: PASSED (verifies `@username on Threads`).
- `LinkMetadataExtractorTest.formatDomainToPlatformName_formatsCorrectly`: PASSED (includes `threads.net` -> `@Threads`).
- `SaveBookmarkViewModelTest.onConfirmSave_setsIsSavingTrueDuringExecution`: PASSED (verifies loading state during async save).
- `SaveBookmarkViewModelTest.onConfirmSave_whileSaving_ignoresDuplicateInvocation`: PASSED (verifies re-entrance guard).

---

## 2. End-to-End Manual UI Verification

### Scenario A: Save Button Loading Feedback
1. Launch the app (`./gradlew installDebug`).
2. Paste any valid link in the Quick Save bar on the home screen.
3. Tap the `+` button to open the Save Bookmark bottom sheet.
4. Tap the yellow `Save to "Unsorted"` button.
5. **Verify**:
   - The button text immediately changes to a centered black loading spinner.
   - Rapid tapping does nothing (button is disabled).
   - Once saved, the modal closes and displays a success toast.

### Scenario B: Threads Post Metadata Extraction
1. Copy a Threads post link (e.g., `https://www.threads.net/@zuck/post/Cx123` or `https://threads.net/@devemdobro`).
2. Paste and save via the bottom sheet.
3. Check the newly created bookmark card on the Home Screen / Collection list.
4. **Verify**:
   - Card displays the `@Threads` platform badge.
   - Title matches the author format (`Author (@user) on Threads` or `@user on Threads`).
   - Favicon displays the Threads icon.
