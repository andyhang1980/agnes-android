package com.agnes.studio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoAssetAdapter extends RecyclerView.Adapter<VideoAssetAdapter.ViewHolder> {

    private final List<File> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(File file);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void addItem(File file) {
        items.add(file);
        notifyItemInserted(items.size() - 1);
    }

    public void clear() {
        int size = items.size();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    public List<File> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_asset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = items.get(position);
        holder.tvName.setText(file.getName());
        holder.ivPlay.setOnClickListener(v -> {
            holder.vvVideo.setVideoURI(android.net.Uri.fromFile(file));
            holder.vvVideo.start();
            holder.ivPlay.setVisibility(View.GONE);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(file);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        VideoView vvVideo;
        ImageView ivPlay;
        TextView tvName;

        ViewHolder(View view) {
            super(view);
            vvVideo = view.findViewById(R.id.vv_asset);
            ivPlay = view.findViewById(R.id.iv_play);
            tvName = view.findViewById(R.id.tv_asset_name);
        }
    }
}
