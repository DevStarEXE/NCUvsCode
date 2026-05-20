package com.vscode.danmaku.core;

import javafx.scene.canvas.GraphicsContext;

/**
 * 遊戲中所有動態物件的基礎類別.
 */
public abstract class GameObject {
    public double x, y;
    public double width, height;
    protected boolean isAlive;

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isAlive = true;
    }

    public abstract void update(long now);
    public abstract void draw(GraphicsContext gc);

    // 基礎碰撞偵測 (使用 AABB 包圍盒)
    public boolean collidesWith(GameObject other) {
        return this.isAlive && other.isAlive &&
                this.x < other.x + other.width &&
                this.x + this.width > other.x &&
                this.y < other.y + other.height &&
                this.y + this.height > other.y;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }
}