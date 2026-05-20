package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class Player extends GameObject {
    private static final double SPEED = 5.0;
    private final Map<String, Boolean> keysPressed = new HashMap<>();
    private long lastShotTime = 0;
    private static final long SHOT_DELAY = 100_000_000; // 0.1s

    public Player(double x, double y) {
        super(x, y, 32, 32); // 玩家尺寸 32x32
    }

    @Override
    public void update(long now) {
        // 處理移動控制 (keysPressed 由 GameManager 更新)
        if (keysPressed.getOrDefault("UP", false)) y -= SPEED;
        if (keysPressed.getOrDefault("DOWN", false)) y += SPEED;
        if (keysPressed.getOrDefault("LEFT", false)) x -= SPEED;
        if (keysPressed.getOrDefault("RIGHT", false)) x += SPEED;

        // 邊界判定
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        // 注意：這裡省略了場景邊界，GameManager 會提供
    }

    @Override
    public void draw(GraphicsContext gc) {
        // 繪製玩家 (VS Code Ribbon Logo 風格)
        gc.setStroke(Color.web("#007ACC")); // VS Code Blue
        gc.setLineWidth(3);
        gc.strokeRect(x, y, width, height);
        // 畫一個交叉的絲帶形狀
        gc.strokeLine(x, y + height / 2, x + width, y + height / 2);
        gc.strokeLine(x + width / 2, y, x + width / 2, y + height);
    }

    public void shoot(long now, java.util.List<Bullet> bullets) {
        // 射擊邏輯 (按下 SPACE 時由 GameManager 呼叫)
        if (now - lastShotTime > SHOT_DELAY) {
            // 發射 "INT" 子彈
            bullets.add(new Bullet(x + width / 2, y, Bullet.Type.INT));
            lastShotTime = now;
        }
    }

    // 更新按鍵狀態的方法
    public void setKeyPressed(String key, boolean pressed) {
        keysPressed.put(key, pressed);
    }
}