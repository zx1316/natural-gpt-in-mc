package com.zx1316.naturalgptinmc.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class ChatCompletionRequest {
    private String model;
    private ArrayList<ChatMessage> messages;
    private double temperature;
    @SerializedName("top_p")
    private double topP;
    @SerializedName("presence_penalty")
    private double presencePenalty;
    @SerializedName("frequency_penalty")
    private double frequencyPenalty;

    public ChatCompletionRequest(String model, ArrayList<ChatMessage> messages, double temperature, double topP, double presencePenalty, double frequencyPenalty) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.topP = topP;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
    }

    public String getModel() {
        return model;
    }

    public ArrayList<ChatMessage> getMessages() {
        return messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getTopP() {
        return topP;
    }

    public double getPresencePenalty() {
        return presencePenalty;
    }

    public double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setMessages(ArrayList<ChatMessage> messages) {
        this.messages = messages;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setTopP(double topP) {
        this.topP = topP;
    }

    public void setPresencePenalty(double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public void setFrequencyPenalty(double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }
}
