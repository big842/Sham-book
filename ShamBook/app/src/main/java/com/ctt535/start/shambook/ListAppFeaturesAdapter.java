package com.ctt535.start.shambook;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by vumin_000 on 23/07/2016.
 */
public class ListAppFeaturesAdapter extends ArrayAdapter<String> {
    Activity context;
    ArrayList<Integer> featureImageId;
    ArrayList<String> featuresName;

    public ListAppFeaturesAdapter(Activity context, ArrayList<Integer> featureImageId,  ArrayList<String> featuresName) {
        super(context, R.layout.list_app_feature, featuresName);

        this.context = context;
        this.featureImageId = featureImageId;
        this.featuresName = featuresName;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.list_app_feature, null,true);

        ImageView ftureImg = (ImageView) rowView.findViewById(R.id.img_feature);
        TextView ftureName = (TextView) rowView.findViewById(R.id.text_feature);

        ftureImg.setImageResource(featureImageId.get(position));
        ftureName.setText(featuresName.get(position));
        return rowView;
    }

}
