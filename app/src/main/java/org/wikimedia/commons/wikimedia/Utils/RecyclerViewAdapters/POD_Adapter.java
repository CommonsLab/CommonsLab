package org.wikimedia.commons.wikimedia.Utils.RecyclerViewAdapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.wikimedia.commons.wikimedia.R;

import java.util.ArrayList;

import apiwrapper.commons.wikimedia.org.Models.FeedItem;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

/**
 * Created by Valdio Veliu on 27/01/2017.
 */

public class POD_Adapter extends RecyclerView.Adapter<POD_Adapter.ViewHolder> {

    private ArrayList<FeedItem> feedItems;
    private Context context;

    public POD_Adapter(ArrayList<FeedItem> feedItems, Context context) {
        this.feedItems = feedItems;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item, parent, false);
        POD_Adapter.ViewHolder holder = new POD_Adapter.ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String imageURL = feedItems.get(position).getMediaLink();
        int artificialHeight;
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = 200 * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT); //het height to 200dp
        artificialHeight = (int) px;


        if (position % 3 == 0) { //large image
            if (imageURL.endsWith(".gif"))
                Glide.with(context)
                        .load(imageURL)
                        .asGif()
                        .override(SIZE_ORIGINAL, artificialHeight)
                        .centerCrop()
                        .into(holder.imageView);
            else
                Glide.with(context)
                        .load(imageURL)
                        .override(SIZE_ORIGINAL, artificialHeight)
                        .centerCrop()
                        .into(holder.imageView);
        } else {

            if (imageURL.endsWith(".gif"))
                Glide.with(context)
                        .load(imageURL)
                        .asGif()
                        .centerCrop()
                        .into(holder.imageView);
            else
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

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.recyclerviewImageView);
        }
    }
}
