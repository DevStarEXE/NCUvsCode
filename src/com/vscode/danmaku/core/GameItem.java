package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameItem {
    public double x, y;
    public double width = 24, height = 24;
    public int type; // 1: 金色(雙排), 2: 橘紅色(三排)
    private final Color color;

    public GameItem(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.color = (type == 1) ? Color.web("#FFD700") : Color.web("#FF4500");
    }

    public void draw(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillRect(x, y, width, height);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, width, height);
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        gc.fillText("P", x + 7, y + 17);
    }

    public boolean collidesWith(Player p) {
        return p.x < x + width && p.x + p.width > x &&
                p.y < y + height && p.y + p.height > y;
    }
}