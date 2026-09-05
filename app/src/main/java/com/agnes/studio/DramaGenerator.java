package com.agnes.studio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DramaGenerator {

    private final ApiService api;

    public DramaGenerator(ApiService api) {
        this.api = api;
    }

    public interface Callback {
        void onProgress(String step, String message);
        void onComplete(DramaResult result);
        void onError(String error);
    }

    // ==================== 生成剧本 ====================

    public String generateScript(String topic, String style, String textModel) throws IOException {
        String systemPrompt = "你是一个专业短剧编剧。根据用户给的主题，生成一个完整的短剧剧本。" +
                "要求：1.包含3-5个场景 2.每个场景有画面描述和台词 3.适合短视频平台风格";
        String prompt = "主题：" + topic + "\n风格：" + style + "\n\n请生成短剧剧本，格式如下：\n" +
                "场景1：[画面描述]\n台词：[角色名]：[台词内容]\n\n场景2：...";
        return api.chatCompletion(textModel, prompt, systemPrompt);
    }

    // ==================== 剧本分镜 ====================

    public String generateShots(String script, String textModel) throws IOException {
        String systemPrompt = "你是专业分镜师。将剧本转换为分镜列表，每个分镜包含：画面描述、镜头类型、时长建议。" +
                "输出JSON数组格式。";
        String prompt = "剧本：\n" + script + "\n\n请为每个场景生成分镜，JSON格式：\n" +
                "[{\"scene\": 1, \"shot\": \"画面描述\", \"camera\": \"镜头类型\", \"duration\": 5}]";
        return api.chatCompletion(textModel, prompt, systemPrompt);
    }

    // ==================== 生成视频素材 ====================

    public String[] generateVisuals(String shotsJson, String imageModel, String videoModel,
                                     Callback callback) throws IOException {
        try {
            JSONArray shots = new JSONArray(shotsJson);
            String[] videoUrls = new String[shots.length()];

            for (int i = 0; i < shots.length(); i++) {
                JSONObject shot = shots.getJSONObject(i);
                String description = shot.getString("shot");

                callback.onProgress("生成画面", "正在为第 " + (i + 1) + "/" + shots.length() + " 个镜头生成素材...");

                // 生成图片
                String imageUrl = api.generateImage(imageModel, description, null);

                // 生成视频
                String taskId = api.generateVideo(videoModel, description, imageUrl);
                videoUrls[i] = waitForVideo(taskId, videoModel, callback);
            }

            return videoUrls;
        } catch (JSONException e) {
            throw new IOException("分镜解析失败", e);
        }
    }

    // ==================== 等待视频生成 ====================

    private String waitForVideo(String taskId, String model, Callback callback) throws IOException {
        int maxAttempts = 120; // 最多等待10分钟
        for (int i = 0; i < maxAttempts; i++) {
            JSONObject status = api.getVideoStatus(taskId, model);
            String state = status.optString("status", "");

            if ("completed".equals(state) || "success".equals(state)) {
                return status.optString("video_url", "");
            } else if ("failed".equals(state)) {
                throw new IOException("视频生成失败: " + status.optString("error", "未知错误"));
            }

            callback.onProgress("等待生成", "视频生成中... (" + (i + 1) * 5 + "秒)");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("等待被中断", e);
            }
        }
        throw new IOException("视频生成超时");
    }

    // ==================== 一键生成短剧 ====================

    public void generateDrama(String topic, String style, String textModel,
                               String imageModel, String videoModel, Callback callback) {
        new Thread(() -> {
            try {
                // Step 1: 生成剧本
                callback.onProgress("生成剧本", "正在根据主题生成短剧剧本...");
                String script = generateScript(topic, style, textModel);

                // Step 2: 剧本分镜
                callback.onProgress("生成分镜", "正在将剧本转换为分镜...");
                String shots = generateShots(script, textModel);

                // Step 3: 生成视频素材
                callback.onProgress("生成素材", "正在为每个镜头生成视频...");
                String[] videoUrls = generateVisuals(shots, imageModel, videoModel, callback);

                // Step 4: 完成
                DramaResult result = new DramaResult();
                result.script = script;
                result.shots = shots;
                result.videoUrls = videoUrls;
                callback.onComplete(result);

            } catch (IOException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ==================== 结果类 ====================

    public static class DramaResult {
        public String script;
        public String shots;
        public String[] videoUrls;
    }
}
