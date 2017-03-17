package org.wikimedia.commons.wikimedia.Utils.RecyclerViewClick;

import android.view.View;

/**
 * Created by valdio on 04/12/2016.
 */

public interface RecyclerViewItemClickInterface {
    public void onClick(View view, int position);

    public void onLongClick(View view, int position);
}