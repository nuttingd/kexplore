<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" alt="Kexplore icon" />
</p>

# Kexplore

A Kubernetes cluster explorer for Android. Browse resources, stream logs, exec into pods, forward ports, and monitor cluster health — all from your phone.

## Features

- **Resource browsing** — Workloads, Networking, Config, Storage, Cluster-scoped resources, and Custom Resources (CRDs), with search, status filtering, and label filtering
- **Resource actions** — Delete, scale, restart, cordon/uncordon nodes, trigger CronJobs
- **Pod logs** — Real-time streaming with search/regex highlighting and multi-container multiplexing
- **Pod exec** — Interactive terminal sessions with command history
- **Port forwarding** — Forward Services and Pods with session management and open-in-browser
- **Cluster health dashboard** — Failed pods, unhealthy deployments, not-ready nodes, warning events
- **Real-time event stream** — Watch cluster events with type and search filtering
- **Metrics charts** — CPU and memory graphs for pods and nodes with configurable poll interval
- **Notifications** — Background monitoring with push alerts for cluster anomalies
- **Home screen widget** — Pod and node status at a glance
- **Connection management** — Manual entry, kubeconfig file import, or QR code scanning
- **Offline cache** — Browse previously loaded resources without a connection

## Building

Requires JDK 17+ and Android SDK 35. Min SDK is 28 (Android 9).

```sh
./gradlew assembleDebug
```

For a signed release build, copy `keystore.properties.example` to `keystore.properties` and fill in your keystore details, then:

```sh
./gradlew assembleRelease
```

## Testing

```sh
./gradlew test
```

## License

[MIT](LICENSE)
