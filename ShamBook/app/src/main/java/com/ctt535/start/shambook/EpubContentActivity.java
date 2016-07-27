package com.ctt535.start.shambook;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Iterator;
import java.util.List;

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

    private View lastChapter;
    private WebView webviewContent;

    private FrameLayout frameChapter;
    private DrawerLayout tableOfChapters;
    private ArrayAdapter chapterAdapter;
    private ArrayAdapter settingAdapter;
    private ListView listPdfPages;
    private ListView leftDrawer;

    private BookInformation currentBookInfo;
    private ArrayList<String> listBookChapter;
    private ArrayList<String> listBookContent;
    private ArrayList<String> listChapterFile;

    private ArrayList<String> listSettingText;
    private ArrayList<Integer> listSettingImage;

    private ImageButton turnBackBtn;
    private ImageButton tableContentBtn;
    private ImageButton settingBtn;

    private int newUiOptions;
    private String bookSourcePath;
    private StringBuilder bookHtml;

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
        listPdfPages = (ListView) findViewById(R.id.list_pdf_pages);
        listPdfPages.setVisibility(View.GONE);

        //Find id of layout
        leftDrawer = (ListView) findViewById(R.id.listChapter);
        tableOfChapters = (DrawerLayout) findViewById(R.id.tableOfChapters);
        webviewContent = (WebView) findViewById(R.id.webviewContent);
        frameChapter = (FrameLayout) findViewById(R.id.frameChapter);

        webviewContent.getSettings().setJavaScriptEnabled(true);
        webviewContent.setWebViewClient( new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                for (int i=0; i< listChapterFile.size(); i++) {
                    if (url.contains(listChapterFile.get(i))) {
                        leftDrawer.performItemClick(leftDrawer.getChildAt(i), i, leftDrawer.getItemIdAtPosition(i));
                        return true;
                    }
                }
                return true;
            }
        });

        //Initial table content and list setting and set function for leftview
        initSettingList();
        setFunctionForLeftImageButton();

        //Get book path store in the previous activity
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        final String currentBookPath = settings.getString("currentBookPath", "");

        //Read information of book
        if(currentBookPath != null){
            progress = ProgressDialog.show(context, "Please wait...", "Loading book content..." , true);
            progress.setCancelable(true);

            new Thread(new Runnable() {
                @Override
                public void run(){
                    // do the thing that takes a long time
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run(){
                            try {
                                //Create book resources folder and load input stream
                                createBookResourceFolder(currentBookPath);
                                InputStream epubInputStream = new FileInputStream(currentBookPath);

                                // Load Book from inputStream and load resource
                                Book currentBook = (new EpubReader()).readEpub(epubInputStream);
                                currentBookInfo = readBookInformation(currentBook, currentBookPath);
                                loadResource(bookSourcePath, currentBook);

                                listBookChapter = new ArrayList<>();
                                listChapterFile = new ArrayList<>();
                                listBookContent = new ArrayList<>();

                                readTableContent(currentBook.getTableOfContents().getTocReferences());
                                readBookContent(currentBook);

                                epubInputStream.close();

                                //Show table content in leftview first
                                showTableContent();
                            }catch (Exception e){}
                            progress.dismiss();
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public void onBackPressed(){
        //Delete book resource folder that already created
        File file = new File(bookSourcePath);
        deleteDirectory(file);

        Intent intent = new Intent(EpubContentActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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
            listChapterFile.add(tocReference.getResource().getHref());

            listBookChapter.add(tocReference.getTitle());
            readTableContent(tocReference.getChildren());
        }
    }

    private void readBookContent(Book currentBook){
        Spine spine = currentBook.getSpine();
        List<SpineReference> spineList = spine.getSpineReferences();
        int count = spineList.size();
        int chapPos = 0;

        bookHtml = new StringBuilder();
        bookHtml.append("<!DOCTYPE html><html><header><style>img{width: 100%; height: 100%;} " +
                "</style></header></html>\n");

        for (int i = 0; i < count; i++) {
            try {
                Resource res = spine.getResource(i);
                InputStream is = res.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String fileName = res.getHref();

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.replace("../", "");
                    bookHtml.append(line + "\n").toString();
                }

                if(chapPos < listChapterFile.size() - 1) {
                    if (fileName.equals(listChapterFile.get(chapPos))) {
                        listBookContent.add(bookHtml.toString());
                        bookHtml = new StringBuilder();
                        chapPos++;
                    }
                }

                res.close();is.close();reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        listBookContent.add(bookHtml.toString());

        bookHtml = new StringBuilder();
        for (String content: listBookContent)
            bookHtml.append(content);
        webviewContent.loadDataWithBaseURL("file://"+ bookSourcePath, bookHtml.toString(), "text/html", "utf-8", null);
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
                if(position == 2) {
                    showDialogChooseColor();
                }else if(position == 3){
                    new AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_information)
                            .setTitle("Book Information")
                            .setMessage("Title: " + currentBookInfo.title +
                                    "\n\nAuthors: " + currentBookInfo.authors +
                                    "\n\nFormat: " + currentBookInfo.format +
                                    "\n\nBook path: " + currentBookInfo.filePath)
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

    private void showDialogChooseColor(){
        ArrayList<Integer> listColor = new ArrayList<>();
        listColor.add(Color.parseColor("#fcdada"));
        listColor.add(Color.parseColor("#fcd0ba"));
        listColor.add(Color.parseColor("#e3e3e3"));
        listColor.add(Color.parseColor("#bfbdbc"));
        listColor.add(Color.parseColor("#ffffff"));
        listColor.add(Color.parseColor("#c3feee"));
        listColor.add(Color.parseColor("#fcdca4"));
        listColor.add(Color.parseColor("#c2a5ff"));
        listColor.add(Color.parseColor("#fbf78f"));
        listColor.add(Color.parseColor("#e1b8fe"));
        listColor.add(Color.parseColor("#fca4bd"));
        listColor.add(Color.parseColor("#c1b7fc"));

        // Custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.color_dialog_layout);
        ItemColorClickInDialog(listColor, dialog);
        Button cancelDialog = (Button) dialog.findViewById(R.id.cancelDialog);
        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void ItemColorClickInDialog(final ArrayList<Integer> listColor, final Dialog dialog){
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
        leftDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showContentInChapter(view, position);
            }
        });
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

    private BookInformation readBookInformation(Book book, String currentBookPath){
        BookInformation bookInfo = new BookInformation();

        bookInfo.authors = book.getMetadata().getAuthors().toString();
        bookInfo.title = book.getTitle();
        bookInfo.filePath = currentBookPath;
        bookInfo.type = book.getMetadata().getTypes().toString();
        bookInfo.publishers = book.getMetadata().getPublishers().toString();
        bookInfo.precentRead = 0;

        //Get book format
        String bformat = book.getMetadata().getFormat();
        if (bformat.toLowerCase().contains("epub"))
            bookInfo.format = "epub";

        return bookInfo;
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

        lastChapter = view;
        view.setBackgroundColor(0xFFD0F79A);
        tableOfChapters.closeDrawers();

        bookHtml = new StringBuilder();
        for (int i = pos; i < listBookContent.size(); i++)
            bookHtml.append(listBookContent.get(i));

        webviewContent.loadDataWithBaseURL("file://"+ bookSourcePath, bookHtml.toString(), "text/html", "utf-8", null);
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
