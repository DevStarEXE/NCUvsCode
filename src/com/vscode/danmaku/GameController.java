package com.vscode.danmaku;

import com.vscode.danmaku.core.GameManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

public class GameController {

    @FXML
    private Canvas gameCanvas; // 引用 FXML 裡的 Canvas

    @FXML
    private Pane rootPane; // 引用 FXML 的根物件，用於鍵盤接聽

    private GameManager gameManager;

    @FXML
    public void initialize() {
        gameManager = new GameManager(gameCanvas);

        // --- 修正：等待 Canvas 真正被加進 Scene 之後，才綁定鍵盤事件 ---
        gameCanvas.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                // 當 newScene 不為空時，代表畫面已經準備好了，這時綁定才安全！
                newScene.setOnKeyPressed(event -> gameManager.handleKeyPressed(event));
                newScene.setOnKeyReleased(event -> gameManager.handleKeyReleased(event));
            }
        });

        // 啟動遊戲迴圈
        gameManager.start();
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        gameManager.handleKeyPressed(event);
    }

    @FXML
    private void handleKeyReleased(KeyEvent event) {
        gameManager.handleKeyReleased(event);
    }
}