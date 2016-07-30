package com.ctt535.start.shambook;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by vumin_000 on 18/07/2016.
 */
public class ListPDFViewAdapter extends ArrayAdapter<Bitmap> {
    private final Activity context;
    private final Bitmap[] listPage;

    public ListPDFViewAdapter(Activity context, Bitmap[] listPage) {
        super(context, R.layout.pdf_image_layout, listPage);
        this.context=context;
        this.listPage=listPage;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.pdf_image_layout, null,true);

        ImageView imageView = (ImageView) rowView.findViewById(R.id.fileImage);
        imageView.setImageBitmap(listPage[position]);

        return rowView;
    }
}
