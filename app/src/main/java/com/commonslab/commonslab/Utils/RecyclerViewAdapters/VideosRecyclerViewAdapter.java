package com.commonslab.commonslab.Utils.RecyclerViewAdapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.commonslab.commonslab.R;

import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Models.Thumbnail;

/**
 * Created by valdio on 04/12/2016.
 */

public class VideosRecyclerViewAdapter extends RecyclerView.Adapter<VideosRecyclerViewAdapter.ViewHolder> {


    private ArrayList<Thumbnail> contributions;
    private Context context;

    public VideosRecyclerViewAdapter(ArrayList<Thumbnail> contributions, Context context) {
        this.contributions = contributions;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String imageURL = contributions.get(position).getThumbnailURL();
        if (position > 0 && (position + 2) % 3 == 0) {
            //one index after the large image is displayed
            // create 4:3 size, videos thumbnail do not have 4:3 display relations
            int artificialHeight = (contributions.get(position).getThumbnailWidth() / 4) * 3;
            Glide.with(context)
                    .load(imageURL)
                    .override(contributions.get(position - 1).getThumbnailWidth(), artificialHeight)
                    .centerCrop()
                    .into(holder.imageView);

        } else if (position > 0 && (position + 1) % 3 == 0) {
            //one index before the large image is displayed
            //load the size of the previously displayed view
            Glide.with(context)
                    .load(imageURL)
                    .override(contributions.get(position - 1).getThumbnailWidth(), contributions.get(position - 1).getThumbnailHeight())
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            Glide.with(context)
                    .load(imageURL)
                    .override(contributions.get(position).getThumbnailWidth(), contributions.get(position).getThumbnailHeight())
                    .centerCrop()
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        if (contributions == null)
            return 0;
        return contributions.size();
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