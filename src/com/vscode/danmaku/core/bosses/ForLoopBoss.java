package com.vscode.danmaku.core.bosses;

import com.vscode.danmaku.core.EnemyBullet;
import com.vscode.danmaku.core.GameObject; // 替換成你實際的子彈/物件類別
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

public class ForLoopBoss {
    public double x = 400, y = 100;
    private double width = 60, height = 60;

    private final int maxHp = 200; // 迴圈 BOSS 比較硬
    private int hp = maxHp;
    private boolean isAlive = true;

    // 移動與射擊控制
    private double vx = 3.0;
    private long lastShootTime = 0;

    // 迴圈狀態變數
    private int iterationCount = 0;

    public void update(long now, List<EnemyBullet> enemyBullets) {
        if (!isAlive) return;

        // --- 1. 陣列走訪式移動 (Array Traversal) ---
        x += vx;
        if (x < 50 || x > 750 - width) {
            vx = -vx; // 碰到邊界反彈
        }

        // --- 2. For Loop 彈幕發射邏輯 ---
        if (lastShootTime == 0) lastShootTime = now;

        // 每 1.5 秒執行一次迴圈攻擊 (1.5億奈秒)
        if (now - lastShootTime > 2_500_000_000L) {
            executeNestedLoopAttack(enemyBullets);
            lastShootTime = now;
            iterationCount++;
        }
    }

    /**
     * 核心攻擊：巢狀迴圈彈幕 (Nested Loop Danmaku)
     */
    private void executeNestedLoopAttack(List<EnemyBullet> enemyBullets) {
        double centerX = x + width / 2;
        double centerY = y + height / 2;

        System.out.println("執行迴圈攻擊，Iteration: " + iterationCount);

        // 外層迴圈：發射 3 圈子彈
        for (int ring = 0; ring < 3; ring++) {
            double speed = 2.5 + ring; // 每一圈速度越來越快

            // 內層迴圈：每一圈發射 18 發子彈 (360度 / 20度)
            for (int angle = 0; angle < 360; angle += 20) {
                // 隨著 iterationCount 讓角度偏移，產生旋轉交錯的幾何圖形
                double finalAngle = angle + (iterationCount * 10) + (ring * 5);
                enemyBullets.add(new EnemyBullet(centerX, centerY, speed, finalAngle));
            }
        }
    }

    public boolean hit(GameObject playerBullet) {
        if (!isAlive) return false;
        // 簡單矩形碰撞
        if (playerBullet.x < x + width && playerBullet.x + playerBullet.width > x &&
                playerBullet.y < y + height && playerBullet.y + playerBullet.height > y) {

            hp--;
            playerBullet.setAlive(false);
            if (hp <= 0) {
                isAlive = false;
                return true;
            }
        }
        return false;
    }

    public void draw(GraphicsContext gc) {
        if (!isAlive) return;

        // 畫出一個像處理器/程式碼區塊的方形核心
        gc.setFill(Color.web("#2B2B2B"));
        gc.fillRect(x, y, width, height);

        gc.setStroke(Color.web("#00FF00")); // 終端機螢光綠
        gc.setLineWidth(3);
        gc.strokeRect(x, y, width, height);

        // 畫出目前的迴圈變數 i
        gc.setFill(Color.web("#00FF00"));
        gc.setFont(new javafx.scene.text.Font("Monospaced", 20));
        gc.fillText("[ i = " + iterationCount + " ]", x - 25, y - 10);
    }

    public boolean isAlive() { return isAlive; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
}