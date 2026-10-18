# Saeedi Essence Order List

A simple order-list maker for shop orders. The web app is `index.html`; its fonts and PDF library are bundled locally. The Android project wraps the same app in a lightweight native WebView and adds Android file saving and sharing.

## What it does

- Add items with company name, category, weight, and quantity
- Apply one order company to items that do not have a separate company
- Group the list by category or company
- Show quantity totals for each group and for the whole order
- Export as PDF, a shareable picture, plain text, or a printed list
- Light and dark themes
- Automatically save the order on the current device

## Install the Android app

A directly installable **debug-signed APK** is built by GitHub Actions. It supports Android 10 (API 29) and newer.

1. Open the [**Build Android APK** workflow](https://github.com/Zain6075/Order-/actions/workflows/android-apk.yml) (or the repository's **Actions** tab). A push to the app branch also starts this workflow automatically.
2. After a successful build, download [`downloads/Saeedi-Essence-Android.apk`](downloads/Saeedi-Essence-Android.apk) directly, or get the **Saeedi-Essence-Android-debug** artifact from the latest run.
3. Open the APK on the Android device and approve Android's “install unknown apps” prompt if shown.

The APK bundles the app, fonts, and PDF library, so making an order does not require an internet connection. The **Save** button writes PNG/PDF files to `Downloads/Saeedi Essence Orders`; **Share** opens Android's share sheet. Orders are kept in this app's private browser storage and are not shared with the web-browser copy. Uninstalling the app or clearing its app data removes its saved orders.

This is a debug build for direct installation, not a Play Store release. A Play Store release would need a production signing key and release configuration.

### Build locally

Install JDK 17, Android SDK Platform 35, Build Tools 35.0.0, and Gradle 8.9, then run:

```sh
cd android
gradle :app:assembleDebug
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

## Use the web app

Open `index.html` in a modern browser. Orders are saved only in that browser's local storage. To publish with GitHub Pages, publish the repository root (not just the HTML file), so `index.html`, `fonts/`, and `vendor/` are all available. The root URL then opens the app.

To create a browser shortcut instead of installing the APK, open the hosted HTTPS page in Chrome and use **⋮ → Add to Home screen**. This shortcut opens the website; it is not the standalone Android package.

## Privacy and limitations

- Orders are stored locally on the device. They do not synchronize between browsers or devices.
- The installed APK includes the PDF generator and fonts and works offline. The hosted web version needs the site assets to be available.
- The PDF is rendered from an image, so its text is not selectable or searchable.
- Android downloads and native sharing are available inside the APK. Browser versions use the browser's own download/share capabilities.
