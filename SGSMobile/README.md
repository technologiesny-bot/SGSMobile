# SGS 2.0 Mobile

This repository contains the Android mobile app project for the SGS 2.0 Mission-Control HUD and forensic vision interface.

## Included
- Android Studio-ready project structure
- Demo forensic image processing and HUD overlays
- Palm scan detection
- Trauma / wound detection logic
- First-aid protocol engine
- GitHub Actions workflow to compile an APK in CI

## Quick start
1. Open the project in Android Studio.
2. Let Gradle sync.
3. Build -> Build APK(s).

## Build with GitHub Actions
The workflow in `.github/workflows/android.yml` will build the debug APK automatically on push and pull request.
