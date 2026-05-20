package com;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class MenuController {

    @FXML
    private Button btnStart;

    @FXML
    private Button btnSetting;

    @FXML
    private Button btnExit;

    private MediaPlayer menuMediaPlayer;

    @FXML
    public void initialize() {
        // ==========================================
        // 1. 初始化背景音樂 (MenuMusic.mp3)
        // ==========================================
        try {
            URL musicResource = getClass().getResource("/resource/fxml/audio/MenuMusic.mp3");
            if (musicResource != null) {
                Media menuMusic = new Media(musicResource.toExternalForm());
                menuMediaPlayer = new MediaPlayer(menuMusic);

                // 設定背景音樂無限循環播放
                menuMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);

                // 將音樂播放器音量與全域 SoundManager 的 Property 進行雙向/單向動態綁定
                // JavaFX MediaPlayer 接受 0.0 ~ 1.0 數值，故 Slider 的 0.0 ~ 100.0 需除以 100
                menuMediaPlayer.volumeProperty().bind(
                        SoundManager.getInstance().musicVolumeProperty().divide(100.0)
                );

                // 播放音樂
                menuMediaPlayer.play();
            } else {
                System.out.println("警告：找不到 /resource/fxml/audio/MenuMusic.mp3 音訊檔案！");
            }
        } catch (Exception e) {
            System.out.println("背景音樂載入失敗：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Start 按鈕點擊事件：進入 Level 關卡選擇頁面
     */
    @FXML
    void handleStartGame(ActionEvent event) {
        // 離開主選單前往遊戲關卡時，停止主選單背景音樂
        if (menuMediaPlayer != null) {
            menuMediaPlayer.stop();
        }
        // 切換至 LevelControl.fxml (建議命名為 level-view.fxml)
        switchScene(event, "/resource/fxml/level-view.fxml", "選擇關卡 - Level");
    }

    /**
     * Setting 按鈕點擊事件：進入設定頁面
     */
    @FXML
    void handleGoToSetting(ActionEvent event) {
        // 前往設定頁面時不關閉音樂，讓使用者調整 Slider 時能即時聽到音量反饋
        switchScene(event, "/resource/fxml/setting-view.fxml", "遊戲設定");
    }

    /**
     * Exit 按鈕點擊事件：安全退出遊戲系統
     */
    @FXML
    void handleExitGame(ActionEvent event) {
        // 釋放媒體播放器資源
        if (menuMediaPlayer != null) {
            menuMediaPlayer.dispose();
        }
        System.out.println("系統安全關閉");
        // 關閉整個 JavaFX 應用程式平台
        Platform.exit();
        System.exit(0);
    }

    /**
     * 共用核心場景切換方法（嚴格遵循 /fxml/ 絕對路徑架構）
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 載入新場景（預設視窗大小為 800x600）
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            System.out.println("場景切換失敗，無法解析 FXML 路徑: " + fxmlPath);
            e.printStackTrace();
        }
    }
}