package com.ctt535.start.shambook;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ListFileAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final List<Integer> listFolder;
    private final List<String> listName;
    private final int backgroundColor;

    public ListFileAdapter(Activity context, List<Integer> listFolder, List<String> fileName, int backgroundColor) {
        super(context, R.layout.list_file_layout, fileName);
        this.context=context;
        this.listFolder=listFolder;
        this.listName=fileName;
        this.backgroundColor = backgroundColor;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.list_file_layout, null,true);

        ImageView imageView = (ImageView) rowView.findViewById(R.id.fileImage);
        TextView fileView = (TextView) rowView.findViewById(R.id.fileName);

        if(listFolder.get(position) == 1)
            imageView.setImageResource(R.drawable.folder_icon);
        else
            imageView.setImageResource(R.drawable.file_icon);

        fileView.setText(" " + listName.get(position));
        rowView.setBackgroundColor(backgroundColor);
        return rowView;
    }
}
