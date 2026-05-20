package com.vscode.danmaku.core.bosses.LinkedWorm;

import com.vscode.danmaku.core.GameObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;



public class LinkedListBoss extends GameObject {
    // 新增：用來記錄玩家的 Y 座標
    private final int maxHp = 50;
    private int bossHp = maxHp;

    private boolean isGameOver = false;
    private boolean isVictory = false;

    // --- 新增：隨機竄動控制變數 ---
    private double randomTargetX = 400; // 蟲蟲當前想去的隨機 X 座標
    private double speedX = 3.0;        // 當前的橫向移動速度 (會忽快忽慢)
    private long lastChangeTime = 0;    // 上次改變主意的時間

    private long lastGrowTime = 0;
    private static final long GROW_INTERVAL = 10_000_000_000L; // 10秒 = 100億奈秒

    // 記錄玩家的目標座標
    private double targetY;
    private double targetX; // 新增這行

    public void setTargetY(double y) { this.targetY = y; }
    public void setTargetX(double x) { this.targetX = x; } // 新增這個 Setter

    private static final int NUM_NODES = 10;
    private static final double SPEED = 2;
    private final List<BossNode> nodes = new ArrayList<>();

    // 座標緩衝 (Position Buffer)：核心移動算法
    // 儲存頭部移動路徑的座標隊列.
    private final LinkedList<Double[]> positionBuffer = new LinkedList<>();
    // 每個身體節點讀取緩衝中落後於 Head 的幀數.
    // 依序遞增以產生平滑擺動.
    private static final int BUFFER_OFFSET = 6;

    public LinkedListBoss(double x, double y) {
        super(x, y, 0, 0); // 本身尺寸為0，完全依賴節點

        // 初始化 Head Node
        nodes.add(new BossNode(x, y, true));

        // 初始化身體節點
        for (int i = 1; i < NUM_NODES; i++) {
            nodes.add(new BossNode(x, y + i * 25, false)); // 依序排在後面
        }
    }

    // 新增：讓蟲蟲長大一節的方法
    private void grow() {
        // 取得目前的尾巴節點
        BossNode tail = nodes.get(nodes.size() - 1);

        // 建立新節點，初始位置和當前的尾巴重疊 (如果你之前是用 setX/setY，這裡請改成 tail.getX(), tail.getY())
        BossNode newNode = new BossNode(tail.x, tail.y, false);

        // 將新節點掛載到 Linked List 的最後面
        nodes.add(newNode);

        System.out.println("[DEBUG] Linked List 發生動態配置！目前長度：" + nodes.size());
    }

    @Override
    public void update(long now) {
        if (!isAlive) return;

        // --- 計時器與動態增長邏輯 (保持不變) ---
        if (lastGrowTime == 0) lastGrowTime = now;
        if (now - lastGrowTime > GROW_INTERVAL) {
            grow();
            lastGrowTime = now;
        }

        // --- 1. 不規則的隨機 X 軸竄動 ---
        if (lastChangeTime == 0) lastChangeTime = now;

        // 觸發條件：如果已經到達隨機目標點，或者過了1秒且觸發30%的機率(模擬蟲子突然改變主意)
        if (Math.abs(x - randomTargetX) < speedX || (now - lastChangeTime > 1_000_000_000L && Math.random() > 0.7)) {

            double canvasWidth = 771;
            double nodeWidth = 20;

            // 在畫布範圍內，隨機挑選一個新的 X 座標作為目標
            randomTargetX = Math.random() * (canvasWidth - nodeWidth);

            // 隨機改變移動速度 (模擬忽快忽慢的暴衝，速度介於 2.0 到 8.0 之間)
            speedX = 2.0 + Math.random() * 6.0;

            lastChangeTime = now; // 重置改變主意的時間
        }

        // 朝著隨機目標點移動
        if (x < randomTargetX) {
            x += speedX;
        } else if (x > randomTargetX) {
            x -= speedX;
        }

        // --- 2. Y 軸跟蹤玩家 (保持不變) ---
        if (y < targetY) {
            y += SPEED * 0.3;
        } else if (y > targetY) {
            y -= SPEED * 0.3;
        }

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
    public boolean hit(GameObject bullet) {
        if (!isAlive || bossHp <= 0) return false;

        BossNode headNode = nodes.get(0);
        if (headNode.collidesWith(bullet)) {
            bossHp--;
            bullet.setAlive(false); // 子彈銷毀

            if (bossHp <= 0) {
                isAlive = false;
                return true; // 回傳 true，代表造成擊殺
            }
        }
        return false; // 只是擊中，還沒死
    }
    public boolean isHittingPlayer(GameObject player) {
        if (!isAlive) return false;

        // 檢查玩家是否碰到蟲蟲的「任何一個節點」
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

