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
    private void initialize() {
        // 初始化 GameManager
        gameManager = new GameManager(gameCanvas);
        System.out.println("DEBUG: Game Controller Initialized.");

        // 在 FXML 介面加載完成後，確保鍵盤事件被接聽
        Platform.runLater(() -> {
            rootPane.getScene().setOnKeyPressed(this::handleKeyPressed);
            rootPane.getScene().setOnKeyReleased(this::handleKeyReleased);
            rootPane.requestFocus(); // 聚焦 root 以獲得按鍵
        });

        // 開始遊戲循環
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