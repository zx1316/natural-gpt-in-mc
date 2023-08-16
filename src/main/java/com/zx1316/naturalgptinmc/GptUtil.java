package com.zx1316.naturalgptinmc;

import com.google.gson.Gson;
import com.zx1316.naturalgptinmc.data.AiPreset;
import com.zx1316.naturalgptinmc.data.ChatCompletionRequest;
import com.zx1316.naturalgptinmc.data.ChatCompletionResponse;
import com.zx1316.naturalgptinmc.data.ChatMessage;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class GptUtil {
    private static final String SYSTEM_TEMPLATE = "I want you to strictly follow the character setting to play %s in the first person and generate the response of %s in group chat. Character setting: %s";
    public static boolean isUsing;
    private static final Gson GSON = new Gson();

    public static void chat(ServerPlayer serverPlayer, String message, GptSavedData data) {
        for (String str : data.getAiData().keySet()) {
            if (message.contains(str)) {
                data.setCurrent(str);
                break;
            }
        }
        String aiName = data.getCurrent();
        String setting = "";
        for (AiPreset preset : Config.presets) {
            if (preset.getName().equals(aiName)) {
                setting = preset.getSetting();
                break;
            }
        }
        String system = String.format(SYSTEM_TEMPLATE, aiName, aiName, setting);
        int cnt = Main.ENCODING.countTokensOrdinary(system) + Main.ENCODING.countTokensOrdinary(aiName + ":");
        MinecraftServer server = serverPlayer.server;
        LinkedList<String> history = data.getAiData().get(aiName);
        String latest = serverPlayer.getName().getString() + ": " + message;
        history.addLast(latest);
        Iterator<String> iterator = history.descendingIterator();
        int cnt1 = 0;
        while (iterator.hasNext() && cnt < Config.length) {
            String str = iterator.next();
            cnt += Main.ENCODING.countTokensOrdinary(str + "\n");
            cnt1++;
        }
        for (int i = 0, times = history.size() - cnt1; i < times; i++) {
            history.pollFirst();
        }
        StringBuilder sb = new StringBuilder();
        for (String str : history) {
            sb.append(str).append("\n");
        }
        String user = sb.append(aiName).append(":").toString();
        isUsing = true;
        Thread gpt = new Thread(() -> {
            RequestConfig config;
            if (Config.proxyHost.equals("")) {
                config = RequestConfig.custom().setSocketTimeout(Config.timeoutSeconds * 1000).build();
            } else {
                config = RequestConfig.custom().setProxy(new HttpHost(Config.proxyHost, Config.proxyPort)).setSocketTimeout(Config.timeoutSeconds * 1000).build();
            }
            ArrayList<ChatMessage> chatMessages = new ArrayList<>();
            chatMessages.add(new ChatMessage("system", system));
            chatMessages.add(new ChatMessage("user", user));
            ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest(Config.model, chatMessages, Config.temperature, Config.topP, Config.presencePenalty, Config.presencePenalty);
            HttpPost httpPost = new HttpPost(Config.url);
            httpPost.setEntity(new StringEntity(GSON.toJson(chatCompletionRequest), ContentType.APPLICATION_JSON));
            httpPost.setHeader("Authorization", "Bearer " + Config.apiKey);
            httpPost.setConfig(config);
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            try (CloseableHttpClient httpClient = httpClientBuilder.setSSLSocketFactory(getSslConnectionSocketFactory()).build()) {
                CloseableHttpResponse response = httpClient.execute(httpPost);
                int code = response.getStatusLine().getStatusCode();
                if (code == 200) {
                    ChatCompletionResponse chatCompletionResponse = GSON.fromJson(EntityUtils.toString(response.getEntity(), "UTF-8"), ChatCompletionResponse.class);
                    String reply = chatCompletionResponse.getChoices().get(0).getMessage().getContent();
                    String startStr = aiName + ": ";
                    if (reply.startsWith(startStr)) {
                        reply = reply.substring(startStr.length());
                    }
                    history.addLast(startStr + reply);
                    CommandSourceStack commandSourceStack = serverPlayer.createCommandSourceStack().withPermission(Config.permissionLevel);
                    int i = 0, j;
                    while ((j = reply.indexOf("cmd(", i)) != -1) {
                        int k = j;
                        while (reply.charAt(j) != ')') {
                            j++;
                        }
                        String command = reply.substring(k + 4, j);
                        boolean flag = true;
                        for (String str : Config.commandBlacklist) {
                            if (command.contains(str)) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag && server.getCommands().performCommand(commandSourceStack, command) != 0) {
                            reply = reply.replace(reply.substring(k, j + 1), "");
                            i = k;
                        } else {
                            i = j;
                        }
                    }
                    sendMessageToEveryone(server, reply, aiName);
                } else {
                    history.removeLastOccurrence(latest);
                    sendMessageToEveryone(server, "HTTP " + code + " error. Please try again later.", aiName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                history.removeLastOccurrence(latest);
                sendMessageToEveryone(server, "An error occurred. Please try again later.", aiName);
            }
            data.setDirty();
            isUsing = false;
        });
        gpt.setDaemon(true);
        gpt.start();
    }

    private static SSLConnectionSocketFactory getSslConnectionSocketFactory() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    public static void sendMessageToEveryone(MinecraftServer server, String msg, String sender) {
        String[] split = msg.split("\n");
        String[] playerNames = server.getPlayerNames();
        for (String name : playerNames) {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayerByName(name);
            if (serverPlayer != null) {
                for (String str : split) {
                    if (!str.trim().equals("")) {
                        serverPlayer.sendMessage(new TextComponent("<" + sender + "> " + str), Util.NIL_UUID);
                    }
                }
            }
        }
        for (String str : split) {
            if (!str.trim().equals("")) {
                server.sendMessage(new TextComponent("<" + sender + "> " + str), Util.NIL_UUID);
            }
        }
    }
}
