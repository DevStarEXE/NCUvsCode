package com.vscode.danmaku.core.bosses;

import com.vscode.danmaku.core.EnemyBullet;
import com.vscode.danmaku.core.Bullet;
import com.vscode.danmaku.core.EnemyLaser;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;

public class ForLoopBoss {
    public double x = 370, y = 120;
    public double width = 60, height = 60;

    private int phase = 1; // 1: i, 2: i+j, 3: i+j+k
    
    private final int baseHp = (int)(100 * com.vscode.danmaku.core.GameManager.difficultyMultiplier);
    private int hpI = baseHp;
    private int hpJ = baseHp;
    private int hpK = baseHp;

    private int maxHpI = hpI;
    private int maxHpJ = hpJ;
    private int maxHpK = hpK;

    private int iCount = 1;
    private int jCount = 1;
    private int kCount = 0;
    
    private boolean iAttackedThisKCycle = false;
    private boolean jAttackedThisKCycle = false;

    private boolean isAlive = true;

    private double vx = 2.0;
    private long lastIAttackTime = 0;
    private long lastJAttackTime = 0;

    // Sniper / j attack logic
    private double prevPlayerX = -1;
    private double prevPlayerY = -1;
    private boolean isSniperAiming = false;
    private long sniperAimStartTime = 0;
    private double sniperAimAngle = 0;
    private double sniperAimProgress = 0.0;
    private int jBurstRemaining = 0;
    private long lastJBurstTime = 0;
    private double lockedSniperAngle = 0;
    private double sniperStartX, sniperStartY;

    public void update(long now, List<EnemyBullet> enemyBullets, List<EnemyLaser> enemyLasers, double playerX, double playerY) {
        if (!isAlive) return;

        // 1. Movement: Stop only during Sniper aiming
        boolean isSniperBusy = isSniperAiming;
        if (!isSniperBusy) {
            x += vx;
            double margin = 50 + (phase * 20);
            if (x < margin || x > 750 - width - margin) {
                vx = -vx;
            }
        }

        // 2. Track player for sniper
        if (prevPlayerX == -1) {
            prevPlayerX = playerX;
            prevPlayerY = playerY;
        }
        double pvx = playerX - prevPlayerX;
        double pvy = playerY - prevPlayerY;

        // 3. Phase Attacks
        handleIAttack(now, enemyBullets, playerX, playerY);
        
        if (phase >= 2) {
            handleJAttack(now, enemyBullets, playerX, playerY, pvx, pvy);
        }
        
        if (phase >= 3) {
            handleKCycle(now, enemyLasers, playerX, playerY);
        }

        prevPlayerX = playerX;
        prevPlayerY = playerY;
    }

    private void handleIAttack(long now, List<EnemyBullet> enemyBullets, double playerX, double playerY) {
        if (lastIAttackTime == 0) lastIAttackTime = now;
        
        // i attack every 2.5 seconds
        if (now - lastIAttackTime > 2_500_000_000L) {
            double centerX = x + width / 2;
            double centerY = y + height / 2;
            
            int waves = 1; // K enhancement removed
            int bulletsPerWave = iCount; 
            
            for (int w = 0; w < waves; w++) {
                if (bulletsPerWave == 1) {
                    double angle = Math.toDegrees(Math.atan2(playerY - centerY, playerX - centerX));
                    enemyBullets.add(new EnemyBullet(centerX, centerY, 2.0 + (w * 0.5), angle));
                } else {
                    for (int i = 0; i < bulletsPerWave; i++) {
                        double speed = 2.0 + (w * 0.5);
                        double angle = (360.0 / bulletsPerWave) * i + (iCount * 10);
                        enemyBullets.add(new EnemyBullet(centerX, centerY, speed, angle));
                    }
                }
            }
            
            iCount++;
            lastIAttackTime = now;
            iAttackedThisKCycle = true;
        }
    }

    private void handleJAttack(long now, List<EnemyBullet> enemyBullets, double playerX, double playerY, double pvx, double pvy) {
        if (!isSniperAiming && jBurstRemaining == 0 && now - lastJAttackTime > 3_000_000_000L) {
            isSniperAiming = true;
            sniperAimStartTime = now;
            sniperStartX = x + width / 2;
            sniperStartY = y + height / 2;
        }

        if (isSniperAiming) {
            long aimDuration = now - sniperAimStartTime;
            sniperAimProgress = (double) aimDuration / 3_000_000_000L; // Total 3 seconds process

            if (sniperAimProgress < 1.0) {
                // Tracking stage (0.0 - 0.66): Warning line follows player for ~2 seconds
                if (sniperAimProgress < 0.66) {
                    double dist = Math.hypot(playerX - sniperStartX, playerY - sniperStartY);
                    double sniperSpeed = 18.0;
                    double framesToHit = dist / sniperSpeed;
                    double predictedX = playerX + (pvx * framesToHit);
                    double predictedY = playerY + (pvy * framesToHit);
                    sniperAimAngle = Math.toDegrees(Math.atan2(predictedY - sniperStartY, predictedX - sniperStartX));
                    lockedSniperAngle = sniperAimAngle;
                } 
                // Charging/Locked stage (0.66 - 1.0): Line stops for ~1 second
                else {
                    sniperAimAngle = lockedSniperAngle;
                }
            } else {
                isSniperAiming = false;
                jBurstRemaining = jCount;
                lastJBurstTime = 0; 
            }
        }
        
        if (jBurstRemaining > 0 && now - lastJBurstTime > 150_000_000L) {
            // J bullets strictly follow locked lines from FIXED source
            int trajectories = 1; // K enhancement removed
            double spread = 15.0; // Fixed spread angle
            
            for (int t = 0; t < trajectories; t++) {
                double offset = (t - (trajectories - 1) / 2.0) * spread;
                enemyBullets.add(new EnemyBullet(sniperStartX, sniperStartY, 18.0, lockedSniperAngle + offset));
            }
            
            jBurstRemaining--;
            lastJBurstTime = now;
            if (jBurstRemaining == 0) {
                lastJAttackTime = now;
                jCount++;
                jAttackedThisKCycle = true;
            }
        }
    }

    private void handleKCycle(long now, List<EnemyLaser> enemyLasers, double playerX, double playerY) {
        if (iAttackedThisKCycle && jAttackedThisKCycle) {
            kCount++;
            iAttackedThisKCycle = false;
            jAttackedThisKCycle = false;
            
            // K Attack: Player-centered lasers based on kCount
            int laserCount = 1 + (kCount / 2);
            for (int i = 0; i < laserCount; i++) {
                // Determine origin based on iteration to spread around
                double startX, startY;
                int side = (kCount + i) % 4;
                if (side == 0) { startX = Math.random() * 800; startY = 0; }
                else if (side == 1) { startX = 800; startY = Math.random() * 600; }
                else if (side == 2) { startX = Math.random() * 800; startY = 600; }
                else { startX = 0; startY = Math.random() * 600; }
                
                enemyLasers.add(new EnemyLaser(startX, startY, playerX, playerY, now));
            }
            
            System.out.println("[SYSTEM] K_LAYER LASER_STRIKE: Count=" + laserCount);
        }
    }

    public boolean hit(Bullet playerBullet) {
        if (!isAlive) return false;

        boolean hitDetected = false;
        double bx = playerBullet.x;
        double by = playerBullet.y;
        double bw = playerBullet.width;
        double bh = playerBullet.height;

        if (phase == 1) {
            if (bx < x + width && bx + bw > x && by < y + height && by + bh > y) {
                hpI--;
                hitDetected = true;
                if (hpI <= 0) {
                    phase = 2;
                    System.out.println("[EVOLUTION] PHASE 2: J_LAYER COMPILED");
                }
            }
        } else if (phase == 2) {
            if (bx < x + width + 20 && bx + bw > x - 20 && by < y + height + 20 && by + bh > y - 20) {
                hpJ--;
                hitDetected = true;
                if (hpJ <= 0) {
                    phase = 3;
                    System.out.println("[EVOLUTION] PHASE 3: K_LAYER COMPILED");
                }
            }
        } else if (phase == 3) {
            if (bx < x + width + 40 && bx + bw > x - 40 && by < y + height + 40 && by + bh > y - 40) {
                hpK--;
                hitDetected = true;
                if (hpK <= 0) {
                    isAlive = false;
                    return true;
                }
            }
        }

        if (hitDetected) {
            playerBullet.setAlive(false);
        }
        return false;
    }

    public void draw(GraphicsContext gc) {
        if (!isAlive) return;

        double centerX = x + width / 2;
        double centerY = y + height / 2;

        // Draw sniper line if aiming
        if (isSniperAiming) {
            double lineWidth = 5 * (1.0 - sniperAimProgress) + 2;
            if (sniperAimProgress >= 0.7) { // Sync with locked phase
                gc.setStroke(Color.web("#00FFFF", 1.0)); // Cyan for Locked
                gc.setLineDashes();
            } else {
                gc.setStroke(Color.web("#FFFF00", 0.8)); // Yellow for Tracking
                gc.setLineDashes(15, 10);
            }
            gc.setLineWidth(lineWidth);
            
            int trajectories = 1; // Strictly 1 warning line
            double spread = 15.0;
            
            for (int t = 0; t < trajectories; t++) {
                double offset = (t - (trajectories - 1) / 2.0) * spread;
                double angle = sniperAimAngle + offset;
                double endX = sniperStartX + Math.cos(Math.toRadians(angle)) * 1500;
                double endY = sniperStartY + Math.sin(Math.toRadians(angle)) * 1500;
                gc.strokeLine(sniperStartX, sniperStartY, endX, endY);
            }
            gc.setLineDashes();
        }

        // Draw Layers
        // Core i
        gc.setFill(Color.web("#111111"));
        gc.fillRect(x, y, width, height);
        gc.setStroke(Color.web("#00FF00"));
        gc.setLineWidth(2);
        gc.strokeRect(x, y, width, height);
        gc.setFill(Color.web("#00FF00"));
        gc.setFont(new Font("Monospaced", 14));
        gc.fillText("[i]", x + 18, y + 35);

        // Wrapper j
        if (phase >= 2) {
            gc.setStroke(Color.web("#00FFFF", 0.8));
            gc.setLineWidth(3);
            gc.strokeRect(x - 20, y - 20, width + 40, height + 40);
            gc.setFill(Color.web("#00FFFF"));
            gc.setFont(new Font("Monospaced", 12));
            gc.fillText("j_wrap", x - 20, y - 25);
        }

        // Wrapper k
        if (phase >= 3) {
            gc.setStroke(Color.web("#FF00FF", 0.8));
            gc.setLineWidth(4);
            gc.strokeRect(x - 40, y - 40, width + 80, height + 80);
            gc.setFill(Color.web("#FF00FF"));
            gc.setFont(new Font("Monospaced", 12));
            gc.fillText("k_wrap", x - 40, y - 45);
        }
    }

    public boolean isAlive() { return isAlive; }
    public int getPhase() { return phase; }
    public int getHpI() { return hpI; }
    public int getHpJ() { return hpJ; }
    public int getHpK() { return hpK; }
    public int getMaxHpI() { return maxHpI; }
    public int getMaxHpJ() { return maxHpJ; }
    public int getMaxHpK() { return maxHpK; }
}
