# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kexplore is an Android app for exploring Kubernetes cluster resources. Built with Kotlin, Jetpack Compose, and Material3.

## Build & Test Commands

```sh
./gradlew assembleDebug          # debug build
./gradlew assembleRelease        # signed release (needs keystore.properties)
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs device/emulator)
./gradlew lintDebug              # lint checks
```

Requires JDK 17 and Android SDK 35. Min SDK is 28.

## Architecture

Single-module Android app (`app/`) using MVVM with Jetpack Compose.

**Core components:**

- **MainViewModel** — single `UiState` data class exposed as `StateFlow`; drives all UI state.
- **MainActivity** — Compose UI with Scaffold + TopAppBar structure.
- **KexploreApp** — Application subclass.

**Package:** `dev.nutting.kexplore`

## Key Patterns

- State flows through a single `UiState` data class in the ViewModel
- No DI framework; keep things simple until complexity warrants it

## Release Pipeline

Uses semantic-release on the `main` branch with Conventional Commits. CI (GitHub Actions) builds a signed APK and creates a GitHub release. Version codes follow `MAJOR*10000 + MINOR*100 + PATCH` (managed by `scripts/set-version.sh`).
