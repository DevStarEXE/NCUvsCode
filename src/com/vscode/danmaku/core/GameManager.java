package com.vscode.danmaku.core;

import com.vscode.danmaku.core.bosses.LinkedWorm.LinkedListBoss;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private final Canvas gameCanvas;
    private final GraphicsContext gc;
    private Player player;
    private LinkedListBoss boss;
    private final List<Bullet> playerBullets = new ArrayList<>();
    private AnimationTimer gameLoop;

    // 用於物件池優化 (Demo 省略，直接使用 List)
    // private final List<Bullet> bulletPool = new ArrayList<>();

    public GameManager(Canvas gameCanvas) {
        this.gameCanvas = gameCanvas;
        this.gc = gameCanvas.getGraphicsContext2D();
        init();
    }

    private void init() {
        // 初始化玩家 (底部中央)
        player = new Player(gameCanvas.getWidth() / 2 - 16, gameCanvas.getHeight() - 60);
        // 初始化 BOSS (頂部中央)
        boss = new LinkedListBoss(gameCanvas.getWidth() / 2, 50);

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                draw(gc);
            }
        };
    }

    public void start() { gameLoop.start(); }
    public void stop() { gameLoop.stop(); }

    private void update(long now) {
        // 1. 更新玩家
        player.update(now);
        // 按下 SPACE 時發射
        if (isSpacePressed()) {
            player.shoot(now, playerBullets);
        }

        // 2. 更新子彈
        for (Bullet b : playerBullets) {
            b.update(now);
        }

        // 3. 更新 BOSS (LinkedList移動邏輯在此更新)
        boss.update(now);

        // 4. 碰撞偵測 (玩家子彈 vs BOSS Head)
        for (Bullet b : playerBullets) {
            if (b.isAlive()) {
                boss.hit(b);
            }
        }

        // 5. 清理已銷毀物件 (非常重要)
        playerBullets.removeIf(b -> !b.isAlive());
    }

    private void draw(GraphicsContext gc) {
        // 1. 清空畫布 (Dark Mode 背景)
        gc.setFill(Color.web("#1E1E1E")); // VS Code Dark Theme
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // 2. 繪製 BOSS
        boss.draw(gc);

        // 3. 繪製子彈
        for (Bullet b : playerBullets) {
            b.draw(gc);
        }

        // 4. 繪製玩家
        player.draw(gc);

        // 繪製 UI 分數 (Demo 省略 SceneBuilder 控制器更新分數)
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(new Font("Monospaced", 12));
        gc.fillText("SCORE: 00000", 10, 20);
    }

    // 鍵盤控制橋接方法 (由控制器呼叫)
    public void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> player.setKeyPressed("UP", true);
            case DOWN -> player.setKeyPressed("DOWN", true);
            case LEFT -> player.setKeyPressed("LEFT", true);
            case RIGHT -> player.setKeyPressed("RIGHT", true);
            case SPACE -> setSpacePressed(true);
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> player.setKeyPressed("UP", false);
            case DOWN -> player.setKeyPressed("DOWN", false);
            case LEFT -> player.setKeyPressed("LEFT", false);
            case RIGHT -> player.setKeyPressed("RIGHT", false);
            case SPACE -> setSpacePressed(false);
        }
    }

    // 簡單的空格鍵狀態管理
    private boolean spacePressed = false;
    public boolean isSpacePressed() { return spacePressed; }
    public void setSpacePressed(boolean spacePressed) { this.spacePressed = spacePressed; }
}