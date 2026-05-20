package com.vscode.danmaku;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 專案編譯/運行指南:
 *
 * 1. 確保已正確設定 JavaFX SDK 路徑 (--module-path) 和模組 (--add-modules).
 * 2. 如果使用 IntelliJ, 建議建立一個 Application 運行配置.
 * 3. VM 選項參考 (請將 PATH_TO_FX 替換為你的實際 JavaFX lib 路徑):
 * --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GameInterface.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm()); // 加載 CSS

        primaryStage.setTitle("Microsoft VS Code: Danmaku Debugger");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}