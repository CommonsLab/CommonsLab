package com.commonslab.commonslab.Utils.RecyclerViewAdapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

import com.bumptech.glide.Glide;

import apiwrapper.commons.wikimedia.org.Models.FeedItem;

import com.commonslab.commonslab.R;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

/**
 * Created by Valdio Veliu on 27/01/2017.
 */

public class MOD_Adapter extends RecyclerView.Adapter<MOD_Adapter.ViewHolder> {

    private ArrayList<FeedItem> feedItems;
    private Context context;

    public MOD_Adapter(ArrayList<FeedItem> feedItems, Context context) {
        this.feedItems = feedItems;
        this.context = context;
    }

    @Override
    public MOD_Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item, parent, false);
        MOD_Adapter.ViewHolder holder = new MOD_Adapter.ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(MOD_Adapter.ViewHolder holder, int position) {
        String imageURL = null;
        try {
            imageURL = feedItems.get(position).getMediaLink();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int artificialHeight;
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = 200 * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT); //het height to 200dp
        artificialHeight = (int) px;

        if (imageURL != null)
            if (position % 3 == 0) { //large image
                Glide.with(context)
                        .load(imageURL)
                        .override(SIZE_ORIGINAL, artificialHeight)
                        .centerCrop()
                        .into(holder.imageView);
            } else {
                Glide.with(context)
                        .load(imageURL)
                        .centerCrop()
                        .into(holder.imageView);
            }
    }

    @Override
    public int getItemCount() {
        if (feedItems == null)
            return 0;
        return feedItems.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public LinearLayout containerLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.recyclerviewImageView);
            containerLayout = (LinearLayout) itemView.findViewById(R.id.recyclerview_overlay_container);
            containerLayout.setVisibility(View.VISIBLE);
        }
    }
}
