package com.vscode.menu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // ================================================================
            // 嚴格遵循你的目錄規範：
            // 開頭的 "/" 代表 resources 根目錄，精確定位到 fxml 資料夾下的 menu-view.fxml
            // ================================================================
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/resource/fxml/menu-view.fxml"));

            // 建立主選單場景，並設定寬高為 800x600（與你的 FXML 設計尺寸一致）
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);

            // 設定視窗標題
            stage.setTitle("遊戲主選單");

            // 將場景置入舞台並展現視窗
            stage.setScene(scene);
            stage.setResizable(false); // 固定視窗大小，避免背景圖片因拉伸而失真（期末報告加分細節）
            stage.show();

            System.out.println("JavaFX 主程式啟動成功，已載入主選單頁面。");

        } catch (IOException e) {
            System.out.println("錯誤：無法載入 /fxml/menu-view.fxml，請檢查檔案名稱與路徑是否正確！");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("程式啟動時發生未知錯誤：");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 啟動 JavaFX 應用程式生命週期
        launch(args);
    }
}
