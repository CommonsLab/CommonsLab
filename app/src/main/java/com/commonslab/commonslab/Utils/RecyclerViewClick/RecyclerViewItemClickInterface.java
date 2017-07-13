package com.commonslab.commonslab.Utils.RecyclerViewClick;

import android.view.View;

/**
 * Created by valdio on 04/12/2016.
 */

public interface RecyclerViewItemClickInterface {
    public void onClick(View view, int position);

    public void onLongClick(View view, int position);
}