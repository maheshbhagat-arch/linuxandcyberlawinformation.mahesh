# PDF Suite (Android)

A simple Android app that demonstrates:

1. **PDF Reader**: opens a PDF from device storage and renders page 1.
2. **PDF Generator**: creates a one-page PDF from typed title/body content.

## Requirements

- Android Studio Iguana+ (or newer)
- Android SDK 34
- Min SDK 24

## How to run

1. Open `android-pdf-app` in Android Studio.
2. Let Gradle sync complete.
3. Run on emulator/device.

## Notes

- Generated files are saved in app-internal storage (`filesDir`).
- Reader currently previews only the first page for simplicity.
