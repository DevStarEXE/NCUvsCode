package com.vscode.danmaku.core.bosses;

import com.vscode.danmaku.core.EnemyBullet2; // 使用新型子彈
import com.vscode.danmaku.core.Bullet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;
import java.util.Random;

public class BinarySearchBoss {
    // Boss 位置與大小
    public double x = 370, y = 100;
    public double width = 60, height = 60;

    // Boss 血量與存活狀態
    private final int maxHp = (int)(300 * com.vscode.danmaku.core.GameManager.difficultyMultiplier);
    private int hp = maxHp;

    private boolean isAlive = true;

    // --- 移動控制變數 ---
    private double vx = 2.0;
    private double vy = 1.0;
    private final Random random = new Random();
    private long lastMoveChangeTime = 0; // 控制何時隨機改變移動方向

    // --- 攻擊控制變數 ---
    private long lastShootTime = 0;
    private final long ATTACK_INTERVAL = 5_000_000_000L; // 5秒 (單位：奈秒 nanoseconds)
    private int searchStep = 0; // 二元搜尋的計數器 (用於改變彈幕樣式)

    /**
     * Boss 的主更新邏輯
     * @param now 當前系統奈秒時間
     * @param enemyBullets2 遊戲主舞台管理 EnemyBullet2 的 List
     * @param playerX 玩家目前的 X 座標
     * @param playerY 玩家目前的 Y 座標
     */
    public void update(long now, List<EnemyBullet2> enemyBullets2, double playerX, double playerY, double stageWidth, double stageHeight) {
        if (!isAlive) return;

        // ==========================================
        // 1. 移動邏輯：在視窗上方 1/3 隨機左右上下移動
        // ==========================================
        x += vx;
        y += vy;

        // 計算視窗上方 1/3 的邊界上限 (假設視窗高為 stageHeight)
        double topBoundary = 30;
        double bottomBoundary = stageHeight / 3;
        double leftBoundary = 40;
        double rightBoundary = stageWidth - width - 40;

        // 碰壁反彈偵測
        if (x < leftBoundary || x > rightBoundary) {
            vx = -vx;
            x = Math.max(leftBoundary, Math.min(x, rightBoundary));
        }
        if (y < topBoundary || y > bottomBoundary) {
            vy = -vy;
            y = Math.max(topBoundary, Math.min(y, bottomBoundary));
        }

        // 每隔 1.5 秒，隨機微調一次速度向量，讓隨機移動顯得更自然滑順
        if (now - lastMoveChangeTime > 1_500_000_000L) {
            // 隨機產生 -2.5 到 2.5 之間的速度，但避開接近 0 的死角
            vx = (random.nextDouble() * 4.0 - 2.0) + (vx > 0 ? 0.5 : -0.5);
            vy = (random.nextDouble() * 2.0 - 1.0) + (vy > 0 ? 0.3 : -0.3);
            lastMoveChangeTime = now;
        }

        // ==========================================
        // 2. 攻擊邏輯：每 5 秒攻擊一次 (二元搜尋彈幕)
        // ==========================================
        if (lastShootTime == 0) lastShootTime = now;
        if (now - lastShootTime > ATTACK_INTERVAL) {
            executeBinarySearchAttack(enemyBullets2, playerX, playerY);
            lastShootTime = now;
            searchStep++;
        }
    }

    /**
     * 觸發二元搜尋主題彈幕
     */
    private void executeBinarySearchAttack(List<EnemyBullet2> enemyBullets2, double playerX, double playerY) {
        double centerX = x + width / 2;
        double centerY = y + height / 2;

        // 決定二元搜尋的切分次數 (模擬 Low, Mid, High 的概念)
        // searchStep % 3 會在 0, 1, 2 之間循環，象徵搜尋範圍逐步縮小
        int step = searchStep % 3;

        int bulletCount;
        int homingFrames;
        double spreadSpeed;

        if (step == 0) {
            // 【Mid 0 - 全範圍查找】：子彈數量多，散開範圍大，追蹤時間長 (大範圍鋪場)
            bulletCount = 24;
            spreadSpeed = 4.0;     // 散開速度快 = 範圍大 [cite: 2]
            homingFrames = 120;    // 追蹤 120 幀 (約 2 秒) [cite: 6]
            System.out.println("[Binary Search] Status: O(log N) - Initializing Full Array Search...");
        } else if (step == 1) {
            // 【Mid 1 - 範圍折半】：數量變少，擴散範圍減半，追蹤時間縮短
            bulletCount = 12;
            spreadSpeed = 2.5;     // 散開範圍中等 [cite: 2]
            homingFrames = 60;     // 追蹤 60 幀 (約 1 秒) [cite: 6]
            System.out.println("[Binary Search] Status: Midpoint Split - Target in range [Low, Mid]");
        } else {
            // 【Mid 2 - 精準鎖定】：數量最少，幾乎不擴散，極短時間追蹤後高速暴衝 (逼迫精準閃躲)
            bulletCount = 6;
            spreadSpeed = 1.2;     // 散開範圍極小 [cite: 2]
            homingFrames = 25;     // 追蹤 25 幀 (約 0.4 秒後硬直衝刺)
            System.out.println("[Binary Search] Status: Element Found! Executing precise lock-on.");
        }

        // 發射圓圈彈幕
        for (int i = 0; i < bulletCount; i++) {
            double angle = i * (360.0 / bulletCount);

            // 建立 EnemyBullet2 實例
            EnemyBullet2 bullet = new EnemyBullet2(
                    centerX, centerY,
                    2,            // baseSpeed: 子彈基本衝刺速度
                    angle,          // 初始擴散角度
                    spreadSpeed,    // 擴散速度 (變數控制範圍) [cite: 2]
                    35,             // spreadDuration: 固定擴散 35 幀才轉向
                    homingFrames    // homingDuration: 追蹤時間限制 (變數控制) [cite: 2]
            );

            enemyBullets2.add(bullet);
        }
    }

    /**
     * 受到玩家子彈攻擊時的判定
     */
    public boolean hit(Bullet playerBullet) {
        if (!isAlive) return false;

        if (playerBullet.x < x + width && playerBullet.x + playerBullet.width > x &&
                playerBullet.y < y + height && playerBullet.y + playerBullet.height > y) {
            hp -= playerBullet.damage; // 扣除玩家子彈攻擊力
            playerBullet.setAlive(false);
            if (hp <= 0) {
                isAlive = false;
                return true; // 代表 Boss 被消滅
            }
        }
        return false;
    }

    /**
     * 繪製 Boss 外觀
     */
    public void draw(GraphicsContext gc) {
        if (!isAlive) return;

        // 繪製藍綠色系的外觀 (充滿資工/演算法科技感)
        gc.setStroke(Color.web("#00FF66"));
        gc.setLineWidth(3);
        gc.strokeRect(x, y, width, height);

        // 填充深色內部
        gc.setFill(Color.web("#0D1117"));
        gc.fillRect(x + 2, y + 2, width - 4, height - 4);

        // 繪製二元搜尋折半的視覺裝飾線 (中間的 Mid 線)
        gc.setStroke(Color.web("#00AAFF", 0.6));
        gc.setLineWidth(1.5);
        gc.strokeLine(x + width / 2, y, x + width / 2, y + height);

        // 繪製 Boss 文字資訊
        gc.setFont(new Font("Monospaced", 12));
        gc.setFill(Color.web("#00FF66"));
        gc.fillText("BinarySearch_Boss", x - 25, y - 25);

        gc.setFont(new Font("Monospaced", 11));
        gc.setFill(Color.web("#8B949E"));
        gc.fillText("HP: " + hp, x + 5, y - 8);

        // 視覺化當前的搜尋狀態
        gc.setFont(new Font("Monospaced", 10));
        gc.setFill(Color.YELLOW);
        int step = searchStep % 3;
        gc.fillText("STEP: M" + step, x + 10, y + 35);
    }

    public boolean isAlive() { return isAlive; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
}