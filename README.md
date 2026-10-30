# 💰 Salary Saathi - Smart Personal Finance & AI Companion

**Salary Saathi** is an intelligent, modern, and privacy-focused personal finance manager designed specifically for salaried professionals. Built with **Kotlin**, **Jetpack Compose**, and **Material 3**, Salary Saathi helps users track monthly salary, incentives, daily expenses, savings goals, and budgets effortlessly with the assistance of **AI Saathi** (powered by Google Gemini).

---

## ✨ Highlights & Key Features

### 💵 1. Salary & Income Management
* **Flexible Salary Cycle:** Set custom monthly salary start dates (e.g., 1st or 30th of every month).
* **Multiple Income Sources:** Track base salary, performance bonuses, overtime, investments, and side gigs.
* **Income Breakdown:** Visual representation of gross earnings vs spent income.

### 🤖 2. AI Saathi Assistant (Powered by Gemini)
* **Natural Language Expense Logging:** Simply type or ask *"Added ₹150 for coffee"* or *"Spent ₹2500 on groceries"*, and AI Saathi automatically records it under the right category.
* **Undo & Quick Actions:** Instant visual feedback with interactive action badges and single-tap undo capability.
* **Smart Financial Insights:** Get personalized recommendations on monthly savings, budget alerts, and spending trends.

### 📱 3. Auto-Parsing UPI & Bank Alerts
* **Notification Listener:** Automatically captures SMS/UPI transaction notifications from bank apps.
* **One-Tap Quick Logging:** Parse amount, vendor, and category from notifications directly into your ledger.
* **Custom Theme Notification Cards:** Beautiful, theme-colored notification cards with single-icon standard system style.

### 📊 4. Category Budgeting & Expense Tracking
* **Custom Category Limits:** Set budget caps for Groceries, Rent, Utilities, Dining, Entertainment, Transport, and Shopping.
* **Visual Progress Indicators:** Color-coded budget bars (Green, Yellow, Red) to alert you before overspending.
* **Detailed Analytics:** Filter expenses by date, category, payment mode, or keyword search.

### 🎯 5. Savings Goals & Milestones
* **Goal Tracker:** Set targets for Emergency Fund, Vacation, Gadgets, or Festival Savings.
* **Progress Badges:** Visual milestone badges celebrating your savings achievements.

### 🔒 6. Security & Customization
* **4-Digit App PIN Lock:** Secure dot-indicator PIN screen protecting financial data.
* **Dynamic Color Themes:** Support for multiple M3 color schemes (Emerald, Sapphire, Obsidian Dark, Indigo, Crimson).
* **Initials Profile Avatars:** Clean dynamic avatar generated from user's full name.

---

## 🛠 Tech Stack & Architecture

* **Language:** 100% Kotlin
* **UI Framework:** Jetpack Compose with Material Design 3 (M3)
* **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
* **Asynchronous Programming:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Local Database:** Room Database with KSP (Kotlin Symbol Processing)
* **Local Preferences:** Jetpack DataStore (Preferences)
* **AI Integration:** Firebase AI Logic / Google Gemini API SDK
* **Navigation:** Jetpack Navigation Compose with `@Serializable` type-safe routes
* **Testing:** Robolectric JVM Unit & UI Tests

---

## 🚀 How to Run the Project Locally

### Prerequisites
* Android Studio Jellyfish (2023.3.1) or newer
* JDK 17 or higher
* Android SDK 24+ (Min SDK 24, Target SDK 34)

### Setup Steps
1. **Clone the Repository:**
   ```bash
   git clone https://github.com/web-mdazads/aistudio-repository-template.git
   cd aistudio-repository-template
   ```
2. **Open in Android Studio:**
   * Select `File` -> `Open...` and select the project directory.
   * Allow Gradle to sync dependencies automatically.
3. **Build & Run:**
   * Connect an Android device or launch an Android Emulator (API 24+).
   * Press `Shift + F10` or click **Run 'app'**.

---

## 📲 How to Export / Download APK

### Method 1: Export APK directly from AI Studio
1. In the **Google AI Studio** workspace editor interface, locate the top bar or settings menu.
2. Click on the **Settings** / **Export** icon at the top right corner.
3. Select **Export APK** / **Download APK**.
4. Once the build process finishes, the downloadable `.apk` file will be saved to your computer or device downloads folder.

### Method 2: Build APK via Android Studio
1. Open the cloned repository in **Android Studio**.
2. Go to `Build` menu in the top bar.
3. Click `Build Bundle(s) / APK(s)` -> **`Build APK(s)`**.
4. Once completed, a notification popup will appear showing **`APK(s) generated successfully`**.
5. Click **`locate`** to find your output `.apk` file in `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🌐 How to Make your GitHub Repository Public

If your repository is currently **Private** and you want to make it **Public** on GitHub:

1. Open your repository on GitHub: `https://github.com/web-mdazads/aistudio-repository-template` (or via your GitHub Mobile App).
2. Click on the **⚙️ Settings** tab located near the top right of the repository page.
3. Scroll all the way down to the **"Danger Zone"** section at the bottom of the page.
4. Locate **"Change repository visibility"** (or *Change visibility*).
5. Click the **Change visibility** button.
6. Select **"Make public"**.
7. Read the confirmation prompt, type your repository name (e.g., `web-mdazads/aistudio-repository-template`) to confirm, and click **"I understand, change repository visibility"**.
8. Your repository is now **Public** and accessible to everyone!

---

## 📄 License
This project is open-source under the MIT License. Feel free to customize and build upon it!
