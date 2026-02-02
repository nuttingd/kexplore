# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kexplore is an Android app for exploring Kubernetes cluster resources. Built with Kotlin, Jetpack Compose, and Material3.

## Build & Test Commands

```sh
./gradlew assembleDebug          # debug build
./gradlew assembleRelease        # signed release (needs keystore.properties)
./gradlew test                   # unit tests
./gradlew lintDebug              # lint checks
```

Requires JDK 17 and Android SDK 35. Min SDK is 28.

## Architecture

Single-module Android app (`app/`) using MVVM with Jetpack Compose.

**Core components:**

- **MainViewModel** — app-level connection state exposed as `StateFlow<UiState>`; owns `KubernetesRepository` and `MetricsRepository` as `StateFlow` properties.
- **ResourceListViewModel** — fetches and auto-refreshes resource lists for the current resource type.
- **ResourceDetailViewModel** — fetches a single resource once, derives both detail and YAML views from the same object.
- **PodExecViewModel** — manages a pod exec session lifecycle (connect, send, close).
- **PodLogsViewModel** — streams pod logs with channel-based batching and a max-lines cap.
- **ConnectionViewModel** — manages saved connections via `ConnectionStore`.
- **KexploreApp** — Application subclass; hosts the shared `ConnectionStore` singleton.
- **MainActivity** — Compose UI with Scaffold + TopAppBar structure.

**Package:** `dev.nutting.kexplore`

## Key Patterns

- `MainViewModel` exposes `repository` and `metricsRepository` as `StateFlow` so screens can `collectAsState()` reactively.
- Screen-level ViewModels (`ResourceListViewModel`, `ResourceDetailViewModel`, etc.) handle data fetching; composables only render state.
- `ConnectionStore` is a singleton on `KexploreApp`, shared by `MainViewModel` and `ConnectionViewModel`.
- No DI framework; keep things simple until complexity warrants it.

## Release Pipeline

Uses semantic-release on the `main` branch with Conventional Commits. CI (GitHub Actions) builds a signed APK and creates a GitHub release. Version codes follow `MAJOR*10000 + MINOR*100 + PATCH` (managed by `scripts/set-version.sh`).
