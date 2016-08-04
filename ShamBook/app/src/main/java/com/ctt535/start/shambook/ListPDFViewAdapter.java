package com.ctt535.start.shambook;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by minhdai on 01/08/2016.
 */
public class ListPDFViewAdapter extends ArrayAdapter<Bitmap> {
    private Context context;
    private Bitmap[] allpages;
    private int pWidth;
    private int pHeight;
    public static float ZoomFactor = 1.0f;

    public ListPDFViewAdapter(Context context, Bitmap[] allpages) {
        super(context, R.layout.pdf_image_layout, allpages);

        this.context = context;
        this.allpages = allpages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.pdf_image_layout, parent, false);

        ImageView pageImage = (ImageView)rowView.findViewById(R.id.fileImage);
        Bitmap bPage = allpages[position];
        if(bPage != null) {
            pWidth = bPage.getWidth();
            pHeight = bPage.getHeight();

            pageImage.setLayoutParams(new LinearLayout.LayoutParams(Math.round((float) bPage.getWidth() * ZoomFactor),
                    Math.round((float) bPage.getHeight() * ZoomFactor)));
        }else{
            pageImage.setLayoutParams(new LinearLayout.LayoutParams(Math.round((float) pWidth * ZoomFactor),
                    Math.round((float) pHeight * ZoomFactor)));
        }

        pageImage.setImageBitmap(bPage);
        return rowView;
    }

}

