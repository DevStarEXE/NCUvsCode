package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class EnemyBullet2 {
    public double x, y;
    public double vx, vy;
    public double radius = 6.0;
    private boolean isAlive = true;

    // --- 行為狀態控制 ---
    private enum State { SPREADING, HOMING, STRAIGHT }
    private State currentState;

    // --- 外部控制變數 ---
    private final double baseSpeed;        // 基礎移動速度
    private final int spreadDuration;      // 階段 1：散開持續的幀數 (例如 30 幀約 0.5 秒)
    private final int homingDuration;      // 階段 2：追蹤持續的幀數 (由變數控制)

    // --- 內部計時器 ---
    private int frameCounter = 0;

    /**
     * EnemyBullet2 建構子
     *  startX 初始 X 座標 (Boss 位置)
     *  startY 初始 Y 座標 (Boss 位置)
     *  baseSpeed 子彈追蹤與直線衝刺時的基礎速度
     *  initialAngleDegree 初始發射角度 (角度制，例如 0 ~ 360)
     *  spreadSpeed 控制散開範圍的速度變數 (數值越大，擴散半徑越大)
     *  spreadDuration 散開持續時間 (單位：幀數)
     *  homingDuration 追蹤時間限制 (單位：幀數)
     */
    public EnemyBullet2(double startX, double startY, double baseSpeed, double initialAngleDegree,
                        double spreadSpeed, int spreadDuration, int homingDuration) {
        this.x = startX;
        this.y = startY;
        this.baseSpeed = baseSpeed;
        this.spreadDuration = spreadDuration;
        this.homingDuration = homingDuration;

        // 初始狀態為：擴散 (SPREADING)
        this.currentState = State.SPREADING;

        // 階段 1：根據傳入的擴散速度 (spreadSpeed) 計算初始向量
        double radian = Math.toRadians(initialAngleDegree);
        this.vx = Math.cos(radian) * spreadSpeed;
        this.vy = Math.sin(radian) * spreadSpeed;
    }

    /**
     * 更新子彈狀態 (需傳入 Player 當前的中心座標)
     * @param playerCenterX 玩家目前的中心點 X
     * @param playerCenterY 玩家目前的中心點 Y
     */
    public void update(double playerCenterX, double playerCenterY) {
        frameCounter++;

        switch (currentState) {
            case SPREADING:
                // 階段 1：向外散開
                if (frameCounter >= spreadDuration) {
                    // 擴散時間到，重設計時器並切換至追蹤階段
                    frameCounter = 0;
                    currentState = State.HOMING;
                }
                break;

            case HOMING:
                // 階段 2：即時修正方向，追蹤 Player 位置
                double dx = playerCenterX - this.x;
                double dy = playerCenterY - this.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance > 0) {
                    // 將向量單位化，並乘以基礎速度
                    this.vx = (dx / distance) * baseSpeed;
                    this.vy = (dy / distance) * baseSpeed;
                }

                if (frameCounter >= homingDuration) {
                    // 追蹤時間到，保持目前的 vx, vy 不變，切換至直線衝刺
                    currentState = State.STRAIGHT;
                }
                break;

            case STRAIGHT:
                // 階段 3：鎖定最後方向，直線移動 (不改變 vx, vy)
                break;
        }

        // 根據目前的向量更新位置
        x += vx;
        y += vy;
    }

    // 沿用原本 EnemyBullet 的 Canvas 繪圖方法
    public void draw(GraphicsContext gc) {
        // 可以改個顏色（例如亮橘色）來跟普通子彈做區隔
        gc.setFill(Color.web("#FF4500"));
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    // 沿用原本的 Getter / Setter 與碰撞偵測
    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { this.isAlive = alive; }

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