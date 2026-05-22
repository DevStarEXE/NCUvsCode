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

    // 碰撞偵測 (傳入玩家的座標與寬高)
    public boolean collidesWithPlayer(double px, double py, double pWidth, double pHeight) {
        // 使用圓形與矩形的碰撞檢查：找到矩形上最接近圓心的點
        double closestX = Math.max(px, Math.min(x, px + pWidth));
        double closestY = Math.max(py, Math.min(y, py + pHeight));

        double distanceX = x - closestX;
        double distanceY = y - closestY;

        double distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);
        return distanceSquared < (radius * radius);
    }
}