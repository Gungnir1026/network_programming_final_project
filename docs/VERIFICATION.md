# 驗證紀錄

驗證日期：2026-10-21（Asia/Taipei）。

## 本地環境與結果

| 項目 | 結果 |
| --- | --- |
| 系統／工具 | macOS，Temurin OpenJDK 26.0.1 |
| 建置目標 | `javac --release 17 -encoding UTF-8`；JAR 入口 `eclassroom.Main` |
| 核心測試 | `CoreTest`：42 項檢查通過 |
| TCP 整合測試 | `ServerIntegrationTest`：75 項檢查通過 |
| 桌面流程 | `PreviewCapture`：登入、白板、中文聊天、授權、三題作答與 100 分結果通過 |
| 原始資料 | 13 個版本分別以乾淨輸出目錄編譯，11 個通過、2 個失敗；Java 原始檔 SHA-256 已記錄 |

## 檢查範圍

核心測試涵蓋姓名與 Port 輸入、JAR 內圖示、正確／錯誤答案、重複提交、結束後提交及新測驗重置。白板測試以實際滑鼠事件驅動元件，比對本地與遠端渲染像素，檢查筆畫只傳送一次、完整筆畫復原、重做、點擊擦除、擦除復原、清空復原、遠端不改變本地工具，以及唯讀模式。

TCP 測試建立真的老師、兩位學生及尚未完成登入的 socket，檢查名單不含半連線者、重名與第二位老師拒絕、中文聊天回送、舉手路由、老師專用操作、授權／收回、筆畫與復原／重做／清空不回送發送者、錯誤白板指令、公告、發卷、換題、答案只送給老師、重複及非當前題提交、成績公布後關閉作答，以及老師離線與替代老師登入。

桌面流程透過正式 `ClientConnection` 連到本機伺服器，在 Swing 執行緒操作真實視窗與按鈕。老師畫板與學生畫板逐像素一致；學生從介面作答，老師計分再經伺服器轉送成績。README 圖片由這些視窗的 `printAll` 輸出，使用示範姓名，沒有合成操作結果。

## 自動化與驗證限制

執行 `sh scripts/test.sh`，或在 Windows 執行 `.\scripts\test.ps1`，可重跑核心與 TCP 測試；有圖形桌面的 macOS／Linux 可執行 `sh scripts/preview.sh` 重現桌面流程並更新圖片。

GitHub Actions 已設定 Ubuntu（Java 17／25）、macOS（Java 17）與 Windows（Java 17）。CI 尚未在遠端執行；Windows PowerShell 腳本與 Java 17 執行時亦尚未在本機直接測試。Java 17 相容性目前由 `--release 17` 編譯目標保證語法與標準 API 範圍，並由後續 CI 驗證執行行為。

CI 的 action 版本依據官方 [actions/checkout](https://github.com/actions/checkout) 與 [actions/setup-java](https://github.com/actions/setup-java) 文件。驗證使用本機回環 TCP；未測試實體多電腦區域網路、同時繪圖衝突或大量連線負載。
