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

- **MainViewModel** — app-level connection state exposed as `StateFlow<UiState>`; owns `KubernetesRepository` and `MetricsRepository` as `StateFlow` properties. Runs anomaly polling via `AnomalyChecker`.
- **ResourceListViewModel** — fetches and auto-refreshes resource lists; supports cache-first loading via `ResourceCache`.
- **ResourceDetailViewModel** — fetches a single resource, derives detail/YAML/dependencies views. Supports mutation actions (delete, scale, restart, trigger, cordon).
- **PodExecViewModel** — manages a pod exec session lifecycle (connect, send, close).
- **PodLogsViewModel** — streams pod logs with channel-based batching, search/highlighting, and multi-container multiplex mode.
- **EventStreamViewModel** — watches cluster events in real-time with channel-based batching and type/search filtering.
- **HealthDashboardViewModel** — aggregates cluster health (failed pods, unhealthy deployments/nodes, warning events).
- **ConnectionViewModel** — manages saved connections via `ConnectionStore`; supports manual entry, kubeconfig import, and QR code import.
- **QrScanViewModel** — manages camera permission and QR code decode state for kubeconfig scanning.
- **KexploreApp** — Application subclass; hosts shared `ConnectionStore` and `ResourceCache` singletons. Schedules widget refresh via WorkManager.
- **MainActivity** — Compose UI with Scaffold + TopAppBar structure.

**Data layer:**

- **KubernetesRepository** — wraps fabric8 client for all K8s API operations (list, get, delete, scale, restart, exec, logs, watch).
- **MetricsRepository** — fetches pod/node metrics from the Metrics API.
- **MetricsCollector** — polling-based metrics collection with ring buffer for chart display.
- **AnomalyChecker** — checks for failed pods, unhealthy deployments, and not-ready nodes for tab badges.
- **DependencyResolver** — resolves resource dependency trees (Deployment → ReplicaSet → Pod, Service → Ingress).
- **ResourceCache** — file-based disk cache for offline resource browsing using kotlinx-serialization.

**Widget:**

- **ClusterStatusWidget** — Glance AppWidget showing pod/node status summary on the home screen.
- **WidgetRefreshWorker** — WorkManager worker that fetches cluster status every 15 minutes.

**Package:** `dev.nutting.kexplore`

## Key Patterns

- `MainViewModel` exposes `repository` and `metricsRepository` as `StateFlow` so screens can `collectAsState()` reactively.
- Screen-level ViewModels (`ResourceListViewModel`, `ResourceDetailViewModel`, etc.) handle data fetching; composables only render state.
- `ConnectionStore` is a singleton on `KexploreApp`, shared by `MainViewModel` and `ConnectionViewModel`.
- No DI framework; keep things simple until complexity warrants it.

## Release Pipeline

Uses semantic-release on the `main` branch with Conventional Commits. CI (GitHub Actions) builds a signed APK and creates a GitHub release. Version codes follow `MAJOR*10000 + MINOR*100 + PATCH` (managed by `scripts/set-version.sh`).
