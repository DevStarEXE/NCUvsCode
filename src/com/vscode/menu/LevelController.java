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
        String selected = clickedBtn.getText(); // 這裡會抓到 "FOR LOOP"

        System.out.println("準備進入關卡：" + selected);
        startGame(selected, event);
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
    private void startGame(String levelType, ActionEvent event) {
        // 1. 取得視窗
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        // 2. 設定一個「全域旗標」，讓遊戲主體知道要生出哪隻王
        // 我們假設在 GameManager 裡新增一個 static 變數
        com.vscode.danmaku.core.GameManager.selectedLevel = levelType;

        try {
            // 切換至遊戲主程式
            com.vscode.danmaku.Main gameApp = new com.vscode.danmaku.Main();
            gameApp.start(stage);
        } catch (Exception e) {
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