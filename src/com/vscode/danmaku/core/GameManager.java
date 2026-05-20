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
    private int score = 0; // 新增分數變數
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
        // 取得視窗邊界
        double cw = gameCanvas.getWidth();
        double ch = gameCanvas.getHeight();

        // 1. 更新玩家與【邊界限制】
        player.update(now);
        if (player.x < 0) player.x = 0;
        if (player.x > cw - player.width) player.x = cw - player.width;
        if (player.y < 0) player.y = 0;
        if (player.y > ch - player.height) player.y = ch - player.height;

        if (isSpacePressed()) {
            player.shoot(now, playerBullets);
        }

        // 2. 更新子彈與【出界銷毀】
        for (Bullet b : playerBullets) {
            b.update(now);
            // 如果子彈飛出視窗的上、下、左、右
            if (b.y + b.height < 0 || b.y > ch || b.x < 0 || b.x > cw) {
                b.setAlive(false); // 標記為死亡，稍後會被清除
            }
        }

        // 3. 更新 BOSS (將玩家目前的 X 和 Y 座標都傳給蟲蟲)
        boss.setTargetX(player.x); // <--- 新增這一行！告訴蟲蟲玩家的 X 在哪
        boss.setTargetY(player.y);
        boss.update(now);

        // 4. 碰撞偵測 (子彈 vs BOSS) 與【分數系統】
        for (Bullet b : playerBullets) {
            if (b.isAlive()) {
                boolean killedBoss = boss.hit(b);
                if (killedBoss) {
                    score += 1000; // 擊殺 BOSS 獲得 1000 分
                    System.out.println("BOSS 擊破！分數 +1000");
                } else if (!b.isAlive()) {
                    score += 10;   // 成功命中但未擊殺獲得 10 分
                }
            }
        }

        // 5. 碰撞偵測 (BOSS vs 玩家) -> Game Over
        if (boss.isHittingPlayer(player)) {
            player.setAlive(false);
            System.out.println("====== GAME OVER ======");
            stop(); // 停止遊戲
            return;
        }

        // 6. 清理已銷毀物件 (包含打中敵人的、飛出畫面的子彈)
        playerBullets.removeIf(b -> !b.isAlive());
    }

    private void draw(GraphicsContext gc) {
        // 1. 清空畫布 (Dark Mode 背景)
        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // 2. 繪製 BOSS
        boss.draw(gc);

        // --- 新增：3. 繪製 BOSS 血條在畫面正上方中央 ---
        if (boss.isAlive()) {
            double barWidth = 400;               // 血條總長度
            double barHeight = 14;               // 血條高度
            double cw = gameCanvas.getWidth();
            double barX = (cw - barWidth) / 2;   // 計算置中的 X 座標 (cw 為 gameCanvas.getWidth())
            double barY = 20;                    // 距離畫布頂端 20 像素

            // 3a. 畫出血條背景（深灰色底層）
            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);

            // 3b. 計算血量剩餘比例
            double hpRatio = (double) boss.getBossHp() / boss.getMaxHp();
            double currentBarWidth = barWidth * hpRatio;

            // 3c. 畫出當前血量（紅色，對應 VS Code 錯誤提示紅）
            gc.setFill(Color.web("#F44336"));
            gc.fillRect(barX, barY, currentBarWidth, barHeight);

            // 3d. 畫出血條的外框線（淺灰色，增加質感）
            gc.setStroke(Color.web("#888888"));
            gc.setLineWidth(1.5);
            gc.strokeRect(barX, barY, barWidth, barHeight);

            // 3e. 在血條上方加上 BOSS 識別文字
            gc.setFill(Color.web("#859900")); // 綠色字體
            gc.setFont(new Font("Monospaced", 11));
            gc.fillText("[PROCESS] BOSS: LINKED_LIST_WORM", barX, barY - 6);
        }

        // 4. 繪製子彈
        for (Bullet b : playerBullets) {
            b.draw(gc);
        }

        // 5. 繪製玩家
        player.draw(gc);

        // 6. 繪製 UI 分數 (保持不變)
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(new Font("Monospaced", 18));
        gc.fillText("SCORE: " + String.format("%05d", score), 20, 30);
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