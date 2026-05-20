package com.vscode.menu;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class SoundManager {
    private static SoundManager instance;

    // 使用 Property 可以方便未來與 Media Player 進行雙向綁定 (Binding)
    private final DoubleProperty musicVolume = new SimpleDoubleProperty(50.0); // 預設值 50
    private final DoubleProperty soundVolume = new SimpleDoubleProperty(50.0); // 預設值 50

    private SoundManager() {}

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public DoubleProperty musicVolumeProperty() { return musicVolume; }
    public double getMusicVolume() { return musicVolume.get(); }
    public void setMusicVolume(double value) { this.musicVolume.set(value); }

    public DoubleProperty soundVolumeProperty() { return soundVolume; }
    public double getSoundVolume() { return soundVolume.get(); }
    public void setSoundVolume(double value) { this.soundVolume.set(value); }
}