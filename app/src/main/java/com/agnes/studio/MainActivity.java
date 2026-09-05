package com.agnes.studio;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ApiService api;
    private CacheManager cache;
    private DrawerLayout drawerLayout;
    private WorkflowDialog workflowDialog;
    private static final int REQ_FIRST_FRAME = 1001;
    private static final int REQ_LAST_FRAME = 1002;

    // 主界面
    private EditText etInput;
    private TextView tvLog;
    private ScrollView svLog;
    private TextView tvImageLabel, tvVideoLabel, tvTextLabel;
    private RecyclerView rvImages, rvVideos;
    private ScrollView svTextAssets;
    private TextView tvTextAssets;
    private View tvEmpty;

    // 侧边栏 - 文本
    private Spinner spTextUrl, spTextModel;
    private EditText etTextCustomUrl, etTextKey, etTextCustomModel;

    // 侧边栏 - 图片
    private Spinner spImageUrl, spImageModel;
    private EditText etImageCustomUrl, etImageKey, etImageCustomModel;

    // 侧边栏 - 视频
    private Spinner spVideoUrl, spVideoModel, spVideoMode;
    private EditText etVideoCustomUrl, etVideoKey, etVideoCustomModel;

    // 侧边栏 - 首帧/尾帧
    private ImageView ivFirstFrame, ivLastFrame;
    private TextView tvFirstFrameName, tvLastFrameName;
    private File firstFrameFile, lastFrameFile;

    // 侧边栏 - 提示词
    private EditText etScriptPrompt, etShotsPrompt, etNegativePrompt;

    // 适配器
    private ImageAssetAdapter imageAdapter;
    private VideoAssetAdapter videoAdapter;

    // 状态
    private String lastScript;
    private String lastShots;
    private String lastImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        api = new ApiService(this);
        cache = new CacheManager(this);
        workflowDialog = new WorkflowDialog(this);

        initToolbar();
        initDrawer();
        initMainContent();
        initDrawerContent();
        loadConfig();
        loadCachedData();
        setupAutoSave();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initDrawer() {
        NavigationView navView = findViewById(R.id.nav_drawer);
        navView.setVerticalScrollBarEnabled(true);
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

        imageAdapter = new ImageAssetAdapter();
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(imageAdapter);
        imageAdapter.setOnItemClickListener(file -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "image/*");
            startActivity(intent);
        });

        videoAdapter = new VideoAssetAdapter();
        rvVideos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvVideos.setAdapter(videoAdapter);
        videoAdapter.setOnItemClickListener(file -> playVideo(file));

        setupPromptSpinner();
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
            tvPreview.setText("当前: " + key);
            appendLog("已加载提示词: " + key);
        });
    }

    private void initDrawerContent() {
        spTextUrl = findViewById(R.id.drawer_sp_text_url);
        spTextModel = findViewById(R.id.drawer_sp_text_model);
        etTextCustomUrl = findViewById(R.id.drawer_et_text_custom_url);
        etTextKey = findViewById(R.id.drawer_et_text_key);
        etTextCustomModel = findViewById(R.id.drawer_et_text_custom_model);

        spImageUrl = findViewById(R.id.drawer_sp_image_url);
        spImageModel = findViewById(R.id.drawer_sp_image_model);
        etImageCustomUrl = findViewById(R.id.drawer_et_image_custom_url);
        etImageKey = findViewById(R.id.drawer_et_image_key);
        etImageCustomModel = findViewById(R.id.drawer_et_image_custom_model);

        spVideoUrl = findViewById(R.id.drawer_sp_video_url);
        spVideoModel = findViewById(R.id.drawer_sp_video_model);
        spVideoMode = findViewById(R.id.drawer_sp_video_mode);
        etVideoCustomUrl = findViewById(R.id.drawer_et_video_custom_url);
        etVideoKey = findViewById(R.id.drawer_et_video_key);
        etVideoCustomModel = findViewById(R.id.drawer_et_video_custom_model);

        ivFirstFrame = findViewById(R.id.drawer_iv_first_frame);
        ivLastFrame = findViewById(R.id.drawer_iv_last_frame);
        tvFirstFrameName = findViewById(R.id.drawer_tv_first_frame_name);
        tvLastFrameName = findViewById(R.id.drawer_tv_last_frame_name);

        etScriptPrompt = findViewById(R.id.drawer_et_script_prompt);
        etShotsPrompt = findViewById(R.id.drawer_et_shots_prompt);
        etNegativePrompt = findViewById(R.id.drawer_et_negative_prompt);

        List<String> urlNames = new ArrayList<>();
        for (String[] item : ApiService.API_URLS) urlNames.add(item[0]);
        urlNames.add("自定义");
        ArrayAdapter<String> urlAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, urlNames);
        spTextUrl.setAdapter(urlAdapter);
        spImageUrl.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, urlNames));
        spVideoUrl.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, urlNames));

        setupModelSpinner(spTextModel, ApiService.TEXT_MODELS, etTextCustomModel);
        setupModelSpinner(spImageModel, ApiService.IMAGE_MODELS, etImageCustomModel);
        setupModelSpinner(spVideoModel, ApiService.VIDEO_MODELS, etVideoCustomModel);

        String[] videoModes = {"text（文字生成）", "keyframe（首帧控制）", "keyframe（尾帧控制）", "keyframe（首尾帧控制）"};
        spVideoMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, videoModes));

        spTextUrl.setOnItemSelectedListener(new SimpleItemSelectedListener(pos ->
                etTextCustomUrl.setVisibility(pos >= ApiService.API_URLS.length ? View.VISIBLE : View.GONE)));
        spImageUrl.setOnItemSelectedListener(new SimpleItemSelectedListener(pos ->
                etImageCustomUrl.setVisibility(pos >= ApiService.API_URLS.length ? View.VISIBLE : View.GONE)));
        spVideoUrl.setOnItemSelectedListener(new SimpleItemSelectedListener(pos ->
                etVideoCustomUrl.setVisibility(pos >= ApiService.API_URLS.length ? View.VISIBLE : View.GONE)));

        findViewById(R.id.drawer_btn_pick_first_frame).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_FIRST_FRAME);
        });
        findViewById(R.id.drawer_btn_clear_first_frame).setOnClickListener(v -> {
            firstFrameFile = null;
            ivFirstFrame.setImageBitmap(null);
            tvFirstFrameName.setText("未选择");
        });

        findViewById(R.id.drawer_btn_pick_last_frame).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQ_LAST_FRAME);
        });
        findViewById(R.id.drawer_btn_clear_last_frame).setOnClickListener(v -> {
            lastFrameFile = null;
            ivLastFrame.setImageBitmap(null);
            tvLastFrameName.setText("未选择");
        });

        findViewById(R.id.drawer_btn_save_config).setOnClickListener(v -> {
            saveConfig();
            Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        });
    }

    // ==================== 自动保存 ====================

    private void setupAutoSave() {
        etInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                cache.saveInput(s.toString());
            }
        });
    }

    private void loadCachedData() {
        String cachedInput = cache.loadInput();
        if (!cachedInput.isEmpty()) etInput.setText(cachedInput);

        String cachedScriptPrompt = cache.loadScriptPrompt();
        if (!cachedScriptPrompt.isEmpty()) etScriptPrompt.setText(cachedScriptPrompt);

        String cachedShotsPrompt = cache.loadShotsPrompt();
        if (!cachedShotsPrompt.isEmpty()) etShotsPrompt.setText(cachedShotsPrompt);

        String cachedNeg = cache.loadNegativePrompt();
        if (!cachedNeg.isEmpty()) etNegativePrompt.setText(cachedNeg);

        lastScript = cache.loadLastScript();
        lastShots = cache.loadLastShots();

        if (lastScript != null && !lastScript.isEmpty()) {
            showTextAsset("=== 上次剧本 ===\n\n" + lastScript);
            appendLog("已恢复上次剧本");
        }
        if (lastShots != null && !lastShots.isEmpty()) {
            showTextAsset("=== 上次分镜 ===\n\n" + lastShots);
            appendLog("已恢复上次分镜");
        }

        List<String> imagePaths = cache.getAssets("image");
        for (String path : imagePaths) {
            File f = new File(path);
            if (f.exists()) showImageAsset(f);
        }

        List<String> videoPaths = cache.getAssets("video");
        for (String path : videoPaths) {
            File f = new File(path);
            if (f.exists()) showVideoAsset(f);
        }

        if (!imagePaths.isEmpty() || !videoPaths.isEmpty() || (lastScript != null && !lastScript.isEmpty())) {
            appendLog("已从缓存恢复 " + imagePaths.size() + " 张图片, " + videoPaths.size() + " 个视频");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveConfig();
        cache.saveScriptPrompt(etScriptPrompt.getText().toString().trim());
        cache.saveShotsPrompt(etShotsPrompt.getText().toString().trim());
        cache.saveNegativePrompt(etNegativePrompt.getText().toString().trim());
    }

    // ==================== 配置 ====================

    private void loadConfig() {
        loadSectionUrl(spTextUrl, etTextCustomUrl, "text");
        loadSectionUrl(spImageUrl, etImageCustomUrl, "image");
        loadSectionUrl(spVideoUrl, etVideoCustomUrl, "video");
        etTextKey.setText(api.getSectionKey("text"));
        etImageKey.setText(api.getSectionKey("image"));
        etVideoKey.setText(api.getSectionKey("video"));
        loadSectionModel(spTextModel, etTextCustomModel, "text", ApiService.TEXT_MODELS);
        loadSectionModel(spImageModel, etImageCustomModel, "image", ApiService.IMAGE_MODELS);
        loadSectionModel(spVideoModel, etVideoCustomModel, "video", ApiService.VIDEO_MODELS);
    }

    private void loadSectionUrl(Spinner spinner, EditText customEt, String section) {
        String url = api.getSectionUrl(section);
        for (int i = 0; i < ApiService.API_URLS.length; i++) {
            if (ApiService.API_URLS[i][1].equals(url)) { spinner.setSelection(i); customEt.setVisibility(View.GONE); return; }
        }
        spinner.setSelection(ApiService.API_URLS.length);
        customEt.setVisibility(View.VISIBLE);
        customEt.setText(url);
    }

    private void loadSectionModel(Spinner spinner, EditText customEt, String section, String[][] models) {
        String model = api.getSectionModel(section);
        if (model.isEmpty()) { spinner.setSelection(0); customEt.setVisibility(View.GONE); return; }
        for (int i = 0; i < models.length; i++) {
            if (models[i][0].equals(model)) { spinner.setSelection(i); customEt.setVisibility(View.GONE); return; }
        }
        spinner.setSelection(models.length);
        customEt.setVisibility(View.VISIBLE);
        customEt.setText(model);
    }

    private void saveConfig() {
        saveSection("text", spTextUrl, etTextCustomUrl, etTextKey, spTextModel, etTextCustomModel, ApiService.TEXT_MODELS);
        saveSection("image", spImageUrl, etImageCustomUrl, etImageKey, spImageModel, etImageCustomModel, ApiService.IMAGE_MODELS);
        saveSection("video", spVideoUrl, etVideoCustomUrl, etVideoKey, spVideoModel, etVideoCustomModel, ApiService.VIDEO_MODELS);
    }

    private void saveSection(String section, Spinner urlSpinner, EditText customUrlEt, EditText keyEt,
                             Spinner modelSpinner, EditText customModelEt, String[][] models) {
        int urlPos = urlSpinner.getSelectedItemPosition();
        if (urlPos < ApiService.API_URLS.length) api.setSectionUrl(section, ApiService.API_URLS[urlPos][1]);
        else { api.setSectionUrl(section, "custom"); api.setSectionCustomUrl(section, customUrlEt.getText().toString().trim()); }
        api.setSectionKey(section, keyEt.getText().toString().trim());
        int modelPos = modelSpinner.getSelectedItemPosition();
        if (modelPos < models.length) api.setSectionModel(section, models[modelPos][0]);
        else { api.setSectionModel(section, "custom"); api.setSectionCustomModel(section, customModelEt.getText().toString().trim()); }
    }

    private void setupModelSpinner(Spinner spinner, String[][] models, EditText customEt) {
        List<String> names = new ArrayList<>();
        for (String[] m : models) names.add(m[1]);
        names.add("自定义");
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names));
        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener(pos ->
                customEt.setVisibility(pos >= models.length ? View.VISIBLE : View.GONE)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        try {
            Uri uri = data.getData();
            InputStream is = getContentResolver().openInputStream(uri);
            if (requestCode == REQ_FIRST_FRAME) {
                firstFrameFile = new File(getCacheDir(), "first_frame.jpg");
                copyStreamToFile(is, firstFrameFile);
                ivFirstFrame.setImageBitmap(BitmapFactory.decodeFile(firstFrameFile.getAbsolutePath()));
                tvFirstFrameName.setText(firstFrameFile.getName());
            } else if (requestCode == REQ_LAST_FRAME) {
                lastFrameFile = new File(getCacheDir(), "last_frame.jpg");
                copyStreamToFile(is, lastFrameFile);
                ivLastFrame.setImageBitmap(BitmapFactory.decodeFile(lastFrameFile.getAbsolutePath()));
                tvLastFrameName.setText(lastFrameFile.getName());
            }
            if (is != null) is.close();
        } catch (Exception e) {
            Toast.makeText(this, "选择图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyStreamToFile(InputStream is, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
        fos.close();
    }

    // ==================== 工具 ====================

    private String getTextBaseUrl() { return api.getSectionBaseUrl("text"); }
    private String getTextKey() { return api.getSectionKey("text"); }
    private String getTextModel() { return api.getSectionModelName("text", ApiService.TEXT_MODELS); }
    private String getImageBaseUrl() { return api.getSectionBaseUrl("image"); }
    private String getImageKey() { return api.getSectionKey("image"); }
    private String getImageModel() { return api.getSectionModelName("image", ApiService.IMAGE_MODELS); }
    private String getVideoBaseUrl() { return api.getSectionBaseUrl("video"); }
    private String getVideoKey() { return api.getSectionKey("video"); }
    private String getVideoModel() { return api.getSectionModelName("video", ApiService.VIDEO_MODELS); }
    private String getNegativePrompt() { return etNegativePrompt.getText().toString().trim(); }

    private String getFirstFrameUri() {
        if (firstFrameFile == null) return null;
        try { return api.fileToDataUri(firstFrameFile); } catch (Exception e) { return null; }
    }

    private String getLastFrameUri() {
        if (lastFrameFile == null) return null;
        try { return api.fileToDataUri(lastFrameFile); } catch (Exception e) { return null; }
    }

    private boolean shouldUseFirstFrame() {
        int mode = spVideoMode.getSelectedItemPosition();
        return mode == 1 || mode == 3;
    }

    private boolean shouldUseLastFrame() {
        int mode = spVideoMode.getSelectedItemPosition();
        return mode == 2 || mode == 3;
    }

    private void appendLog(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvLog.append("[" + time + "] " + msg + "\n");
        svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN));
    }

    private void showImageAsset(File file) {
        tvImageLabel.setVisibility(View.VISIBLE);
        rvImages.setVisibility(View.VISIBLE);
        imageAdapter.addItem(file);
        tvEmpty.setVisibility(View.GONE);
        cache.addAsset("image", file.getAbsolutePath(), System.currentTimeMillis());
    }

    private void showVideoAsset(File file) {
        tvVideoLabel.setVisibility(View.VISIBLE);
        rvVideos.setVisibility(View.VISIBLE);
        videoAdapter.addItem(file);
        tvEmpty.setVisibility(View.GONE);
        cache.addAsset("video", file.getAbsolutePath(), System.currentTimeMillis());
    }

    private void showTextAsset(String text) {
        tvTextLabel.setVisibility(View.VISIBLE);
        svTextAssets.setVisibility(View.VISIBLE);
        tvTextAssets.append(text + "\n\n");
        tvEmpty.setVisibility(View.GONE);
    }

    private void playVideo(File file) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("video_path", file.getAbsolutePath());
        startActivity(intent);
    }

    // ==================== 工作流 ====================

    private void generateScript() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) { Toast.makeText(this, "请输入主题", Toast.LENGTH_SHORT).show(); return; }
        if (getTextKey().isEmpty()) { Toast.makeText(this, "请先配置文本 API Key", Toast.LENGTH_SHORT).show(); return; }

        workflowDialog.show();
        workflowDialog.setStepActive(1);
        workflowDialog.appendLog("开始生成剧本...");
        workflowDialog.appendLog("模型: " + getTextModel());

        new Thread(() -> {
            try {
                String sp = etScriptPrompt.getText().toString().trim();
                if (sp.isEmpty()) sp = PromptTemplates.DEFAULT_SCRIPT_SYSTEM;
                workflowDialog.appendLog("系统提示词: " + sp.substring(0, Math.min(50, sp.length())) + "...");
                String result = api.chatCompletion(getTextBaseUrl(), getTextKey(), getTextModel(), input, sp);
                lastScript = result;
                cache.saveLastScript(result);
                workflowDialog.setStepDone(1);
                workflowDialog.appendLog("剧本完成 (" + result.length() + " 字)");
                runOnUiThread(() -> showTextAsset("=== 剧本 ===\n\n" + result));
            } catch (IOException e) {
                workflowDialog.setStepError(1, e.getMessage());
                workflowDialog.appendLog("错误: " + e.getMessage());
            }
            workflowDialog.dismiss();
            runOnUiThread(() -> appendLog("剧本生成完成"));
        }).start();
    }

    private void generateShots() {
        if (lastScript == null || lastScript.isEmpty()) { Toast.makeText(this, "请先生成剧本", Toast.LENGTH_SHORT).show(); return; }

        workflowDialog.show();
        workflowDialog.setStepActive(1);
        workflowDialog.setStepDone(1);
        workflowDialog.setStepActive(2);
        workflowDialog.appendLog("开始生成分镜...");

        new Thread(() -> {
            try {
                String sp = etShotsPrompt.getText().toString().trim();
                if (sp.isEmpty()) sp = PromptTemplates.DEFAULT_SHOTS_SYSTEM;
                String result = api.chatCompletion(getTextBaseUrl(), getTextKey(), getTextModel(), lastScript, sp);
                lastShots = result;
                cache.saveLastShots(result);
                workflowDialog.setStepDone(2);
                workflowDialog.appendLog("分镜完成 (" + result.length() + " 字)");
                runOnUiThread(() -> showTextAsset("=== 分镜 ===\n\n" + result));
            } catch (IOException e) {
                workflowDialog.setStepError(2, e.getMessage());
                workflowDialog.appendLog("错误: " + e.getMessage());
            }
            workflowDialog.dismiss();
            runOnUiThread(() -> appendLog("分镜生成完成"));
        }).start();
    }

    private void generateImage() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) { Toast.makeText(this, "请输入图片描述", Toast.LENGTH_SHORT).show(); return; }
        if (getImageKey().isEmpty()) { Toast.makeText(this, "请先配置图片 API Key", Toast.LENGTH_SHORT).show(); return; }

        workflowDialog.show();
        workflowDialog.setStepActive(3);
        workflowDialog.appendLog("开始生成图片...");
        workflowDialog.appendLog("模型: " + getImageModel());

        new Thread(() -> {
            try {
                String imageUrl = api.generateImage(getImageBaseUrl(), getImageKey(), getImageModel(), input);
                workflowDialog.appendLog("图片 URL 获取成功, 下载中...");
                byte[] data = api.downloadFile(imageUrl);
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "agnes_" + System.currentTimeMillis() + ".png");
                FileOutputStream fos = new FileOutputStream(file); fos.write(data); fos.close();
                lastImagePath = file.getAbsolutePath();
                workflowDialog.setStepDone(3);
                workflowDialog.appendLog("图片完成: " + file.getName());
                runOnUiThread(() -> showImageAsset(file));
            } catch (IOException e) {
                workflowDialog.setStepError(3, e.getMessage());
                workflowDialog.appendLog("错误: " + e.getMessage());
            }
            workflowDialog.dismiss();
            runOnUiThread(() -> appendLog("图片生成完成"));
        }).start();
    }

    private void generateVideo() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) { Toast.makeText(this, "请输入视频描述", Toast.LENGTH_SHORT).show(); return; }
        if (getVideoKey().isEmpty()) { Toast.makeText(this, "请先配置视频 API Key", Toast.LENGTH_SHORT).show(); return; }

        workflowDialog.show();
        workflowDialog.setStepActive(4);
        workflowDialog.appendLog("开始生成视频...");
        workflowDialog.appendLog("模型: " + getVideoModel());

        String ff = shouldUseFirstFrame() ? getFirstFrameUri() : null;
        String lf = shouldUseLastFrame() ? getLastFrameUri() : null;
        String neg = getNegativePrompt();
        String mode = (ff != null || lf != null) ? "keyframe" : "text";
        workflowDialog.appendLog("模式: " + mode);
        if (ff != null) workflowDialog.appendLog("已添加首帧图片");
        if (lf != null) workflowDialog.appendLog("已添加尾帧图片");
        if (!neg.isEmpty()) workflowDialog.appendLog("负面提示词: " + neg);

        new Thread(() -> {
            try {
                String taskId = api.generateVideo(getVideoBaseUrl(), getVideoKey(), getVideoModel(), input, ff, lf, neg);
                workflowDialog.appendLog("任务ID: " + taskId);
                for (int i = 0; i < 120; i++) {
                    if (workflowDialog.isCancelled()) { workflowDialog.appendLog("已取消"); return; }
                    Thread.sleep(5000);
                    org.json.JSONObject status = api.getVideoStatus(taskId, getVideoModel());
                    String state = status.optString("status", "");
                    final int sec = (i + 1) * 5;
                    workflowDialog.appendLog("轮询 #" + (i + 1) + " - 状态: " + state + " (" + sec + "秒)");
                    if ("completed".equals(state) || "success".equals(state)) {
                        String videoUrl = status.optString("video_url", "");
                        if (videoUrl.isEmpty()) {
                            org.json.JSONObject meta = status.optJSONObject("metadata");
                            if (meta != null) videoUrl = meta.optString("url", "");
                        }
                        workflowDialog.appendLog("下载视频...");
                        byte[] data = api.downloadFile(videoUrl);
                        File file = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "agnes_" + System.currentTimeMillis() + ".mp4");
                        FileOutputStream fos = new FileOutputStream(file); fos.write(data); fos.close();
                        workflowDialog.setStepDone(4);
                        workflowDialog.appendLog("视频完成: " + file.getName());
                        runOnUiThread(() -> showVideoAsset(file));
                        break;
                    } else if ("failed".equals(state)) {
                        workflowDialog.setStepError(4, "生成失败");
                        workflowDialog.appendLog("视频生成失败");
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                workflowDialog.setStepError(4, e.getMessage());
                workflowDialog.appendLog("错误: " + e.getMessage());
            }
            workflowDialog.dismiss();
            runOnUiThread(() -> appendLog("视频生成完成"));
        }).start();
    }

    private void generateDrama() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) { Toast.makeText(this, "请输入短剧主题", Toast.LENGTH_SHORT).show(); return; }
        if (getTextKey().isEmpty()) { Toast.makeText(this, "请先配置文本 API Key", Toast.LENGTH_SHORT).show(); return; }
        findViewById(R.id.btn_generate).setEnabled(false);

        workflowDialog.show();
        workflowDialog.setProgress("🚀 一键生成短剧");
        workflowDialog.appendLog("主题: " + input);

        final String textModel = getTextModel();
        final String imageModel = getImageModel();
        final String videoModel = getVideoModel();
        final String scriptPrompt = etScriptPrompt.getText().toString().trim().isEmpty() ? PromptTemplates.DEFAULT_SCRIPT_SYSTEM : etScriptPrompt.getText().toString().trim();
        final String shotsPrompt = etShotsPrompt.getText().toString().trim().isEmpty() ? PromptTemplates.DEFAULT_SHOTS_SYSTEM : etShotsPrompt.getText().toString().trim();

        new Thread(() -> {
            try {
                workflowDialog.setStepActive(1);
                workflowDialog.appendLog("① 生成剧本 [" + textModel + "]");
                String script = api.chatCompletion(getTextBaseUrl(), getTextKey(), textModel, input, scriptPrompt);
                lastScript = script;
                cache.saveLastScript(script);
                workflowDialog.setStepDone(1);
                workflowDialog.appendLog("剧本完成 (" + script.length() + " 字)");

                workflowDialog.setStepActive(2);
                workflowDialog.appendLog("② 生成分镜 [" + textModel + "]");
                String shots = api.chatCompletion(getTextBaseUrl(), getTextKey(), textModel, script, shotsPrompt);
                lastShots = shots;
                cache.saveLastShots(shots);
                workflowDialog.setStepDone(2);
                workflowDialog.appendLog("分镜完成 (" + shots.length() + " 字)");

                workflowDialog.setStepActive(3);
                workflowDialog.appendLog("③ 文生图 [" + imageModel + "]");
                String img = api.generateImage(getImageBaseUrl(), getImageKey(), imageModel, input);
                workflowDialog.appendLog("图片 URL 获取成功");

                workflowDialog.setStepActive(4);
                String ff = shouldUseFirstFrame() ? getFirstFrameUri() : null;
                String lf = shouldUseLastFrame() ? getLastFrameUri() : null;
                String neg = getNegativePrompt();
                workflowDialog.appendLog("④ 视频 [" + videoModel + "] 模式:" + ((ff != null || lf != null) ? "keyframe" : "text"));
                String taskId = api.generateVideo(getVideoBaseUrl(), getVideoKey(), videoModel, input, ff, lf, neg);
                workflowDialog.appendLog("任务ID: " + taskId);

                String videoPath = null;
                for (int i = 0; i < 120; i++) {
                    if (workflowDialog.isCancelled()) return;
                    Thread.sleep(5000);
                    org.json.JSONObject st = api.getVideoStatus(taskId, videoModel);
                    String state = st.optString("status", "");
                    workflowDialog.appendLog("轮询 #" + (i + 1) + " - " + state);
                    if ("completed".equals(state) || "success".equals(state)) {
                        String vu = st.optString("video_url", "");
                        if (vu.isEmpty()) {
                            org.json.JSONObject meta = st.optJSONObject("metadata");
                            if (meta != null) vu = meta.optString("url", "");
                        }
                        workflowDialog.appendLog("下载视频...");
                        byte[] vd = api.downloadFile(vu);
                        File vf = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "drama_" + System.currentTimeMillis() + ".mp4");
                        FileOutputStream fos = new FileOutputStream(vf); fos.write(vd); fos.close();
                        videoPath = vf.getAbsolutePath();
                        break;
                    } else if ("failed".equals(state)) throw new IOException("视频失败");
                }

                workflowDialog.appendLog("下载图片...");
                byte[] imgData = api.downloadFile(img);
                File imgFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "drama_" + System.currentTimeMillis() + ".png");
                FileOutputStream imgFos = new FileOutputStream(imgFile); imgFos.write(imgData); imgFos.close();

                workflowDialog.setStepDone(3);
                workflowDialog.setStepDone(4);
                workflowDialog.appendLog("===== 全部完成 =====");

                final String vp = videoPath;
                final File imgFinal = imgFile;
                runOnUiThread(() -> {
                    showTextAsset("=== 剧本 ===\n\n" + script);
                    showTextAsset("=== 分镜 ===\n\n" + shots);
                    showImageAsset(imgFinal);
                    if (vp != null) showVideoAsset(new File(vp));
                });
            } catch (Exception e) {
                workflowDialog.appendLog("错误: " + e.getMessage());
            }
            workflowDialog.dismiss();
            runOnUiThread(() -> {
                findViewById(R.id.btn_generate).setEnabled(true);
                appendLog("全流程完成");
            });
        }).start();
    }

    // ==================== 内部类 ====================

    private static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final OnSelectListener l;
        interface OnSelectListener { void onSelected(int position); }
        SimpleItemSelectedListener(OnSelectListener l) { this.l = l; }
        @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { l.onSelected(pos); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
    }

    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
