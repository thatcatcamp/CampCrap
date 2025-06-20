# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CampCrap is an Android application built with Kotlin and Jetpack Compose. The app uses a standard Android project structure with:
- Package name: `com.capricallctx.campcrap`
- Min SDK: 24, Target SDK: 35
- Kotlin version: 2.0.21
- Compose BOM: 2024.09.00

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