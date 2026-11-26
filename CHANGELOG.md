# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project uses [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-11-26

### Added
- Modern Xposed API (libxposed API 101–102) module entry (`XposedModule`) with `XposedService` live status detection and `RemotePreferences` live sync.
- `system_server` hooks (`LocationProviderManager`, `LocationManagerService`) to drop `network` provider fixes and requests while preserving pure `gps` and mock location providers.
- Location-off bypass blocker that strips `mLocationSettingsIgnored` from GMS `LocationRequest` and `LastLocationRequest` objects.
- `system_server` APEX Wi-Fi hook (`WifiServiceImpl` via `SystemServiceManager` classloader interception) returning empty Wi-Fi scan results to GMS on Android 11–16+.
- GMS / GSF client-side hooks on stable Android Framework APIs (`WifiManager`, `WifiScanner`, `WifiRttManager`, `TelephonyManager`, `BluetoothLeScanner`, `LocationManager`).
- Optional system service hardening hooks for `com.android.phone` (`PhoneInterfaceManager`) and `com.android.bluetooth` (`GattService`, `ScanController`, `TransitionalScanHelper`).
- Material 3 control UI with independent toggles for each interception layer and optional `PlaceboLA` debug logging (English and Traditional Chinese).
