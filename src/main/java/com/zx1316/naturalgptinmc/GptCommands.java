package com.zx1316.naturalgptinmc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zx1316.naturalgptinmc.data.AiPreset;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class GptCommands {
    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("gpt")
                .then(Commands.literal("chat")
                        .then(Commands.argument("message", MessageArgument.message())
                                .executes(p -> {
                                    CommandSourceStack commandSourceStack = p.getSource();
                                    if (GptUtil.isUsing) {
                                        commandSourceStack.sendFailure(new TextComponent("The AI is busy, please try again later."));
                                        return 1;
                                    }
                                    ServerPlayer serverPlayer = commandSourceStack.getPlayerOrException();
                                    MinecraftServer server = commandSourceStack.getServer();
                                    GptSavedData data = server.overworld().getDataStorage().computeIfAbsent(GptSavedData::new, GptSavedData::new, "naturalgptinmc_data");
                                    String message = MessageArgument.getMessage(p, "message").getString();
                                    String name = serverPlayer.getName().getString();
                                    GptUtil.sendMessageToEveryone(server, message, name + " to " + data.getCurrent());
                                    GptUtil.chat(serverPlayer, message, data);
                                    return 0;
                                })))
                .then(Commands.literal("info")
                        .executes(p -> {
                            CommandSourceStack commandSourceStack = p.getSource();
                            MinecraftServer server = commandSourceStack.getServer();
                            GptSavedData data = server.overworld().getDataStorage().computeIfAbsent(GptSavedData::new, GptSavedData::new, "naturalgptinmc_data");
                            StringBuilder sb = new StringBuilder("All characters:");
                            for (AiPreset preset : Config.presets) {
                                sb.append("\n").append(preset.getName());
                                if (preset.getName().equals(data.getCurrent())) {
                                    sb.append(" (current)");
                                }
                            }
                            commandSourceStack.sendSuccess(new TextComponent(sb.toString()), true);
                            return 1;
                        }))
                .then(Commands.literal("query")
                        .then(Commands.argument("character", MessageArgument.message())
                                .executes(p -> {
                                    String character = MessageArgument.getMessage(p, "character").getString();
                                    CommandSourceStack commandSourceStack = p.getSource();
                                    for (AiPreset preset : Config.presets) {
                                        if (preset.getName().equals(character)) {
                                            commandSourceStack.sendSuccess(new TextComponent("Character setting: " + preset.getSetting()), true);
                                            return 1;
                                        }
                                    }
                                    commandSourceStack.sendFailure(new TextComponent("No such character."));
                                    return 0;
                                })))
                .then(Commands.literal("change")
                        .then(Commands.argument("character", MessageArgument.message())
                                .executes(p -> {
                                    CommandSourceStack commandSourceStack = p.getSource();
                                    String character = MessageArgument.getMessage(p, "character").getString();
                                    MinecraftServer server = commandSourceStack.getServer();
                                    GptSavedData data = server.overworld().getDataStorage().computeIfAbsent(GptSavedData::new, GptSavedData::new, "naturalgptinmc_data");
                                    if (data.getAiData().containsKey(character)) {
                                        data.setCurrent(character);
                                        data.setDirty();
                                        commandSourceStack.sendSuccess(new TextComponent("Change the character to " + character + " successfully."), true);
                                        return 1;
                                    }
                                    commandSourceStack.sendFailure(new TextComponent("No such character."));
                                    return 0;
                                })))
                .then(Commands.literal("reset")
                        .then(Commands.argument("character", MessageArgument.message())
                                .executes(p -> {
                                    CommandSourceStack commandSourceStack = p.getSource();
                                    String character = MessageArgument.getMessage(p, "character").getString();
                                    MinecraftServer server = commandSourceStack.getServer();
                                    GptSavedData data = server.overworld().getDataStorage().computeIfAbsent(GptSavedData::new, GptSavedData::new, "naturalgptinmc_data");
                                    if (data.getAiData().containsKey(character)) {
                                        data.getAiData().get(character).clear();
                                        data.setDirty();
                                        commandSourceStack.sendSuccess(new TextComponent("Reset " + character + " successfully."), true);
                                        return 1;
                                    }
                                    commandSourceStack.sendFailure(new TextComponent("No such character."));
                                    return 0;
                                })));
        pDispatcher.register(literalArgumentBuilder);
    }
}
