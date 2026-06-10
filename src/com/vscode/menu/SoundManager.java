package com.vscode.menu;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class SoundManager {
    private static SoundManager instance;

    // 使用 Property 可以方便未來與 Media Player 進行雙向綁定 (Binding)
    private final DoubleProperty musicVolume = new SimpleDoubleProperty(50.0); // 預設值 50
    private final DoubleProperty soundVolume = new SimpleDoubleProperty(50.0); // 預設值 50

    // ===== 音效與音樂物件 =====
    private AudioClip victorySound;
    private AudioClip gameOverSound;
    private MediaPlayer bossMusicPlayer;

    private SoundManager() {
        // 初始化音效與音樂
        try {
            // 載入勝利音效
            URL victoryRes = getClass().getResource("/resource/fxml/sounds/victory.mp3");
            if (victoryRes != null) {
                victorySound = new AudioClip(victoryRes.toExternalForm());
                victorySound.volumeProperty().bind(soundVolume.divide(100.0));
            } else {
                System.err.println("【SoundManager】找不到勝利音效檔案！");
            }

            // 載入遊戲結束音效 (death.mp3)
            URL gameOverRes = getClass().getResource("/resource/fxml/sounds/gameover.mp3");
            if (gameOverRes != null) {
                gameOverSound = new AudioClip(gameOverRes.toExternalForm());
                gameOverSound.volumeProperty().bind(soundVolume.divide(100.0));
            } else {
                System.err.println("【SoundManager】找不到遊戲結束音效檔案！");
            }

            // 載入 Boss 戰音樂 (BossBGM.mp3)
            URL bossMusicRes = getClass().getResource("/resource/fxml/audio/Bossfight.mp3");
            if (bossMusicRes != null) {
                Media bossMedia = new Media(bossMusicRes.toExternalForm());
                bossMusicPlayer = new MediaPlayer(bossMedia);
                bossMusicPlayer.volumeProperty().bind(musicVolume.divide(100.0));
                bossMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            } else {
                System.err.println("【SoundManager】找不到 Boss 戰音樂檔案！");
            }

        } catch (Exception e) {
            System.err.println("【SoundManager】初始化失敗: " + e.getMessage());
        }
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    // ===== 播放方法 =====
    public void playVictory() {
        if (victorySound != null) {
            victorySound.play();
        }
    }

    public void playGameOverSound() {
        if (gameOverSound != null) {
            gameOverSound.play();
        }
    }

    public void playBossFightMusic() {
        if (bossMusicPlayer != null) {
            bossMusicPlayer.play();
        }
    }

    public void stopBossFightMusic() {
        if (bossMusicPlayer != null) {
            bossMusicPlayer.stop();
        }
    }

    public DoubleProperty musicVolumeProperty() { return musicVolume; }
    public double getMusicVolume() { return musicVolume.get(); }
    public void setMusicVolume(double value) { this.musicVolume.set(value); }

    public DoubleProperty soundVolumeProperty() { return soundVolume; }
    public double getSoundVolume() { return soundVolume.get(); }
    public void setSoundVolume(double value) { this.soundVolume.set(value); }
}