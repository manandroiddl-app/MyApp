AI 協作開發 SOP (Standard Operating Procedure)
🚨 核心鐵律 1：單一頁面限制 (Single Screen Constraint)
每次只聚焦一個頁面的 Change：絕對不跨頁面、不跨多個獨立功能模組同時進行修改。每一次任務只針對當前討論的單一 Screen 及與其直接相關的專屬檔案進行增量更新，確保範圍可控、易於測試與除錯。

🚨 核心鐵律 2：完整輸出原則 (Full Code Output)
每次修改皆提供 Full Code：當需要更新任何檔案（如 Screen.kt、Repository.kt、Models.kt 等）時，必須完整輸出該檔案的全部程式碼（包含所有 Import 與既有邏輯），絕對不使用省略號（...）或部分片段，方便你直接複製並全選覆蓋 GitHub / Android Studio 裡面的檔案。

一、 增量開發原則 (On Top Preservation)
預設保留現有架構：除非明確指示「重構」或「替換」，否則所有功能變更一律以 On Top（在現有基礎上增量） 模式進行，完整保留現有功能與邏輯。

嚴禁憑空推理：若對當前的 Source Code、API 契約、Data Class 欄位或 UI 邏輯有任何不確定或模糊之處，必須第一時間主動要求提供最新的原始檔案，絕不自行揣測或憑空產生。

二、 影響分析 (Impact Analysis)
變更前掃描：每次修改前，先評估新需求對相關檔案（Model 欄位、API Method 名稱、ViewModel 狀態、Hilt 依依賴注入等）會造成甚麼連帶影響。

介面雙向對齊：確保新程式碼與現有的其他模組（例如 MainActivity、其他 Screen、Repository 介面）100% 雙向對齊，防止修改 A 卻破壞 B 的情況發生。

三、 雙重自我審查 (Self-Review Checklist)
在輸出任何程式碼或方案給使用者之前，必須強制進行內部 Check：

[ ] 單一頁面合規：確認本次變更是否僅限於一個頁面與其專屬元件。

[ ] Full Code 完整性：確認產出的程式碼為完整可執行的 Full Code，沒有任何省略號或漏掉的邏輯/Import。

[ ] 語法與類型對齊：確認 Retrofit 介面回傳型態、Repository 解析邏輯與 ViewModel/UI State 的資料型態完全一致。

[ ] Compose 規範檢查：確認沒有缺少關鍵的 Compose Runtime Imports（如 getValue、setValue、collectAsState 等）。
