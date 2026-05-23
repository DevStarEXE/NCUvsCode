package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class EnemyBullet3 {
    public double x, y;
    public double vx, vy;

    // --- 修改：將單一半徑 radius 改為橢圓的半寬度與半高度 ---
    public double halfWidth = 10.0;  // 橢圓形的左右半長（控制子彈長度）
    public double halfHeight = 5.0;  // 橢圓形的上下半長（控制子彈厚度）

    private boolean isAlive = true;

    // 發射時間間隔延遲計時器
    private int delayFrames;
    private boolean isActivated = false;

    /**
     * EnemyBullet3 建構子
     * @param startX 初始 X 座標
     * @param startY 初始 Y 座標
     * @param speed 子彈的移動速度 (正數)
     * @param isFromLeft true 代表從左向右跑；false 代表從右向左跑
     * @param delayFrames 這顆子彈生成後，要等待多少幀才開始移動
     */
    public EnemyBullet3(double startX, double startY, double speed, boolean isFromLeft, int delayFrames) {
        this.x = startX;
        this.y = startY;
        this.vy = 0;
        this.delayFrames = delayFrames;

        if (isFromLeft) {
            this.vx = speed;
        } else {
            this.vx = -speed;
        }

        if (this.delayFrames <= 0) {
            this.isActivated = true;
        }
    }

    public void update() {
        if (!isAlive) return;

        if (!isActivated) {
            delayFrames--;
            if (delayFrames <= 0) {
                isActivated = true;
            }
            return;
        }

        x += vx;
        y += vy;
    }

    public void draw(GraphicsContext gc) {
        if (!isActivated || !isAlive) return;

        // 設定夾擊子彈的亮青色
        gc.setFill(Color.web("#00FFFF"));

        // --- 修改：繪製橢圓形 ---
        // fillOval 的參數格式為：(左上角X, 左上角Y, 總寬度, 總高度)
        // 透過將中心點 (x, y) 減去半寬高，並帶入 (halfWidth * 2) 與 (halfHeight * 2) 即可畫出以 (x,y) 為中心的橢圓
        gc.fillOval(x - halfWidth, y - halfHeight, halfWidth * 2, halfHeight * 2);
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    /**
     * 碰撞偵測（針對橢圓形特性的寬容度判定）
     */
    public boolean collidesWithPlayer(double px, double py, double pWidth, double pHeight) {
        if (!isActivated) return false;

        double pCenterX = px + pWidth / 2;
        double pCenterY = py + pHeight / 2;

        // 由於子彈變成扁平的橫向橢圓，使用傳統圓形碰撞判定會使上下空隙判定過大。
        // 這裡採用「AABB 矩形邊界」搭配「碰撞寬容度調整（-3像素）」的做法，最適合這種高速橫向洗板的子彈
        double bulletLeft = x - halfWidth + 3;
        double bulletRight = x + halfWidth - 3;
        double bulletTop = y - halfHeight + 2;
        double bulletBottom = y + halfHeight - 2;

        double playerLeft = px + 4;
        double playerRight = px + pWidth - 4;
        double playerTop = py + 4;
        double playerBottom = py + pHeight - 4;

        return bulletLeft < playerRight && bulletRight > playerLeft &&
                bulletTop < playerBottom && bulletBottom > playerTop;
    }
}
