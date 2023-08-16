package com.zx1316.naturalgptinmc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GameplayEvents {
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        if (message.charAt(0) == Config.ignoreMark) {
            return;
        }
        ServerPlayer serverPlayer = event.getPlayer();
        MinecraftServer server = serverPlayer.server;
        GptSavedData data = server.overworld().getDataStorage().computeIfAbsent(GptSavedData::new, GptSavedData::new, "naturalgptinmc_data");
        boolean flag = false;
        for (String str : data.getAiData().keySet()) {
            if (message.contains(str)) {
                flag = true;
                break;
            }
        }
        if (flag) {
            if (GptUtil.isUsing) {
                serverPlayer.displayClientMessage(MutableComponent.create(new LiteralContents("The AI is busy, please try again later.")).withStyle(ChatFormatting.RED), false);
                data.getAiData().get(data.getCurrent()).addLast(event.getUsername() + ": " + event.getMessage());
                data.setDirty();
            } else {
                GptUtil.chat(serverPlayer, message, data);
            }
        } else {
            data.getAiData().get(data.getCurrent()).addLast(event.getUsername() + ": " + event.getMessage());
            data.setDirty();
        }
    }

    @SubscribeEvent
    public static void onCommandRegistry(RegisterCommandsEvent event) {
        GptCommands.register(event.getDispatcher());
    }
}
