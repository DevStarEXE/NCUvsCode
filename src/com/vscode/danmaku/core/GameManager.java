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

    private final List<Bullet> playerBullets = new ArrayList<>();
    private AnimationTimer gameLoop;

    public static String selectedLevel = "BOSS";

    private LinkedListBoss linkedListBoss;
    private ForLoopBoss forLoopBoss;
    private final List<EnemyBullet> enemyBullets = new ArrayList<>();

    public GameManager(Canvas gameCanvas) {
        this.gameCanvas = gameCanvas;
        this.gc = gameCanvas.getGraphicsContext2D();

        // 初始化玩家位置
        this.player = new Player(380, 500);

        // 初始化遊戲核心引擎迴圈
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
        } else {
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

        // 1. 更新玩家位置與邊界安全限制
        player.update(now);
        if (player.x < 0) player.x = 0;
        if (player.x > cw - player.width) player.x = cw - player.width;
        if (player.y < 0) player.y = 0;
        if (player.y > ch - player.height) player.y = ch - player.height;

        // 2. 【全自動開火機制】不干擾躲避彈幕的手感
        player.shoot(now, playerBullets);

        // 3. 更新玩家發射的子彈與效能優化出界銷毀
        for (Bullet b : playerBullets) {
            b.update(now);
            if (b.y + b.height < 0 || b.y > ch || b.x < 0 || b.x > cw) {
                b.setAlive(false);
            }
        }

        // 4. 更新動態 BOSS 狀態與判定機制
        if (forLoopBoss != null && forLoopBoss.isAlive()) {
            // 傳入 player 的 X, Y，提供核心階段進行精準運算狙擊
            forLoopBoss.update(now, enemyBullets, player.x, player.y);

            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (forLoopBoss.hit(b)) {
                        score += 5000; // 破譯三層巢狀迴圈給予高分獎勵！
                        isVictory = true;
                    } else if (!b.isAlive()) {
                        score += 15;
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
                    }
                }
            }

            if (linkedListBoss.isHittingPlayer(player)) {
                player.setAlive(false);
                isGameOver = true;
            }
        }

        // 5. 更新敵方幾何彈幕
        for (EnemyBullet eb : enemyBullets) {
            eb.update();
            if (eb.x < -20 || eb.x > cw + 20 || eb.y < -20 || eb.y > ch + 20) {
                eb.setAlive(false);
            }
        }

        // 6. 敵方彈幕 vs 玩家碰撞判定
        for (EnemyBullet eb : enemyBullets) {
            if (eb.isAlive() && eb.collidesWithPlayer(player.x, player.y, player.width, player.height)) {
                player.setAlive(false);
                isGameOver = true;
                break;
            }
        }

        // 7. 記憶體資源清理
        playerBullets.removeIf(b -> !b.isAlive());
        enemyBullets.removeIf(eb -> !eb.isAlive());
    }

    private void draw(GraphicsContext gc) {
        // 暗色調 IDE 開發背景
        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // 1. 繪製當前關卡 BOSS 本體
        if (forLoopBoss != null) forLoopBoss.draw(gc);
        if (linkedListBoss != null) linkedListBoss.draw(gc);

        // 2. 頂部動態血條整合 UI
        drawBossHealthBar(gc);

        // 3. 繪製所有子彈與玩家物件
        for (Bullet b : playerBullets) { b.draw(gc); }
        for (EnemyBullet eb : enemyBullets) { eb.draw(gc); }
        if (player != null && player.isAlive()) { player.draw(gc); }

        // 4. 繪製計分板
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(new Font("Monospaced", 18));
        gc.fillText("SCORE: " + String.format("%05d", score), 20, 30);

        // 5. 繪製終局結算黑化遮罩
        if (isGameOver || isVictory) {
            gc.setFill(Color.web("#000000", 0.75));
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

    /**
     * 支援多層血量顯示的頂部血條 UI 核心
     */
    private void drawBossHealthBar(GraphicsContext gc) {
        double barWidth = 400;
        double barHeight = 14;
        double barX = (gameCanvas.getWidth() - barWidth) / 2;
        double barY = 25;

        if (forLoopBoss != null && forLoopBoss.isAlive()) {
            // 背景底槽
            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);

            // 動態依據目前殘留層級填滿血條顏色
            double hpRatio;
            if (forLoopBoss.isKAlive()) {
                hpRatio = (double) forLoopBoss.getHpK() / 100;
                gc.setFill(Color.web("#FF00FF")); // 外層 K 紫色血條
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.fillText("[COMPILING] NESTED_LOOP: Layer_K (Shielding)", barX, barY - 8);
            } else if (forLoopBoss.isJAlive()) {
                hpRatio = (double) forLoopBoss.getHpJ() / 100;
                gc.setFill(Color.web("#00FFFF")); // 中層 J 青藍色血條
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.fillText("[COMPILING] NESTED_LOOP: Layer_J (Warning)", barX, barY - 8);
            } else {
                hpRatio = (double) forLoopBoss.getHpI() / 120;
                gc.setFill(Color.web("#FF3333")); // 核心 I 致命紅血條
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.fillText("[OVERLOAD] NESTED_LOOP: Core_I (Critical)", barX, barY - 8);
            }

            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(barX, barY, barWidth, barHeight);
        }
        else if (linkedListBoss != null && linkedListBoss.isAlive()) {
            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);

            double hpRatio = (double) linkedListBoss.getBossHp() / linkedListBoss.getMaxHp();
            gc.setFill(Color.web("#F44336"));
            gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);

            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(barX, barY, barWidth, barHeight);

            gc.setFill(Color.web("#859900"));
            gc.setFont(new Font("Monospaced", 11));
            gc.fillText("[PROCESS] BOSS: LINKED_LIST_WORM", barX, barY - 8);
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