# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CampCrap is an Android application built with Kotlin and Jetpack Compose. The app uses a standard Android project structure with:
- Package name: `com.capricallctx.campcrap`
- Min SDK: 26, Target SDK: 37
- Kotlin version: 2.4.0
- Compose BOM: 2026.06.01

## Build Commands

Build the project:
```bash
./gradlew build
```

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

Build and install debug APK:
```bash
./gradlew installDebug
```

Clean build:
```bash
./gradlew clean
```

## Architecture

The app follows standard Android architecture patterns:
- Single Activity (MainActivity) with Jetpack Compose UI
- Material 3 design system implementation
- Theme defined in `ui/theme/` package with Color, Theme, and Type definitions
- Assets stored in `app/src/main/assets/` (currently contains bg0.png and truck.png)

## Key Files

- `app/build.gradle.kts` - Main app build configuration
- `gradle/libs.versions.toml` - Version catalog for dependency management
- `MainActivity.kt` - Single activity hosting Compose UI with landing screen
- `ui/theme/` - Theme definitions for the app
- `GoogleDriveService.kt` - Service for Google Drive spreadsheet operations
- `GoogleAuthManager.kt` - Authentication manager for Google Drive API

## Google Drive Integration

The app integrates with Google Drive API to save inventory data as spreadsheets:
- Two spreadsheets per year: "People_Inventory_YYYY" and "Stuff_Inventory_YYYY"
- Automatic creation and updating of spreadsheets
- OAuth2 authentication with Google accounts
- CSV format data export to Google Sheets

## Required Setup

To use Google Drive functionality:
1. Enable Google Drive API in Google Cloud Console
2. Create OAuth 2.0 credentials for Android
3. Add the OAuth client ID to the app configuration

## CI / Release Notes

- `versionCode`/`versionName` in `app/build.gradle.kts` are read from the
  `VERSION_CODE`/`VERSION_NAME` env vars (set by all three workflows from
  `github.run_number`), falling back to `2`/`"1.1"` for local builds. Do not
  hardcode these again - every CI-built AAB used to ship as versionCode 1,
  which is what caused Play Console to reject uploads with "version 1
  already used" (fixed 2026-07-23).
- Play Store auto-upload (`auto-release.yml`, internal track) needs the
  Android Publisher API enabled on the GCP project behind
  `PLAY_STORE_SERVICE_ACCOUNT_JSON` (project `138609127719` as of
  2026-07-23), and the app's *first* release must be uploaded manually
  through the Play Console web UI (as an AAB, not APK) before the API can
  push further releases - it can't bootstrap an app's first release. Both
  were done manually on 2026-07-23; if a *new* Play Store app/listing is
  ever created for this package, redo both steps first.
- If that upload step fails, it won't fail the workflow (`continue-on-error:
  true`) - check the job summary for a "Play Store Upload Failed" section
  instead of trusting the green checkmark.