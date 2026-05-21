package com.vscode.menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import java.io.IOException;

public class SettingController {

    @FXML
    private Slider sliderMusic;

    @FXML
    private Slider sliderSound;
    
    @FXML
    private ComboBox<String> comboDifficulty;

    @FXML
    private Button btnBack;

    @FXML
    public void initialize() {
        // 取得音量管理單例並進行雙向綁定
        SoundManager soundManager = SoundManager.getInstance();
        sliderMusic.valueProperty().bindBidirectional(soundManager.musicVolumeProperty());
        sliderSound.valueProperty().bindBidirectional(soundManager.soundVolumeProperty());
        
        comboDifficulty.getItems().addAll("EASY (0.5x HP)", "NORMAL (1x HP)", "HARD (2x HP)");
        if (com.vscode.danmaku.core.GameManager.difficultyMultiplier == 0.5) {
            comboDifficulty.getSelectionModel().select(0);
        } else if (com.vscode.danmaku.core.GameManager.difficultyMultiplier == 2.0) {
            comboDifficulty.getSelectionModel().select(2);
        } else {
            comboDifficulty.getSelectionModel().select(1);
        }
        
        comboDifficulty.setOnAction(e -> {
            int index = comboDifficulty.getSelectionModel().getSelectedIndex();
            if (index == 0) com.vscode.danmaku.core.GameManager.difficultyMultiplier = 0.5;
            else if (index == 2) com.vscode.danmaku.core.GameManager.difficultyMultiplier = 2.0;
            else com.vscode.danmaku.core.GameManager.difficultyMultiplier = 1.0;
            System.out.println("難度已變更為: " + com.vscode.danmaku.core.GameManager.difficultyMultiplier);
        });
    }

    @FXML
    void handleBackToMenu(ActionEvent event) {
        try {
            // 切換回 Menu 頁面 (請根據你實際的 menu fxml 路徑調整)
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/resource/fxml/menu-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            stage.setScene(scene);
            stage.setTitle("主選單");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}