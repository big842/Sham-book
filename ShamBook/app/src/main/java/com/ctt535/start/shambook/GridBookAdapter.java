package com.ctt535.start.shambook;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by vumin_000 on 23/07/2016.
 */
public class GridBookAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Bitmap> bookImages;

    // Constructor
    public GridBookAdapter(Context c, ArrayList<Bitmap> bookImages) {
        mContext = c;
        this.bookImages = bookImages;
    }

    public int getCount() {
        return bookImages.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(300, 370));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
        else{
            imageView = (ImageView) convertView;
        }

        imageView.setImageBitmap(bookImages.get(position));
        return imageView;
    }
}
