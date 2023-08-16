package com.zx1316.naturalgptinmc;

import com.zx1316.naturalgptinmc.data.AiPreset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class GptSavedData extends SavedData {
    private final HashMap<String, LinkedList<String>> aiData = new HashMap<>();
    private String current;

    public GptSavedData() {
        for (AiPreset preset : Config.presets) {
            aiData.put(preset.getName(), new LinkedList<>());
        }
        current = Config.presets.get(0).getName();
    }

    public GptSavedData(@NotNull CompoundTag root) {
        for (AiPreset preset : Config.presets) {
            aiData.put(preset.getName(), new LinkedList<>());
        }
        if (root.contains("History")) {
            CompoundTag compoundTag = (CompoundTag) root.get("History");
            for (Map.Entry<String, LinkedList<String>> entry : aiData.entrySet()) {
                if (compoundTag.contains(entry.getKey())) {
                    ListTag listTag = compoundTag.getList(entry.getKey(), 10);
                    for (Tag tag : listTag) {
                        CompoundTag current = (CompoundTag) tag;
                        entry.getValue().add(current.getString("Msg"));
                    }
                }
            }
        }
        if (root.contains("Current")) {
            current = root.getString("Current");
        } else {
            current = Config.presets.get(0).getName();
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag pCompoundTag) {
        CompoundTag compoundTag = new CompoundTag();
        for (Map.Entry<String, LinkedList<String>> entry : aiData.entrySet()) {
            ListTag listTag = new ListTag();
            for (String msg : entry.getValue()) {
                CompoundTag compoundTag1 = new CompoundTag();
                compoundTag1.putString("Msg", msg);
                listTag.add(compoundTag1);
            }
            compoundTag.put(entry.getKey(), listTag);
        }
        pCompoundTag.put("History", compoundTag);
        pCompoundTag.putString("Current", current);
        return pCompoundTag;
    }

    public HashMap<String, LinkedList<String>> getAiData() {
        return aiData;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }
}
