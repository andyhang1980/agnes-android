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

    public String generateScript(String topic, String style,
                                  String baseUrl, String apiKey, String model) throws IOException {
        String systemPrompt = "你是一个专业短剧编剧。根据用户给的主题，生成一个完整的短剧剧本。" +
                "要求：1.包含3-5个场景 2.每个场景有画面描述和台词 3.适合短视频平台风格";
        String prompt = "主题：" + topic + "\n风格：" + style + "\n\n请生成短剧剧本";
        return api.chatCompletion(baseUrl, apiKey, model, prompt, systemPrompt);
    }

    public String generateShots(String script,
                                 String baseUrl, String apiKey, String model) throws IOException {
        String systemPrompt = "你是专业分镜师。将剧本转换为分镜列表，每个分镜包含画面描述、镜头类型、时长。";
        String prompt = "剧本：\n" + script + "\n\n请生成分镜列表";
        return api.chatCompletion(baseUrl, apiKey, model, prompt, systemPrompt);
    }

    public String[] generateVisuals(String shotsJson,
                                     String imageBaseUrl, String imageKey, String imageModel,
                                     String videoBaseUrl, String videoKey, String videoModel,
                                     Callback callback) throws IOException {
        try {
            JSONArray shots = new JSONArray(shotsJson);
            String[] videoUrls = new String[shots.length()];
            for (int i = 0; i < shots.length(); i++) {
                JSONObject shot = shots.getJSONObject(i);
                String desc = shot.getString("shot");
                callback.onProgress("素材", "第 " + (i + 1) + "/" + shots.length() + " 个镜头");
                String imageUrl = api.generateImage(imageBaseUrl, imageKey, imageModel, desc);
                String taskId = api.generateVideo(videoBaseUrl, videoKey, videoModel, desc, null, null, null);
                videoUrls[i] = waitForVideo(taskId, videoModel, callback);
            }
            return videoUrls;
        } catch (JSONException e) {
            throw new IOException("分镜解析失败", e);
        }
    }

    private String waitForVideo(String taskId, String model, Callback callback) throws IOException {
        for (int i = 0; i < 120; i++) {
            JSONObject status = api.getVideoStatus(taskId, model);
            String state = status.optString("status", "");
            if ("completed".equals(state) || "success".equals(state)) {
                return status.optString("video_url", "");
            } else if ("failed".equals(state)) {
                throw new IOException("视频生成失败");
            }
            callback.onProgress("等待", "视频生成中 " + ((i + 1) * 5) + "秒");
            try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
        }
        throw new IOException("视频生成超时");
    }

    public static class DramaResult {
        public String script;
        public String shots;
        public String[] videoUrls;
    }
}
