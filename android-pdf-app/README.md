# PDF Suite - Neural Edition (Android)

This app is now multi-screen and includes the requested feature families:

1. **Neural Scanner (Image -> Multi-page PDF)**
   - Multi-image pick and compile.
   - Optical modes: B&W / Enhance / Scan.
   - Simulated AI auto-crop.
   - Reorder sequence (reverse action as demo).
   - Custom "Neural Signature" watermark drawn on pages.

2. **Neural Reader**
   - Open local PDF and render pages.
   - Zoom controls from 50% to 200%.
   - Prev/Next page navigation with indicator.
   - Home indexes local PDFs via MediaStore query.

3. **Gemini-3 Brain-Link (Simulated AI)**
   - Attach PDF and run a summary protocol.
   - Ask contextual questions with persistent chat thread (session).

4. **Cyber-Vault**
   - PIN lock simulation.
   - AES-256 (SHA-256 key derivation + AES/GCM encryption).
   - Encrypted vault files in app-private storage.
   - Decrypt to cache for transient use.

## Build & Run

1. Open `android-pdf-app` in Android Studio.
2. Sync Gradle.
3. Build APK from **Build > Build APK(s)**.

## Notes

- Brain-Link is a simulated local assistant; no remote AI API key needed.
- Vault uses local encryption and app storage directories.
