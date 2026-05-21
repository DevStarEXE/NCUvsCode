package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class EnemyBullet {
    public double x, y;
    public double vx, vy;
    public double radius = 6.0;
    private boolean isAlive = true;

    // 傳入發射座標、速度與角度 (利用三角函數計算 X/Y 分量)
    public EnemyBullet(double startX, double startY, double speed, double angleDegree) {
        this.x = startX;
        this.y = startY;
        // 將角度轉換為弧度，計算 X 和 Y 方向的速度
        double radian = Math.toRadians(angleDegree);
        this.vx = Math.cos(radian) * speed;
        this.vy = Math.sin(radian) * speed;
    }

    public void update() {
        x += vx;
        y += vy;
    }

    public void draw(GraphicsContext gc) {
        gc.setFill(Color.web("#FF00FF")); // 亮紫色，帶有駭客/錯誤代碼的感覺
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { this.isAlive = alive; }

    // 簡單的圓形碰撞偵測 (傳入玩家的中心點與半徑)
    public boolean collidesWithPlayer(double px, double py, double pWidth, double pHeight) {
        double pCenterX = px + pWidth / 2;
        double pCenterY = py + pHeight / 2;
        double distance = Math.sqrt(Math.pow(x - pCenterX, 2) + Math.pow(y - pCenterY, 2));
        return distance < (radius + pWidth / 2 - 5); // -5 是給玩家一點碰撞寬容度 (Hitbox 變小)
    }
}