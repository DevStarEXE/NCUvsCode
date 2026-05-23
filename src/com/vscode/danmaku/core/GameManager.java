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
import java.util.Random;

import com.vscode.danmaku.core.bosses.ForLoopBoss;
import com.vscode.danmaku.core.bosses.LinkedWorm.LinkedListBoss;
import com.vscode.danmaku.core.bosses.RecursionBoss;
import com.vscode.danmaku.core.bosses.BinarySearchBoss;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

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

    // bosses
    private LinkedListBoss linkedListBoss;
    private ForLoopBoss forLoopBoss;
    private RecursionBoss recursionBoss;
    private BinarySearchBoss binarySearchBoss;

    // enemyAttack
    private final List<EnemyBullet> enemyBullets = new ArrayList<>();
    private final List<EnemyBullet2> enemyBullet2s = new ArrayList<>();
    private final List<EnemyBullet3> enemyBullet3s = new ArrayList<>();
    private final List<EnemyLaser> recursionLasers = new ArrayList<>();

    private boolean isAutoShooting = true;

    private final List<GameItem> itemsOnMap = new ArrayList<>();
    private long lastItemSpawnTime = 0;
    private long powerUpEndTime = 0;
    private long timeStopEndTime = 0;
    private final Random random = new Random();

    // 教學關卡相關
    private int tutorialPhase = 0; 
    private long tutorialTimer = 0;
    private int tutorialStepCount = 0;
    private double targetX, targetY, targetSize = 50;
    
    // 鐳射與複雜彈幕相關
    private boolean laserWarning = false;
    private boolean laserActive = false;
    private int laserType = 0; // 0: Vertical, 1: Sloped, 2: Circular
    private List<Double> laserPositions = new ArrayList<>(); // X coords or Angles
    private List<Double> laserSlopes = new ArrayList<>();
    private long laserStartTime = 0;

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
        } else if (selectedLevel.equals("RECURSION")) {
            recursionBoss = new RecursionBoss();
            System.out.println("生成 Recursion Boss");
        } else if (selectedLevel.equals("BINARY SEARCH")) {
            binarySearchBoss = new BinarySearchBoss();
            System.out.println("生成 Binary Search Boss");
        } else if (selectedLevel.equals("LINKED LIST")) {
            linkedListBoss = new LinkedListBoss(400.0, 100.0);
            System.out.println("生成 LinkedList Boss");
        } else if (selectedLevel.equals("TUTORIAL")) {
            tutorialPhase = 1;
            tutorialTimer = 0;
            tutorialStepCount = 0;
            targetX = 200;
            targetY = 200;
            System.out.println("開始教學關卡：第 1 階段 - 移動");
        } else {
            System.out.println("未知關卡或是尚未實作: " + selectedLevel);
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

        // 教學關卡邏輯
        if (selectedLevel.equals("TUTORIAL")) {
            switch (tutorialPhase) {
                case 1 -> { // 移動到特定區域 (5次)
                    double pCX = player.x + player.width / 2;
                    double pCY = player.y + player.height / 2;
                    if (Math.sqrt(Math.pow(pCX - targetX, 2) + Math.pow(pCY - targetY, 2)) < targetSize) {
                        tutorialStepCount++;
                        if (tutorialStepCount >= 5) {
                            tutorialPhase = 2;
                            tutorialStepCount = 0;
                            tutorialTimer = 0;
                            System.out.println("教學關卡：第 2 階段 - 使用技能");
                        } else {
                            // 隨機生成下一個位置
                            targetX = 100 + random.nextDouble() * (cw - 200);
                            targetY = 100 + random.nextDouble() * (ch - 200);
                        }
                    }
                }
                case 2 -> { // 使用技能 (持續生成邊緣子彈)
                    if (tutorialTimer == 0) tutorialTimer = now;
                    // 每 2.5 秒生成一波邊緣合圍子彈
                    if (now - tutorialTimer > 2_500_000_000L) {
                        double pCX = player.x + player.width / 2;
                        double pCY = player.y + player.height / 2;
                        // 從四個邊緣各選幾個點射向玩家
                        for (int i = 1; i < 10; i += 2) {
                            spawnBorderBullet(i * (cw / 10), 0, pCX, pCY);
                            spawnBorderBullet(i * (cw / 10), ch, pCX, pCY);
                            spawnBorderBullet(0, i * (ch / 10), pCX, pCY);
                            spawnBorderBullet(cw, i * (ch / 10), pCX, pCY);
                        }
                        tutorialTimer = now;
                    }
                }
                case 3 -> { // 躲開 10 顆子彈 (瞄準玩家)
                    if (tutorialTimer == 0) tutorialTimer = now;
                    if (now - tutorialTimer > 1_200_000_000L) {
                        // 計算瞄準玩家的角度
                        double pCX = player.x + player.width / 2;
                        double pCY = player.y + player.height / 2;
                        double mX = cw / 2;
                        double mY = ch / 2;
                        double angle = Math.toDegrees(Math.atan2(pCY - mY, pCX - mX));
                        enemyBullets.add(new EnemyBullet(mX, mY, 3.5, angle)); 
                        tutorialTimer = now;
                    }
                    if (tutorialStepCount >= 10) {
                        tutorialPhase = 4;
                        tutorialStepCount = 0;
                        tutorialTimer = 0;
                        System.out.println("教學關卡：第 4 階段 - 躲避鐳射");
                    }
                }
                case 4 -> { // 躲避鐳射 10 次 (包含斜率、多條、圓形等複雜模式)
                    if (tutorialTimer == 0) tutorialTimer = now;
                    long elapsed = now - tutorialTimer;

                    if (!laserWarning && !laserActive && elapsed > 1_500_000_000L) {
                        laserWarning = true;
                        laserStartTime = now;
                        laserPositions.clear();
                        laserSlopes.clear();
                        
                        // 隨機決定模式 (降低圓形機率: 0: 45%, 1: 45%, 2: 10%)
                        int rand = random.nextInt(100);
                        if (rand < 45) laserType = 0;
                        else if (rand < 90) laserType = 1;
                        else laserType = 2;

                        if (laserType == 0) { // 垂直模式 (至少 5 條)
                            int count = 5 + random.nextInt(3);
                            for (int i = 0; i < count; i++) {
                                laserPositions.add(50 + random.nextDouble() * (cw - 100));
                            }
                        } else if (laserType == 1) { // 不同斜率模式 (至少 5 條)
                            int count = 5 + random.nextInt(2);
                            for (int i = 0; i < count; i++) {
                                laserPositions.add(random.nextDouble() * cw); // 起點 X
                                laserSlopes.add(-1.5 + random.nextDouble() * 3.0); // 斜率
                            }
                        } else { // 圓形擴張預警
                            laserPositions.add(cw / 2);
                            laserPositions.add(ch / 2);
                        }
                    }

                    if (laserWarning && now - laserStartTime > 1_200_000_000L) {
                        laserWarning = false;
                        laserActive = true;
                        laserStartTime = now;
                    }

                    if (laserActive) {
                        // 碰撞檢查 (簡化邏輯)
                        boolean hit = false;
                        if (laserType == 0) {
                            for (double lx : laserPositions) {
                                if (Math.abs(player.x + player.width/2 - lx) < 25) hit = true;
                            }
                        } else if (laserType == 1) {
                            for (int i = 0; i < laserPositions.size(); i++) {
                                double sx = laserPositions.get(i);
                                double slope = laserSlopes.get(i);
                                // 點到直線距離公式簡化版
                                double py = player.y + player.height/2;
                                double px = player.x + player.width/2;
                                double targetX = sx + slope * py;
                                if (Math.abs(px - targetX) < 20) hit = true;
                            }
                        } else { // 圓形
                            double dx = player.x + player.width/2 - laserPositions.get(0);
                            double dy = player.y + player.height/2 - laserPositions.get(1);
                            double dist = Math.sqrt(dx*dx + dy*dy);
                            if (dist > 180 && dist < 220) hit = true;
                        }

                        if (hit && now % 300_000_000L < 20_000_000L) {
                            tutorialStepCount--;
                            if (tutorialStepCount < 0) tutorialStepCount = 0;
                        }
                        
                        if (now - laserStartTime > 800_000_000L) {
                            laserActive = false;
                            tutorialStepCount++;
                            tutorialTimer = now;
                        }
                    }

                    if (tutorialStepCount >= 10) {
                        isVictory = true;
                    }
                }
            }
        }

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

        // boss & player update
        boolean isTimeStopped = (now < timeStopEndTime);

        if (!isTimeStopped) {
            if (forLoopBoss != null && forLoopBoss.isAlive()) {
                forLoopBoss.update(now, enemyBullets, recursionLasers, player.x, player.y);
            }
            else if (recursionBoss != null && recursionBoss.isAlive()) {
                recursionBoss.update(now, enemyBullets, recursionLasers, player.x + player.width/2, player.y + player.height/2, cw, ch);
            }
            else if (linkedListBoss != null && linkedListBoss.isAlive()) {
                linkedListBoss.setTargetX(player.x);
                linkedListBoss.setTargetY(player.y);
                linkedListBoss.update(now, enemyBullets, player.x, player.y);
            } else if (binarySearchBoss != null && binarySearchBoss.isAlive()) {
                binarySearchBoss.update(now, enemyBullet2s, enemyBullet3s, player.x + player.width/2, player.y + player.height/2, cw, ch);
            }
        }

        // 碰撞判定 (不論時間是否停止都處理，讓玩家在停止時能造成傷害)
        if (forLoopBoss != null && forLoopBoss.isAlive()) {
            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (forLoopBoss.hit(b)) {
                        score += 5000;
                        isVictory = true;
                    } else if (!b.isAlive()) {
                        score += 15;
                        player.addCharge(5.0);
                    }
                }
            }
        }
        else if (recursionBoss != null && recursionBoss.isAlive()) {
            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (recursionBoss.hit(b)) {
                        score += 5000;
                        isVictory = true;
                    } else if (!b.isAlive()) {
                        score += 15;
                        player.addCharge(5.0);
                    }
                }
            }
        }
        else if (linkedListBoss != null && linkedListBoss.isAlive()) {
            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    boolean hit = linkedListBoss.hit(b);
                    if (hit) {
                        if (!linkedListBoss.isAlive()) {
                            score += 5000;
                            isVictory = true;
                        }
                    } else if (!b.isAlive()) {
                        score += 15;
                        player.addCharge(5.0);
                    }
                }
            }
            if (linkedListBoss.isHittingPlayer(player)) {
                player.takeDamage(1, now);
                if (!player.isAlive()) {
                    isGameOver = true;
                }
            }
        } else if (binarySearchBoss != null && binarySearchBoss.isAlive()) {
            for (Bullet b : playerBullets) {
                if (b.isAlive()) {
                    if (binarySearchBoss.hit(b)) {
                        score += 5000;
                        isVictory = true;
                    } else if (!b.isAlive()) {
                        score += 15;
                        player.addCharge(5.0);
                    }
                }
            }
        }

        // enemybullets update
        if (!isTimeStopped) {
            for (EnemyBullet eb : enemyBullets) {
                eb.update();
                if (eb.x < -20 || eb.x > cw + 20 || eb.y < -20 || eb.y > ch + 20) {
                    eb.setAlive(false);
                    // 教學關卡：躲過子彈 (消失在螢幕外)
                    if (selectedLevel.equals("TUTORIAL") && tutorialPhase == 3) {
                        tutorialStepCount++;
                    }
                }
            }

            // enemybullet2s update
            double pCX = player.x + player.width / 2;
            double pCY = player.y + player.height / 2;
            for (EnemyBullet2 eb2 : enemyBullet2s) {
                eb2.update(pCX, pCY);
                if (eb2.x < -20 || eb2.x > cw + 20 || eb2.y < -20 || eb2.y > ch + 20) {
                    eb2.setAlive(false);
                }
            }

            // enemybullet3 update
            for (EnemyBullet3 eb3 : enemyBullet3s)
            {
                eb3.update();
                if (eb3.x < -20 || eb3.x > cw + 20 || eb3.y < -20 || eb3.y > ch+20)
                {
                    eb3.setAlive(false);
                }
            }
        }



        // 碰撞玩家判定 (不論時間是否停止都處理)
        for (EnemyBullet eb : enemyBullets) {
            if (eb.isAlive() && eb.collidesWithPlayer(player.x, player.y, player.width, player.height)) {
                if (selectedLevel.equals("TUTORIAL") && (tutorialPhase == 2 || tutorialPhase == 3)) {
                    eb.setAlive(false);
                    if (tutorialPhase == 3) {
                        tutorialStepCount--;
                        if (tutorialStepCount < 0) tutorialStepCount = 0;
                    }
                } else {
                    eb.setAlive(false);
                    player.takeDamage(1, now);
                    if (!player.isAlive()) {
                        isGameOver = true;
                    }
                }
                break;
            }
        }
        for (EnemyBullet2 eb2 : enemyBullet2s) {
            if (eb2.isAlive() && eb2.collidesWithPlayer(player.x, player.y, player.width, player.height)) {
                eb2.setAlive(false);
                player.takeDamage(1, now);
                if (!player.isAlive()) {
                    isGameOver = true;
                }
                break;
            }
        }

        for (EnemyBullet3 eb3 : enemyBullet3s)
        {
            if (eb3.isAlive() && eb3.collidesWithPlayer(player.x, player.y, player.width, player.height))
            {
                player.setAlive(false);
                isGameOver = true;
                break;
            }
        }
        
        playerBullets.removeIf(b -> !b.isAlive());
        enemyBullets.removeIf(eb -> !eb.isAlive());
        enemyBullet2s.removeIf(eb2 -> !eb2.isAlive());
        enemyBullet3s.removeIf( eb3 -> !eb3.isAlive());

        // recursion lasers update and collision
        for (EnemyLaser el : recursionLasers) {
            el.update(now);
            if (el.collidesWithPlayer(player.x, player.y, player.width, player.height)) {
                player.takeDamage(1, now);
                if (!player.isAlive()) {
                    isGameOver = true;
                }
                break;
            }
        }
        recursionLasers.removeIf(el -> el.isFinished());
    }

    private void draw(GraphicsContext gc) {
        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        if (forLoopBoss != null) forLoopBoss.draw(gc);
        if (recursionBoss != null) recursionBoss.draw(gc);
        if (linkedListBoss != null) linkedListBoss.draw(gc);
        if (binarySearchBoss != null) binarySearchBoss.draw(gc);

        drawBossHealthBar(gc);

        for (GameItem item : itemsOnMap) {
            item.draw(gc);
        }

        for (Bullet b : playerBullets) { b.draw(gc); }
        for (EnemyBullet eb : enemyBullets) { eb.draw(gc); }
        for (EnemyBullet2 eb2 : enemyBullet2s) { eb2.draw(gc); }
        for (EnemyBullet3 eb3 : enemyBullet3s) { eb3.draw(gc); }
        for (EnemyLaser el : recursionLasers) { el.draw(gc); }
        if (player != null && player.isAlive()) { player.draw(gc); }

        // 教學關卡視覺元素與文字提示
        if (selectedLevel.equals("TUTORIAL")) {
            gc.setFont(new Font("Monospaced", 16));
            gc.setFill(Color.YELLOW);
            
            switch (tutorialPhase) {
                case 1 -> {
                    gc.fillText("STEP 1: Move to the TARGET AREA", 20, 100);
                    // 繪製目標區域
                    gc.setStroke(Color.CYAN);
                    gc.setLineWidth(2);
                    gc.strokeOval(targetX - targetSize, targetY - targetSize, targetSize * 2, targetSize * 2);
                    gc.setFill(Color.web("#00FFFF", 0.3));
                    gc.fillOval(targetX - targetSize, targetY - targetSize, targetSize * 2, targetSize * 2);
                }
                case 2 -> {
                    gc.fillText("STEP 2: Press [C] to TRIGGER BORDER INVASION", 20, 100);
                }
                case 3 -> {
                    gc.fillText("STEP 3: Dodge 10 Aimed Bullets (" + tutorialStepCount + "/10)", 20, 100);
                    // 繪製中間的「怪物」
                    gc.setFill(Color.RED);
                    gc.fillRect(gameCanvas.getWidth() / 2 - 20, gameCanvas.getHeight() / 2 - 20, 40, 40);
                }
                case 4 -> {
                    gc.fillText("STEP 4: Dodge 10 Lasers (" + tutorialStepCount + "/10)", 20, 100);
                    
                    if (laserWarning) {
                        gc.setStroke(Color.web("#FFFF00", 0.8)); // Yellow for Tutorial Warning
                        gc.setLineWidth(4);
                        gc.setLineDashes(15, 10);
                        if (laserType == 0) { // 垂直
                            for (double lx : laserPositions) gc.strokeLine(lx, 0, lx, gameCanvas.getHeight());
                        } else if (laserType == 1) { // 斜率
                            for (int i = 0; i < laserPositions.size(); i++) {
                                double sx = laserPositions.get(i);
                                double slope = laserSlopes.get(i);
                                gc.strokeLine(sx, 0, sx + slope * gameCanvas.getHeight(), gameCanvas.getHeight());
                            }
                        } else { // 圓形
                            gc.strokeOval(laserPositions.get(0) - 200, laserPositions.get(1) - 200, 400, 400);
                        }
                        gc.setLineDashes(0);
                    }
                    if (laserActive) {
                        gc.setFill(Color.web("#FF0000", 0.8));
                        if (laserType == 0) {
                            for (double lx : laserPositions) gc.fillRect(lx - 20, 0, 40, gameCanvas.getHeight());
                        } else if (laserType == 1) {
                            for (int i = 0; i < laserPositions.size(); i++) {
                                double sx = laserPositions.get(i);
                                double slope = laserSlopes.get(i);
                                // 用多邊形繪製斜鐳射
                                gc.fillPolygon(new double[]{sx-15, sx+15, sx+slope*gameCanvas.getHeight()+15, sx+slope*gameCanvas.getHeight()-15},
                                               new double[]{0, 0, gameCanvas.getHeight(), gameCanvas.getHeight()}, 4);
                            }
                        } else { // 圓形
                            gc.setStroke(Color.web("#FF0000", 0.8));
                            gc.setLineWidth(30);
                            gc.strokeOval(laserPositions.get(0) - 200, laserPositions.get(1) - 200, 400, 400);
                        }
                    }
                }
            }
        }

        // 時間暫停視覺效果 (藍色覆蓋層)
        if (System.nanoTime() < timeStopEndTime) {
            gc.setFill(Color.web("#00AAFF", 0.15));
            gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
            gc.setFill(Color.web("#00AAFF"));
            gc.setFont(new Font("Monospaced", 24));
            gc.fillText("<< TIME STOPPED >>", gameCanvas.getWidth() / 2 - 120, 80);
        }

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(new Font("Monospaced", 18));
        gc.fillText("SCORE: " + String.format("%05d", score), 20, 30);

        // --- Player Stats UI ---
        if (player != null) {
            double statBarWidth = 150;
            double statBarHeight = 14;
            double statX = 20;

            // HP Bar
            gc.setFill(Color.web("#331111"));
            gc.fillRect(statX, 42, statBarWidth, statBarHeight);
            double hpRatio = (double) player.getHp() / player.getMaxHp();
            gc.setFill(Color.web("#FF4444"));
            gc.fillRect(statX, 42, statBarWidth * hpRatio, statBarHeight);
            gc.setStroke(Color.web("#888888"));
            gc.setLineWidth(1);
            gc.strokeRect(statX, 42, statBarWidth, statBarHeight);
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Monospaced", 10));
            gc.fillText("HP " + player.getHp() + "/" + player.getMaxHp(), statX + 5, 53);

            // Shield Bar
            gc.setFill(Color.web("#112233"));
            gc.fillRect(statX, 62, statBarWidth, statBarHeight);
            double shieldRatio = (double) player.getShield() / player.getMaxShield();
            gc.setFill(Color.web("#00AAFF"));
            gc.fillRect(statX, 62, statBarWidth * shieldRatio, statBarHeight);
            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(statX, 62, statBarWidth, statBarHeight);
            gc.setFill(Color.WHITE);
            gc.fillText("SHIELD " + player.getShield() + "/" + player.getMaxShield(), statX + 5, 73);

            // ATK Stats
            gc.setFill(Color.web("#FFD700"));
            gc.setFont(new Font("Monospaced", 14));
            gc.fillText("ATK: " + player.getAttackPower(), statX, 95);
        }

        // 左下角充能 UI
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
                gc.setFill(Color.web("#FFD700"));
                gc.fillRect(uiX + 2, uiY - 20, 156, 26);
                gc.setFill(Color.BLACK);
                gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 12));
                gc.fillText("[C] BOMB READY!", uiX + 10, uiY - 2);
            } else {
                gc.setFill(Color.web("#007ACC"));
                gc.fillRect(uiX + 2, uiY - 20, 156 * progress, 26);
                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("Monospaced", 12));
                gc.fillText("[C] CHARGING " + (int)(progress * 100) + "%", uiX + 10, uiY - 2);
            }
        }

        // 右下角武器模式 UI
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
            gc.setFill(i == currentMode ? activeDotColor : Color.web("#444444"));
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
            int phase = forLoopBoss.getPhase();
            
            if (phase == 1) {
                currentHp = forLoopBoss.getHpI(); maxHp = forLoopBoss.getMaxHpI();
                hpRatio = (double) currentHp / maxHp; percent = (int)(hpRatio * 100);
                gc.setFill(Color.web("#00FF00"));
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.setFill(Color.WHITE);
                gc.setFont(new Font("Monospaced", 11));
                gc.fillText(String.format("[COMPILING] NESTED_LOOP: Core_I  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
            } else if (phase == 2) {
                currentHp = forLoopBoss.getHpJ(); maxHp = forLoopBoss.getMaxHpJ();
                hpRatio = (double) currentHp / maxHp; percent = (int)(hpRatio * 100);
                gc.setFill(Color.web("#00FFFF"));
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.setFill(Color.WHITE);
                gc.setFont(new Font("Monospaced", 11));
                gc.fillText(String.format("[EVOLVING] NESTED_LOOP: Layer_J  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
            } else {
                currentHp = forLoopBoss.getHpK(); maxHp = forLoopBoss.getMaxHpK();
                hpRatio = (double) currentHp / maxHp; percent = (int)(hpRatio * 100);
                gc.setFill(Color.web("#FF00FF"));
                gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
                gc.setFill(Color.WHITE);
                gc.setFont(new Font("Monospaced", 11));
                gc.fillText(String.format("[FINALIZING] NESTED_LOOP: Layer_K  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
            }
            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(barX, barY, barWidth, barHeight);
        }
        else if (recursionBoss != null && recursionBoss.isAlive()) {
            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);
            int currentHp = recursionBoss.getHp();
            int maxHp = recursionBoss.getMaxHp();
            double hpRatio = (double) currentHp / maxHp;
            int percent = (int)(hpRatio * 100);
            gc.setFill(Color.web("#800080"));
            gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(barX, barY, barWidth, barHeight);
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Monospaced", 11));
            gc.fillText(String.format("RECURSION: Base Case  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
        } else if (binarySearchBoss != null && binarySearchBoss.isAlive()) {
            gc.setFill(Color.web("#333333"));
            gc.fillRect(barX, barY, barWidth, barHeight);
            int currentHp = binarySearchBoss.getHp();
            int maxHp = binarySearchBoss.getMaxHp();
            double hpRatio = (double) currentHp / maxHp;
            int percent = (int)(hpRatio * 100);
            gc.setFill(Color.web("#00FF66"));
            gc.fillRect(barX, barY, barWidth * hpRatio, barHeight);
            gc.setStroke(Color.web("#888888"));
            gc.strokeRect(barX, barY, barWidth, barHeight);
            gc.setFill(Color.web("#00FF66"));
            gc.setFont(new Font("Monospaced", 11));
            gc.fillText(String.format("[SEARCH] BOSS: BINARY_SEARCH  %d/%d  %d%%", currentHp, maxHp, percent), barX, barY - 8);
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
            case ESCAPE -> {
                if (isGameOver || isVictory) {
                    returnToMenu();
                } else {
                    showExitConfirmation();
                }
            }
        }
    }

    private void showExitConfirmation() {
        stop();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("離開確認");
            alert.setHeaderText(null);
            alert.setContentText("確定要離開遊戲嗎？");

            ButtonType okButton = new ButtonType("是", ButtonType.OK.getButtonData());
            ButtonType noButton = new ButtonType("否", ButtonType.CANCEL.getButtonData());
            alert.getButtonTypes().setAll(okButton, noButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == okButton) {
                returnToMenu();
            } else {
                start();
            }
        });
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
                if (selectedLevel.equals("TUTORIAL") && tutorialPhase == 2) {
                    // 階段 2 特殊效果：從螢幕邊框往玩家靠近
                    double pCX = player.x + player.width / 2;
                    double pCY = player.y + player.height / 2;
                    double cw = gameCanvas.getWidth();
                    double ch = gameCanvas.getHeight();
                    
                    // 從四個邊緣生成子彈往玩家射擊
                    for (int i = 0; i < 10; i++) {
                        // 上邊緣
                        spawnBorderBullet(i * (cw / 10), 0, pCX, pCY);
                        // 下邊緣
                        spawnBorderBullet(i * (cw / 10), ch, pCX, pCY);
                        // 左邊緣
                        spawnBorderBullet(0, i * (ch / 10), pCX, pCY);
                        // 右邊緣
                        spawnBorderBullet(cw, i * (ch / 10), pCX, pCY);
                    }
                    
                    tutorialPhase = 3;
                    tutorialTimer = 0;
                    tutorialStepCount = 0;
                    System.out.println("教學關卡：第 3 階段 - 躲避子彈");
                }
                if (player != null && player.useBomb()) {
                    if (difficultyMultiplier == 0.5) {
                        System.out.println("[核心呼叫] 消耗 1 次充能，執行 TIME_STOP.EXE：凍結時間 5 秒！");
                        timeStopEndTime = System.nanoTime() + 5_000_000_000L;
                    } else {
                        System.out.println("[核心呼叫] 消耗 1 次充能，執行 BOMB.EXE：清空全螢幕敵方子彈！");
                        for (EnemyBullet eb : enemyBullets) { eb.setAlive(false); }
                        for (EnemyBullet2 eb2 : enemyBullet2s) { eb2.setAlive(false); }
                        enemyBullets.clear();
                        enemyBullet2s.clear();
                    }
                } else {
                    System.out.println("[警告] 充能不足，無法執行技能！");
                }
            }
        }
    }

    private void spawnBorderBullet(double sx, double sy, double tx, double ty) {
        double angle = Math.toDegrees(Math.atan2(ty - sy, tx - sx));
        enemyBullets.add(new EnemyBullet(sx, sy, 2.0, angle));
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