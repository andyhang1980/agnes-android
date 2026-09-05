package com.agnes.studio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageAssetAdapter extends RecyclerView.Adapter<ImageAssetAdapter.ViewHolder> {

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
                .inflate(R.layout.item_image_asset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = items.get(position);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        holder.ivAsset.setImageBitmap(bitmap);
        holder.tvName.setText(file.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(file);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAsset;
        TextView tvName;

        ViewHolder(View view) {
            super(view);
            ivAsset = view.findViewById(R.id.iv_asset);
            tvName = view.findViewById(R.id.tv_asset_name);
        }
    }
}
