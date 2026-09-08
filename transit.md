交通到站頁面, 台後的技術架構會 revamp 一下

而家頁面所有資料都係用 API endpoint 即時擷取


revamp 方向:

路線: 由 API endpoint 轉為 batch update 去 Room DB

車站: 由 API endpoint 轉為 batch update 去 Room DB

路線和相關車站: 由 API endpoint 轉為 batch update 去 Room DB

車站ETA: 仍然由API endpoint 提供



high level plan

Phase 1: 設計及建立 table 和 view

Phase 2: 建立 batch update load 去 room DB

Phase 3: 將 room DB 裡的路線, 車站等資料接入頁面 (i.e. switch from API endpoint to Room DB) 

======================================================
💡 技術細節提醒與潛在坑位 (Edge Cases)
首次啟動 (Cold Start) 體驗:

當使用者第一次安裝 App 開啟時，Room DB 還是空的。建議在 APK 內內建一份 Pre-packaged Database（createFromAsset()），讓 App 開箱即用，後續再由 Phase 2 的 Background Worker 進行版本更新。

多營運商/多路線變更 (e.g. 臨時改道):

某些臨時改道或特別班次可能未反映在批次 DB 中，需評估當 ETA API 回傳未知的 station_id 時的 UI Fallback 邏輯。
