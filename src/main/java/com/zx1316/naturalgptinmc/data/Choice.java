package com.zx1316.naturalgptinmc.data;

import com.google.gson.annotations.SerializedName;

public class Choice {
    private int index;
    private ChatMessage message;
    @SerializedName("finish_reason")
    private String finishReason;

    public Choice() {}

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
}
