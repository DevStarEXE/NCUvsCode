package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Bullet extends GameObject {
    private static final double SPEED = 10.0;

    public enum Type {
        INT, CHAR, BOOL // 語法類型
    }

    private final Type type;

    public Bullet(double x, double y, Type type) {
        super(x, y, 10, 10);
        this.type = type;
    }

    @Override
    public void update(long now) {
        y -= SPEED; // 向下移動
        if (y + height < 0) { // 移出螢幕
            isAlive = false;
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFont(new Font("Monospaced", 10)); // 等寬字型

        switch (type) {
            case INT:
                gc.setFill(Color.web("#B5CEA8")); // Keyword (淺綠)
                gc.fillText("int", x, y + height);
                break;
            case CHAR:
                gc.setFill(Color.web("#CE9178")); // String (淺橘)
                gc.fillText("char", x, y + height);
                break;
            case BOOL:
                gc.setFill(Color.web("#DCDCAA")); // Function (淺黃)
                gc.fillText("bool", x, y + height);
                break;
        }
    }
}