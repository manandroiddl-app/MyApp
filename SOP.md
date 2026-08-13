# 📋 Android App 開發協作 SOP (Standard Operating Procedure)

## 🚨 核心鐵律：單一頁面限制 (Single Screen Constraint)
* **每次只聚焦一個頁面的 Change**：絕對不跨頁面、不跨多個獨立功能模組同時進行修改。每一次任務只針對當前討論的單一 Screen 及與其直接相關的專屬檔案進行增量更新，確保範圍可控、易於測試與除錯。

---

## 一、 增量開發原則 (On Top Preservation)
1. **預設保留現有架構**：除非明確指示「重構」或「替換」，否則所有功能變更一律以 **On Top（在現有基礎上增量）** 模式進行。
2. **嚴禁憑空推理**：若對當前的 Source Code、API 契約、Data Class 欄位或 UI 邏輯有任何不確定或模糊之處，**必須第一時間主動要求提供最新的原始檔案**，絕不自行揣測或憑空產生。

---

## 二、 影響分析 (Impact Analysis)
1. **變更前掃描**：每次修改前，先評估新需求對相關檔案（Model 欄位、API Method 名稱、ViewModel 狀態、Hilt 依賴注入等）會造成甚麼連帶影響。
2. **介面雙向對齊**：確保新程式碼與現有的其他模組（例如 `MainActivity`、其他 Screen、Repository 介面）**100% 雙向對齊**，防止修改 A 卻破壞 B 的情況發生。

---

## 三、 雙重自我審查 (Self-Review Checklist)
在輸出任何程式碼給使用者之前，必須強制進行以下 Check：
* [ ] **單一頁面合規**：確認本次變更是否僅限於一個頁面與其專屬元件。
* [ ] **完整性檢查**：確認原本有的 Import、ViewState 欄位、UI 區塊、邏輯分支沒有被無意間刪除或遺漏。
* [ ] **語法與類型對齊**：確認 Retrofit 介面回傳型態、Repository 解析邏輯與 ViewModel/UI State 的資料型態完全一致。
* [ ] **Compose 規範檢查**：確認沒有缺少關鍵的 Compose Runtime Imports（如 `getValue`、`setValue`、`collectAsState` 等）。
