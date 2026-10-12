# Scanwell

A free document scanner for Android. No watermark, no page limit, no account, no ads.

## What it does

- Scans paper with automatic edge detection, cropping and filters (Google ML Kit document scanner)
- Keeps scans in a library with search, folders, rename and delete
- Reads the text on each page so you can copy it and search inside scans
- Exports PDF (three file sizes) or JPEG, to share or to save on the phone
- Edits pages after scanning: add, reorder, delete
- ID card mode: front and back on one A4 page
- "Scan" shortcut when you long-press the app icon

## Open and run it

1. Install Android Studio (latest stable).
2. File > Open, choose this folder (the one containing `settings.gradle.kts`).
3. Wait for "Gradle sync" to finish. The first time downloads a lot and can take 10 to 20 minutes.
4. Plug in an Android phone with USB debugging on, then press the green Run button.

The full walkthrough, from installing the tools to publishing on Google Play, is in the guide that came with this project.

## Where things are

| Path | What is in it |
| --- | --- |
| `app/build.gradle.kts` | App ID, version number, libraries |
| `gradle/libs.versions.toml` | Every version number in one place |
| `app/src/main/AndroidManifest.xml` | The app's declaration to Android |
| `app/src/main/java/com/scanwell/app/MainActivity.kt` | The app's window; starts the scanner |
| `.../MainViewModel.kt` | The app's logic: saving, exporting, reading text |
| `.../ui/` | The screens (library, document, page viewer, dialogs) |
| `.../data/` | How scans are stored on the phone |
| `.../pdf/` | The PDF writer |
| `.../scan/` | Importing pages, ID card mode, text reading |
| `.../share/` | Sharing to other apps |
| `app/src/main/res/` | Icons, app name, theme |
| `app/src/test/` | Automatic tests for the PDF writer and helpers |

## Make it yours

- **App name:** `app/src/main/res/values/strings.xml`, the `app_name` line. Also the title in `ui/LibraryScreen.kt`.
- **App ID:** `applicationId` in `app/build.gradle.kts`. Change it before your first upload to Google Play; it is permanent after that. Leave `namespace` alone.
- **Icon:** `app/src/main/res/drawable/ic_launcher_foreground.xml` and the color in `res/values/colors.xml`.

## Good to know

- The scanner and the text reader come from Google Play services, so the app needs a phone with Google Play. The first use of each needs internet once so Play services can download them.
- Scans are stored in the app's private storage. Uninstalling the app deletes them, so export anything you want to keep.
- Not built yet: signatures on documents and password-protected PDFs.
