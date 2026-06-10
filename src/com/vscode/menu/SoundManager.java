package com.vscode.menu;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.media.AudioClip;
import java.net.URL;

public class SoundManager {
    private static SoundManager instance;

    // 使用 Property 可以方便未來與 Media Player 進行雙向綁定 (Binding)
    private final DoubleProperty musicVolume = new SimpleDoubleProperty(50.0); // 預設值 50
    private final DoubleProperty soundVolume = new SimpleDoubleProperty(50.0); // 預設值 50

    // ===== 新增：勝利音效物件 =====
    private AudioClip victorySound;

    private SoundManager() {
        // 初始化音效
        try {
            // 請確保你的音效檔案放在 src/main/resources/sounds/victory.mp3
            URL resource = getClass().getResource("/resource/fxml/sounds/victory.mp3");
            if (resource != null)
            {
                victorySound = new AudioClip(resource.toExternalForm());

                // 讓 AudioClip 的音量即時連動你的 soundVolumeProperty
                // 因為 AudioClip 的音量範圍是 0.0 ~ 1.0，而你們的預設值是 50.0，所以除以 100
                victorySound.setVolume(soundVolume.get() / 100.0);
                soundVolume.addListener((obs, oldVal, newVal) -> {
                    victorySound.setVolume(newVal.doubleValue() / 100.0);
                });
            }
            else
            {
                System.err.println("【SoundManager】找不到勝利音效檔案！請檢查路徑。");
            }
        }
        catch (Exception e)
        {
            System.err.println("【SoundManager】音效載入失敗: " + e.getMessage());
        }
    }

    public static SoundManager getInstance() {
        if (instance == null)
        {
            instance = new SoundManager();
        }
        return instance;
    }

    // ===== 新增：播放勝利音效的方法 =====
    public void playVictory()
    {
        if (victorySound != null) {
            // 每次播放前確保抓到最新的音量（多一層保險）
            victorySound.setVolume(getSoundVolume() / 100.0);
            victorySound.play();
        }
    }

    public DoubleProperty musicVolumeProperty() { return musicVolume; }
    public double getMusicVolume() { return musicVolume.get(); }
    public void setMusicVolume(double value) { this.musicVolume.set(value); }

    public DoubleProperty soundVolumeProperty() { return soundVolume; }
    public double getSoundVolume() { return soundVolume.get(); }
    public void setSoundVolume(double value) { this.soundVolume.set(value); }
}