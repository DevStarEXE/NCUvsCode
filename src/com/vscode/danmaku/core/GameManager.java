package com.vscode.danmaku.core;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

import com.vscode.danmaku.core.bosses.ForLoopBoss;
import com.vscode.danmaku.core.bosses.LinkedWorm.LinkedListBoss;

public class GameManager {
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private int score = 0;
    private final Canvas gameCanvas;
    private final GraphicsContext gc;
    private Player player;



    // --- 修正 1：移除多餘的 boss 變數，統一使用下面宣告的兩隻王 ---
    private final List<Bullet> playerBullets = new ArrayList<>();
    private AnimationTimer gameLoop;

    public static String selectedLevel = "BOSS";

    private LinkedListBoss linkedListBoss;
    private ForLoopBoss forLoopBoss; // 只保留這個宣告

    private List<EnemyBullet> enemyBullets = new ArrayList<>();

    public GameManager(Canvas gameCanvas) {
        this.gameCanvas = gameCanvas;
        this.gc = gameCanvas.getGraphicsContext2D();

        // --- 補回遺失的玩家初始化 (假設你的 Player 需要座標) ---
        this.player = new Player(400, 500);

        // --- 補回遺失的遊戲迴圈 (AnimationTimer) ---
        this.gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now); // 每幀更新邏輯
                draw(gc);    // 每幀重新繪圖
            }
        };

        init();
    }

    private void init() {
        if (selectedLevel.equals("FOR LOOP")) {
            forLoopBoss = new ForLoopBoss();
            System.out.println("生成 For Loop Boss");
        } else {
            // 加上 X 和 Y 座標 (例如 X=400, Y=100)
            linkedListBoss = new LinkedListBoss(400.0, 100.0);
            System.out.println("生成 LinkedList Boss");
        }
    }

    public void start() { gameLoop.start(); }
    public void stop() { gameLoop.stop(); }

    private void update(long now) {
        if (isGameOver || isVictory) return;

        double cw = gameCanvas.getWidth();
        double ch = gameCanvas.getHeight();

        // 1. 更新玩家與邊界限制
        player.update(now);
        if (player.x < 0) player.x = 0;
        if (player.x > cw - player.width) player.x = cw - player.width;
        if (player.y < 0) player.y = 0;
        if (player.y > ch - player.height) player.y = ch - player.height;

        if (isSpacePressed()) {
            player.shoot(now, playerBullets);
        }

        // 2. 更新玩家子彈與出界銷毀
        for (Bullet b : playerBullets) {
            b.update(now);
            if (b.y + b.height < 0 || b.y > ch || b.x < 0 || b.x > cw) {
                b.setAlive(false);
            }
        }

        // --- 修正 2：統一處理 BOSS 的更新與碰撞邏輯 ---
        if (forLoopBoss != null && forLoopBoss.isAlive()) {
            forLoopBoss.update(now, enemyBullets);

            // 判定玩家子彈打 For Loop
            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (forLoopBoss.hit(b)) {
                        score += 2000;
                        isVictory = true;
                        System.out.println("For Loop BOSS 擊破！");
                    } else if (!b.isAlive()) { // 如果子彈死亡代表有打中但王還沒死
                        score += 10;
                    }
                }
            }
        }
        else if (linkedListBoss != null && linkedListBoss.isAlive()) {
            linkedListBoss.setTargetX(player.x);
            linkedListBoss.setTargetY(player.y);
            linkedListBoss.update(now);

            // 判定玩家子彈打蠕蟲
            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (linkedListBoss.hit(b)) {
                        score += 1000;
                        isVictory = true;
                    } else if (!b.isAlive()) {
                        score += 10;
                    }
                }
            }

            // 判定蠕蟲撞玩家
            if (linkedListBoss.isHittingPlayer(player)) {
                player.setAlive(false);
                isGameOver = true;
            }
        }

        // 3. 更新敵方子彈與出界處理 (For Loop 專用)
        for (EnemyBullet eb : enemyBullets) {
            eb.update();
            if (eb.x < 0 || eb.x > cw || eb.y < 0 || eb.y > ch) {
                eb.setAlive(false);
            }
        }

        // 4. 碰撞偵測 (敵方子彈 vs 玩家)
        for (EnemyBullet eb : enemyBullets) {
            if (eb.isAlive() && eb.collidesWithPlayer(player.x, player.y, player.width, player.height)) {
                player.setAlive(false);
                isGameOver = true;
                break;
            }
        }

        // 5. 清理已銷毀物件
        playerBullets.removeIf(b -> !b.isAlive());
        enemyBullets.removeIf(eb -> !eb.isAlive());
    }

    private void draw(GraphicsContext gc) {
        // 1. 清空畫布
        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // --- 修正 3：動態取得當前存活的 BOSS 狀態來畫血條 ---
        int currentHp = 0;
        int maxHp = 1;
        String bossName = "";
        boolean isBossAlive = false;

        if (forLoopBoss != null && forLoopBoss.isAlive()) {
            forLoopBoss.draw(gc);
            currentHp = forLoopBoss.getHp();
            maxHp = forLoopBoss.getMaxHp();
            bossName = "FOR_LOOP_GEOMETRY";
            isBossAlive = true;
        } else if (linkedListBoss != null && linkedListBoss.isAlive()) {
            linkedListBoss.draw(gc);
            currentHp = linkedListBoss.getBossHp();
            maxHp = linkedListBoss.getMaxHp();
            bossName = "LINKED_LIST_WORM";
            isBossAlive = true;
        }

        // 2. 繪製 BOSS 血條
        if (isBossAlive) {
            double barWidth = 400;
            double barHeight = 14;
            double cw = gameCanvas.getWidth();
            double barX = (cw - barWidth) / 2;
            double barY = 20;

            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);

            double hpRatio = (double) currentHp / maxHp;
            double currentBarWidth = barWidth * hpRatio;

            gc.setFill(Color.web("#F44336"));
            gc.fillRect(barX, barY, currentBarWidth, barHeight);

            gc.setStroke(Color.web("#888888"));
            gc.setLineWidth(1.5);
            gc.strokeRect(barX, barY, barWidth, barHeight);

            gc.setFill(Color.web("#859900"));
            gc.setFont(new Font("Monospaced", 11));
            gc.fillText("[PROCESS] BOSS: " + bossName, barX, barY - 6);
        }

        // 3. 繪製子彈與玩家
        for (Bullet b : playerBullets) { b.draw(gc); }
        for (EnemyBullet eb : enemyBullets) { eb.draw(gc); }
        if (player != null) { player.draw(gc); }

        // 4. 繪製 UI 分數
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(new Font("Monospaced", 18));
        gc.fillText("SCORE: " + String.format("%05d", score), 20, 30);

        // 5. 繪製結算畫面
        if (isGameOver || isVictory) {
            gc.setFill(Color.web("#000000", 0.7));
            gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

            gc.setFont(new Font("Monospaced", 60));
            double centerX = gameCanvas.getWidth() / 2;
            double centerY = gameCanvas.getHeight() / 2;

            if (isVictory) {
                gc.setFill(Color.web("#4CAF50"));
                gc.fillText("V I C T O R Y", centerX - 190, centerY - 20);
            } else if (isGameOver) {
                gc.setFill(Color.web("#F44336"));
                gc.fillText("G A M E  O V E R", centerX - 220, centerY - 20);
            }

            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Monospaced", 20));
            gc.fillText("Press [ESC] to Return Title", centerX - 150, centerY + 50);
            gc.fillText("Final Score: " + score, centerX - 90, centerY + 90);
        }
    }

    public void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> player.setKeyPressed("UP", true);
            case DOWN -> player.setKeyPressed("DOWN", true);
            case LEFT -> player.setKeyPressed("LEFT", true);
            case RIGHT -> player.setKeyPressed("RIGHT", true);
            case SPACE -> setSpacePressed(true);
        }
        if (event.getCode() == KeyCode.ESCAPE && (isGameOver || isVictory)) {
            returnToMenu();
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> player.setKeyPressed("UP", false);
            case DOWN -> player.setKeyPressed("DOWN", false);
            case LEFT -> player.setKeyPressed("LEFT", false);
            case RIGHT -> player.setKeyPressed("RIGHT", false);
            case SPACE -> setSpacePressed(false);
        }
    }

    private boolean spacePressed = false;
    public boolean isSpacePressed() { return spacePressed; }
    public void setSpacePressed(boolean spacePressed) { this.spacePressed = spacePressed; }

    private void returnToMenu() {
        try {
            stop();
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/resource/fxml/menu-view.fxml"));
            Scene menuScene = new Scene(fxmlLoader.load(), 800, 600);
            stage.setScene(menuScene);
            stage.setTitle("遊戲主選單");
            System.out.println("已返回主選單");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("返回主選單失敗！請檢查路徑。");
        }
    }
}