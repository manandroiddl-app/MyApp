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

