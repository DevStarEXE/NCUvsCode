package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Player extends GameObject {
    private static final double SPEED = 5.0;
    private final Map<String, Boolean> keysPressed = new HashMap<>();
    private long lastShotTime = 0;
    private static final long SHOT_DELAY = 100_000_000; // 0.1s

    private int fireMode = 0;
    private Color activeBuffColor = null;

    // ==========================================
    // 修改：血量、護盾、攻擊力系統
    // ==========================================
    private int maxHp = 5;
    private int hp = maxHp;
    private int maxShield = 3;
    private int shield = maxShield;
    private int attackPower = 1;

    // ==========================================
    // 修改：BOMB 充能系統 (更慢、上限 1)
    // ==========================================
    private int bombCount = 1;               // 一開始自帶 1 次大招
    private final int MAX_BOMBS = 1;         // 上限改為 1 顆
    private double currentCharge = 0.0;
    private final double MAX_CHARGE = 1000.0;

    public Player(double x, double y) {
        super(x, y, 16, 16);
    }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getShield() { return shield; }
    public int getMaxShield() { return maxShield; }
    public int getAttackPower() { return attackPower; }

    private long invincibilityEndTime = 0;

    public void takeDamage(int damage, long now) {
        if (now < invincibilityEndTime) return;

        if (shield > 0) {
            shield -= damage;
            if (shield < 0) {
                hp += shield; // shield 變成負數，扣除剩餘的血量
                shield = 0;
            }
        } else {
            hp -= damage;
        }
        if (hp <= 0) {
            setAlive(false);
        } else {
            invincibilityEndTime = now + 1_000_000_000L; // 1 second i-frames
        }
    }

    public boolean isInvincible(long now) {
        return now < invincibilityEndTime;
    }

    public void addCharge(double amount) {
        if (bombCount >= MAX_BOMBS) return; // 滿了就不再充能

        currentCharge += amount;
        if (currentCharge >= MAX_CHARGE) {
            bombCount++;
            currentCharge = 0; // 達到上限後進度歸零
            System.out.println("充能完畢！獲得 BOMB 技能。");
        }
    }

    public boolean useBomb() {
        if (bombCount > 0) {
            bombCount--;
            return true;
        }
        return false;
    }

    @Override
    public void update(long now) {
        double prevX = x;
        double prevY = y;

        if (keysPressed.getOrDefault("UP", false)) y -= SPEED;
        if (keysPressed.getOrDefault("DOWN", false)) y += SPEED;
        if (keysPressed.getOrDefault("LEFT", false)) x -= SPEED;
        if (keysPressed.getOrDefault("RIGHT", false)) x += SPEED;

        if (x < 0) x = 0;
        if (y < 0) y = 0;

        // --- 修改：大幅降低移動充能的速度 ---
        double dist = Math.hypot(x - prevX, y - prevY);
        if (dist > 0) {
            // 原本是 0.5，現在改為 0.15。大約需要一直移動 20 多秒才能滿一條
            addCharge(dist * 0.15);
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        Color drawColor = (activeBuffColor != null) ? activeBuffColor : Color.web("#007ACC");

        gc.setStroke(drawColor);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, width, height);

        gc.strokeLine(x, y + height / 2, x + width, y + height / 2);
        gc.strokeLine(x + width / 2, y, x + width / 2, y + height);
    }

    public void shoot(long now, List<Bullet> bullets) {
        if (now - lastShotTime > SHOT_DELAY) {
            double bulletX = x + width / 2;
            double bulletY = y;

            switch (fireMode) {
                case 0:
                    bullets.add(new Bullet(bulletX, bulletY, Bullet.Type.INT, attackPower));
                    break;
                case 1:
                    bullets.add(new Bullet(bulletX - 10, bulletY, Bullet.Type.INT, attackPower));
                    bullets.add(new Bullet(bulletX + 10, bulletY, Bullet.Type.INT, attackPower));
                    break;
                case 2:
                    bullets.add(new Bullet(bulletX - 16, bulletY, Bullet.Type.INT, attackPower));
                    bullets.add(new Bullet(bulletX, bulletY - 8, Bullet.Type.INT, attackPower));
                    bullets.add(new Bullet(bulletX + 16, bulletY, Bullet.Type.INT, attackPower));
                    break;
            }
            lastShotTime = now;
        }
    }

    public int getFireMode() { return this.fireMode; }
    public Color getActiveBuffColor() { return this.activeBuffColor; }
    public void setFireMode(int mode) { this.fireMode = mode; }
    public void setActiveBuffColor(Color color) { this.activeBuffColor = color; }

    public int getBombCount() { return bombCount; }
    public double getChargeProgress() { return currentCharge / MAX_CHARGE; }
    public int getMaxBombs() { return MAX_BOMBS; }

    public void setKeyPressed(String key, boolean pressed) {
        keysPressed.put(key, pressed);
    }
}