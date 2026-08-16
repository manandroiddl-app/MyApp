# 📋 Android App 開發協作 SOP (Standard Operating Procedure)

## 🚨 核心鐵律 (Core Constraints)

### 1. 單一頁面限制 (Single Screen Constraint)
* **每次只聚焦一個頁面的 Change**：絕對不跨頁面、不跨多個獨立功能模組同時進行修改。每一次任務只針對當前討論的單一 Screen 及與其直接相關的專屬檔案進行增量更新，確保變更範圍可控、易於測試與除錯。

### 2. 完整輸出原則 (Full Code Output)
* **每次修改皆提供 Full Code**：當需要更新任何檔案（如 `Screen.kt`、`Repository.kt`、`Models.kt` 等）時，必須**完整輸出該檔案的全部程式碼（包含所有 Import 與既有邏輯）**，絕對不使用省略號（`...`）或部分片段，方便直接複製並全選覆蓋 GitHub / Android Studio 中的檔案。

---

## 🔍 開發前準備：真實數據查證 (Payload & Schema Verification)

1. **先查證 API 原始 Payload，再編寫程式碼**：在針對任何 API 進行功能增量或修復前，必須先**精確對齊並查證官方 API 的實時 / 真實 JSON Payload 結構與欄位名稱**。
2. **Key 值與資料型態雙重比對**：嚴格確認 JSON 的層級構造（嵌套物件 vs 陣列）、Key 的大小寫與命名習慣（例如 `forecastWind`、`forecastMinrh`），絕不憑印象或假設編寫解析邏輯，確保數據抓取百分之百準確。

---

## 🛡️ 開發三大原則

### 一、 增量開發原則 (On Top Preservation)
1. **預設保留現有架構**：除非明確指示「重構」或「替換」，否則所有功能變更一律以 **On Top（在現有基礎上增量）** 模式進行，完整保留現有功能與邏輯。
2. **嚴禁憑空推理**：若對當前的 Source Code、API 契約、Data Class 欄位或 UI 邏輯有任何不確定或模糊之處，**必須第一時間主動要求提供最新的原始檔案**，絕不自行揣測或憑空產生。

### 二、 影響分析 (Impact Analysis)
1. **變更前掃描**：每次修改前，先評估新需求對相關檔案（Model 欄位、API Method 名稱、ViewModel 狀態、Hilt 依賴注入等）會造成甚麼連帶影響。
2. **介面雙向對齊**：確保新程式碼與現有的其他模組（例如 `MainActivity`、其他 Screen、Repository 介面）**100% 雙向對齊**，防止修改 A 卻破壞 B 的情況發生。

### 三、 雙重自我審查 (Self-Review Checklist)
在輸出任何程式碼給使用者之前，必須強制進行內部 Check：
* [ ] **單一頁面合規**：確認本次變更是否僅限於一個頁面與其專屬元件。
* [ ] **Full Code 完整性**：確認產出的程式碼為完整可執行的 Full Code，沒有任何省略號或漏掉的邏輯/Import。
* [ ] **Payload 結構對齊**：確認 Repository 解析的 JSON Key 與官方 API 的 Real Payload 完全吻合。
* [ ] **語法與類型對齊**：確認 Retrofit 介面回傳型態、Repository 解析邏輯與 ViewModel/UI State 的資料型態完全一致。
* [ ] **Compose 規範檢查**：確認沒有缺少關鍵的 Compose Runtime Imports（如 `getValue`、`setValue`、`collectAsState` 等）及圖片渲染元件（如 Coil `AsyncImage`）。

📌 額外補充規則 (使用者要求)
顯示完整檔案路徑：每次輸出 Full Code 時，必須在頂部明確標示檔案的完整相對路徑（如 app/src/main/java/com/example/lifeapp/...）。

基準版本對齊：目前所有修改皆以用戶提供為基礎。


🚨 程式碼版本確認機制 (Version Integrity Verification)
疑慮即詢問：只要我對當前的檔案內容、版本進度或程式碼結構有任何不確定、模糊或缺乏完整上下文時，我絕對不會自行揣測、憑空推斷或使用舊版/簡化版。

主動請求最新 Source Code：我會第一時間主動向你要求分享最新版本的原始碼（例如 Zip 檔或相關檔案的 Full Code），確保每次改動都是 100% 精準建立在 真正最新的版本 On Top 上。

避免 Version Divergence (版本分支脫節)：防止因為版本錯配而產生「覆蓋了已完成功能」或「需要花大量時間改 Bug」的情況。
