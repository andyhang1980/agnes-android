package com.agnes.studio;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ApiService api;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    // 主界面组件
    private EditText etInput;
    private TextView tvLog;
    private ScrollView svLog;
    private TextView tvImageLabel, tvVideoLabel, tvTextLabel;
    private RecyclerView rvImages, rvVideos;
    private ScrollView svTextAssets;
    private TextView tvTextAssets;
    private View tvEmpty;

    // 侧边栏组件
    private Spinner drawerSpUrl, drawerSpTextModel, drawerSpStoryboardModel, drawerSpImageModel, drawerSpVideoModel;
    private EditText drawerEtCustomUrl, drawerEtApiKey, drawerEtScriptPrompt, drawerEtShotsPrompt;
    private TextView drawerTvKeyStats;

    // 适配器
    private ImageAssetAdapter imageAdapter;
    private VideoAssetAdapter videoAdapter;

    // Agnes 默认模型
    private static final String AGNES_TEXT = "agnes-2.5-flash";
    private static final String AGNES_IMAGE = "agnes-image-2.5-flash";
    private static final String AGNES_VIDEO = "agnes-video-2.5-flash";

    // 当前状态
    private String lastScript;
    private String lastShots;
    private String lastImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new ApiService(this);

        initToolbar();
        initDrawer();
        initMainContent();
        initDrawerContent();
        loadConfig();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initDrawer() {
        NavigationView navView = findViewById(R.id.nav_drawer);
        navView.setNavigationItemSelectedListener(this);
    }

    private void initMainContent() {
        etInput = findViewById(R.id.et_input);
        tvLog = findViewById(R.id.tv_log);
        svLog = findViewById(R.id.sv_log);
        tvImageLabel = findViewById(R.id.tv_image_label);
        tvVideoLabel = findViewById(R.id.tv_video_label);
        tvTextLabel = findViewById(R.id.tv_text_label);
        rvImages = findViewById(R.id.rv_images);
        rvVideos = findViewById(R.id.rv_videos);
        svTextAssets = findViewById(R.id.sv_text_assets);
        tvTextAssets = findViewById(R.id.tv_text_assets);
        tvEmpty = findViewById(R.id.tv_empty);

        // 图片资产列表
        imageAdapter = new ImageAssetAdapter();
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(imageAdapter);

        // 视频资产列表
        videoAdapter = new VideoAssetAdapter();
        rvVideos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvVideos.setAdapter(videoAdapter);

        // 高级提示词选择
        setupPromptSpinner();

        // 工作流按钮
        findViewById(R.id.btn_gen_script).setOnClickListener(v -> generateScript());
        findViewById(R.id.btn_gen_shots).setOnClickListener(v -> generateShots());
        findViewById(R.id.btn_gen_image).setOnClickListener(v -> generateImage());
        findViewById(R.id.btn_gen_video).setOnClickListener(v -> generateVideo());
        findViewById(R.id.btn_generate).setOnClickListener(v -> generateDrama());
        findViewById(R.id.btn_clear_log).setOnClickListener(v -> {
            tvLog.setText("");
            appendLog("日志已清空");
        });
    }

    private void setupPromptSpinner() {
        Spinner spPrompt = findViewById(R.id.sp_prompt_template);
        List<String> templates = new ArrayList<>(PromptTemplates.SCRIPT_PROMPTS.keySet());
        spPrompt.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, templates));

        TextView tvPreview = findViewById(R.id.tv_prompt_preview);

        findViewById(R.id.btn_apply_prompt).setOnClickListener(v -> {
            int pos = spPrompt.getSelectedItemPosition();
            String key = templates.get(pos);
            String value = PromptTemplates.SCRIPT_PROMPTS.get(key);
            etInput.setHint("提示词: " + key);
            tvPreview.setText("当前提示词: " + key + "\n" + value.substring(0, Math.min(100, value.length())) + "...");
            appendLog("已加载提示词: " + key);
        });
    }

    private void initDrawerContent() {
        drawerSpUrl = findViewById(R.id.drawer_sp_url);
        drawerSpTextModel = findViewById(R.id.drawer_sp_text_model);
        drawerSpStoryboardModel = findViewById(R.id.drawer_sp_storyboard_model);
        drawerSpImageModel = findViewById(R.id.drawer_sp_image_model);
        drawerSpVideoModel = findViewById(R.id.drawer_sp_video_model);
        drawerEtCustomUrl = findViewById(R.id.drawer_et_custom_url);
        drawerEtApiKey = findViewById(R.id.drawer_et_api_key);
        drawerEtScriptPrompt = findViewById(R.id.drawer_et_script_prompt);
        drawerEtShotsPrompt = findViewById(R.id.drawer_et_shots_prompt);
        drawerTvKeyStats = findViewById(R.id.drawer_tv_key_stats);

        // API URL 下拉
        List<String> urlNames = new ArrayList<>();
        for (String[] item : ApiService.API_URLS) urlNames.add(item[0]);
        drawerSpUrl.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, urlNames));

        // 模型下拉
        setupModelSpinner(drawerSpTextModel, ApiService.TEXT_MODELS);
        setupModelSpinner(drawerSpStoryboardModel, ApiService.TEXT_MODELS);
        setupModelSpinner(drawerSpImageModel, ApiService.IMAGE_MODELS);
        setupModelSpinner(drawerSpVideoModel, ApiService.VIDEO_MODELS);

        // URL 选择监听
        drawerSpUrl.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String url = ApiService.API_URLS[position][1];
                drawerEtCustomUrl.setVisibility(url.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Key 管理
        findViewById(R.id.drawer_btn_add_key).setOnClickListener(v -> {
            String key = drawerEtApiKey.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, "请输入 Key", Toast.LENGTH_SHORT).show();
                return;
            }
            api.addApiKey(key);
            drawerEtApiKey.setText("");
            updateKeyDisplay();
            appendLog("Key 已添加: " + maskKey(key));
        });

        findViewById(R.id.drawer_btn_remove_key).setOnClickListener(v -> {
            List<String> keys = api.getApiKeyList();
            if (keys.isEmpty()) {
                Toast.makeText(this, "暂无 Key", Toast.LENGTH_SHORT).show();
                return;
            }
            String current = api.getCurrentApiKey();
            api.removeApiKey(current);
            updateKeyDisplay();
            appendLog("Key 已删除: " + maskKey(current));
        });

        // 保存配置
        findViewById(R.id.drawer_btn_save_config).setOnClickListener(v -> {
            saveConfig();
            Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
            appendLog("配置已保存");
        });

        // 默认提示词
        drawerEtScriptPrompt.setText(PromptTemplates.DEFAULT_SCRIPT_SYSTEM);
        drawerEtShotsPrompt.setText(PromptTemplates.DEFAULT_SHOTS_SYSTEM);
    }

    private void setupModelSpinner(Spinner spinner, String[][] models) {
        List<String> names = new ArrayList<>();
        for (String[] m : models) names.add(m[1]);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names));
    }

    // ==================== 配置管理 ====================

    private void loadConfig() {
        String currentUrl = api.getBaseUrl();
        for (int i = 0; i < ApiService.API_URLS.length; i++) {
            if (ApiService.API_URLS[i][1].equals(currentUrl)) {
                drawerSpUrl.setSelection(i);
                break;
            }
        }
        updateKeyDisplay();
    }

    private void saveConfig() {
        // 保存 URL
        int urlPos = drawerSpUrl.getSelectedItemPosition();
        String url = ApiService.API_URLS[urlPos][1];
        if (url.isEmpty()) {
            url = drawerEtCustomUrl.getText().toString().trim();
        }
        if (!url.isEmpty()) api.setBaseUrl(url);

        // 保存提示词
        String scriptPrompt = drawerEtScriptPrompt.getText().toString().trim();
        String shotsPrompt = drawerEtShotsPrompt.getText().toString().trim();
        if (!scriptPrompt.isEmpty()) {
            getSharedPreferences("agnes_prefs", MODE_PRIVATE).edit()
                    .putString("script_prompt", scriptPrompt)
                    .putString("shots_prompt", shotsPrompt)
                    .apply();
        }
    }

    private String getBaseUrl() {
        int pos = drawerSpUrl.getSelectedItemPosition();
        String url = ApiService.API_URLS[pos][1];
        if (url.isEmpty()) url = drawerEtCustomUrl.getText().toString().trim();
        api.setBaseUrl(url);
        return url;
    }

    private boolean isAgnesApi() {
        return drawerSpUrl.getSelectedItemPosition() <= 1;
    }

    private String getTextModel() {
        if (isAgnesApi()) return AGNES_TEXT;
        return ApiService.TEXT_MODELS[drawerSpTextModel.getSelectedItemPosition()][0];
    }

    private String getStoryboardModel() {
        if (isAgnesApi()) return AGNES_TEXT;
        return ApiService.TEXT_MODELS[drawerSpStoryboardModel.getSelectedItemPosition()][0];
    }

    private String getImageModel() {
        if (isAgnesApi()) return AGNES_IMAGE;
        return ApiService.IMAGE_MODELS[drawerSpImageModel.getSelectedItemPosition()][0];
    }

    private String getVideoModel() {
        if (isAgnesApi()) return AGNES_VIDEO;
        return ApiService.VIDEO_MODELS[drawerSpVideoModel.getSelectedItemPosition()][0];
    }

    private void updateKeyDisplay() {
        List<String> keys = api.getApiKeyList();
        drawerTvKeyStats.setText("Keys: " + api.getKeyStats() + " | 轮询中");
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    // ==================== 导航菜单 ====================

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        // 可以在这里处理侧边栏菜单点击
        drawerLayout.closeDrawers();
        return true;
    }

    // ==================== 日志 ====================

    private void appendLog(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvLog.append("[" + time + "] " + msg + "\n");
        svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN));
    }

    // ==================== 资产显示 ====================

    private void showImageAsset(File file) {
        tvImageLabel.setVisibility(View.VISIBLE);
        rvImages.setVisibility(View.VISIBLE);
        imageAdapter.addItem(file);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showVideoAsset(File file) {
        tvVideoLabel.setVisibility(View.VISIBLE);
        rvVideos.setVisibility(View.VISIBLE);
        videoAdapter.addItem(file);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showTextAsset(String text) {
        tvTextLabel.setVisibility(View.VISIBLE);
        svTextAssets.setVisibility(View.VISIBLE);
        tvTextAssets.append(text + "\n\n");
        tvEmpty.setVisibility(View.GONE);
    }

    // ==================== 工作流：① 剧本 ====================

    private void generateScript() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入主题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (api.getApiKeyList().isEmpty()) {
            Toast.makeText(this, "请先配置 API Key", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("① 生成剧本");
        pd.setMessage("正在生成...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                String systemPrompt = drawerEtScriptPrompt.getText().toString().trim();
                if (systemPrompt.isEmpty()) systemPrompt = PromptTemplates.DEFAULT_SCRIPT_SYSTEM;

                appendLog("开始生成剧本...");
                appendLog("模型: " + getTextModel());
                appendLog("Key: " + maskKey(api.getCurrentApiKey()));

                String result = api.chatCompletion(getTextModel(), input, systemPrompt);
                lastScript = result;

                runOnUiThread(() -> {
                    pd.dismiss();
                    showTextAsset("=== 剧本 ===\n\n" + result);
                    appendLog("剧本生成完成");
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    appendLog("剧本生成失败: " + e.getMessage());
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 工作流：② 分镜 ====================

    private void generateShots() {
        if (lastScript == null || lastScript.isEmpty()) {
            Toast.makeText(this, "请先生成剧本", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("② 生成分镜");
        pd.setMessage("正在生成...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                String systemPrompt = drawerEtShotsPrompt.getText().toString().trim();
                if (systemPrompt.isEmpty()) systemPrompt = PromptTemplates.DEFAULT_SHOTS_SYSTEM;

                appendLog("开始生成分镜...");
                appendLog("模型: " + getStoryboardModel());

                String result = api.chatCompletion(getStoryboardModel(), lastScript, systemPrompt);
                lastShots = result;

                runOnUiThread(() -> {
                    pd.dismiss();
                    showTextAsset("=== 分镜 ===\n\n" + result);
                    appendLog("分镜生成完成");
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    appendLog("分镜生成失败: " + e.getMessage());
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 工作流：③ 文生图 ====================

    private void generateImage() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入图片描述", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("③ 文生图");
        pd.setMessage("正在生成...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                appendLog("开始生成图片...");
                appendLog("模型: " + getImageModel());

                String imageUrl = api.generateImage(getImageModel(), input, null);
                byte[] imageData = api.downloadFile(imageUrl);

                String filename = "agnes_" + System.currentTimeMillis() + ".png";
                File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File file = new File(dir, filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(imageData);
                fos.close();
                lastImagePath = file.getAbsolutePath();

                runOnUiThread(() -> {
                    pd.dismiss();
                    showImageAsset(file);
                    appendLog("图片已保存: " + filename);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    appendLog("图片生成失败: " + e.getMessage());
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 工作流：④ 图生视频 ====================

    private void generateVideo() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入视频描述", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("④ 图生视频");
        pd.setMessage("正在提交...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                appendLog("开始生成视频...");
                appendLog("模型: " + getVideoModel());

                String taskId = api.generateVideo(getVideoModel(), input, lastImagePath);
                appendLog("任务ID: " + taskId);

                for (int i = 0; i < 120; i++) {
                    Thread.sleep(5000);
                    org.json.JSONObject status = api.getVideoStatus(taskId, getVideoModel());
                    String state = status.optString("status", "");

                    if ("completed".equals(state) || "success".equals(state)) {
                        String videoUrl = status.optString("video_url", "");
                        byte[] videoData = api.downloadFile(videoUrl);

                        String filename = "agnes_" + System.currentTimeMillis() + ".mp4";
                        File dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                        File file = new File(dir, filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(videoData);
                        fos.close();

                        runOnUiThread(() -> {
                            pd.dismiss();
                            showVideoAsset(file);
                            appendLog("视频已保存: " + filename);
                        });
                        return;
                    } else if ("failed".equals(state)) {
                        throw new IOException("视频生成失败");
                    }
                    final int sec = (i + 1) * 5;
                    runOnUiThread(() -> pd.setMessage("生成中... " + sec + "秒"));
                }
                throw new IOException("超时");
            } catch (IOException | InterruptedException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    appendLog("视频生成失败: " + e.getMessage());
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 一键全流程 ====================

    private void generateDrama() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入短剧主题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (api.getApiKeyList().isEmpty()) {
            Toast.makeText(this, "请先配置 API Key", Toast.LENGTH_SHORT).show();
            return;
        }

        findViewById(R.id.btn_generate).setEnabled(false);
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("一键生成短剧");
        pd.setMessage("准备中...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                String textModel = getTextModel();
                String imageModel = getImageModel();
                String videoModel = getVideoModel();

                // ① 剧本
                pd.setMessage("① 生成剧本...");
                appendLog("========== 开始全流程 ==========");
                appendLog("① 生成剧本 [模型: " + textModel + "]");
                String scriptPrompt = drawerEtScriptPrompt.getText().toString().trim();
                if (scriptPrompt.isEmpty()) scriptPrompt = PromptTemplates.DEFAULT_SCRIPT_SYSTEM;
                String script = api.chatCompletion(textModel, input, scriptPrompt);
                lastScript = script;
                appendLog("剧本生成完成");

                // ② 分镜
                pd.setMessage("② 生成分镜...");
                appendLog("② 生成分镜 [模型: " + textModel + "]");
                String shotsPrompt = drawerEtShotsPrompt.getText().toString().trim();
                if (shotsPrompt.isEmpty()) shotsPrompt = PromptTemplates.DEFAULT_SHOTS_SYSTEM;
                String shots = api.chatCompletion(textModel, script, shotsPrompt);
                lastShots = shots;
                appendLog("分镜生成完成");

                // ③ 文生图
                pd.setMessage("③ 生成封面图...");
                appendLog("③ 文生图 [模型: " + imageModel + "]");
                String imageUrl = api.generateImage(imageModel, input, null);

                // ④ 图生视频
                pd.setMessage("④ 生成视频...");
                appendLog("④ 图生视频 [模型: " + videoModel + "]");
                String taskId = api.generateVideo(videoModel, input, null);
                appendLog("视频任务ID: " + taskId);

                // 等待视频
                String videoPath = null;
                for (int i = 0; i < 120; i++) {
                    Thread.sleep(5000);
                    org.json.JSONObject status = api.getVideoStatus(taskId, videoModel);
                    String state = status.optString("status", "");
                    if ("completed".equals(state) || "success".equals(state)) {
                        String videoUrl = status.optString("video_url", "");
                        byte[] videoData = api.downloadFile(videoUrl);
                        String filename = "drama_" + System.currentTimeMillis() + ".mp4";
                        File dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                        File file = new File(dir, filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(videoData);
                        fos.close();
                        videoPath = file.getAbsolutePath();
                        appendLog("视频生成完成");
                        break;
                    } else if ("failed".equals(state)) {
                        throw new IOException("视频生成失败");
                    }
                    pd.setMessage("等待视频... " + ((i + 1) * 5) + "秒");
                }

                // 保存图片
                byte[] imgData = api.downloadFile(imageUrl);
                String imgFile = "drama_" + System.currentTimeMillis() + ".png";
                File imgDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File imgPath = new File(imgDir, imgFile);
                FileOutputStream imgFos = new FileOutputStream(imgPath);
                imgFos.write(imgData);
                imgFos.close();

                final String finalVideoPath = videoPath;
                final File finalImgFile = imgPath;

                runOnUiThread(() -> {
                    pd.dismiss();
                    findViewById(R.id.btn_generate).setEnabled(true);

                    showTextAsset("=== 剧本 ===\n\n" + script);
                    showTextAsset("=== 分镜 ===\n\n" + shots);
                    showImageAsset(finalImgFile);
                    if (finalVideoPath != null) {
                        showVideoAsset(new File(finalVideoPath));
                    }
                    appendLog("========== 全流程完成 ==========");
                });

            } catch (IOException | InterruptedException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    findViewById(R.id.btn_generate).setEnabled(true);
                    appendLog("全流程失败: " + e.getMessage());
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
