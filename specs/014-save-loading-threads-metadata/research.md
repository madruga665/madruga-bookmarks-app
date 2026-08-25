# Research & Technical Decisions: Save Button Loading & Threads Metadata Extractor

## 1. Save Button Loading State & Interaction Guard

### Decision
Enhance `SaveBookmarkBottomSheet`'s primary confirmation button to consume `uiState.isSaving`. When `isSaving == true`:
- The button disables click interactions (`enabled = !uiState.isSaving`).
- The button label text is replaced with a centered 24dp `CircularProgressIndicator` styled with a 2.5dp black stroke matching Neobrutalism borders.
- A fixed minimum container height (52.dp) is maintained to avoid layout jump during state transition.
- In `SaveBookmarkViewModel.onConfirmSave`, a re-entrance guard (`if (current.isSaving || current.targetUrl.isBlank()) return`) prevents duplicate coroutine launches if rapid taps bypass the UI boundary.

### Rationale
- Immediate visual feedback gives users certainty that their action is being processed while external metadata extraction and Room DB insertion complete.
- Disabling the button prevents concurrent network requests and duplicate database records (`BookmarkEntity`).
- Retaining identical button outer bounds prevents jarring UI layout shifts.

### Alternatives Considered
- *Full-screen blocking modal overlay*: Rejected because it obscures the bottom sheet and feels jarring for a quick save action.
- *Showing "Saving..." text next to spinner*: Rejected during alignment interview in favor of a clean, centered Neobrutalist spinner to preserve button visual balance and avoid text wrapping on localized screens.

---

## 2. Threads Link Metadata Extractor Strategy

### Decision
Implement `extractThreadsMetadata(cleanUrl: String, domain: String): LinkMetadata` inside `LinkMetadataExtractor.kt`:
1. **Domain Detection**: Route URLs containing `threads.net` in `extractMetadata`.
2. **Strategy A (Social Bot OpenGraph Scraping)**:
   - Connect via `Jsoup` with `BOT_USER_AGENT` (`facebookexternalhit/1.1`) and `TWITTER_BOT_USER_AGENT` (`Twitterbot/1.0`).
   - Extract `meta[property=og:title]`, `meta[property=og:description]`, `meta[property=og:image]`, and `meta[name=description]`.
3. **Strategy B (Threads oEmbed API Endpoint)**:
   - Fallback query to official oEmbed endpoint `https://www.threads.net/oembed?url={encodedUrl}&format=json` or embed page scraper if available.
4. **Strategy C (Resilient URL Structure Fallback)**:
   - If blocked by login wall or network failure, parse URI path parts (e.g. `/@{username}/post/{id}` or `/@{username}` or `/t/{id}`).
   - Format title as `Author (@username) on Threads` or `@username on Threads` (or `Threads Post` for shortlinks).
   - Use default Threads favicon `https://www.google.com/s2/favicons?domain=threads.net&sz=128`.
   - Set `sourcePlatform = "@Threads"`.
5. **Domain Formatter**:
   - Update `formatDomainToPlatformName` to return `"@Threads"` when domain contains `"threads.net"` or `"threads"`.

### Rationale
- Threads is part of Meta's ecosystem and serves full OpenGraph tags to social scrapers (`facebookexternalhit`), enabling rich previews without requiring authenticated API tokens.
- Structured URL fallback guarantees offline resilience and clean titles even when network is unavailable or posts are login-restricted.
- Harmonizes with existing Twitter/X and Instagram extraction patterns in the codebase.

### Alternatives Considered
- *Official Meta Threads Graph API*: Rejected because it requires OAuth App Review, user authentication, and access tokens for saving public links, violating Frictionless Capture (Constitution Principle II).
- *Generic web scraping fallback only*: Rejected because generic scraper fails on Meta login walls, returning empty titles or "Threads • Log in" instead of author and post info.

---

## 3. Localization & Design System Tokens

### Decision
- Button loading state uses `NeobrutalismTheme.colors.onSurface` (black/high-contrast border) for the `CircularProgressIndicator` ensuring accessibility against `accentYellow` container.
- Tags and accessibility content descriptions are registered in `strings.xml` and `strings.xml (pt-rBR)`.

### Rationale
- Adheres to repository Neobrutalism design system and multi-language support (English & Portuguese).
