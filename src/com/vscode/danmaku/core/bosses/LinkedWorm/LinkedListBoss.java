package com.vscode.danmaku.core.bosses.LinkedWorm;

import com.vscode.danmaku.core.EnemyBullet;
import com.vscode.danmaku.core.GameObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LinkedListBoss extends GameObject {
    private final int maxHp = 400;
    private int bossHp = maxHp;

    private boolean isGameOver = false;
    private boolean isVictory = false;

    private double randomTargetX = 400;
    private double speedX = 3.0;
    private long lastChangeTime = 0;

    private long lastGrowTime = 0;
    private static final long GROW_INTERVAL = 10_000_000_000L; 

    private double targetY;
    private double targetX;

    public void setTargetY(double y) { this.targetY = y; }
    public void setTargetX(double x) { this.targetX = x; }

    private static final int NUM_NODES = 10;
    private static final double SPEED = 2;
    private final List<BossNode> nodes = new ArrayList<>();

    private final LinkedList<Double[]> positionBuffer = new LinkedList<>();
    private static final int BUFFER_OFFSET = 6;

    // --- 技能相關變數 ---
    private enum BossState { NORMAL, RETRACTING, SHOOTING }
    private BossState state = BossState.NORMAL;
    private long lastSkillTime = 0;
    private int shotsFired = 0;
    private long lastShotTime = 0;

    public LinkedListBoss(double x, double y) {
        super(x, y, 0, 0); 

        nodes.add(new BossNode(x, y, true));

        for (int i = 1; i < NUM_NODES; i++) {
            nodes.add(new BossNode(x, y + i * 25, false)); 
        }
    }

    private void grow() {
        BossNode tail = nodes.get(nodes.size() - 1);
        BossNode newNode = new BossNode(tail.x, tail.y, false);
        nodes.add(newNode);
        System.out.println("[DEBUG] Linked List 發生動態配置！目前長度：" + nodes.size());
    }

    @Override
    public void update(long now) {
        // 為了相容 GameObject 介面保留，實作改在多載的方法中
    }

    // 多載的 update，可接收 enemyBullets 和玩家座標
    public void update(long now, List<EnemyBullet> enemyBullets, double playerX, double playerY) {
        if (!isAlive) return;

        if (lastGrowTime == 0) lastGrowTime = now;
        if (lastSkillTime == 0) lastSkillTime = now; // 初始技能計時

        // 增長邏輯
        if (now - lastGrowTime > GROW_INTERVAL) {
            grow();
            lastGrowTime = now;
        }

        if (state == BossState.NORMAL) {
            // 每 20 秒觸發一次收尾巴技能
            if (now - lastSkillTime > 20_000_000_000L) {
                state = BossState.RETRACTING;
                System.out.println("[BOSS SKILL] LinkedList Worm 準備釋放擴散攻擊！");
                return; // 當下不處理其他移動
            }

            // --- 1. 不規則的隨機 X 軸竄動 ---
            if (lastChangeTime == 0) lastChangeTime = now;

            if (Math.abs(x - randomTargetX) < speedX || (now - lastChangeTime > 1_000_000_000L && Math.random() > 0.7)) {
                double canvasWidth = 771;
                double nodeWidth = 20;
                randomTargetX = Math.random() * (canvasWidth - nodeWidth);
                speedX = 2.0 + Math.random() * 6.0;
                lastChangeTime = now;
            }

            if (x < randomTargetX) {
                x += speedX;
            } else if (x > randomTargetX) {
                x -= speedX;
            }

            // --- 2. Y 軸跟蹤玩家 ---
            if (y < targetY) {
                y += SPEED * 0.3;
            } else if (y > targetY) {
                y -= SPEED * 0.3;
            }

            nodes.get(0).x = x;
            nodes.get(0).y = y;

            positionBuffer.addFirst(new Double[]{x, y});

            int maxBuffer = (nodes.size() - 1) * BUFFER_OFFSET + 1;
            while (positionBuffer.size() > maxBuffer) {
                positionBuffer.removeLast();
            }

            for (int i = 1; i < nodes.size(); i++) {
                BossNode node = nodes.get(i);
                int readIndex = i * BUFFER_OFFSET;

                if (readIndex < positionBuffer.size()) {
                    Double[] delayedCoords = positionBuffer.get(readIndex);
                    node.x = delayedCoords[0];
                    node.y = delayedCoords[1] + 15; 
                }
            }
        } else if (state == BossState.RETRACTING) {
            // 移動到畫面上方 (目標 X 移到畫面中間，Y 移到 80)
            double targetHx = 385;
            double targetHy = 80;
            
            double dx = targetHx - x;
            double dy = targetHy - y;

            // 頭部移動
            if (Math.abs(dx) > 4) x += Math.signum(dx) * 4; else x = targetHx;
            if (Math.abs(dy) > 4) y += Math.signum(dy) * 4; else y = targetHy;

            nodes.get(0).x = x;
            nodes.get(0).y = y;

            // 身體節點向頭部收縮
            boolean allRetracted = true;
            for (int i = 1; i < nodes.size(); i++) {
                BossNode node = nodes.get(i);
                double ndx = x - node.x;
                double ndy = y - node.y;
                
                if (Math.hypot(ndx, ndy) > 5) {
                    // 隨著距離加快收縮速度
                    node.x += ndx * 0.08 + Math.signum(ndx) * 2;
                    node.y += ndy * 0.08 + Math.signum(ndy) * 2;
                    allRetracted = false;
                } else {
                    node.x = x;
                    node.y = y;
                }
            }

            // 當頭部到達指定位置且所有節點都收縮完畢時
            if (x == targetHx && y == targetHy && allRetracted) {
                state = BossState.SHOOTING;
            }
        } else if (state == BossState.SHOOTING) {
            // 持續發射對應次數 (次數 = 節點數量)
            int totalShots = nodes.size();
            
            // 每 0.4 秒發射一次 (4億奈秒)
            if (now - lastShotTime > 400_000_000L) {
                // 放射發射「長度」數量的子彈
                int bulletCount = nodes.size();
                double angleStep = 360.0 / bulletCount; 
                
                // 讓每一波的起始角度稍微偏移，形成交錯的彈幕網
                double startAngle = shotsFired * (angleStep / 2.0);

                for (int i = 0; i < bulletCount; i++) {
                    double angle = startAngle + i * angleStep;
                    // 以頭部為中心發射
                    enemyBullets.add(new EnemyBullet(x + 10, y + 10, 4.5, angle));
                }
                
                shotsFired++;
                lastShotTime = now;
                
                System.out.println("[BOSS SKILL] 發射第 " + shotsFired + " / " + totalShots + " 波！");

                if (shotsFired >= totalShots) {
                    System.out.println("[BOSS SKILL] 技能結束，共發射 " + totalShots + " 波！");
                    shotsFired = 0; // 重置計數器

                    // 重置 Buffer，並填滿目前座標，讓節點從頭部重新長出，而不會亂跳
                    positionBuffer.clear();
                    for (int i = 0; i < (nodes.size() * BUFFER_OFFSET + 1); i++) {
                        positionBuffer.add(new Double[]{x, y});
                    }

                    // 恢復正常狀態並重置技能冷卻
                    state = BossState.NORMAL;
                    lastSkillTime = now;
                }
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (!isAlive) return;

        // 1. 繪製節點鏈結
        gc.setStroke(Color.web("#C586C0")); 
        gc.setLineWidth(1.5);
        for (int i = 0; i < nodes.size() - 1; i++) {
            BossNode current = nodes.get(i);
            BossNode next = nodes.get(i + 1);
            if (current.isAlive() && next.isAlive()) {
                gc.strokeLine(current.x + 10, current.y + 10, next.x + 10, next.y + 10);
            }
        }

        // 2. 繪製所有節點
        for (BossNode node : nodes) {
            node.draw(gc);
        }

        // 3. 如果在收縮/發射狀態，繪製一顆包圍頭部的能量球
        if (state != BossState.NORMAL) {
            double maxRadius = 20 + nodes.size() * 1.5;
            
            // 計算收縮進度
            int retractedCount = 0;
            for (BossNode n : nodes) {
                if (Math.hypot(x - n.x, y - n.y) < 10) retractedCount++;
            }
            double progress = (double) retractedCount / nodes.size();
            
            double currentRadius = maxRadius * progress;
            
            if (progress > 0.1) {
                gc.setFill(Color.web("#C586C0", 0.6 * progress));
                gc.fillOval(x + 10 - currentRadius, y + 10 - currentRadius, currentRadius * 2, currentRadius * 2);
                
                gc.setStroke(Color.web("#FF00FF", progress));
                gc.setLineWidth(2 + 2 * progress);
                gc.strokeOval(x + 10 - currentRadius, y + 10 - currentRadius, currentRadius * 2, currentRadius * 2);
            }
            
            if (progress > 0.8) {
                gc.setFill(Color.WHITE);
                gc.fillText("CHARGING", x - 15, y - currentRadius - 10);
            }
        }
    }

    public boolean hit(GameObject bullet) {
        if (!isAlive || bossHp <= 0) return false;

        // 技能期間的能量球 hitbox
        if (state != BossState.NORMAL) {
            double radius = 20 + nodes.size() * 1.5;
            double bx = bullet.x + bullet.width/2;
            double by = bullet.y + bullet.height/2;
            if (Math.hypot((x + 10) - bx, (y + 10) - by) < radius) {
                bossHp--;
                bullet.setAlive(false);
                if (bossHp <= 0) isAlive = false;
                return true;
            }
            return false;
        }

        BossNode headNode = nodes.get(0);
        if (headNode.collidesWith(bullet)) {
            bossHp--;
            bullet.setAlive(false); 

            if (bossHp <= 0) {
                isAlive = false;
                return true; 
            }
        }
        return false; 
    }

    public boolean isHittingPlayer(GameObject player) {
        if (!isAlive) return false;

        // 技能期間的能量球 hitbox
        if (state != BossState.NORMAL) {
            double radius = 20 + nodes.size() * 1.5;
            double px = player.x + player.width/2;
            double py = player.y + player.height/2;
            return Math.hypot((x + 10) - px, (y + 10) - py) < radius;
        }

        for (BossNode node : nodes) {
            if (node.collidesWith(player)) {
                return true;
            }
        }
        return false;
    }

    public int getBossHp() { return bossHp; }
    public int getMaxHp() { return maxHp; }
}