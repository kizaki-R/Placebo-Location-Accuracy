<picture>
  <source media="(prefers-color-scheme: dark)" srcset="images/readme-banner-dark-en-1280x400.png">
  <img alt="Placebo Location Accuracy" src="images/readme-banner-light-en-1280x400.png">
</picture>

# Placebo Location Accuracy

English | [繁體中文](README.zh-TW.md)

Some Android ROMs and Google Play services builds keep flipping **Google Location Accuracy** back on after you disable it—even when system Location is turned off. Fighting the toggle directly breaks Settings or triggers silent resets.

Placebo Location Accuracy takes the opposite route. Leave the system **Google Location Accuracy** toggle on; this LSPosed module neutralizes the network-location stack underneath so the switch does nothing. When you turn on Location, apps get pure GPS (GNSS) satellite fixes only. When you turn off Location, GMS privilege bypass flags (`locationSettingsIgnored`) are stripped.

## Requirements

- Android 9–16+ (API 28–36+, primarily targeting Android 13, 14, 15, and 16)
- Rooted device with a Modern Xposed framework supporting **libxposed API 101+** (tested on LSPosed 2.2.0 / API 102)

## What It Intercepts

Each toggle in the app operates independently and syncs immediately via `RemotePreferences` without rebooting:

| Toggle | Default | Behavior |
|---|---|---|
| **Neutralize Network Location (Pure GPS Only)** | On | Drops `network` provider fixes and requests in `system_server` (`LocationProviderManager`, `LocationManagerService`) and inside GMS `LocationManager`. Leaves `gps` and mock location providers untouched. |
| **Block GMS Wi-Fi & RTT Scans** | On | Returns empty scan lists to GMS from `WifiServiceImpl` (including Android 11–16 APEX `service-wifi.jar`) and GMS-side `WifiManager`, `WifiScanner`, and `WifiRttManager`. Normal Wi-Fi connectivity is unaffected. |
| **Block GMS Cell Tower Info** | On | Returns empty cell tower lists to GMS from `PhoneInterfaceManager` and GMS-side `TelephonyManager` (`getAllCellInfo`, `getCellLocation`, `requestCellInfoUpdate`). Calls, SMS, and mobile data are unaffected. |
| **Block GMS BLE Background Scans** | On | Drops BLE scans started by GMS in `com.android.bluetooth` (`GattService`, `ScanController`, `TransitionalScanHelper`) and GMS-side `BluetoothLeScanner`. Paired Bluetooth devices are unaffected. |
| **Block Location-Off Privilege Bypass** | On | Clears `mLocationSettingsIgnored` on GMS `LocationRequest` and `LastLocationRequest` objects in `system_server`. |
| **Verbose Debug Logging** | Off | Logs intercepted calls under tag `PlaceboLA` for verification with `adb logcat`. |

If you only want to block one source (for example, cell tower triangulation while keeping Wi-Fi positioning), turn off **Neutralize Network Location** and enable only the specific scan blocker you want.

## Installation

1. Install the APK.
2. Open your LSPosed Manager and enable **Placebo Location Accuracy**.
3. Check the module scope:
   - **Required**: System Framework (`system`), Google Play services (`com.google.android.gms`)
   - **Recommended**: Google Services Framework (`com.google.android.gsf`), Phone Services (`com.android.phone`), Bluetooth (`com.android.bluetooth`)
4. Reboot (or soft-reboot `system_server`) once so hooks load into `system_server`.
5. Open **Placebo Location Accuracy**. The status card connects through `XposedService` and displays the active framework name and API version.

## Verification

1. Enable **Verbose Debug Logging** inside the app.
2. Watch logs over ADB:
   ```bash
   adb logcat -s PlaceboLA
   ```
3. Keep **Google Location Accuracy** on and open a map app indoors with no GPS sky view:
   - Network-only approximate fixes should stop resolving (`dumpsys location` keeps `network provider: last location=null`).
   - Moving outdoors should lock normally via pure GPS satellites.
   - Note: if you temporarily turn all module toggles off and let an app obtain a network fix, GMS and `fused` provider retain that cached `last location` in memory for a few minutes even after you re-enable the toggles. Force-stop GMS and the map app to clear stale cache immediately.
4. Turn off **Verbose Debug Logging** when finished.

## Building From Source

Requires JDK 17+ and Android SDK 36.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## License

Licensed under the [Apache License 2.0](LICENSE).
Derivative works and forks must retain the attribution statement in [NOTICE](NOTICE) in accordance with Section 4(d) of the license.
