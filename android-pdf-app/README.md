# PDF Suite - Neural Edition (Android)

Now updated with a cleaner red "PDF tools" style UI and smoother multi-screen flow.

## Modules

1. **Neural Scanner (Image -> Multi-page PDF)**
   - Select multiple images from gallery.
   - Capture image from camera.
   - Optical modes: B&W / Enhance / Scan.
   - Simulated AI auto-crop.
   - Reorder nodes before compile.
   - Custom watermark signature across pages.

2. **Neural Reader**
   - Open and render local PDFs.
   - Zoom control from 50% to 200%.
   - Prev/Next page controls with page indicator.

3. **Gemini-3 Brain-Link (Simulated)**
   - Attach PDF and run summary action.
   - Ask follow-up questions in persistent session chat.

4. **Cyber-Vault**
   - PIN lock simulation.
   - AES/GCM encrypted file storage.
   - Decrypt-to-cache preview workflow.

## Build & Run

1. Open `android-pdf-app` in Android Studio.
2. Sync Gradle.
3. Build APK from **Build > Build APK(s)**.

## Notes

- Brain-Link is currently a local simulated assistant (no live cloud API wired yet).
- Vault encryption is local and stored in app-private directories.
