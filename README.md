# Scanwell

**A document scanner for Android. No watermark, no page limit, no ads, no account.**

Scanwell turns your phone into a scanner. Capture paper documents, save them as clean PDFs, copy the text out of them, and keep everything organized on your phone.

<!-- Screenshots: put 3 or 4 phone screenshots in a docs/screenshots folder, then uncomment the line below. -->
<!--
<p align="center">
  <img src="docs/screenshots/library.png" width="220" alt="Library">
  <img src="docs/screenshots/document.png" width="220" alt="A scanned document">
  <img src="docs/screenshots/text.png" width="220" alt="Text read from a scan">
</p>
-->

## Features

- **Scan:** automatic edge detection, cropping and image filters. You can also import photos from your gallery.
- **Export:** share or save as PDF or JPEG, with no watermark. Choose from three PDF sizes for email and upload limits.
- **Password-protect PDFs:** lock any scan or any PDF on your phone with a password (AES-256). Optionally also lock editing and copying. You can open protected PDFs and remove a password you know.
- **Compress PDFs:** shrink a PDF to Low, Medium or High and see the before and after size. Your original is never changed.
- **Text recognition:** copy or share the text on any page.
- **Search:** find a scan by its name or by any word printed inside it.
- **Organize:** rename scans and file them in folders.
- **Edit pages:** add more pages to a scan, reorder them, or delete them.
- **ID card mode:** scan the front and back of a card onto one A4 page, ready to print.
- **Quick scan:** long-press the app icon and choose **Scan** to go straight to the camera.

## Download

Get the latest APK from the [Releases](../../releases) page.

To install it, open the APK on your phone. If Android asks, allow installs from the app you opened it with. Google Play Protect may warn you because the app isn't on the Play Store yet; choose to install anyway.

**Requirements**
- Android 8.0 or newer
- Google Play services, which handles the scanner and the text reader
- At least 1.7 GB of RAM
- Internet the first time you scan or read text, while Google Play services downloads those parts. After that, Scanwell works offline.

## Privacy

- Your scans are stored only on your phone, in the app's private storage.
- Scanwell has no accounts, no ads, no analytics of its own, and no servers.
- Scanning and text recognition run on the phone through [Google ML Kit](https://developers.google.com/ml-kit). ML Kit sends Google some technical information, such as the device model and performance data, for diagnostics. See Google's [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure).
- Passwords are used to lock or unlock a file and then forgotten. They are never stored, logged or sent anywhere, and a forgotten password cannot be recovered.
- Uninstalling Scanwell deletes your scans, so export anything you want to keep.

## Build it yourself

You need [Android Studio](https://developer.android.com/studio) (Quail 2 or newer) and an Android phone with Google Play.

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR-USERNAME/Scanwell.git
   ```
2. In Android Studio, choose **Open** and select the `Scanwell` folder.
3. Wait for Gradle sync to finish. The first sync downloads several gigabytes and can take 10 to 20 minutes.
4. Turn on USB debugging on your phone, plug it in, and press **Run**.

Use a real phone rather than the emulator, since the scanner needs a real camera.

To build a release APK, choose **Build > Generate Signed Bundle/APK**, select **APK**, and sign it with your own key.

### Tests

The PDF writer, compression levels, password rules and the helper code have unit tests that run on your computer:

```bash
./gradlew test
```

Or right-click `app/src/test/java/com/scanwell/app` in Android Studio and choose **Run Tests**.

## How it works

| Folder | What it does |
| --- | --- |
| `ui/` | The screens, built with Jetpack Compose |
| `MainViewModel.kt` | The app's logic: saving, exporting and reading text |
| `data/` | Stores each scan as JPEG pages plus a small JSON file |
| `pdf/` | A small PDF writer that places the JPEG pages into the PDF as they are, plus `PdfTools`, which uses PDFBox to encrypt, unlock and compress PDFs on the phone |
| `scan/` | Imports scanned pages, builds ID card pages, reads text |
| `share/` | Shares files with other apps |

All paths are under `app/src/main/java/com/scanwell/app/`.

**Built with:** Kotlin, Jetpack Compose with Material 3, Google ML Kit (Document Scanner and Text Recognition), PDFBox for Android (PDF encryption and compression), and Android Gradle Plugin 9.3.

## Roadmap

- [ ] Sign documents with a drawn signature
- [x] Password-protected PDFs
- [x] PDF compression
- [ ] A built-in PDF reader
- [ ] Release on Google Play

Ideas and bug reports are welcome in [Issues](../../issues).

## Contributing

This is a personal project, but pull requests are welcome. For anything bigger than a small fix, open an issue first so we can talk it over.

## License

<!-- Pick a license before publishing. MIT is a common, simple choice: GitHub can add it for you
     via "Add file > Create new file", typing LICENSE as the name, and choosing a template.
     Without a license, nobody else may legally reuse this code. -->
No license has been chosen yet.
