package com.vscode.danmaku.core;

import com.vscode.danmaku.core.bosses.BinarySearchBoss;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
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
import java.util.Random;

import com.vscode.danmaku.core.bosses.ForLoopBoss;
import com.vscode.danmaku.core.bosses.LinkedWorm.LinkedListBoss;

public class GameManager {
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private int score = 0;
    private final Canvas gameCanvas;
    private final GraphicsContext gc;
    private Player player;

    private final List<Bullet> playerBullets = new ArrayList<>();
    private AnimationTimer gameLoop;

    public static String selectedLevel = "BOSS";
    public static double difficultyMultiplier = 1.0;

    // level enemy
    private LinkedListBoss linkedListBoss;
    private ForLoopBoss forLoopBoss;
    private BinarySearchBoss binarySearchBoss;
    private final List<EnemyBullet> enemyBullets = new ArrayList<>();
    private final List<EnemyBullet2> enemyBullet2s = new ArrayList<>();

    private boolean isAutoShooting = true;

    private final List<GameItem> itemsOnMap = new ArrayList<>();
    private long lastItemSpawnTime = 0;
    private long powerUpEndTime = 0;
    private final Random random = new Random();

    public GameManager(Canvas gameCanvas) {
        this.gameCanvas = gameCanvas;
        this.gc = gameCanvas.getGraphicsContext2D();
        this.player = new Player(380, 500);

        this.gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                draw(gc);
            }
        };

        init();
    }

    private void init() {
        if (selectedLevel.equals("FOR LOOP")) {
            forLoopBoss = new ForLoopBoss();
            System.out.println("生成階層式 For Loop Boss");
        }
        else if (selectedLevel.equals("Binary")) {
            binarySearchBoss = new BinarySearchBoss();
            System.out.println("生成階層式 Binary Search Boss");
        }
        else {
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

        player.update(now);
        if (player.x < 0) player.x = 0;
        if (player.x > cw - player.width) player.x = cw - player.width;
        if (player.y < 0) player.y = 0;
        if (player.y > ch - player.height) player.y = ch - player.height;

        if (powerUpEndTime != 0 && now > powerUpEndTime) {
            player.setFireMode(0);
            player.setActiveBuffColor(null);
            powerUpEndTime = 0;
        }

        if (lastItemSpawnTime == 0) lastItemSpawnTime = now;
        if (now - lastItemSpawnTime > 10_000_000_000L) {
            double rx = 80 + random.nextDouble() * (cw - 160);
            double ry = 250 + random.nextDouble() * (ch - 400);
            int rType = random.nextInt(2) + 1;
            itemsOnMap.add(new GameItem(rx, ry, rType));
            lastItemSpawnTime = now;
        }

        for (int i = itemsOnMap.size() - 1; i >= 0; i--) {
            GameItem item = itemsOnMap.get(i);
            if (item.collidesWith(player)) {
                powerUpEndTime = now + 5_000_000_000L;
                if (item.type == 1) {
                    player.setFireMode(1);
                    player.setActiveBuffColor(Color.web("#FFD700"));
                } else {
                    player.setFireMode(2);
                    player.setActiveBuffColor(Color.web("#FF4500"));
                }
                itemsOnMap.remove(i);
                score += 100;
            }
        }

        if (isAutoShooting) {
            player.shoot(now, playerBullets);
        }

        for (Bullet b : playerBullets) {
            b.update(now);
            if (b.y + b.height < 0 || b.y > ch || b.x < 0 || b.x > cw) {
                b.setAlive(false);
            }
        }

        // --- 修改：大幅降低命中充能的速度 ---
        // boss & player update
        if (forLoopBoss != null && forLoopBoss.isAlive()) {
            forLoopBoss.update(now, enemyBullets, player.x, player.y);
            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (forLoopBoss.hit(b)) {
                        score += 5000;
                        isVictory = true;
                    } else if (!b.isAlive()) {
                        score += 15;
                        player.addCharge(5.0); // 原本 20，改為 5 (約需命中 200 發才能滿)
                    }
                }
            }
        }
        else if (linkedListBoss != null && linkedListBoss.isAlive()) {
            linkedListBoss.setTargetX(player.x);
            linkedListBoss.setTargetY(player.y);
            linkedListBoss.update(now);
            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (linkedListBoss.hit(b)) {
                        score += 1000;
                        isVictory = true;
                    } else if (!b.isAlive()) {
                        score += 10;
                        player.addCharge(5.0);
                    }
                }
            }
            if (linkedListBoss.isHittingPlayer(player)) {
                player.setAlive(false);
                isGameOver = true;
            }
        } else if (binarySearchBoss != null && binarySearchBoss.isAlive()) {
            double playerCX = player.x + player.width / 2;
            double playerCY = player.y + player.height / 2;

            // 更新 BinarySearchBoss
            binarySearchBoss.update(now, enemyBullet2s, playerCX, playerCY, cw, ch);

            // 遍歷並更新所有 EnemyBullet2
            for (int i = enemyBullet2s.size() - 1; i >= 0; i--) {
                EnemyBullet2 b = enemyBullet2s.get(i);
                b.update(playerCX, playerCY); // 傳入玩家中心進行追蹤

                // 畫出子彈
                b.draw(gc);

                // 邊界判定或碰撞判定...
                if (!b.isAlive() || b.x < 0 || b.x > cw || b.y < 0 || b.y > ch) {
                    enemyBullet2s.remove(i);
                }
            }

            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (binarySearchBoss.hit(b)) {
                        score += 5000;
                        isVictory = true;
                    } else if (!b.isAlive()) {
                        score += 15;
                        player.addCharge(5.0); // 原本 20，改為 5 (約需命中 200 發才能滿)
                    }
                }
            }
        }

        // enemybullt
        for (EnemyBullet eb : enemyBullets) {
            eb.update();
            if (eb.x < -20 || eb.x > cw + 20 || eb.y < -20 || eb.y > ch + 20) {
                eb.setAlive(false);
            }
        }

        for (EnemyBullet eb : enemyBullets) {
            if (eb.isAlive() && eb.collidesWithPlayer(player.x, player.y, player.width, player.height)) {
                player.setAlive(false);
                isGameOver = true;
                break;
            }
        }

        //enemybullet2
        for (EnemyBullet2 eb2 : enemyBullet2s) {
            double playerCenterX = player.getX() + player.width / 2;
            double playerCenterY = player.getY() + player.height / 2;
            eb2.update(playerCenterX, playerCenterY);
            if (eb2.x < -20 || eb2.x > cw + 20 || eb2.y < -20 || eb2.y > ch + 20) {
                eb2.setAlive(false);
            }
        }

        for (EnemyBullet2 eb2 : enemyBullet2s) {
            if (eb2.isAlive() && eb2.collidesWithPlayer(player.x, player.y, player.width, player.height)) {
                player.setAlive(false);
                isGameOver = true;
                break;
            }
        }

        // remove bullet
        playerBullets.removeIf(b -> !b.isAlive());
        enemyBullets.removeIf(eb -> !eb.isAlive());
        enemyBullet2s.removeIf(eb2 -> !eb2.isAlive());
    }

    private void draw(GraphicsContext gc) {
        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        if (forLoopBoss != null) forLoopBoss.draw(gc);
        if (linkedListBoss != null) linkedListBoss.draw(gc);
        if (binarySearchBoss != null) binarySearchBoss.draw(gc);

        drawBossHealthBar(gc);

        for (GameItem item : itemsOnMap) {
            item.draw(gc);
        }

        for (Bullet b : playerBullets) { b.draw(gc); }
        for (EnemyBullet eb : enemyBullets) { eb.draw(gc); }
        for (EnemyBullet2 eb2 : enemyBullet2s) { eb2.draw(gc); }
        if (player != null && player.isAlive()) { player.draw(gc); }

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(new Font("Monospaced", 18));
        gc.fillText("SCORE: " + String.format("%05d", score), 20, 30);

        // ==========================================
        // 修改：精簡版的左下角充能 UI (因為上限只有 1)
        // ==========================================
        double uiX = 20;
        double uiY = gameCanvas.getHeight() - 25;

        gc.setStroke(Color.web("#555555"));
        gc.setLineWidth(1.5);
        gc.strokeRect(uiX, uiY - 22, 160, 30);
        gc.setFill(Color.web("#222222"));
        gc.fillRect(uiX + 1, uiY - 21, 158, 28);

        if (player != null) {
            int bombs = player.getBombCount();
            double progress = player.getChargeProgress();

            if (bombs >= player.getMaxBombs()) {
                // 滿充能 (1次)，直接顯示 READY
                gc.setFill(Color.web("#FFD700"));
                gc.fillRect(uiX + 2, uiY - 20, 156, 26);

                gc.setFill(Color.BLACK);
                gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 12));
                gc.fillText("[C] BOMB READY!", uiX + 10, uiY - 2);
            } else {
                // 充能中 (0次)，顯示藍色條與百分比
                gc.setFill(Color.web("#007ACC"));
                gc.fillRect(uiX + 2, uiY - 20, 156 * progress, 26);

                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("Monospaced", 12));
                gc.fillText("[C] CHARGING " + (int)(progress * 100) + "%", uiX + 10, uiY - 2);
            }
        }

        double modeUiX = gameCanvas.getWidth() - 130;
        double modeUiY = gameCanvas.getHeight() - 25;

        gc.setFill(Color.web("#888888"));
        gc.setFont(new Font("Monospaced", 11));
        gc.fillText("WEAPON_WEAVE:", modeUiX, modeUiY - 2);

        int currentMode = (player != null) ? player.getFireMode() : 0;
        Color activeDotColor = (player != null && player.getActiveBuffColor() != null)
                ? player.getActiveBuffColor() : Color.web("#007ACC");

        double dotStartX = modeUiX + 90;
        double dotY = modeUiY - 10;

        for (int i = 0; i < 3; i++) {
            if (i == currentMode) {
                gc.setFill(activeDotColor);
            } else {
                gc.setFill(Color.web("#444444"));
            }
            gc.fillOval(dotStartX + (i * 12), dotY, 7, 7);
        }

        if (isGameOver || isVictory) {
            gc.setFill(Color.web("#000000", 0.75));
            gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
            gc.setFont(new Font("Monospaced", 60));
            double centerX = gameCanvas.getWidth() / 2;
            double centerY = gameCanvas.getHeight() / 2;
            if (isVictory) {
                gc.setFill(Color.web("#4CAF50"));
                gc.setEffect(new DropShadow(30, Color.rgb(76,175,80, 0.7)));
                gc.fillText("V I C T O R Y", centerX - 190, centerY - 20);
                gc.setEffect(null);
            } else if (isGameOver) {
                gc.setFill(Color.web("#F44336"));
                gc.setEffect(new DropShadow(30, Color.rgb(244, 67, 54, 0.7)));
                gc.fillText("G A M E  O V E R", centerX - 220, centerY - 20);
                gc.setEffect(null);
            }
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Monospaced", 20));
            gc.fillText("Press [ESC] to Return Title", centerX - 150, centerY + 50);
            gc.fillText("Final Score: " + score, centerX - 90, centerY + 90);
        }
    }

    private void drawBossHealthBar(GraphicsContext gc) {
        double barWidth = 400;
        double barHeight = 14;
        double barX = (gameCanvas.getWidth() - barWidth) / 2;
        double barY = 25;

        if (forLoopBoss != null && forLoopBoss.isAlive()) {
            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);
            double hpRatio;
            int currentHp, maxHp, percent;
            if (forLoopBoss.isKAlive()) {
                currentHp = forLoopBoss.getHpK(); maxHp = forLoopBoss.getMaxHpK();
                hpRatio = (double) currentHp / maxHp; percent = (int)(hpRatio * 100);
                gc.setFill(Color.web("#FF00FF"));
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.fillText(String.format("[COMPILING] NESTED_LOOP: Layer_K (Shielding)  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
            } else if (forLoopBoss.isJAlive()) {
                currentHp = forLoopBoss.getHpJ(); maxHp = forLoopBoss.getMaxHpJ();
                hpRatio = (double) currentHp / maxHp; percent = (int)(hpRatio * 100);
                gc.setFill(Color.web("#00FFFF"));
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.fillText(String.format("[COMPILING] NESTED_LOOP: Layer_J (Warning)  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
            } else {
                currentHp = forLoopBoss.getHpI(); maxHp = forLoopBoss.getMaxHpI();
                hpRatio = (double) currentHp / maxHp; percent = (int)(hpRatio * 100);
                gc.setFill(Color.web("#FF3333"));
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.fillText(String.format("[OVERLOAD] NESTED_LOOP: Core_I (Critical)  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
            }
            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(barX, barY, barWidth, barHeight);
        }
        else if (linkedListBoss != null && linkedListBoss.isAlive()) {
            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);
            int currentHp = linkedListBoss.getBossHp();
            int maxHp = linkedListBoss.getMaxHp();
            double hpRatio = (double) currentHp / maxHp;
            int percent = (int)(hpRatio * 100);
            gc.setFill(Color.web("#F44336"));
            gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(barX, barY, barWidth, barHeight);
            gc.setFill(Color.web("#859900"));
            gc.setFont(new Font("Monospaced", 11));
            gc.fillText(String.format("[PROCESS] BOSS: LINKED_LIST_WORM  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
        }
        else if (binarySearchBoss != null && binarySearchBoss.isAlive())
        {
            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);
            int currentHp = binarySearchBoss.getHp();
            int maxHp = binarySearchBoss.getMaxHp();
            double hpRatio = (double) currentHp / maxHp;
            int percent = (int)(hpRatio * 100);
            gc.setFill(Color.web("#F44336"));
            gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(barX, barY, barWidth, barHeight);
            gc.setFill(Color.web("#859900"));
            gc.setFont(new Font("Monospaced", 11));
            gc.fillText(String.format("[PROCESS] BOSS: BINARY SEARCH  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
        }
    }

    public void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> player.setKeyPressed("UP", true);
            case W -> player.setKeyPressed("UP", true);
            case DOWN -> player.setKeyPressed("DOWN", true);
            case S -> player.setKeyPressed("DOWN", true);
            case LEFT -> player.setKeyPressed("LEFT", true);
            case A -> player.setKeyPressed("LEFT", true);
            case RIGHT -> player.setKeyPressed("RIGHT", true);
            case D -> player.setKeyPressed("RIGHT", true);
        }
        if (event.getCode() == KeyCode.ESCAPE && (isGameOver || isVictory)) {
            returnToMenu();
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> player.setKeyPressed("UP", false);
            case W -> player.setKeyPressed("UP", false);
            case DOWN -> player.setKeyPressed("DOWN", false);
            case S -> player.setKeyPressed("DOWN", false);
            case LEFT -> player.setKeyPressed("LEFT", false);
            case A -> player.setKeyPressed("LEFT", false);
            case RIGHT -> player.setKeyPressed("RIGHT", false);
            case D -> player.setKeyPressed("RIGHT", false);

            case SPACE -> {
                isAutoShooting = !isAutoShooting;
                System.out.println("自動射擊狀態：" + (isAutoShooting ? "開啟" : "關閉"));
            }

            case C -> {
                if (player != null && player.useBomb()) {
                    System.out.println("[核心呼叫] 消耗 1 次充能，執行 BOMB.EXE：清空全螢幕敵方子彈！");
                    enemyBullets.clear();
                } else {
                    System.out.println("[警告] 充能不足，無法執行 BOMB.EXE！");
                }
            }
        }
    }

    private void returnToMenu() {
        try {
            stop();
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/resource/fxml/menu-view.fxml"));
            Scene menuScene = new Scene(fxmlLoader.load(), 800, 600);
            stage.setScene(menuScene);
            stage.setTitle("遊戲主選單");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}