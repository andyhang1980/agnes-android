package com.agnes.studio;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkflowDialog {

    private final Dialog dialog;
    private final TextView tvStep1, tvStep2, tvStep3, tvStep4;
    private final TextView tvLog;
    private final ScrollView svLog;
    private final TextView tvTitle;
    private final Button btnCancel;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private OnCancelListener cancelListener;
    private boolean cancelled = false;

    public interface OnCancelListener {
        void onCancel();
    }

    public WorkflowDialog(Context context) {
        dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        View view = View.inflate(context, R.layout.dialog_workflow, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawableResource(android.R.color.white);
        }

        tvTitle = view.findViewById(R.id.dialog_title);
        tvStep1 = view.findViewById(R.id.dialog_step1);
        tvStep2 = view.findViewById(R.id.dialog_step2);
        tvStep3 = view.findViewById(R.id.dialog_step3);
        tvStep4 = view.findViewById(R.id.dialog_step4);
        tvLog = view.findViewById(R.id.dialog_log);
        svLog = view.findViewById(R.id.dialog_sv_log);
        btnCancel = view.findViewById(R.id.dialog_btn_cancel);

        btnCancel.setOnClickListener(v -> {
            cancelled = true;
            if (cancelListener != null) cancelListener.onCancel();
            dialog.dismiss();
        });
    }

    public void setOnCancelListener(OnCancelListener listener) {
        this.cancelListener = listener;
    }

    public void show() {
        resetSteps();
        cancelled = false;
        dialog.show();
    }

    public void dismiss() {
        uiHandler.post(() -> {
            if (dialog.isShowing()) dialog.dismiss();
        });
    }

    public boolean isCancelled() {
        return cancelled;
    }

    private void resetSteps() {
        tvStep1.setText("① 生成剧本 ⏳");
        tvStep1.setTextColor(0xFF999999);
        tvStep2.setText("② 生成分镜 ⏳");
        tvStep2.setTextColor(0xFF999999);
        tvStep3.setText("③ 文生图 ⏳");
        tvStep3.setTextColor(0xFF999999);
        tvStep4.setText("④ 生成视频 ⏳");
        tvStep4.setTextColor(0xFF999999);
        tvLog.setText("");
        tvTitle.setText("🚀 生成中...");
    }

    public void setStepActive(int step) {
        uiHandler.post(() -> {
            TextView tv = getStepView(step);
            if (tv != null) {
                tv.setTextColor(0xFF2196F3);
            }
        });
    }

    public void setStepDone(int step) {
        uiHandler.post(() -> {
            TextView tv = getStepView(step);
            if (tv != null) {
                String prefix = getStepPrefix(step);
                tv.setText(prefix + " ✅");
                tv.setTextColor(0xFF4CAF50);
            }
        });
    }

    public void setStepError(int step, String error) {
        uiHandler.post(() -> {
            TextView tv = getStepView(step);
            if (tv != null) {
                String prefix = getStepPrefix(step);
                tv.setText(prefix + " ❌ " + error);
                tv.setTextColor(0xFFF44336);
            }
        });
    }

    public void setProgress(String msg) {
        uiHandler.post(() -> tvTitle.setText(msg));
    }

    public void appendLog(String msg) {
        uiHandler.post(() -> {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            tvLog.append("[" + time + "] " + msg + "\n");
            svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN));
        });
    }

    private TextView getStepView(int step) {
        switch (step) {
            case 1: return tvStep1;
            case 2: return tvStep2;
            case 3: return tvStep3;
            case 4: return tvStep4;
            default: return null;
        }
    }

    private String getStepPrefix(int step) {
        switch (step) {
            case 1: return "① 生成剧本";
            case 2: return "② 生成分镜";
            case 3: return "③ 文生图";
            case 4: return "④ 生成视频";
            default: return "";
        }
    }
}
