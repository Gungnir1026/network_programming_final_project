# 放到 GitHub 展示

建議儲存庫名稱：`eclassroom-java`。

建議 About description：

> Java Swing 與 TCP Socket 實作的即時互動電子教室，提供共享白板、畫筆授權、聊天室、舉手、公告、抽籤與隨堂測驗。

建議 Topics：`java`、`swing`、`tcp`、`socket-programming`、`whiteboard`、`education`、`course-project`。

## 使用整理好的上傳包

`dist/eclassroom-github-ready.zip` 只收錄原始碼、資源、測試、腳本、文件與 CI 設定。解壓縮後，將 `eclassroom/` 裡面的內容作為儲存庫根目錄；包含 `.gitignore`、`.gitattributes` 和 `.github/`。

原始 `ip/`、建置暫存與 `.class` 不在包內。JAR 位於本機 `dist/eclassroom.jar`，可另外附在 GitHub Release，方便下載者直接執行。若要重新產生上傳包，執行 `python3 scripts/package.py`，此封裝步驟另外需要 Python 3；建置與執行 Java 專案不需要 Python。

## 使用本機 Git

目前目錄已整理為儲存庫內容，但尚未建立遠端或推送。若從本目錄開始一個新的儲存庫，可依序執行：

```sh
git init -b main
git add README.md CHANGELOG.md .gitignore .gitattributes .github src scripts docs
git status
git commit -m "Organize E-Classroom networking course project"
```

然後到你的 GitHub 新建空儲存庫，依該儲存庫頁面提供的網址設定 `origin` 並推送。若使用既有儲存庫，先在該儲存庫的工作目錄加入這份整理版內容，保留既有歷史。

## 作品集介紹文字

> E-Classroom 是網路程式設計期末專題，以 Java Swing 建立老師與學生桌面介面，透過 TCP Socket 同步白板筆畫與課堂事件。功能涵蓋畫筆授權、中文聊天室、舉手、公告、抽籤及隨堂測驗；以多執行緒處理連線，並使用操作堆疊支援白板復原與重做。

README 已內嵌實際程式畫面、架構圖及執行方式。原始課程版本與後續整理修正的界線見 [版本判定](VERSION_AUDIT.md) 與 [整理紀錄](../CHANGELOG.md)。
