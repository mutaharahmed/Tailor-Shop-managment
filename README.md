# Azad Taillors

An offline-first Android business management application built for **Azad Taillors** — a tailoring and ready-made garment shop. Designed for fast counter-side workflows, financial ledger accuracy, bilingual English/Sindhi operation, and zero dependency on internet connectivity or cloud services.

---

## Key Design Principles

- **Strictly Offline-First:** Operates completely offline (including Airplane Mode). No `INTERNET` permission is declared in `AndroidManifest.xml`, and there are no cloud databases, remote APIs, analytics, ads, or online authentication.
- **Strict Light-Only Theme:** Always renders in a clean, high-contrast light Material 3 interface (`Theme.Material.Light.NoActionBar`, `forceDarkAllowed = false`, `AppCompatDelegate.MODE_NIGHT_NO`), even when the device is set to system dark mode.
- **Bilingual Support (English & Sindhi):** Full Android string localization (`values/strings.xml` and `values-sd/strings.xml`) with instant in-app language switching, automatic RTL layout mirroring for Sindhi, bundled `Noto Sans Arabic` typography for Sindhi Unicode, and LTR-preserved numeric fields (`PKR` amounts, phone numbers, order codes, receipt numbers, and measurements).
- **Financial & Historical Data Integrity:**
  - All monetary values use integer `Long` **PKR** amounts (no floating-point rounding errors).
  - Customer measurement profiles are **immutable and versioned** so historical orders preserve the exact measurements they were stitched with.
  - Payment transactions are **append-only** with unique `idempotencyKey` guards and bounded refunds/adjustments.

---

## Features

### 1. Dashboard
- Live overview of **Today's Orders**, **In Progress**, **Ready Orders**, and **Overdue Orders**.
- Financial snapshot of **Today's Payments**, **Outstanding Balances**, **Expenses**, and **Low-Stock Alerts**.
- Quick-action shortcuts for creating customers, orders, expenses, and backups.

### 2. Customer Management & Versioned Measurements
- Store customer code, name, phone, address, and notes.
- Supports shared family phone numbers (non-unique phone index) with a non-blocking duplicate-number warning.
- **Immutable Measurement Profiles (`MeasurementProfileEntity`):**
  - Supports garment types: `SHALWAR_KAMEEZ`, `SHIRT`, `PANT_TROUSER`, `WAISTCOAT`, `COAT_JACKET`, `KURTA`, and `CUSTOM`.
  - Supports **Inches** and **Centimeters** with conversion helpers, standard fields, and user-defined custom fields.
  - Saving changes creates a new version (`versionNumber + 1`) rather than mutating past snapshots. Returning-customer order creation prompts to reuse an existing profile or create a new version.

### 3. Tailoring Orders & Design Preferences
- Order lifecycle tracking: `DRAFT` → `CONFIRMED` → `IN_PROGRESS` → `READY` → `DELIVERED` (plus `CANCELLED`).
- Tracks garment type, quantity, stitching price (`PKR`), order date, delivery date, and instructions.
- Configurable style preferences (**Collar**, **Front**, **Cuffs**, **Pockets**, **Sleeves**, **Bottom**, **Fit**, **Embroidery**) plus user-created custom preference options persisted in Room.

### 4. Immutable Payment Ledger & Receipts
- Records **Advance**, **Installment**, **Final Settlement**, **Refund**, and **Adjustment** transactions (`CASH`, `BANK_TRANSFER`, `MOBILE_WALLET`, `CARD`, `OTHER`).
- Enforces idempotency via unique `idempotencyKey` constraints and prevents refunding more than the net paid amount.
- Generates offline **Payment Receipts** with unique receipt numbers (`ReceiptRoute`).

### 5. Ready-Made Inventory & Stock Ledger
- Product catalog with SKU, category, size, color, cost price, selling price, stock quantity, low-stock threshold, and archive support.
- Traceable **`StockMovementEntity`** audit trail (`INITIAL_STOCK`, `RESTOCK`, `SALE`, `RETURN`, `ADJUSTMENT`) with idempotency protection and oversell prevention.

### 6. Expenses & Business Reports
- Categorized shop expense tracking (`RENT`, `ELECTRICITY`, `SUPPLIES`, `WAGES`, `MAINTENANCE`, `HOSPITALITY`, `OTHER`).
- Date-range financial reports summarizing tailoring receipts, ready-made sales, expenses, net cash flow, and outstanding receivables.

### 7. Local Backup & Recovery (WorkManager + SAF)
- **Scheduled Auto-Backups:** Background local backups managed by `WorkManager` (`AutoBackupWorker`).
- **Manual Export & Import:** Portable `.atbackup` ZIP archives exported/imported to external storage or USB/SD via Android **Storage Access Framework (SAF)**.
- **Integrity & Safety Guards:**
  - Executes `PRAGMA wal_checkpoint(TRUNCATE)` before snapshotting the SQLite database.
  - Packages `manifest.json` (schema version, record counts, SHA-256 checksum) alongside `azad_taillors.db`.
  - Validates SQLite magic header, SHA-256 checksum, and `PRAGMA integrity_check` before allowing restore.
  - Shows a **Restore Preview** with record counts and automatically creates a `SAFETY` backup of live data before overwriting.
  - Checks available device storage and prevents deleting the only remaining valid local backup.

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin (`2.2.10`) |
| **UI Framework** | Jetpack Compose + Material 3 (Compose BOM `2024.09.00`) |
| **Architecture** | MVVM + Repository Pattern + Manual DI (`AppContainer`) |
| **Navigation** | Navigation Compose (`2.8.9`) with `@Serializable` type-safe routes |
| **Database** | Room (`2.7.0`) + SQLite (WAL mode) + KSP (`2.3.5`) |
| **Preferences** | Jetpack DataStore Preferences (`1.1.7`) |
| **Background Work** | WorkManager (`2.10.1`) |
| **Serialization** | `kotlinx.serialization` JSON (`1.8.0`) |
| **Async** | Kotlin Coroutines & `Flow` (`1.10.2`) |
| **Testing** | JUnit 4, Robolectric (`4.16.1`), Compose UI Test, Coroutines Test |
| **SDK Targets** | `minSdk 24` · `targetSdk 36` · `compileSdk 36` |

---

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/
│   ├── AzadTaillorsApp.kt                 # Application class & WorkManager Configuration.Provider
│   ├── MainActivity.kt                    # Single-activity entry point (edge-to-edge, light mode)
│   ├── di/AppContainer.kt                 # Manual dependency injection container
│   ├── data/
│   │   ├── backup/
│   │   │   ├── BackupManager.kt           # .atbackup ZIP creation, SAF I/O, SHA-256 & integrity checks
│   │   │   └── AutoBackupWorker.kt        # Scheduled WorkManager local backup worker
│   │   ├── local/
│   │   │   ├── AppDatabase.kt             # Room DB singleton, WAL checkpoint & restore hooks
│   │   │   ├── Converters.kt              # Room enum & type converters
│   │   │   ├── entity/                    # Room entities & domain enums
│   │   │   └── dao/                       # DAOs and reactive query projection models
│   │   ├── preferences/
│   │   │   └── ShopPreferencesRepository.kt
│   │   └── repository/                    # Customer, Measurement, Order/Payment, Inventory, Expense repos
│   ├── ui/
│   │   ├── AzadTaillorsRoot.kt            # Root NavHost, bottom bar, locale & theme wrapper
│   │   ├── components/CommonUi.kt         # Reusable M3 cards, chips, badges, empty states
│   │   ├── navigation/AppRoutes.kt        # Type-safe @Serializable navigation destinations
│   │   ├── screens/                       # Dashboard, Customers, Orders, Inventory, Finance, Backup/Settings
│   │   ├── theme/                         # Color.kt, Theme.kt, Type.kt (strict light theme + fonts)
│   │   └── viewmodel/                     # StateFlow ViewModels & AppViewModelFactory
│   └── util/LocalizationHelper.kt         # In-app English/Sindhi locale & LTR formatting helpers
└── res/
    ├── font/                              # Bundled Plus Jakarta Sans, JetBrains Mono, Noto Sans Arabic
    ├── values/strings.xml                 # English strings
    ├── values-sd/strings.xml              # Sindhi (سنڌي) strings
    └── values/ & values-v29/themes.xml    # Light-only window theme configuration
```

---

## Getting Started

### Prerequisites
- **Android Studio** (supporting AGP `9.1.1` and JDK 11+)
- **Android SDK** (`compileSdk 36`, `minSdk 24`)
- No API keys, Firebase project, or internet connection are required to build or run the app.

### Build & Run
1. Clone the repository:
   ```bash
   git clone <your-repo-url>
   cd <repo-folder>
   ```
2. Open the project in Android Studio and sync Gradle, or build from the command line:
   ```bash
   gradle :app:assembleDebug
   ```

### Running Tests
Unit and Robolectric tests (covering business rules, database integrity, backup/restore, localization, light-theme enforcement, and offline manifest checks) run directly on the JVM without an emulator:

```bash
gradle :app:testDebugUnitTest
```

Test suites located in `app/src/test/java/com/example/`:
- `AzadTaillorsBusinessIntegrityTest.kt` — Tests measurement profile immutability, shared family phone numbers, payment idempotency & refund bounds, inventory oversell prevention, and `.atbackup` creation/corruption-detection/restore with safety backup.
- `ExampleRobolectricTest.kt` — Tests Dashboard navigation, English/Sindhi toggling, light-only UI under system night mode, and absence of `INTERNET` permission.
- `ExampleUnitTest.kt` — Core unit sanity checks.

---

## Backup & Data Safety Notes

- Automatic backups are stored in app-private storage (`files/backups/`). **Same-device backups protect against accidental edits or corruption, not device loss, theft, or app uninstallation.** Always use **Export Backup** (SAF) regularly to save `.atbackup` files to an external USB drive, SD card, or another device.
- Future database schema updates must add explicit `Migration` objects to `AppDatabase.MIGRATIONS` rather than using destructive migrations.

---

## License

Add your preferred license (e.g., MIT, Apache-2.0, or Proprietary) in a `LICENSE` file.
