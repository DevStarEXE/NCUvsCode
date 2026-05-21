# NCUvsCode - 彈幕射擊遊戲
NCUvsCode 是一款結合 IDE 視覺元素與硬核彈幕射擊體驗的遊戲。

玩家將扮演編碼者，在充滿程式碼風格的虛擬空間中挑戰各種由「程式邏輯」組成的 BOSS。

## 背景故事

一切都是從踏入資工系開始…

在修計算機實習的時候發現有一堆看不懂的東西

這是一個一失敗就會結束的遊戲（指被當）

> ——「這雖然是遊戲，但可不是鬧著玩的」某著名封弊者

## 🎮 遊戲特色
### 程式設計主題 BOSS：

挑戰 ForLoopBoss 與 LinkedListBoss等充滿惡意的BOSS，體驗將程式邏輯視覺化為彈幕的創新玩法。

### 動態戰鬥系統：

武器系統：拾取場景中的代碼晶片 (GameItem) 以切換射擊模式。

充能大招：透過走位累積充能，釋放 BOMB.EXE 清理全螢幕彈幕。

IDE 沉浸感：使用 Monospaced 等寬字型與 VS Code 風格配色，打造極致的開發者氛圍。

## 🛠 技術堆疊

語言：Java 17+

框架：JavaFX

架構：MVC (Model-View-Controller)

核心邏輯：基於 AnimationTimer 的遊戲循環與 AABB 碰撞偵測

## 📂 專案結構

```
src/com/vscode/danmaku/
├── core/          # 遊戲引擎核心 (Player, Bullet, GameManager)
├── bosses/        # BOSS 行為邏輯 (ForLoop, Linked List)
├── menu/          # 選單與介面控制 (Menu, Settings, Levels)
└── resource/      # FXML 介面檔案與圖片音效資源
```

## 🚀 如何開始

### 環境需求：
請確保已安裝 JDK 17 或以上版本，並配置好 JavaFX SDK。

### 建置專案：
使用 IntelliJ IDEA 開啟專案。

確保 lib 目錄已包含正確的 JavaFX 函式庫。

執行 com.vscode.danmaku.menu.Main 來啟動遊戲。

## 🕹 操作說明

移動：WASD/方向鍵 (UP, DOWN, LEFT, RIGHT)

自動射擊切換：空白鍵 (SPACE)

釋放 BOMB.EXE：C 鍵 (限充能滿時)

退出/返回：ESC 鍵
