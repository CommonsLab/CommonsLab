package com.commonslab.commonslab.Utils.RecyclerViewAdapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.commonslab.commonslab.R;

import java.util.ArrayList;
import java.util.Random;

import apiwrapper.commons.wikimedia.org.Models.Contribution;

/**
 * Created by valdio on 05/12/2016.
 */

public class AudioRecyclerViewAdapter extends RecyclerView.Adapter<AudioRecyclerViewAdapter.ViewHolder> {

    private ArrayList<Contribution> contributions;
    private Context context;

    public AudioRecyclerViewAdapter(ArrayList<Contribution> contributions, Context context) {
        this.contributions = contributions;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.audio_recyclerview_item, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.audioTitle.setText(contributions.get(position).getTitle().substring(5)); //Remove "File:" form the title
        holder.imageView.setImageDrawable(context.getResources().getDrawable(getRandomDrawableResource()));
    }

    @Override
    public int getItemCount() {
        if (contributions == null)
            return 0;
        return contributions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView audioTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.audio_play_pause);
            audioTitle = (TextView) itemView.findViewById(R.id.audio_title);
        }
    }


    private int getRandomDrawableResource() {
        int resourceId = R.drawable.random_1;
        Random random = new Random();
        int randomIndex = random.nextInt(8 - 1 + 1) + 1;
        switch (randomIndex) {
            case 1:
                resourceId = R.drawable.random_1;
                break;
            case 2:
                resourceId = R.drawable.random_2;
                break;
            case 3:
                resourceId = R.drawable.random_3;
                break;
            case 4:
                resourceId = R.drawable.random_4;
                break;
            case 5:
                resourceId = R.drawable.random_5;
                break;
            case 6:
                resourceId = R.drawable.random_6;
                break;
            case 7:
                resourceId = R.drawable.random_7;
                break;
            case 8:
                resourceId = R.drawable.random_8;
                break;
            default:
                resourceId = R.drawable.random_8;
        }

        return resourceId;
    }
}
