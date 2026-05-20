package com.vscode.menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class LevelController {

    /**
     * 當玩家點擊一般關卡方格 (1, 2, 3, 4) 時觸發
     */
    @FXML
    void handleLevelSelect(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        String levelNumber = clickedBtn.getText();
        System.out.println("玩家選擇了關卡：" + levelNumber);

        // 把 event 一併傳進去
        startGame(levelNumber, event);
    }

    @FXML
    void handleBossSelect(ActionEvent event) {
        System.out.println("警告！準備載入 LinkedList 蠕蟲 BOSS 戰！");

        // 把 event 一併傳進去
        startGame("BOSS", event);
    }

    /**
     * 更新後的 startGame 方法：負責把選單視窗切換成遊戲視窗
     */
    private void startGame(String level, ActionEvent event) {
        // 1. 取得目前的視窗 (Stage)
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        // 2. 呼叫你的遊戲本體並接管視窗
        try {
            System.out.println("正在切換至遊戲畫面...");

            // ==========================================
            // 【關鍵點】這裡要替換成你原本遊戲主程式的呼叫方式
            // ==========================================

            // 寫法 A：如果你原本的遊戲寫在 com.vscode.danmaku.Main 裡面
            com.vscode.danmaku.Main gameApp = new com.vscode.danmaku.Main();
            gameApp.start(stage);

            /* // 寫法 B：如果你有獨立的 GameManager 類別負責產生 Scene
            GameManager gameManager = new GameManager();
            // 可以把 level 傳進去讓遊戲知道要生成哪隻王
            Scene gameScene = gameManager.createGameScene(level);
            stage.setScene(gameScene);
            */

        } catch (Exception e) {
            System.out.println("啟動遊戲時發生錯誤！");
            e.printStackTrace();
        }
    }

    /**
     * 返回主選單
     */
    @FXML
    void handleBackToMenu(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/resource/fxml/menu-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(fxmlLoader.load(), 800, 600));
            stage.setTitle("遊戲主選單");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}