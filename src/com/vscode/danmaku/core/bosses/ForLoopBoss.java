package com.vscode.danmaku.core.bosses;

import com.vscode.danmaku.core.EnemyBullet;
import com.vscode.danmaku.core.Bullet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;

public class ForLoopBoss {
    public double x = 370, y = 120;
    public double width = 60, height = 60;

    // 三層獨立血量
    private int hpK = 0; // 最外層 k
    private int hpJ = 0; // 中間層 j
    private int hpI = 150; // 最內層 i (核心)

    private boolean kAlive = true;
    private boolean jAlive = true;
    private boolean isAlive = true;

    // 移動與常規射擊控制
    private double vx = 2.0;
    private long lastShootTime = 0;
    private int iterationCount = 0;

    // ==========================================
    // 狙擊系統專用變數 (Phase 2 開始啟用)
    // ==========================================
    private double prevPlayerX = -1;
    private double prevPlayerY = -1;
    private boolean isSniperAiming = false;
    private long sniperAimStartTime = 0;
    private double sniperAimAngle = 0;
    private double sniperAimProgress = 0.0;
    private long lastSniperFireTime = 0;

    public void update(long now, List<EnemyBullet> enemyBullets, double playerX, double playerY) {
        if (!isAlive) return;

        // 1. 移動邏輯
        x += vx;
        double currentMargin = kAlive ? 40 : (jAlive ? 20 : 0);
        if (x < 50 + currentMargin || x > 750 - width - currentMargin) {
            vx = -vx;
        }

        // 2. 常規 Nested Loop 攻擊邏輯 (3.5 秒一次)
        if (lastShootTime == 0) lastShootTime = now;
        if (now - lastShootTime > 3_500_000_000L) {
            executeNestedLoopAttack(enemyBullets, playerX, playerY);
            lastShootTime = now;
            iterationCount++;
        }

        // ==========================================
        // 3. 動態預判狙擊系統 (當外層 K 被擊破後啟動)
        // ==========================================
        if (prevPlayerX == -1) {
            prevPlayerX = playerX;
            prevPlayerY = playerY;
        }

        // 計算玩家的瞬間移動速度向量
        double pvx = playerX - prevPlayerX;
        double pvy = playerY - prevPlayerY;

        // 只要 K 死了，J 或 I 都會使用狙擊系統
        if (!kAlive) {
            // J 階段狙擊冷卻較長 (3秒)，I 階段較短 (2.5秒)
            long sniperCooldown = jAlive ? 3_000_000_000L : 2_500_000_000L;

            if (!isSniperAiming && now - lastSniperFireTime > sniperCooldown) {
                isSniperAiming = true;
                sniperAimStartTime = now;
            }

            if (isSniperAiming) {
                long aimDuration = now - sniperAimStartTime;
                // 瞄準時間設定為 0.8 秒
                sniperAimProgress = (double) aimDuration / 800_000_000L;

                double centerX = x + width / 2;
                double centerY = y + height / 2;

                if (sniperAimProgress < 1.0) {
                    // 【瞄準追蹤階段】：前 80% 時間進行動態預判，後 20% 鎖死方向
                    if (sniperAimProgress < 0.6) {
                        double dist = Math.hypot(playerX - centerX, playerY - centerY);
                        // I 的子彈比 J 更快
                        double sniperSpeed = jAlive ? 16.0 : 22.0;
                        double framesToHit = dist / sniperSpeed;

                        double predictedX = playerX + (pvx * framesToHit);
                        double predictedY = playerY + (pvy * framesToHit);

                        sniperAimAngle = Math.toDegrees(Math.atan2(predictedY - centerY, predictedX - centerX));
                    }
                } else {
                    // 【開火階段】
                    isSniperAiming = false;
                    lastSniperFireTime = now;
                    sniperAimProgress = 0.0;

                    if (jAlive) {
                        // 【J 系統】：單發高精準直線狙擊 (不擴散)
                        enemyBullets.add(new EnemyBullet(centerX, centerY, 16.0, sniperAimAngle));
                        System.out.println("[警告] 中層 J 直線狙擊發射！");
                    } else {
                        // 【I 系統】：終極 5 向擴散預判狙擊 (擴散角度: -30, -15, 0, +15, +30)
                        for (int angleOffset = -30; angleOffset <= 30; angleOffset += 15) {
                            enemyBullets.add(new EnemyBullet(centerX, centerY, 22.0, sniperAimAngle + angleOffset));
                        }
                        System.out.println("[致命警告] 核心 I 擴散狙擊發射！");
                    }
                }
            }
        }

        // 更新上一幀座標
        prevPlayerX = playerX;
        prevPlayerY = playerY;
    }

    private void executeNestedLoopAttack(List<EnemyBullet> enemyBullets, double playerX, double playerY) {
        double centerX = x + width / 2;
        double centerY = y + height / 2;

        if (kAlive) {
            // 【階段 1】完整三層迴圈 i, j, k
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 4; j++) {
                    double speed = 1.2 + (j * 0.6);
                    for (int k = 0; k < 360; k += 45) {
                        double finalAngle = k + (i * 15) + (j * 6) + (iterationCount * 12);
                        enemyBullets.add(new EnemyBullet(centerX, centerY, speed, finalAngle));
                    }
                }
            }
        }
        else if (jAlive) {
            // 【階段 2】k 被擊破 -> 二層迴圈 (搭配單發狙擊，給予玩家走位壓力)
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    double speed = 2.0 + (j * 0.5);
                    for (int k = 0; k < 360; k += 30) {
                        double finalAngle = k + (i * 20) + (iterationCount * 15);
                        enemyBullets.add(new EnemyBullet(centerX, centerY, speed, finalAngle));
                    }
                }
            }
        }
        else {
            // 【階段 3】核心爆發：配合五向擴散狙擊，常規攻擊轉為干擾波
            for (int i = -4; i <= 4; i++) {
                double speed = 2.5;
                double angleToPlayer = Math.toDegrees(Math.atan2(playerY - centerY, playerX - centerX));
                double finalAngle = angleToPlayer + (i * 12) + (Math.sin(iterationCount) * 15);
                enemyBullets.add(new EnemyBullet(centerX, centerY, speed, finalAngle));
            }
        }
    }

    public boolean hit(Bullet playerBullet) {
        if (!isAlive) return false;

        // 1. K 層裝甲
        if (kAlive) {
            if (playerBullet.x < x + width + 40 && playerBullet.x + playerBullet.width > x - 40 &&
                    playerBullet.y < y + height + 40 && playerBullet.y + playerBullet.height > y - 40) {
                hpK--;
                playerBullet.setAlive(false);
                if (hpK <= 0) kAlive = false;
                return false;
            }
            return false;
        }

        // 2. J 層裝甲
        if (jAlive) {
            if (playerBullet.x < x + width + 20 && playerBullet.x + playerBullet.width > x - 20 &&
                    playerBullet.y < y + height + 20 && playerBullet.y + playerBullet.height > y - 20) {
                hpJ--;
                playerBullet.setAlive(false);
                if (hpJ <= 0) jAlive = false;
                return false;
            }
            return false;
        }

        // 3. 核心命中
        if (playerBullet.x < x + width && playerBullet.x + playerBullet.width > x &&
                playerBullet.y < y + height && playerBullet.y + playerBullet.height > y) {
            hpI--;
            playerBullet.setAlive(false);
            if (hpI <= 0) {
                isAlive = false;
                return true;
            }
        }
        return false;
    }

    public void draw(GraphicsContext gc) {
        if (!isAlive) return;

        // ==========================================
        // 繪製預判狙擊雷射線 (支援單線與多線擴散)
        // ==========================================
        if (!kAlive && isSniperAiming) {
            double centerX = x + width / 2;
            double centerY = y + height / 2;

            // 雷射特效：隨著時間變細，顏色變深
            double lineWidth = 6 * (1.0 - sniperAimProgress) + 1.5;

            // 如果進度超過 80% (鎖定階段)，雷射變成亮實心紅色
            if (sniperAimProgress >= 0.8) {
                gc.setStroke(Color.web("#FF0000", 0.9));
                gc.setLineDashes();
            } else {
                gc.setStroke(Color.web("#FF4444", 0.4 + 0.4 * sniperAimProgress));
                gc.setLineDashes(10, 10);
            }
            gc.setLineWidth(lineWidth);

            if (jAlive) {
                // 【J 系統】：畫出一條直線預警
                double endX = centerX + Math.cos(Math.toRadians(sniperAimAngle)) * 1500;
                double endY = centerY + Math.sin(Math.toRadians(sniperAimAngle)) * 1500;
                gc.strokeLine(centerX, centerY, endX, endY);
            } else {
                // 【I 系統】：畫出五條擴散線預警，完全對應子彈發射軌跡
                for (int angleOffset = -30; angleOffset <= 30; angleOffset += 15) {
                    double spreadAngle = sniperAimAngle + angleOffset;
                    double endX = centerX + Math.cos(Math.toRadians(spreadAngle)) * 1500;
                    double endY = centerY + Math.sin(Math.toRadians(spreadAngle)) * 1500;
                    gc.strokeLine(centerX, centerY, endX, endY);
                }
            }

            gc.setLineDashes(); // 重置畫筆狀態
        }

        // --- 繪製最外層 k 方塊 ---
        if (kAlive) {
            gc.setStroke(Color.web("#FF00FF"));
            gc.setLineWidth(3);
            gc.strokeRect(x - 40, y - 40, width + 80, height + 80);

            gc.setFill(Color.web("#FF00FF"));
            gc.setFont(new Font("Monospaced", 12));
            gc.fillText("k_layer: HP " + hpK, x - 40, y - 46);
        }

        // --- 繪製中層 j 方塊 ---
        if (jAlive) {
            gc.setStroke(Color.web("#00FFFF"));
            gc.setLineWidth(2.5);
            gc.strokeRect(x - 20, y - 20, width + 40, height + 40);

            if (!kAlive) {
                gc.setFill(Color.web("#00FFFF"));
                gc.setFont(new Font("Monospaced", 12));
                gc.fillText("j_layer: HP " + hpJ, x - 20, y - 26);
            }
        }

        // --- 繪製最內層 i 方塊核心 ---
        gc.setFill(Color.web("#111111"));
        gc.fillRect(x, y, width, height);

        if (!jAlive) {
            // 狙擊發射時，核心爆閃紅色
            if (isSniperAiming && sniperAimProgress >= 0.8) {
                gc.setFill(Color.web("#FF0000"));
                gc.fillRect(x, y, width, height);
            }
            gc.setStroke(Color.web("#FF3333"));
            gc.setLineWidth(4);
        } else {
            gc.setStroke(Color.web("#00FF00"));
            gc.setLineWidth(2);
        }
        gc.strokeRect(x, y, width, height);

        // 核心文字標籤
        gc.setFont(new Font("Monospaced", 14));
        if (!jAlive) {
            gc.setFill(isSniperAiming && sniperAimProgress >= 0.8 ? Color.WHITE : Color.web("#FF3333"));
            gc.fillText("CORE_i", x + 5, y + 34);
            gc.setFont(new Font("Monospaced", 11));
            gc.fillText("HP: " + hpI, x + 10, y - 6);
        } else {
            gc.setFill(Color.web("#00FF00"));
            gc.fillText("[ i ]", x + 14, y + 34);
        }
    }

    public boolean isAlive() { return isAlive; }
    public boolean isKAlive() { return kAlive; }
    public boolean isJAlive() { return jAlive; }
    public int getHpI() { return hpI; }
    public int getHpJ() { return hpJ; }
    public int getHpK() { return hpK; }
}