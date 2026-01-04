package com.ruoyi.web.model.dto;

import io.swagger.annotations.ApiModelProperty;

public class TextToSpeechDTO {

    @ApiModelProperty("要合成的文本")
    private String text;

    @ApiModelProperty("语言，如zh或en")
    private String language;

    @ApiModelProperty("语速，0-100")
    private int speed;

    @ApiModelProperty("音调，0-100")
    private int pitch;

    @ApiModelProperty("音量，0-100")
    private int volume;

    // getter和setter方法
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getPitch() {
        return pitch;
    }

    public void setPitch(int pitch) {
        this.pitch = pitch;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
