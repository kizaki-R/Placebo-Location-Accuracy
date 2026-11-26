![Placebo Location Accuracy](images/readme-banner-dark-zh-TW-1280x400.png)

# Placebo Location Accuracy

[English](README.md) | 繁體中文

部分 Android 系統或 Google Play 服務版本會強制把「Google 定位精確度」開關重新打開，就算關掉總定位也一樣。直接硬改系統設定容易導致設定頁異常，或是被背景服務反覆寫回。

Placebo Location Accuracy 採反方向處理：讓系統裡的「Google 定位精確度」維持開啟，不跟系統對抗，但在底層把網路定位整條鏈路架空。打開系統定位時，App 只拿得到純 GPS（GNSS 衛星）座標；關掉系統定位時，會清掉 GMS 的特權繞過旗標（`locationSettingsIgnored`），徹底停止背景供位。

## 環境需求

- Android 9–16+（API 28–36+，主要支援 Android 13、14、15 與 16）
- 已 Root，且安裝支援 **libxposed API 101+** 的 Modern Xposed 框架（實測環境：LSPosed 2.2.0 / API 102）

## 攔截項目與開關說明

App 內每個開關都獨立運作，透過 `RemotePreferences` 即時同步，切換開關不必重開機：

| 開關項目 | 預設 | 實際作用 |
|---|---|---|
| **架空網路定位（僅保留純 GPS）** | 開啟 | 在 `system_server`（`LocationProviderManager`、`LocationManagerService`）與 GMS 內的 `LocationManager` 丟棄 `network` provider 的定位回報與請求。純 GPS 衛星與模擬定位不受影響。 |
| **封鎖 GMS Wi-Fi 與 RTT 掃描** | 開啟 | 在 `WifiServiceImpl`（含 Android 11–16 APEX `service-wifi.jar`）與 GMS 內的 `WifiManager`、`WifiScanner`、`WifiRttManager` 回傳空掃描清單並丟棄測距。正常 Wi-Fi 連線不受影響。 |
| **封鎖 GMS 基地台資訊** | 開啟 | 在 `PhoneInterfaceManager` 與 GMS 內的 `TelephonyManager`（`getAllCellInfo`、`getCellLocation`、`requestCellInfoUpdate`）回傳空清單。通話、簡訊與行動數據不受影響。 |
| **封鎖 GMS 藍牙 BLE 背景掃描** | 開啟 | 在 `com.android.bluetooth`（`GattService`、`ScanController`、`TransitionalScanHelper`）與 GMS 內的 `BluetoothLeScanner` 丟棄 GMS 發起的 BLE 掃描。已配對的藍牙裝置不受影響。 |
| **封鎖關閉定位下特殊存取** | 開啟 | 清除 `system_server` 中 GMS `LocationRequest` 與 `LastLocationRequest` 的 `mLocationSettingsIgnored` 旗標。 |
| **詳細除錯日誌** | 關閉 | 以標籤 `PlaceboLA` 輸出攔截紀錄，方便透過 `adb logcat` 驗證。 |

若只想擋單一來源（例如只屏蔽電信基地台定位、保留 Wi-Fi 定位），把「架空網路定位（僅保留純 GPS）」關掉，只開「封鎖 GMS 基地台資訊」即可。

## 安裝方式

1. 安裝 APK。
2. 開啟 LSPosed Manager，啟用 **Placebo Location Accuracy**。
3. 勾選作用範圍：
   - **必選**：系統框架（`system`）、Google Play 服務（`com.google.android.gms`）
   - **建議強化**：Google 服務架構（`com.google.android.gsf`）、電話服務（`com.android.phone`）、藍牙（`com.android.bluetooth`）
4. 重新開機（或軟重啟 `system_server`）一次，讓模組載入 `system_server`。
5. 開啟 **Placebo Location Accuracy**，頂部狀態卡會透過 `XposedService` 動態連線並顯示框架名稱與 API 版本。

## 驗證方式

1. 在 App 內打開「詳細除錯日誌」。
2. 透過 ADB 查看即時攔截：
   ```bash
   adb logcat -s PlaceboLA
   ```
3. 保持「Google 定位精確度」開啟，在收不到衛星訊號的室內開啟地圖 App：
   - 不會再透過 Wi-Fi 或基地台秒出近似位置（`dumpsys location` 中 `network provider` 的 `last location` 維持 `null`）。
   - 走到戶外或窗邊時，純 GPS 衛星可正常鎖定定位。
   - 注意：若中途曾把全部開關關閉並讓地圖完成一次網路定位，GMS 與 `fused` provider 會在記憶體保留幾分鐘的舊快取（`last location`），重開保護後不會再更新座標，但舊藍點可能短暫殘留。強制停止 GMS 與地圖 App 即可立即清掉快取。
4. 驗證完畢後關閉「詳細除錯日誌」。

## 從原始碼建置

需要 JDK 17+ 與 Android SDK 36。

```bash
./gradlew testDebugUnitTest assembleDebug
```

產出的 APK 位於 `app/build/outputs/apk/debug/app-debug.apk`。

## 授權條款

本專案採用 [Apache License 2.0](LICENSE) 授權。
依據授權條款第 4(d) 條，任何衍生專案與 Fork 必須保留 [NOTICE](NOTICE) 中的原始設計與架構歸屬聲明。
