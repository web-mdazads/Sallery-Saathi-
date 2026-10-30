# Salary Saathi — Theme Sync, Security Limits, Fast AI & Seamless Resume Upgrade

This update polishes nine core areas across **Salary Saathi**—bringing dynamic theme colors to the Splash Screen and App Icon, adding a 5-attempt PIN cooldown with recovery, fixing Home header and Developer badge clipping, adding horizontal swipe navigation to Legal pages, enabling direct installable `.apk` sharing, delivering occasional usage-based smart spending insights, accelerating **AI Saathi** response speed, and preserving your exact active screen whenever the app resumes from the background.

### User Review & Critical Decisions

> [!IMPORTANT]
> Your preferences from our clarification step are locked into this plan, alongside safe Android best practices for dynamic launcher icons and APK sharing:

- **Confirmed Decision 1 — PIN Lock Attempt Limit**: Allowing up to **5 wrong PIN attempts**, followed by a **30-second live countdown cooldown** and a prominent **Reset PIN via Security Recovery Answer** option (persisted across app restarts so force-closing cannot bypass the timer).
- **Confirmed Decision 2 — Smart Usage Notifications**: Strictly capped at **maximum 1 smart spending/budget insight per day on active days only**—never firing repeatedly or spamming on idle days.
- **Confirmed Decision 3 — Direct Installable APK Sharing**: Tapping **Share App** packages and shares the installed **Salary Saathi `.apk` file** directly via Android's native share sheet (WhatsApp, Telegram, Quick Share, Drive, Bluetooth) with zero extra storage or install permissions required.
- **Launcher Icon Theme Sync**: Your selected Color Palette will apply immediately to the launch Splash Screen, all in-app brand logos, Android 13+ monochrome themed icons, and home-screen launcher icon variants without interrupting your active session.

---

### 1. Overview & Core Concept

- **What It Does**: Eliminates everyday friction points in navigation, visual layout, security, sharing, and AI responsiveness while making Salary Saathi feel cohesive from the moment you tap the themed home-screen icon through every background-and-resume cycle.
- **Target Audience / Persona**: Salaried professionals and freelancers using Salary Saathi throughout the day who switch between Settings, UPI receipts, budgets, and other apps via the phone's Home button and expect instant resumption, crisp typography, secure PIN protection, and sub-second AI guidance.
- **Key Value**:
  - **Seamless Continuity**: Leaving the app from Settings (or any sub-page/tab) via the phone's center Home button and reopening always restores that exact screen and scroll state.
  - **Cohesive Theme Identity**: Your chosen color palette greets you on Frame 1 of the Splash Screen, in-app brand marks, and your device's launcher icon.
  - **Faster, Smarter Companion**: Sub-second streaming from AI Saathi plus occasional, high-signal daily spending digests grounded in your real transactions.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Themed Launch & Launcher Icon**: Selecting a palette in *Settings → Theme & Appearance* (such as Sunset Terracotta, Royal Indigo, Ocean Mint, Ruby Rose, Amethyst Purple, or Emerald) updates the app UI immediately, caches the palette for Frame-1 cold-start Splash rendering, and syncs the home-screen launcher icon variant.
  2. **PIN Attempt Limit & Recovery**: Entering a wrong 4-digit PIN shakes the indicator and displays remaining attempts (*"Incorrect PIN • 3 attempts left"*). On the 5th wrong attempt, the keypad locks for a live **30-second countdown** and highlights **Recover PIN Safely** so you can verify your security answer immediately if needed.
  3. **Clean Home Header & Quick Actions**: The top greeting (*"Good evening, MD Azad!"*) scales cleanly across up to two lines without cutting off your name, the live month/cycle badge displays full readable dates without awkward ellipsis, and all four Quick Action tiles (*Add Expense*, *Add Income*, *Budget*, *Ask AI*) fit cleanly on single lines.
  4. **Swipeable Legal, Privacy & Safety Pages**: Inside *Legal, Privacy & Safety*, swiping left or right anywhere on the page glides smoothly between *Privacy Policy*, *Terms of Use*, *Permissions & Safety*, and *AI & Finance Disclaimer*, keeping the top filter chips synchronized.
  5. **Unclipped Developer Verified Tick**: On *About Developer*, MD Azad's circular close-up avatar displays its complete bottom-right Verified check badge with a crisp contrasting border ring—zero clipping.
  6. **1-Tap Direct APK Share**: Tapping **Share App** (available in *About Developer* and *Settings*) opens the system share sheet with `Salary_Saathi.apk` ready to send directly to friends.
  7. **Occasional Smart Usage Notification**: On days when you actively use the app or log expenses, Salary Saathi sends at most **one** concise digest notification summarizing today's spend and remaining safe daily allowance till payday.
  8. **Instant Screen Resume**: Pressing the phone's center Home button from *Settings*, *About Developer*, *Theme*, *Budget*, or *AI Saathi* and reopening Salary Saathi resumes directly on that exact screen—even if PIN Auto-Lock activates in between.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Warm editorial fintech clarity—layered tonal gradients, crisp vector brand marks, and generous Material 3 spacing.
  - *Color Palette & Mood*: Driven dynamically by `LocalSaathiColors` (`heroGradientStart`, `heroGradientMid`, `heroGradientEnd`, `heroGlowAccent`, `accentPrimary`, `accentTertiary`) across both Light and Dark modes, eliminating hardcoded emerald overrides on the Splash screen and brand badges.
  - *Typography & Hierarchy*: Adaptive header sizing (`titleMedium`/`titleLarge` with clean wrapping) paired with compact `labelSmall` badges and tabular numeral alignment so Indian Rupee figures and cycle dates never collide with action icons.
  - *Component Styling & Layout*: Unclipped avatar badge containers (`Box` layering with surface halo ring), synchronized `HorizontalPager` + `FilterChip` strip, and disabled-state keypad styling during PIN cooldown.

- **Interactive Feedback & Motion**:
  - Live 1-second tick countdown badge during the 30-second PIN lockout with tactile error haptics on lockout start and clean unlock restoration when the timer reaches `0s`.
  - Physics-based horizontal paging (`HorizontalPager`) across the four Legal tabs with automatic chip-bar scroll alignment.
  - Immediate streaming token reveal in AI Saathi so the first words appear rapidly after tapping Send.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Overlay Session Lock + ViewModel Navigation State for Background Resume**
  - *Chosen Approach*: Store active navigation state (selected bottom tab, Settings open state, active Settings sub-route, and Notifications open state) inside the ViewModel + saveable state bundle, keep upstream state hot across backgrounding, and render `SessionLockScreen` as a full-screen security overlay above the main shell rather than unmounting the shell on `ON_STOP`.
  - *Why*: Previously, backgrounding the app unmounted the main UI shell (especially when PIN Lock triggered or state subscriptions timed out after 5 seconds), resetting `remember` state back to `Home`. Keeping navigation state in the ViewModel and overlaying the lock screen guarantees 100% preservation of the exact screen, sub-screen, and scroll position.
  - *Alternatives Considered*: Persisting only the top-level tab in preferences—rejected because returning from *Settings → Theme* or *Settings → About Developer* would still kick the user out of Settings.

- **Decision 2: Eliminating Gemini Token Choke & Model Hop for Fast AI Replies**
  - *Chosen Approach*: Stream directly using `gemini-3.8-flash` with an unchoked output token window (removing the tiny `220`-token cap that Gemini 3 thinking tokens exhausted before emitting visible words, which previously forced slow back-to-back retry calls), compacting the financial summary prompt payload, and pre-warming the lazy model instance when opening the AI tab.
  - *Why*: Cuts Time-to-First-Token dramatically while still enforcing ultra-short 2–3 line Hinglish/Hindi/English answers via system instructions.
  - *Alternatives Considered*: Simulated/canned local replies—rejected to ensure every answer remains genuinely powered by Gemini AI with live financial context.

- **Decision 3: Zero-Permission Self-APK Sharing via Cache FileProvider**
  - *Chosen Approach*: Copy the app's own installed base package (`applicationInfo.publicSourceDir`) into a dedicated `FileProvider` cache folder as `Salary_Saathi.apk` and share via `Intent.ACTION_SEND` (`application/vnd.android.package-archive`).
  - *Why*: Works offline and across WhatsApp, Telegram, Nearby/Quick Share, and Drive while requiring **zero** sensitive Play Store permissions (`REQUEST_INSTALL_PACKAGES` or `QUERY_ALL_PACKAGES` are never needed).

- **Decision 4: Synchronous Palette Cache + Safe Launcher Aliases for Theme Colors**
  - *Chosen Approach*: Mirror the user's `ColorPalettePreset` and `ThemeMode` into synchronous local preferences so Frame 1 of the cold-start Splash Screen renders in the exact theme color with zero green flash, while toggling dedicated `<activity-alias>` launcher icons (`DONT_KILL_APP`) and keeping `MainActivity` permanently enabled.
  - *Why*: Prevents broken notification intents while allowing the device home-screen icon, Android 13+ monochrome icon, Splash Screen, and in-app brand emblems to reflect the chosen theme.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity & Root Overlay Host                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │  Synchronous Theme Cache ──► SalarySaathiTheme (8 Palettes, Light/Dark)│  │
│  │   ├─ Cold Launch Only: Themed SaathiSplashScreen (Hero Palette + Logo) │  │
│  │   ├─ Persistent Main Shell (Tabs + Settings Sub-Routes + Scroll State) │  │
│  │   └─ Full-Screen Security Overlay: SessionLockScreen (5-Try / 30s Cap) │  │
│  └───────────────────────────────────┬────────────────────────────────────┘  │
└──────────────────────────────────────┼───────────────────────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
┌─────────────────────┐   ┌─────────────────────────┐   ┌──────────────────────┐
│ Navigation & Lock   │   │ UI Polish & Sharing     │   │ Fast AI & Smart      │
│ State (ViewModel)   │   │ Surfaces                │   │ Usage Digest         │
│                     │   │                         │   │                      │
│ • Active MainTab    │   │ • Home Header (2-Line   │   │ • Direct Streaming   │
│ • Settings Open &   │   │   Name + Full Date Pill │   │   Gemini 3.8 Flash   │
│   Active Sub-Route  │   │   + Crisp Action Tiles) │   │   (Pre-warmed, Fast) │
│ • PIN Fail Counter  │   │ • Legal HorizontalPager │   │ • Max 1/Day Active   │
│   & 30s Cooldown    │   │   <-> FilterChip Sync   │   │   Spend Digest Alert │
│   Timestamp (Prefs) │   │ • Unclipped Verified    │   │   (Daily Spend +     │
│ • Launcher Icon     │   │   Avatar Badge          │   │    Safe Allowance)   │
│   Alias Sync        │   │ • FileProvider Self-APK │   │                      │
│                     │   │   Share Sheet Exporter  │   │                      │
└─────────────────────┘   └─────────────────────────┘   └──────────────────────┘
```

- **Data Model & State**:
  - **Persistent Navigation Session State**: Holds `activeTab`, `isSettingsOpen`, `settingsSubRoute`, `isNotificationsOpen`, and `legalSelectedTab` in the ViewModel (backed by saveable state) with `SharingStarted.Eagerly` so backgrounding never drops or resets UI navigation.
  - **PIN Security Rate-Limit State**: Adds `failedPinAttempts` (`Int`) and `pinLockoutUntilMillis` (`Long`) to `UserPreferences`, persisted in DataStore so cooldown timers survive process recreation.
  - **Synchronous Theme Snapshot**: Lightweight key-value mirror storing the active `ThemeMode` and `ColorPalettePreset` for instant synchronous read on cold launch before asynchronous DataStore streams emit, plus launcher `<activity-alias>` synchronization.
  - **Smart Usage Digest Milestone**: Uses daily milestone keys (`smart_usage_digest_yyyy-MM-dd`) and last-digest timestamps in `UserPreferences` alongside a `notifySmartAiDigest` preference toggle and `AI_INSIGHT` notification entries in Room.

- **Interactive Component & State Mapping**:
  - **Background & Home-Button Resume**: Pressing the device Home button triggers `ON_STOP`, locking the session overlay (if a PIN is set) *without* unmounting the underlying screen tree or resetting `settingsSubRoute`. Reopening the app unlocks the overlay and reveals the exact screen (`Settings`, `Theme`, `About Developer`, `Legal`, `Budget`, etc.) immediately.
  - **PIN Keypad Rate Limiter**: Each wrong 4-digit entry increments `failedPinAttempts`. Reaching `5` sets `pinLockoutUntilMillis = now + 30_000L` and resets the counter after cooldown or successful Security Recovery reset; a `LaunchedEffect` ticks every `250ms` to update the live countdown label and re-enable the keypad at `0s`.
  - **Swipeable Legal Pager**: Swiping `HorizontalPager` updates `pagerState.currentPage`, which updates the active `FilterChip` and scrolls the chip strip into view; tapping any `FilterChip` launches `pagerState.animateScrollToPage(index)`.
  - **Direct APK Exporter**: Tapping *Share App* copies the base APK into the `shared_apk` cache directory on an I/O dispatcher, attaches a `FileProvider` URI with `FLAG_GRANT_READ_URI_PERMISSION`, and opens `Intent.createChooser` for instant sending.

- **Key Sequences & Integrations**:
  - **Fast AI Saathi Streaming**: Opening the AI tab pre-initializes the lazy `gemini-3.8-flash` client; sending a message builds a compact ~60-token financial context + language directive and streams chunks directly to the chat bubble without thinking-token starvation or redundant fallback hops.
  - **Usage-Driven Daily Digest**: When the user records or approves transactions on an active day (and hasn't received a digest in the last 18+ hours or today), the coordinator computes today's total spend vs. safe daily allowance and posts a single concise notification to both the in-app Notification Center and Android system tray.
