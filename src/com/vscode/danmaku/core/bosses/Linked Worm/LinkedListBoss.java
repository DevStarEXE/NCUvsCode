package com.vscode.danmaku.core.bosses.LinkedWorm;

import com.vscode.danmaku.core.GameObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LinkedListBoss extends GameObject {
    private static final int NUM_NODES = 10;
    private static final double SPEED = 3.0;
    private final List<BossNode> nodes = new ArrayList<>();

    // 座標緩衝 (Position Buffer)：核心移動算法
    // 儲存頭部移動路徑的座標隊列.
    private final LinkedList<Double[]> positionBuffer = new LinkedList<>();
    // 每個身體節點讀取緩衝中落後於 Head 的幀數.
    // 依序遞增以產生平滑擺動.
    private static final int BUFFER_OFFSET = 6;

    private int bossHp = 50; // 總血量 (只有 Head 受傷)

    public LinkedListBoss(double x, double y) {
        super(x, y, 0, 0); // 本身尺寸為0，完全依賴節點

        // 初始化 Head Node
        nodes.add(new BossNode(x, y, true));

        // 初始化身體節點
        for (int i = 1; i < NUM_NODES; i++) {
            nodes.add(new BossNode(x, y + i * 25, false)); // 依序排在後面
        }
    }

    @Override
    public void update(long now) {
        // --- 1. 頭部自動移動 ---
        double sinVal = Math.sin(now * 0.00000001); // 簡單的波浪運動
        x = 400 + 200 * sinVal; // 在 X 軸上擺動
        y += SPEED * 0.2; // 緩慢向下

        // 更新 Head Node 的實體座標
        nodes.get(0).x = x;
        nodes.get(0).y = y;

        // --- 2. 更新座標緩衝 ---
        // 將 Head 的最新座標 Push 進緩衝 (存儲為 [x, y])
        positionBuffer.addFirst(new Double[]{x, y});

        // 確保緩衝大小足夠所有身體節點讀取，移除過時座標
        int maxBuffer = (nodes.size() - 1) * BUFFER_OFFSET + 1;
        while (positionBuffer.size() > maxBuffer) {
            positionBuffer.removeLast();
        }

        // --- 3. 身體節點跟隨頭部 ---
        for (int i = 1; i < nodes.size(); i++) {
            BossNode node = nodes.get(i);
            // 讀取緩衝中落後特定幀數的座標
            int readIndex = i * BUFFER_OFFSET;

            // 確保讀取位置有效
            if (readIndex < positionBuffer.size()) {
                Double[] delayedCoords = positionBuffer.get(readIndex);
                // 更新身體節點實體座標，產生滑順的蠕蟲效果
                node.x = delayedCoords[0];
                node.y = delayedCoords[1] + 15; // 稍微偏移一點
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (!isAlive) return;

        // 1. 繪製節點鏈結 (指標箭頭)
        gc.setStroke(Color.web("#C586C0")); // Pointer Pink
        gc.setLineWidth(1.5);
        for (int i = 0; i < nodes.size() - 1; i++) {
            BossNode current = nodes.get(i);
            BossNode next = nodes.get(i + 1);
            if (current.isAlive() && next.isAlive()) {
                // 從 current 畫線到 next
                gc.strokeLine(current.getX() + 10, current.getY() + 10,
                        next.getX() + 10, next.getY() + 10);
            }
        }

        // 2. 繪製所有節點
        for (BossNode node : nodes) {
            node.draw(gc);
        }
    }

    // 碰撞偵測子彈 hit 判定：只對 Head 節點有效
    public void hit(GameObject bullet) {
        if (!isAlive || bossHp <= 0) return;

        BossNode headNode = nodes.get(0);
        if (headNode.collidesWith(bullet)) {
            // Head 受傷，總血量減少
            bossHp--;
            bullet.setAlive(false); // 子彈銷毀

            if (bossHp <= 0) {
                // BOSS 毀滅，蟲體逐一引爆 (Demo 省略動畫，直接銷毀)
                isAlive = false;
                System.out.println("DEBUG: LinkedList Boss destroyed!");
            } else {
                System.out.println("DEBUG: Boss Head hit! HP: " + bossHp);
            }
        }
    }
}