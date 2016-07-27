package com.ctt535.start.shambook;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ListBookAdapter extends ArrayAdapter<BookInformation> {
    Activity context;
    ArrayList<BookInformation> listBook;
    int backgroundColor;

    public ListBookAdapter(Activity context, ArrayList<BookInformation> listBook, int backgroundColor) {
        super(context, R.layout.list_book_layout, listBook);

        this.context = context;
        this.listBook = listBook;
        this.backgroundColor = backgroundColor;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.list_book_layout, null,true);

        ImageView coverImage = (ImageView) rowView.findViewById(R.id.coverImage);
        TextView bookTitle = (TextView) rowView.findViewById(R.id.bookTitle);
        TextView bookAuthor = (TextView) rowView.findViewById(R.id.bookAuthor);
        TextView percentRead = (TextView) rowView.findViewById(R.id.percentRead);
        TextView bookFormat = (TextView) rowView.findViewById(R.id.bookFormat);
        BookInformation bofo = listBook.get(position);

        coverImage.setImageBitmap(bofo.coverImage);
        bookTitle.setText(bofo.title);

        String temp = "Format: " + bofo.format;
        bookFormat.setText(temp);

        temp = "Read: " + bofo.precentRead + "%";
        percentRead.setText(temp);

        try{
            temp = "Authors: " + bofo.authors.substring(1, bofo.authors.length() - 1);
        }catch (Exception ex){
            temp = "Authors: Unknow";
        }
        bookAuthor.setText(temp);

        rowView.setBackgroundColor(backgroundColor);
        return rowView;
    }
}
