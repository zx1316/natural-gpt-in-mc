package com.zx1316.naturalgptinmc.data;

public class AiPreset {
    private final String name;
    private final String setting;

    public AiPreset(String name, String setting) {
        this.name = name;
        this.setting = setting;
    }

    public String getName() {
        return name;
    }

    public String getSetting() {
        return setting;
    }
}
