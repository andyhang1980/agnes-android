package com.agnes.studio;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CacheManager {

    private static final String PREFS_CACHE = "agnes_cache";
    private final SharedPreferences prefs;

    public CacheManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE);
    }

    // ==================== 输入缓存 ====================

    public void saveInput(String text) {
        prefs.edit().putString("last_input", text).apply();
    }

    public String loadInput() {
        return prefs.getString("last_input", "");
    }

    // ==================== 提示词缓存 ====================

    public void saveScriptPrompt(String prompt) {
        prefs.edit().putString("script_prompt", prompt).apply();
    }

    public String loadScriptPrompt() {
        return prefs.getString("script_prompt", "");
    }

    public void saveShotsPrompt(String prompt) {
        prefs.edit().putString("shots_prompt", prompt).apply();
    }

    public String loadShotsPrompt() {
        return prefs.getString("shots_prompt", "");
    }

    public void saveNegativePrompt(String prompt) {
        prefs.edit().putString("negative_prompt", prompt).apply();
    }

    public String loadNegativePrompt() {
        return prefs.getString("negative_prompt", "");
    }

    // ==================== 上次生成结果缓存 ====================

    public void saveLastScript(String script) {
        prefs.edit().putString("last_script", script).apply();
    }

    public String loadLastScript() {
        return prefs.getString("last_script", "");
    }

    public void saveLastShots(String shots) {
        prefs.edit().putString("last_shots", shots).apply();
    }

    public String loadLastShots() {
        return prefs.getString("last_shots", "");
    }

    // ==================== 资产历史 ====================

    public void addAsset(String type, String path, long timestamp) {
        try {
            String key = "assets_" + type;
            String json = prefs.getString(key, "[]");
            JSONArray arr = new JSONArray(json);
            JSONObject item = new JSONObject();
            item.put("path", path);
            item.put("time", timestamp);
            arr.put(item);
            if (arr.length() > 50) {
                JSONArray trimmed = new JSONArray();
                for (int i = arr.length() - 50; i < arr.length(); i++) {
                    trimmed.put(arr.getJSONObject(i));
                }
                arr = trimmed;
            }
            prefs.edit().putString(key, arr.toString()).apply();
        } catch (JSONException e) {
            // ignore
        }
    }

    public List<String> getAssets(String type) {
        List<String> list = new ArrayList<>();
        try {
            String json = prefs.getString("assets_" + type, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getJSONObject(i).getString("path"));
            }
        } catch (JSONException e) {
            // ignore
        }
        return list;
    }

    // ==================== 工作流状态 ====================

    public void saveWorkflowStep(int step) {
        prefs.edit().putInt("workflow_step", step).apply();
    }

    public int loadWorkflowStep() {
        return prefs.getInt("workflow_step", 0);
    }

    public void clearWorkflowState() {
        prefs.edit().remove("workflow_step").apply();
    }
}
