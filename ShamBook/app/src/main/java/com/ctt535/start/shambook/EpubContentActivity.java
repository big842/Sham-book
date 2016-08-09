package com.ctt535.start.shambook;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

public class EpubContentActivity extends AppCompatActivity {
    static final String PREFS_NAME = "MyPrefsFile";
    private Context context;

    private ProgressDialog progress;
    private SQLiteDatabase bookLibrary;

    private Book currentBook;
    private View lastChapter;
    private WebView webviewContent;
    private WebSettings webSettingsContent;
    private TextView txtTextSize;

    private FrameLayout frameChapter;
    private DrawerLayout tableOfChapters;
    private ArrayAdapter chapterAdapter;
    private ArrayAdapter settingAdapter;
    private ListView leftDrawer;
    private CustomHorizontalScrollView horizontalScroll;

    private BookInformation currentBookInfo;
    private ArrayList<String> listBookChapter;
    private ArrayList<String> listChapterFile;

    private ArrayList<String> listSettingText;
    private ArrayList<Integer> listSettingImage;

    private ImageButton turnBackBtn;
    private ImageButton tableContentBtn;
    private ImageButton settingBtn;

    private int newUiOptions;
    private int defautTextSize;
    private int defaultBackgroundColor;
    private int contentHeight;
    private int currentScrollY;
    private String defaultTextColor;
    private String bookSourcePath;

    Handler hReadBook = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            readTableContent(currentBook.getTableOfContents().getTocReferences());
            readBookContent(currentBook);
            showTableContent();
            progress.dismiss();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Hide action bar
        newUiOptions = getWindow().getDecorView().getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getSupportActionBar().hide();
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);

        //Set content view
        setContentView(R.layout.activity_book_content);
        context =this;

        //Hide imageView because we are reading EPUB
        horizontalScroll = (CustomHorizontalScrollView) findViewById(R.id.horizontalScroll);
        horizontalScroll.setVisibility(View.GONE);

        //Find id of layout
        leftDrawer = (ListView) findViewById(R.id.listChapter);
        tableOfChapters = (DrawerLayout) findViewById(R.id.tableOfChapters);
        frameChapter = (FrameLayout) findViewById(R.id.frameChapter);
        webviewContent = (WebView) findViewById(R.id.webviewContent);

        webSettingsContent = webviewContent.getSettings();
        webviewContent.getSettings().setJavaScriptEnabled(true);

        //Get book path, background color, textsize stored in the previous activity
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        final int currentBookID = settings.getInt("currentBookID", -1);
        defaultBackgroundColor = settings.getInt("epubBackgroundColor", -1);
        defaultTextColor = settings.getString("epubTextColor", "");
        defautTextSize = settings.getInt("epubTextSize", -1);

        //Set default background color, text size
        if(defaultBackgroundColor != -1) {
            frameChapter.setBackgroundColor(defaultBackgroundColor);
            webviewContent.setBackgroundColor(defaultBackgroundColor);
        }
        if(defautTextSize != -1)
            webSettingsContent.setDefaultFontSize(defautTextSize);
        else
            defautTextSize = webSettingsContent.getDefaultFontSize();


        //Initial table content and list setting and set function for leftview
        initSettingList();
        setFunctionForLeftImageButton();

        //Read information of book
        if(currentBookID != -1){
            progress = ProgressDialog.show(context, "Please wait...", "Loading book content..." , true);
            Thread t = new Thread(){
                @Override
                public void run(){
                    try {
                        currentBookInfo = readBookInformation(currentBookID);

                        //Create book resources folder and load input stream
                        createBookResourceFolder(currentBookInfo.getFilePath());
                        InputStream epubInputStream = new FileInputStream(currentBookInfo.getFilePath());

                        // Load Book from inputStream and load resource
                        currentBook = (new EpubReader()).readEpub(epubInputStream);
                        loadResource(bookSourcePath, currentBook);

                        listBookChapter = new ArrayList<>();
                        listChapterFile = new ArrayList<>();

                        Message msg = hReadBook.obtainMessage();
                        hReadBook.sendMessage(msg);
                    }catch (Exception e){
                        e.printStackTrace();
                        progress.dismiss();
                    }
                }
            };
            t.start();
        }
    }

    Handler hBackPress = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            bookLibrary.close();
            Intent intent = new Intent(EpubContentActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            progress.dismiss();
            startActivity(intent);
            finish();
        }
    };

    @Override
    public void onBackPressed(){
        currentScrollY = webviewContent.getScrollY();
        contentHeight = Math.round((float)webviewContent.getContentHeight() * webviewContent.getScale());
        progress = ProgressDialog.show(context, "Please wait...", "Saving book..." , true);

        Thread t = new Thread(){
            @Override
            public void run(){
                try {
                    //Delete book resource folder that already created
                    File file = new File(bookSourcePath);
                    deleteDirectory(file);

                    //Update percent read to database
                    updatePercentReadBooks(currentBookInfo.getId());

                    Message msg = hBackPress.obtainMessage();
                    hBackPress.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                    progress.dismiss();
                }
            }
        };
        t.start();
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);

        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    private void updatePercentReadBooks(int bookId){
        int precentRead = (int)Math.round((currentScrollY *1.0 *100)/contentHeight);

        Date today = new Date();
        String sql = "update books set date_read="+ today.getTime() +", percent_read = "+ precentRead
                + ", current_page = " + currentScrollY + ", total_page = "+ contentHeight + " where id = " + bookId;
        try {
            bookLibrary.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSettingList(){
        listSettingText = new ArrayList<>();
        listSettingText.add("Change text color");
        listSettingText.add("Change text size");
        listSettingText.add("Change background color");
        listSettingText.add("Book information");

        listSettingImage = new ArrayList<>();
        listSettingImage.add(R.drawable.ic_text_color);
        listSettingImage.add(R.drawable.ic_text_size);
        listSettingImage.add(R.drawable.lc_background_color);
        listSettingImage.add(R.drawable.ic_information);
    }

    private void readTableContent(List<TOCReference> tocReferences) {
        if (tocReferences == null) {
            return;
        }

        for (TOCReference tocReference : tocReferences) {
            //Save files position of book content
            String href = tocReference.getResource().getHref();
            if(listChapterFile.size() > 0) {
                if (!href.equals(listChapterFile.get(listChapterFile.size() - 1)))
                    listChapterFile.add(tocReference.getResource().getHref());
            }else{
                listChapterFile.add(href);
            }

            listBookChapter.add(tocReference.getTitle());
            readTableContent(tocReference.getChildren());
        }
    }

    private void readBookContent(Book currentBook){
        if(currentBookInfo.getCurrentPage() != 0)
            webviewContent.setVisibility(View.INVISIBLE);

        Spine spine = currentBook.getSpine();
        List<SpineReference> spineList = spine.getSpineReferences();
        int count = spineList.size();
        int chapPos = 0;
        StringBuilder listBookContentHtml = new StringBuilder();
        listBookContentHtml.append("<!DOCTYPE html><html><header><style>img{width: 100%; height: 100%;} " +
                "html,body{max-width: 99% !important; overflow-y: scroll; overflow-x: hidden;}</style></header><body>\n");
        for (String chapter: listChapterFile){
            String []fName = chapter.split("\\.");
            try {
                listBookContentHtml.append("<a id='chapter_click_" + chapter + "' href='#chapter_name_" + fName[0] + "'></a>");
            }catch (Exception ex){
                listBookContentHtml.append("<a id='chapter_click_" + chapter + "' href='#chapter_name_" + chapter + "'></a>");
            }
        }
        listBookContentHtml.append("</body></html>\n");

        StringBuilder bookHtml = new StringBuilder();
        for (int i = 0; i < count; i++) {
            try {
                Resource res = spine.getResource(i);
                InputStream is = res.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String fileName = res.getHref();
                boolean foundBody = false;

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.replace("../", "");
                    if(!foundBody){
                        if(line.contains("<body")){
                            foundBody = true;
                            if(chapPos < listChapterFile.size() - 1 && fileName.equals(listChapterFile.get(chapPos))) {
                                String []fName = fileName.split("\\.");
                                try {
                                    line += "\n<a id='chapter_name_" + fName[0] + "'></a>";
                                }catch (Exception ex){
                                    line += "\n<a id='chapter_name_" + fileName + "'></a>";
                                }
                            }
                        }
                    }
                    bookHtml.append(line + "\n").toString();
                }

                if(chapPos < listChapterFile.size() - 1) {
                    if (fileName.equals(listChapterFile.get(chapPos))) {
                        listBookContentHtml.append(bookHtml);
                        bookHtml = new StringBuilder();
                        chapPos++;
                    }
                }
                res.close();is.close();reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        listBookContentHtml.append(bookHtml);
        webviewContent.loadDataWithBaseURL("file://"+ bookSourcePath, listBookContentHtml.toString(), "text/html", "utf-8", null);
        if(defaultTextColor != ""){
            webviewContent.loadUrl("javascript:document.body.style.setProperty" +
                    "(\"color\", \"" + defaultTextColor + "\");");
        }

        webviewContent.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(currentBookInfo.getCurrentPage() != 0) {
                    webviewContent.setScrollY(currentBookInfo.getCurrentPage());
                    webviewContent.setVisibility(View.VISIBLE);
                }else{
                    webviewContent.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                for (int i=0; i< listChapterFile.size(); i++) {
                    String []fName = listChapterFile.get(i).split("/");
                    if(fName.length > 1){
                        if (url.contains(fName[fName.length - 1])) {
                            leftDrawer.performItemClick(leftDrawer.getChildAt(i), i, leftDrawer.getItemIdAtPosition(i));
                            return true;
                        }
                    }else{
                        if (url.contains(listChapterFile.get(i))) {
                            leftDrawer.performItemClick(leftDrawer.getChildAt(i), i, leftDrawer.getItemIdAtPosition(i));
                            return true;
                        }
                    }
                }
                return true;
            }
        });

    }

    private void setFunctionForLeftImageButton(){
        turnBackBtn = (ImageButton)findViewById(R.id.btn_back_menu);
        tableContentBtn = (ImageButton)findViewById(R.id.btn_table_content);
        settingBtn = (ImageButton) findViewById(R.id.btn_setting);

        turnBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        tableContentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTableContent();
            }
        });

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingList();
            }
        });
    }

    private void showSettingList(){
        settingAdapter = new ListAppFeaturesAdapter(this, listSettingImage, listSettingText);
        leftDrawer.setAdapter(settingAdapter);
        leftDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                tableOfChapters.closeDrawers();
                if(position == 0) {
                    showDialogChooseTextColor();
                }else if(position == 1){
                    showDialogChooseTextSize();
                }
                else if(position == 2) {
                    showDialogChooseBackgroundColor();
                }else if(position == 3){
                    new AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_information)
                            .setTitle("Book Information")
                            .setMessage("Title: " + currentBookInfo.getName() +
                                    "\n\nAuthors: " + currentBookInfo.getAuthors() +
                                    "\n\nFormat: " + currentBookInfo.getFormat() +
                                    "\n\nBook path: " + currentBookInfo.getFilePath())
                            .setPositiveButton("OK",  new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
                                }
                            }).show();
                }
            }
        });
    }

    private void showDialogChooseTextColor(){
        ArrayList<String> listColor = new ArrayList<>();
        listColor.add("#c9bcbc");
        listColor.add("#b4a298");
        listColor.add("#e3e3e3");
        listColor.add("#000000");
        listColor.add("#ffffff");
        listColor.add("#9ecfc2");
        listColor.add("#718884");
        listColor.add("#8d8d8f");
        listColor.add("#bdbc90");
        listColor.add("#8c8dad");
        listColor.add("#3c3c3c");
        listColor.add("#b1bdac");

        // Custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.book_color_dialog);
        ItemTextColorClickInDialog(listColor, dialog);
        Button cancelDialog = (Button) dialog.findViewById(R.id.cancelDialog);
        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
        });

        dialog.show();
    }

    private void ItemTextColorClickInDialog(final ArrayList<String> listColor, final Dialog dialog){
        ImageButton color1 = (ImageButton) dialog.findViewById(R.id.color1);
        color1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(0), dialog);
            }
        });

        ImageButton color2 = (ImageButton) dialog.findViewById(R.id.color2);
        color2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(1), dialog);
            }
        });

        ImageButton color3 = (ImageButton) dialog.findViewById(R.id.color3);
        color3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(2), dialog);
            }
        });

        ImageButton color4 = (ImageButton) dialog.findViewById(R.id.color4);
        color4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(3), dialog);
            }
        });

        ImageButton color5 = (ImageButton) dialog.findViewById(R.id.color5);
        color5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(4), dialog);
            }
        });

        ImageButton color6 = (ImageButton) dialog.findViewById(R.id.color6);
        color6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(5), dialog);
            }
        });

        ImageButton color7 = (ImageButton) dialog.findViewById(R.id.color7);
        color7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(6), dialog);
            }
        });

        ImageButton color8 = (ImageButton) dialog.findViewById(R.id.color8);
        color8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(7), dialog);
            }
        });

        ImageButton color9 = (ImageButton) dialog.findViewById(R.id.color9);
        color9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(8), dialog);
            }
        });

        ImageButton color10 = (ImageButton) dialog.findViewById(R.id.color10);
        color10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(9), dialog);
            }
        });

        ImageButton color11 = (ImageButton) dialog.findViewById(R.id.color11);
        color11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(10), dialog);
            }
        });

        ImageButton color12 = (ImageButton) dialog.findViewById(R.id.color12);
        color12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextColorForLayout(listColor.get(11), dialog);
            }
        });
    }

    private void setTextColorForLayout(String color, Dialog dialog){
        //Save epub background color
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("epubTextColor", color);
        editor.commit();

        dialog.dismiss();
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);

        defaultTextColor = color;
        webviewContent.loadUrl("javascript:document.body.style.setProperty" +
                "(\"color\", \"" + defaultTextColor + "\");");
    }

    private void showDialogChooseTextSize(){
        // Custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.book_textsize_dialog);

        txtTextSize = (TextView) dialog.findViewById(R.id.textSize);
        txtTextSize.setText(String.format(Locale.getDefault(),"%d", defautTextSize));

        Button btnDecrease = (Button) dialog.findViewById(R.id.btnDecrease);
        btnDecrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(defautTextSize > 0) {
                    defautTextSize--;
                    txtTextSize.setText(String.format(Locale.getDefault(),"%d", defautTextSize));
                }
            }
        });

        Button btnIncrease = (Button) dialog.findViewById(R.id.btnIncrease);
        btnIncrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(defautTextSize < 50) {
                    defautTextSize++;
                    txtTextSize.setText(String.format(Locale.getDefault(),"%d", defautTextSize));
                }
            }
        });

        Button okDialog = (Button) dialog.findViewById(R.id.okDialog);
        okDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String text = txtTextSize.getText().toString();
                    defautTextSize = Integer.parseInt(text);

                    //Save text size
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("epubTextSize", defautTextSize);
                    editor.commit();

                    dialog.dismiss();
                    getWindow().getDecorView().setSystemUiVisibility(newUiOptions);

                    final int scrollPosition = webviewContent.getScrollY();
                    webSettingsContent.setDefaultFontSize(defautTextSize);
                    webviewContent.scrollTo(0, scrollPosition);
                }catch (Exception ex){

                }
            }
        });

        Button cancelDialog = (Button) dialog.findViewById(R.id.cancelDialog);
        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
        });

        dialog.show();
    }

    private void showDialogChooseBackgroundColor(){
        ArrayList<Integer> listColor = new ArrayList<>();
        listColor.add(Color.parseColor("#c9bcbc"));
        listColor.add(Color.parseColor("#b4a298"));
        listColor.add(Color.parseColor("#e3e3e3"));
        listColor.add(Color.parseColor("#000000"));
        listColor.add(Color.parseColor("#ffffff"));
        listColor.add(Color.parseColor("#9ecfc2"));
        listColor.add(Color.parseColor("#718884"));
        listColor.add(Color.parseColor("#8d8d8f"));
        listColor.add(Color.parseColor("#bdbc90"));
        listColor.add(Color.parseColor("#8c8dad"));
        listColor.add(Color.parseColor("#3c3c3c"));
        listColor.add(Color.parseColor("#b1bdac"));

        // Custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.book_color_dialog);
        ItemBackgroundColorClickInDialog(listColor, dialog);
        Button cancelDialog = (Button) dialog.findViewById(R.id.cancelDialog);
        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
        });

        dialog.show();
    }

    private void ItemBackgroundColorClickInDialog(final ArrayList<Integer> listColor, final Dialog dialog){
        ImageButton color1 = (ImageButton) dialog.findViewById(R.id.color1);
        color1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(0), dialog);
            }
        });

        ImageButton color2 = (ImageButton) dialog.findViewById(R.id.color2);
        color2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(1), dialog);
            }
        });

        ImageButton color3 = (ImageButton) dialog.findViewById(R.id.color3);
        color3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(2), dialog);
            }
        });

        ImageButton color4 = (ImageButton) dialog.findViewById(R.id.color4);
        color4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(3), dialog);
            }
        });

        ImageButton color5 = (ImageButton) dialog.findViewById(R.id.color5);
        color5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(4), dialog);
            }
        });

        ImageButton color6 = (ImageButton) dialog.findViewById(R.id.color6);
        color6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(5), dialog);
            }
        });

        ImageButton color7 = (ImageButton) dialog.findViewById(R.id.color7);
        color7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(6), dialog);
            }
        });

        ImageButton color8 = (ImageButton) dialog.findViewById(R.id.color8);
        color8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(7), dialog);
            }
        });

        ImageButton color9 = (ImageButton) dialog.findViewById(R.id.color9);
        color9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(8), dialog);
            }
        });

        ImageButton color10 = (ImageButton) dialog.findViewById(R.id.color10);
        color10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(9), dialog);
            }
        });

        ImageButton color11 = (ImageButton) dialog.findViewById(R.id.color11);
        color11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(10), dialog);
            }
        });

        ImageButton color12 = (ImageButton) dialog.findViewById(R.id.color12);
        color12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColorForLayout(listColor.get(11), dialog);
            }
        });
    }

    private void setBackgroundColorForLayout(int color, Dialog dialog){
        //Save epub background color
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("epubBackgroundColor", color);
        editor.commit();

        frameChapter.setBackgroundColor(color);
        webviewContent.setBackgroundColor(color);
        dialog.dismiss();
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    private void showTableContent(){
        if(listBookChapter.size() == 0) {
            listBookChapter.add("Table of contents is empty");
        }

        chapterAdapter = new ArrayAdapter(context, android.R.layout.simple_list_item_1, listBookChapter){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view;
                if(convertView==null) {
                    LayoutInflater Inflater = (LayoutInflater) EpubContentActivity.this.getSystemService(
                            EpubContentActivity.this.LAYOUT_INFLATER_SERVICE);
                    view = Inflater.inflate(android.R.layout.simple_list_item_1, null);
                }
                else{
                    view=convertView;
                }

                if(position==0){
                    lastChapter = view;
                    view.setBackgroundColor(0xFFC9C7C7);
                }

                TextView txt1=(TextView)view.findViewById(android.R.id.text1);
                txt1.setText(listBookChapter.get(position));

                //Rest of your code
                return view;
            }
        };

        leftDrawer.setAdapter(chapterAdapter);
        if (listBookChapter.size() != 1) {
            leftDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    showContentInChapter(view, position);
                }
            });
        }
    }

    private boolean createBookResourceFolder(String currentBookPath){
        boolean res = false;
        bookSourcePath = "";
        String []listName = currentBookPath.split("/");
        for (int i=0; i< listName.length - 1; i++) {
            if(listName[i] == null || listName[i] == "")
                bookSourcePath += "/";
            else
                bookSourcePath += listName[i] + "/";
        }

        String []fName = listName[listName.length - 1].split("\\.");
        bookSourcePath += fName[0] + "(please_detele_me)/";

        File file = new File(bookSourcePath);
        if (!file.exists()) {
            return file.mkdirs();
        }

        return res;
    }

    private BookInformation readBookInformation(int currentBookID){
        BookInformation bookInfo = new BookInformation();
        String databaseBookPath = getApplication().getFilesDir() + "/" + "book_library";
        bookLibrary = SQLiteDatabase.openDatabase(databaseBookPath, null, SQLiteDatabase.OPEN_READWRITE);

        try {
            String sql = "select * from books where id = " + currentBookID;
            Cursor cur = bookLibrary.rawQuery(sql, null);
            cur.moveToFirst();

            bookInfo.setId(cur.getInt(0));
            bookInfo.setName(cur.getString(1));
            bookInfo.setAuthors(cur.getString(2));
            bookInfo.setFormat(cur.getString(3));
            bookInfo.setFilePath(cur.getString(4));
            bookInfo.setPrecentRead(cur.getInt(6));
            bookInfo.setCurrentPage(cur.getInt(7));
            bookInfo.setTotalPage(cur.getInt(8));

            return bookInfo;
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    private void loadResource(String directory, Book currentBook) {
        try {
            Resources rst = currentBook.getResources();
            Collection<Resource> clrst = rst.getAll();
            Iterator<Resource> itr = clrst.iterator();

            while (itr.hasNext()) {
                Resource rs = itr.next();

                if ((rs.getMediaType() == MediatypeService.JPG)
                        || (rs.getMediaType() == MediatypeService.PNG)
                        || (rs.getMediaType() == MediatypeService.GIF)) {

                    File oppath1 = new File(directory, rs.getHref().replace("OEBPS/", ""));

                    oppath1.getParentFile().mkdirs();
                    oppath1.createNewFile();

                    FileOutputStream fos1 = new FileOutputStream(oppath1);
                    fos1.write(rs.getData());
                    fos1.close();

                } else if (rs.getMediaType() == MediatypeService.CSS) {
                    File oppath = new File(directory, rs.getHref());
                    oppath.getParentFile().mkdirs();
                    oppath.createNewFile();

                    FileOutputStream fos = new FileOutputStream(oppath);
                    fos.write(rs.getData());
                    fos.close();
                }
            }
        } catch (Exception e) {}
    }

    private void  showContentInChapter(View view, final int pos){
        if(lastChapter != null)
            lastChapter.setBackgroundColor(Color.WHITE);

        if(view != null) {
            lastChapter = view;
            view.setBackgroundColor(0xFFD0F79A);
        }

        tableOfChapters.closeDrawers();
        webviewContent.loadUrl("javascript:document.getElementById('chapter_click_"+ listChapterFile.get(pos)+ "').click();");
    }

    private boolean deleteDirectory(File directory) {
        if(! directory.exists() || !directory.isDirectory())    {
            return false;
        }

        String[] files = directory.list();
        for(int i = 0, len = files.length; i < len; i++)    {
            File f = new File(directory, files[i]);
            if(f.isDirectory()) {
                deleteDirectory(f);
            }else   {
                f.delete();
            }
        }
        return directory.delete();
    }
}
