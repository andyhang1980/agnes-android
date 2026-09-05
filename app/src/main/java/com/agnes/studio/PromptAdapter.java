package com.agnes.studio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PromptAdapter extends RecyclerView.Adapter<PromptAdapter.ViewHolder> {

    private final List<String> keys = new ArrayList<>();
    private final List<String> values = new ArrayList<>();
    private int selectedPosition = 0;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String key, String value);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(Map<String, String> data) {
        keys.clear();
        values.clear();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }
        notifyDataSetChanged();
    }

    public String getSelectedKey() {
        if (selectedPosition >= 0 && selectedPosition < keys.size()) {
            return keys.get(selectedPosition);
        }
        return "";
    }

    public String getSelectedValue() {
        if (selectedPosition >= 0 && selectedPosition < values.size()) {
            return values.get(selectedPosition);
        }
        return "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String key = keys.get(position);
        holder.tvName.setText(key);

        holder.itemView.setSelected(position == selectedPosition);
        holder.tvName.setTextColor(position == selectedPosition ? 0xFF2196F3 : 0xFF333333);

        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onItemClick(keys.get(selectedPosition), values.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return keys.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(android.R.id.text1);
        }
    }
}
