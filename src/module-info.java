module NCUvsCode {
    // 必須引用 JavaFX 相關模組
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;

    // 開放你的套件給 javafx.fxml 進行反射存取 (重要：否則無法載入 FXML)
    opens com.vscode.menu to javafx.fxml;
    opens com.vscode.danmaku to javafx.fxml;
    opens com.vscode.danmaku.core to javafx.fxml;

    // 匯出套件供外部使用
    exports com.vscode.menu;
    exports com.vscode.danmaku;
    exports com.vscode.danmaku.core;
}