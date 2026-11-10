# Developing Scanwell

Notes for building and changing Scanwell. For what it does, see the [README](../README.md).

## Build it yourself

You need [Android Studio](https://developer.android.com/studio) (Quail 2 or newer) and an Android phone with Google Play.

1. Clone the repository:
   ```bash
   git clone https://github.com/direkjames/Scanwell.git
   ```
2. In Android Studio, choose **Open** and select the `Scanwell` folder.
3. Wait for Gradle sync to finish. The first sync downloads several gigabytes and can take 10 to 20 minutes.
4. Turn on USB debugging on your phone, plug it in, and press **Run**.

Use a real phone rather than the emulator, since the scanner needs a real camera.

To build a release APK, choose **Build, Generate Signed Bundle/APK**, select **APK**, and sign it with your own key.

## Tests

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

All paths are under `app/src/main/java/com/scanwell/app/`. The build check that runs on every push is in `.github/workflows/build.yml`.

## Ideas

- Sign documents with a drawn signature
- A built-in PDF reader
- A Google Play release

Ideas and bug reports are welcome in [Issues](https://github.com/direkjames/Scanwell/issues).

## Version

The release on GitHub is 2.0, while `versionName` in `app/build.gradle.kts` still says `1.1`. Keep them in step when you cut the next release.
