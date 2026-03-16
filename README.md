# 🔥 Phoenix Invoice Generator

Fully offline Android app to create professional PDF invoices, save locally, and share via WhatsApp.

**100% offline. No login. No internet required.**

---

## ✨ Features

- 🏢 Company details with logo upload
- 👤 Customer details
- 📦 Dynamic items table (add/remove rows, live totals)
- 💰 Auto-calculated subtotal, GST, discount, balance due
- 🎨 3 invoice templates: Modern Clean / GST Invoice / Compact Retail
- 📄 PDF generation using Android built-in Canvas API
- ⬇️ Download PDF to device storage
- 💬 Share via WhatsApp with pre-filled message
- 💾 Remembers your company details between sessions
- ₹ Indian Rupee currency throughout

---

## 📱 Build the APK (Free, No PC needed — uses GitHub Actions)

### Step 1 — Fork or push this repo to GitHub

1. Go to [github.com](https://github.com) on your iPad browser
2. Create a new repository named `phoenix-invoice`
3. Upload all files from this zip (use GitHub's web upload)

### Step 2 — GitHub Actions builds it automatically

Every time you push code, GitHub Actions automatically:
1. Sets up Android SDK
2. Runs `./gradlew assembleDebug`
3. Uploads the `.apk` as a downloadable artifact

### Step 3 — Download your APK

1. Go to your repo on GitHub
2. Click **Actions** tab
3. Click the latest workflow run
4. Scroll to **Artifacts** section
5. Download **Phoenix-Invoice-APK**

### Step 4 — Install on Android phone

1. Send the APK to your Android phone via WhatsApp or email
2. On the phone: Settings → Security → Allow unknown sources
3. Tap the APK file to install

---

## 🗂️ Project Structure

```
PhoenixInvoice/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/phoenix/invoice/
│   │   ├── MainActivity.kt           ← Main screen
│   │   ├── models/
│   │   │   ├── InvoiceData.kt        ← Invoice model
│   │   │   └── InvoiceItem.kt        ← Item model
│   │   ├── adapters/
│   │   │   └── ItemsAdapter.kt       ← RecyclerView adapter
│   │   └── utils/
│   │       ├── PdfGenerator.kt       ← PDF drawing engine
│   │       └── PreferencesManager.kt ← Local storage
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml     ← Main layout
│       │   └── item_invoice_row.xml  ← Item row
│       ├── values/
│       │   ├── colors.xml
│       │   ├── strings.xml
│       │   ├── themes.xml
│       │   └── ids.xml
│       ├── drawable/                 ← Icons, shapes
│       └── xml/                      ← FileProvider config
└── .github/workflows/
    └── build-apk.yml                 ← Auto-build on GitHub
```

---

## 💡 How PDF Storage Works

PDFs are saved to:
```
Android/data/com.phoenix.invoice/files/PhoenixInvoices/Invoice-PHX-YYMM-XXX.pdf
```

Access via Files app → Internal Storage → Android → data → com.phoenix.invoice → files → PhoenixInvoices

---

## 🛠️ Build Locally (if you have Android Studio)

```bash
git clone https://github.com/YOUR_USERNAME/phoenix-invoice
cd phoenix-invoice
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```
