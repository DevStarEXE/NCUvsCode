package com;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import java.io.IOException;

public class SettingController {

    @FXML
    private Slider sliderMusic;

    @FXML
    private Slider sliderSound;

    @FXML
    private Button btnBack;

    @FXML
    public void initialize() {
        // 取得音量管理單例並進行雙向綁定
        SoundManager soundManager = SoundManager.getInstance();
        sliderMusic.valueProperty().bindBidirectional(soundManager.musicVolumeProperty());
        sliderSound.valueProperty().bindBidirectional(soundManager.soundVolumeProperty());
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