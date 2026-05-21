package com.vscode.danmaku.core.bosses;

import com.vscode.danmaku.core.EnemyBullet;
import com.vscode.danmaku.core.Bullet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecursionBoss {
    public double x = 370, y = 100;
    public double width = 80, height = 80;

    private final int maxHp = (int)(300 * com.vscode.danmaku.core.GameManager.difficultyMultiplier);
    private int hp = maxHp;
    private boolean isAlive = true;

    private double vx = 3.0;
    private long lastShootTime = 0;
    private boolean hasFinishedSplitting = true; 

    private static class RecursionNode {
        EnemyBullet bullet;
        int splitCount;
        long spawnTime;
        boolean trackingFinished = false;

        RecursionNode(EnemyBullet bullet, int splitCount, long spawnTime) {
            this.bullet = bullet;
            this.splitCount = splitCount;
            this.spawnTime = spawnTime;
        }
    }

    private List<RecursionNode> activeNodes = new ArrayList<>();

    public RecursionBoss() {
    }

    public void update(long now, List<EnemyBullet> enemyBullets, double playerX, double playerY) {
        if (!isAlive) return;

        // 移動邏輯
        x += vx;
        if (x < 50 || x > 750 - width) {
            vx = -vx;
        }

        // 發射大子彈邏輯
        if (lastShootTime == 0) {
            lastShootTime = now;
        }

        // 每5秒，且上一次循環已經分裂兩次才能生成大子彈
        if (hasFinishedSplitting && now - lastShootTime > 5_000_000_000L) {
            double centerX = x + width / 2;
            double centerY = y + height / 2;
            
            // 朝著玩家方向射擊
            double baseAngle = Math.toDegrees(Math.atan2(playerY - centerY, playerX - centerX));
            
            // 大子彈速度較慢
            EnemyBullet rootBullet = new EnemyBullet(centerX, centerY, 1.5, baseAngle);
            rootBullet.radius = 24.0; // 巨大的根彈幕
            enemyBullets.add(rootBullet);
            
            activeNodes.add(new RecursionNode(rootBullet, 0, now));
            
            lastShootTime = now;
            hasFinishedSplitting = false; // 鎖定直到分裂兩次
        }

        List<RecursionNode> newNodes = new ArrayList<>();
        Iterator<RecursionNode> iterator = activeNodes.iterator();
        
        while (iterator.hasNext()) {
            RecursionNode node = iterator.next();
            
            // 若子彈已死 (被清屏或飛出界)，移除節點
            if (!node.bullet.isAlive() || !enemyBullets.contains(node.bullet)) {
                iterator.remove();
                continue;
            }

            long livedTime = now - node.spawnTime;

            // 所有的彈幕都具備追蹤能力，只要它還沒完成追蹤時間
            if (!node.trackingFinished) {
                 double targetAngle = Math.toDegrees(Math.atan2(playerY - node.bullet.y, playerX - node.bullet.x));
                 double currentAngle = Math.toDegrees(Math.atan2(node.bullet.vy, node.bullet.vx));
                 
                 double diff = targetAngle - currentAngle;
                 while (diff < -180) diff += 360;
                 while (diff > 180) diff -= 360;
                 
                 // 根據分裂階段設定轉向靈敏度，大顆轉得慢，小顆轉得快
                 double maxTurn = (node.splitCount == 0) ? 0.5 : 
                                  (node.splitCount == 1 ? 1.0 : 
                                  (node.splitCount == 2 ? 1.5 : 2.0));
                 
                 if (diff > maxTurn) diff = maxTurn;
                 if (diff < -maxTurn) diff = -maxTurn;
                 
                 double newAngle = currentAngle + diff;
                 double speed = Math.sqrt(node.bullet.vx * node.bullet.vx + node.bullet.vy * node.bullet.vy);
                 
                 node.bullet.vx = Math.cos(Math.toRadians(newAngle)) * speed;
                 node.bullet.vy = Math.sin(Math.toRadians(newAngle)) * speed;
            }

            if (node.splitCount == 0) {
                // 根彈幕飛行 0.6 秒後分裂 (第 1 次分裂)
                if (livedTime > 600_000_000L) {
                    node.bullet.setAlive(false);
                    iterator.remove();
                    
                    double baseAngle = Math.toDegrees(Math.atan2(node.bullet.vy, node.bullet.vx));
                    for (int i = -1; i <= 1; i++) { // 分成 3 顆
                        double angle = baseAngle + i * 40;
                        EnemyBullet child = new EnemyBullet(node.bullet.x, node.bullet.y, 2.5, angle);
                        child.radius = 12.0; 
                        enemyBullets.add(child);
                        newNodes.add(new RecursionNode(child, 1, now));
                    }
                }
            } else if (node.splitCount == 1) {
                // 第一層子彈幕飛行 0.6 秒後分裂 (第 2 次分裂)
                if (livedTime > 600_000_000L) {
                    node.bullet.setAlive(false);
                    iterator.remove();
                    
                    double baseAngle = Math.toDegrees(Math.atan2(node.bullet.vy, node.bullet.vx));
                    for (int i = -1; i <= 1; i++) { // 每顆再分成 3 顆
                        double angle = baseAngle + i * 45;
                        EnemyBullet child = new EnemyBullet(node.bullet.x, node.bullet.y, 4.0, angle);
                        child.radius = 6.0; 
                        enemyBullets.add(child);
                        newNodes.add(new RecursionNode(child, 2, now));
                    }
                }
            } else if (node.splitCount == 2) {
                // 第二層子彈幕飛行 0.6 秒後分裂 (第 3 次分裂)
                if (livedTime > 600_000_000L) {
                    node.bullet.setAlive(false);
                    iterator.remove();
                    
                    double baseAngle = Math.toDegrees(Math.atan2(node.bullet.vy, node.bullet.vx));
                    for (int i = -1; i <= 1; i++) { // 每顆再分成 3 顆
                        double angle = baseAngle + i * 30; // 稍微收束角度
                        EnemyBullet child = new EnemyBullet(node.bullet.x, node.bullet.y, 5.0, angle);
                        child.radius = 3.0; // 終極小型子彈
                        enemyBullets.add(child);
                        newNodes.add(new RecursionNode(child, 3, now));
                    }
                }
            } else if (node.splitCount == 3) {
                // 最小的彈幕跟蹤玩家 1 秒 (1秒後取消跟蹤)
                if (livedTime >= 1_000_000_000L && !node.trackingFinished) {
                    node.trackingFinished = true;
                    // 取消跟蹤後，速度稍微加快飛出畫面
                    double speed = Math.sqrt(node.bullet.vx * node.bullet.vx + node.bullet.vy * node.bullet.vy) * 1.5;
                    double currentAngle = Math.toDegrees(Math.atan2(node.bullet.vy, node.bullet.vx));
                    node.bullet.vx = Math.cos(Math.toRadians(currentAngle)) * speed;
                    node.bullet.vy = Math.sin(Math.toRadians(currentAngle)) * speed;
                }
            }
        }
        
        // 檢查是否有新的第三次分裂產生
        if (!hasFinishedSplitting && !newNodes.isEmpty() && newNodes.get(0).splitCount == 3) {
            hasFinishedSplitting = true;
        }

        activeNodes.addAll(newNodes);
        
        // 如果場上已經沒有活躍的遞迴彈幕，也可以解鎖發射，避免卡死
        if (!hasFinishedSplitting && activeNodes.isEmpty()) {
            hasFinishedSplitting = true;
        }
    }

    public boolean hit(Bullet playerBullet) {
        if (!isAlive) return false;

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

        // 繪製本體
        gc.setFill(Color.web("#800080")); // Purple for recursion
        gc.fillRect(x, y, width, height);
        
        gc.setStroke(Color.web("#DA70D6"));
        gc.setLineWidth(3);
        gc.strokeRect(x, y, width, height);

        // 繪製遞迴圖形特效
        drawFractal(gc, x + width/2, y + height/2, width/2, 3);

        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Monospaced", 14));
        gc.fillText("Recursion", x + 5, y - 5);
    }
    
    // 遞迴繪製圖案
    private void drawFractal(GraphicsContext gc, double cx, double cy, double size, int depth) {
        if (depth <= 0) return;
        gc.setStroke(Color.web("#DA70D6", 0.5 + 0.1 * depth));
        gc.strokeRect(cx - size/2, cy - size/2, size, size);
        drawFractal(gc, cx - size/2, cy - size/2, size/2, depth - 1);
        drawFractal(gc, cx + size/2, cy - size/2, size/2, depth - 1);
        drawFractal(gc, cx - size/2, cy + size/2, size/2, depth - 1);
        drawFractal(gc, cx + size/2, cy + size/2, size/2, depth - 1);
    }

    public boolean isAlive() { return isAlive; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
}