package com.vscode.danmaku.core.bosses.LinkedWorm;

import com.vscode.danmaku.core.GameObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BossNode extends GameObject {
    private boolean isHead;

    public BossNode(double x, double y, boolean isHead) {
        super(x, y, 20, 20); // 節點尺寸 20x20
        this.isHead = isHead;
    }

    @Override
    public void update(long now) {
        // 節點本身不控制移動，由 LinkedListBoss 更新座標
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (!isAlive) return;

        if (isHead) {
            gc.setFill(Color.RED); // 頭部為紅
            gc.setStroke(Color.WHITE);
        } else {
            gc.setFill(Color.DARKGRAY); // 身體為灰
            gc.setStroke(Color.LIGHTGRAY);
        }

        gc.setLineWidth(2);
        gc.fillOval(x, y, width, height); // 畫圓球節點
        gc.strokeOval(x, y, width, height);
    }

    public boolean isHead() { return isHead; }
}