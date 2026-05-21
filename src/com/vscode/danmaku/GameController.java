package com.vscode.danmaku; // 依照你的實際 Package

import com.vscode.danmaku.core.GameManager;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;

public class GameController {

    @FXML
    private Canvas gameCanvas;

    private GameManager gameManager;

    @FXML
    public void initialize() {
        gameManager = new GameManager(gameCanvas);

        // 安全綁定：等待 Canvas 被加入 Scene 之後再綁定鍵盤事件 (解決 NullPointerException)
        gameCanvas.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> gameManager.handleKeyPressed(event));
                newScene.setOnKeyReleased(event -> gameManager.handleKeyReleased(event));
            }
        });

        // 啟動遊戲核心迴圈
        gameManager.start();
    }
}