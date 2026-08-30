# AGENTS.md - AI Agent Development Guide

Operational guide and architectural reference for working within **madruga665-bookmarks-app**.

---

## 1. Project Overview & Constitution

**madruga665-bookmarks-app** is an Android link capture, organization, and bookmark synchronization client built with Jetpack Compose. Governed by [`.specify/memory/constitution.md`](.specify/memory/constitution.md):
- **API-First & Sync Ready**: Single source of truth across mobile & desktop with offline-first local cache.
- **Frictionless Capture**: Quick-input and native OS Share Target intent integration (`Intent.ACTION_SEND`).
- **Flexible Organization**: Instant saving with optional folder/tag organization (never block saving on folder selection).
- **Instant Search**: Fast query discovery (<200ms target) searching titles, URLs, tags, and collections.
- **Neobrutalism UI**: High-contrast, bold borders, hard offset shadows, flat palettes.

---

## 2. Technology Stack

- **Language & Runtime**: Kotlin 2.2.10 | JVM Target 17 | Android minSdk 26, targetSdk 34, compileSdk 37
- **UI Framework**: Jetpack Compose + Material 3 (Compose BOM `2026.08.00`, Navigation Compose `2.9.8`)
- **Dependency Injection**: Hilt `2.60.1` + KSP
- **Local Persistence**: Room Database `2.8.4` (`AppDatabase`), DataStore Preferences `1.2.1`
- **Metadata & Media**: JSoup `1.23.1` (HTML title/meta parsing), Coil `2.7.0` (image/SVG loading)
- **Networking & Sync**: OkHttp `4.12.0` (local HTTP sync server & client for desktop companion pairing)
- **Concurrency**: Kotlin Coroutines & Flow (`StateFlow`, `SharedFlow`)
- **Testing**: JUnit 4 (`4.13.2`), MockK (`1.14.11`), kotlinx-coroutines-test (`1.11.0`), MockWebServer

---

## 3. Directory Layout & Architecture

```text
madruga665-bookmarks-app/
├── app/src/main/
│   ├── java/com/madruga665/bookmarks/
│   │   ├── data/
│   │   │   ├── local/          # Room DB (AppDatabase, Entities, DAOs)
│   │   │   ├── remote/         # LinkMetadataExtractor, Sync (P2P discovery, HTTP server/client, DTOs)
│   │   │   └── repository/     # Repositories (Bookmark, Collection, Settings, Theme, Sync)
│   │   ├── di/                 # Hilt Modules (AppModule, SyncModule)
│   │   └── ui/                 # Screens, ViewModels, UiStates, Theme & Components
│   │       ├── bookmark/       # Detail, edit, tags, and share sheets
│   │       ├── collection/     # Collection list, detail, and creation sheets
│   │       ├── home/           # Main feed, capture bar, and quick actions
│   │       ├── search/         # Discovery and search screens
│   │       ├── settings/       # App preferences, pairing, and export
│   │       └── theme/          # Color.kt, Type.kt, Theme.kt (Neobrutalism tokens)
│   └── res/
│       ├── values/strings.xml          # Default strings (English)
│       └── values-pt-rBR/strings.xml   # Portuguese translations
├── specs/                      # Feature specifications (001 to 015+)
└── .specify/                   # Constitution and SpecKit templates
```

---

## 4. Development & Verification Commands

Execute all Gradle commands from the project root:

```bash
# Run all unit tests (Mandatory verification step)
./gradlew testDebugUnitTest

# Run static analysis and linting
./gradlew check

# Build debug APK
./gradlew assembleDebug
```

---

## 5. Coding & Architectural Standards

### Clean Architecture & Unidirectional Data Flow (UDF)
- **Domain & Repository**: Abstract data sources behind repository interfaces. Keep business rules independent of Android framework internals.
- **ViewModels**: Expose single immutable `UiState` via `StateFlow` using `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InitialState)`.
- **UI Layer**: Composables must be stateless when possible, consuming `UiState` and emitting actions via lambdas `(UiEvent) -> Unit`.
- **Entities & Rich Models**: Encapsulate validation and domain logic within domain models and entities.

### Neobrutalism Design System
- **Colors**: Always use palette tokens from `ui/theme/Color.kt` (e.g., `NeoYellow`, `NeoPink`, `NeoGreen`, `NeoBackground`, `NeoDark`).
- **Borders & Shadows**: Standard `2.dp` solid black border (`Color.Black`), crisp offset shadows (no soft blur), flat surfaces, distinct rounded corners (`8.dp` or `12.dp`).
- **Typography**: Bold, high-contrast, clear visual hierarchy.

### Localization & Resources
- **No Hardcoded Strings**: All user-visible strings must be defined in `app/src/main/res/values/strings.xml`.
- **Brazilian Portuguese**: Mirror all new string keys in `app/src/main/res/values-pt-rBR/strings.xml`.

### Error Handling & Offline Resiliency
- Handle all I/O, parsing, and networking errors gracefully with sealed `Result` or user-friendly `UiState.error` messages.
- Always write to local Room DB first; sync queues changes when connectivity is restored.

---

## 6. SpecKit & Agent Workflow

### Subagent Delegation Rules
- **Codebase Sweeping & Research**: Whenever scanning/sweeping code, exploring architecture, searching patterns, or conducting extensive research, **always instantiate a subagent** (`invoke_subagent` with `research` or `self`) to preserve main context clarity.
- **Orchestration**: The main agent acts as orchestrator, delegating discrete implementation and research tasks to subagents, tracking progress, and validating criteria.

### Lifecycle Steps
1. **Specify (`/speckit-specify`)**:
   - Create branch `git checkout -b <num>-<feature-name>`.
   - Author `specs/<num>-<feature-name>/spec.md` with user scenarios and quality checklist.
   - Run `/grill-me` alignment interview with the user to lock requirements before planning.
2. **Plan (`/speckit-plan`)**: Author `specs/<num>-<feature-name>/plan.md` with technical architecture and data model changes.
3. **Tasks (`/speckit-tasks`)**: Author `specs/<num>-<feature-name>/tasks.md` with dependency-ordered actionable steps.
4. **Implement (`/speckit-implement`)**:
   - Delegate discrete implementation tasks to subagents (`invoke_subagent`).
   - Keep tasks focused, maintaining existing comments and documentation integrity.
5. **Quality Gate & Review (`/code-reviewer`)**:
   - Always run `./gradlew testDebugUnitTest` to verify zero regressions.
   - Run dual-axis code review: **Standards Axis** (DDD, SOLID, Clean Architecture, Fowler smells) + **Spec Axis** (Constitution & Spec compliance).
