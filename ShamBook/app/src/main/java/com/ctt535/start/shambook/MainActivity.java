package com.ctt535.start.shambook;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;


import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_WRITE_STORAGE = 112;
    static final int REQUEST_READ_STORAGE = 113;
    static final String PREFS_NAME = "MyPrefsFile";

    private Context context;
    private ProgressDialog progress;
    private View viewOptionOpenFile = null;
    private SQLiteDatabase bookLibrary;

    Handler hSearch;
    Handler hReadBooksInFolder;
    Handler hLoadALlBooks;

    private ArrayList<BookInformation> recentBooks = new ArrayList<>();
    private ArrayList<BookInformation> allBooks = new ArrayList<>();
    private ArrayList<BookInformation> listBooksCanOpen = null;
    private ArrayList<Integer> listBooksCantRead;

    private ArrayList<String> fileName = null;
    private ArrayList<String> filePath = null;
    private ArrayList<Integer> fileImage = null;

    private ArrayList<String> listAppFeatures;
    private ArrayList<Integer> listFeatureImageId;

    private FrameLayout frameListFeatures;
    private DrawerLayout drawerAppFeatures;
    private LinearLayout openBooksLayout;
    private LinearLayout handleChooseFileLayout;
    private ListView listBookLayout;
    private ListView listFile;
    private ListView listAppFeaturesLayout;
    private GridView gridBookLayout;

    private ListAppFeaturesAdapter listAppFeaturesAdapter;
    private ListBookAdapter listBookAdapter;

    private FrameLayout.LayoutParams marginBottom;

    private boolean isReadFolder = false; //check press choose folder
    private boolean showGridView = false;
    private boolean isAllBooks = false;
    private int listViewColor = Color.WHITE;
    private String databaseBookPath;
    private String rootDirectory="/";
    private String currentPath = "/";
    private String lastFilePath = "/";

    // 0: at open file, 1: at choose file in directory, 2: at list book,
    // 3: at read book, 4: at root directory of choose file
    private int atWhere = 0;

    private ArrayList<BookInformation> foundBooks;
    private String searchText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);
        context = this;

        //Require permission to read file
        boolean hasPermissionRead = (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermissionRead) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
        }

        //Require permission to write file
        boolean hasPermissionWrite = (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermissionWrite) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }

        //Find layout for margin
        marginBottom = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        //Load background color save in file
        loadSettingSaveInFile();

        //Finde id for main activity
        drawerAppFeatures = (DrawerLayout) findViewById(R.id.drawerAppFeatures);
        handleChooseFileLayout = (LinearLayout) findViewById(R.id.handleFileBtn);
        openBooksLayout = (LinearLayout) findViewById(R.id.openBooks);
        listBookLayout = (ListView) findViewById(R.id.listBooks);
        gridBookLayout = (GridView) findViewById(R.id.gridBooks);
        frameListFeatures = (FrameLayout) findViewById(R.id.frameListFeatures);
        frameListFeatures.setBackgroundColor(listViewColor);
        handleChooseFileLayout.setVisibility(View.GONE);

        //Ininit and load leftview
        initAppSettingList();
        loadLeftViewListAppFeatures();

        //Open database and create table books if no exist
        openBookLibrary();

        //Load book path in file
        recentBooks = readRecentBooksInLibrary();

        if(recentBooks == null || recentBooks.size() == 0){
            //Load book path in file
            readAllBooksInLibrary();
        }else {
            isAllBooks = false;
            handleAfterReadRecentBook(recentBooks);
            Toast.makeText(context, "Show all books read recently...", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_READ_STORAGE:
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "The app was not allowed to read to your storage. Hence, " +
                                    "it cannot function properly. " + "Please consider granting it this permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_WRITE_STORAGE:
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, " +
                                    "it cannot function properly. " + "Please consider granting it this permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if(filePath != null && filePath.get(0).equals("/") && atWhere != 4) {
            atWhere = 4; //set app is at root directory of choose file
            getDirectory(filePath.get(0));
        }else if(atWhere == 4){
            //Remove margin bottom for listbooks view
            marginBottom.setMargins(0, 0, 0, 0); //left,top,right,bottom
            listBookLayout.setLayoutParams(marginBottom);

            handleChooseFileLayout.setVisibility(View.GONE);
            showOpenFileLayout(true);
            atWhere = 0;
        }else if(atWhere == 1){
            getDirectory(filePath.get(0));
        }else if(atWhere == 0){
            if(recentBooks.size() != 0 && allBooks.size() != 0) {

                //app is at list book
                atWhere = 2;

                //Remove margin bottom for listbooks view
                marginBottom.setMargins(0, 0, 0, 0); //left,top,right,bottom
                listBookLayout.setLayoutParams(marginBottom);

                //Disable open Books Layout and handleChooseFileLayout
                openBooksLayout.setVisibility(View.GONE);
                handleChooseFileLayout.setVisibility(View.GONE);

                //Enable viewListBooks
                listBookLayout.setVisibility(View.VISIBLE);

                //View book on screen
                if (recentBooks.size() != 0) {
                    if (showGridView)
                        viewBookInformationInScreenAsGrid(recentBooks);
                    else
                        viewBookInformationInScreenAsList(recentBooks);
                } else {
                    if (showGridView)
                        viewBookInformationInScreenAsGrid(allBooks);
                    else
                        viewBookInformationInScreenAsList(allBooks);
                }
            }else{
                showDialogErrorReadFile("Library is empty");
            }
        }else{
            showDialogConfirm("Are you sure that you want to exit Sham Book?");
        }
    }

    private void loadSettingSaveInFile(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        int saveColor = settings.getInt("background_color", -1);
        if(saveColor != -1)
            listViewColor = saveColor;

        //Load type of view save in file
        showGridView = settings.getBoolean("type_view", false);
    }

    private void initAppSettingList(){
        listAppFeatures = new ArrayList<>();
        listAppFeatures.add("Recent read");
        listAppFeatures.add("All books");
        listAppFeatures.add("Change view");
        listAppFeatures.add("Background color");
        listAppFeatures.add("Search books");
        listAppFeatures.add("Add more books");
        listAppFeatures.add("Go to store");
        listAppFeatures.add("About");
        listAppFeatures.add("Exit");

        listFeatureImageId = new ArrayList<>();
        listFeatureImageId.add(R.drawable.ic_recent_read);
        listFeatureImageId.add(R.drawable.ic_all_books);
        listFeatureImageId.add(R.drawable.ic_view_book);
        listFeatureImageId.add(R.drawable.ic_background_color);
        listFeatureImageId.add(R.drawable.ic_search_book);
        listFeatureImageId.add(R.drawable.ic_add_more_books);
        listFeatureImageId.add(R.drawable.ic_book_store);
        listFeatureImageId.add(R.drawable.ic_app_infor);
        listFeatureImageId.add(R.drawable.ic_app_exit);
    }

    private void loadLeftViewListAppFeatures(){
        listAppFeaturesLayout = (ListView) findViewById(R.id.listAppFeatures) ;
        listAppFeaturesAdapter = new ListAppFeaturesAdapter(this, listFeatureImageId, listAppFeatures);
        listAppFeaturesLayout.setAdapter(listAppFeaturesAdapter);
        listAppFeaturesLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerAppFeatures.closeDrawers();
                switch (position) {
                    case 0:
                        if (atWhere == 0 && (recentBooks == null || recentBooks.size() == 0)) {
                            showDialogErrorReadFile("There aren't any books that read recently");
                            return;
                        }

                        //Set isAllBooks = false; to check show grid view for recent book or all book?
                        isAllBooks = false;

                        //Disable open Books Layout and handleChooseFileLayout
                        openBooksLayout.setVisibility(View.GONE);
                        handleChooseFileLayout.setVisibility(View.GONE);

                        //Show listBookLayout
                        listBookLayout.setVisibility(View.VISIBLE);

                        if (recentBooks.size() != 0) {
                            if(showGridView){
                                viewBookInformationInScreenAsGrid(recentBooks);
                            }else {
                                viewBookInformationInScreenAsList(recentBooks);
                            }
                        } else {
                            showDialogErrorReadFile("There aren't any books that read recently");
                        }
                        break;
                    case 1:
                        if (atWhere == 0 && (allBooks == null || allBooks.size() == 0)) {
                            showDialogErrorReadFile("Library is empty");
                            return;
                        }

                        //Set isAllBooks = false; to check show grid view for recent book or all book?
                        isAllBooks = true;

                        //Disable open Books Layout
                        openBooksLayout.setVisibility(View.GONE);
                        handleChooseFileLayout.setVisibility(View.GONE);

                        //Show listBookLayout
                        listBookLayout.setVisibility(View.VISIBLE);

                        if (allBooks.size() != 0) {
                            if(showGridView){
                                viewBookInformationInScreenAsGrid(allBooks);
                            }else {
                                viewBookInformationInScreenAsList(allBooks);
                            }
                        } else {
                            readAllBooksInLibrary();
                        }
                        break;
                    case 2:
                        showGridView = !showGridView;

                        //Save type of view to file
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("type_view", showGridView);
                        editor.commit();

                        if(showGridView){
                            if(isAllBooks)
                                viewBookInformationInScreenAsGrid(allBooks);
                            else
                                viewBookInformationInScreenAsGrid(recentBooks);
                        }else {
                            if(isAllBooks)
                                viewBookInformationInScreenAsList(allBooks);
                            else
                                viewBookInformationInScreenAsList(recentBooks);
                        }
                        break;
                    case 3:
                        showDialogChooseColor();
                        break;
                    case 4:
                        if(isAllBooks) {
                            showDialogSearchText();
                        }else{
                            showDialogErrorReadFile("Sorry! Can't apply search for books read recently...");
                        }
                        break;
                    case 5:
                        showOpenFileLayout(true);
                        atWhere = 0;
                        break;
                    case 6:
                        atWhere = 0;
                        showOpenFileLayout(true);

                        new AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_information)
                            .setTitle("Information")
                            .setMessage("After your books downloaded, choosing 'Add more books' to move to where you save books")
                            .setPositiveButton("OK",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent i = new Intent(android.content.Intent.ACTION_VIEW);
                                            i.setData(Uri.parse("https://play.google.com/store/books"));
                                            startActivity(i);
                                        }
                                    }).show();

                        break;
                    case 7:
                        new AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_information)
                            .setTitle("About Sham book")
                            .setMessage("Sham book is a application used to read the popular electronic book formats.\n" +
                                    "Currently, Sham book can read two formats: .epub, .pdf\n\n" +
                                    "This app developed by MinhDai-AnhTuan\n" +
                                    "Email: 1353006@student.hcmus.edu.vn - 1353041@student.hcmus.edu.vn")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                        break;
                    case 8:
                        showDialogConfirm("Are you sure that you want to exit Sham Book?");
                        break;
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
        dialog.setContentView(R.layout.app_color_dialog);
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
                listViewColor = listColor.get(0);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color2 = (ImageButton) dialog.findViewById(R.id.color2);
        color2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(1);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color3 = (ImageButton) dialog.findViewById(R.id.color3);
        color3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(2);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color4 = (ImageButton) dialog.findViewById(R.id.color4);
        color4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(3);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color5 = (ImageButton) dialog.findViewById(R.id.color5);
        color5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(4);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color6 = (ImageButton) dialog.findViewById(R.id.color6);
        color6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(5);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color7 = (ImageButton) dialog.findViewById(R.id.color7);
        color7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(6);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color8 = (ImageButton) dialog.findViewById(R.id.color8);
        color8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(7);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color9 = (ImageButton) dialog.findViewById(R.id.color9);
        color9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(8);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color10 = (ImageButton) dialog.findViewById(R.id.color10);
        color10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(9);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color11 = (ImageButton) dialog.findViewById(R.id.color11);
        color11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(10);
                setBackgroundColorForLayout(dialog);
            }
        });

        ImageButton color12 = (ImageButton) dialog.findViewById(R.id.color12);
        color12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listViewColor = listColor.get(11);
                setBackgroundColorForLayout(dialog);
            }
        });
    }

    private void setBackgroundColorForLayout(Dialog dialog){
        //Save background color to file
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("background_color", listViewColor);
        editor.commit();

        frameListFeatures.setBackgroundColor(listViewColor);
        if(showGridView)
            gridBookLayout.setBackgroundColor(listViewColor);
        else if(openBooksLayout.getVisibility() == View.VISIBLE) {
            openBooksLayout.setBackgroundColor(listViewColor);
        }else if(atWhere == 2){
            if(isAllBooks)
                viewBookInformationInScreenAsList(allBooks);
            else
                viewBookInformationInScreenAsList(recentBooks);
        }else if(atWhere == 1){
            getDirectory(lastFilePath);
        }
        dialog.dismiss();
    }

    private void showDialogSearchText(){
        hSearch = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if(foundBooks.size() == 0){
                    showDialogErrorReadFile("Can't find any books match with '" + searchText + "'");
                }else{
                    if(showGridView)
                        viewBookInformationInScreenAsGrid(foundBooks);
                    else
                        viewBookInformationInScreenAsList(foundBooks);
                    showDialogSuccessReadFile("Found " + foundBooks.size() + " books with name " + searchText);
                }
                progress.dismiss();
            }
        };

        // Custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.search_dialog_layout);

        Button cancelDialog = (Button) dialog.findViewById(R.id.cancelDialog);
        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button searchBook = (Button) dialog.findViewById(R.id.searchBooks);
        searchBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(allBooks == null || allBooks.size() == 0) {
                    showDialogErrorReadFile("Library is empty...");
                    return;
                }

                EditText inputText = (EditText)dialog.findViewById(R.id.inputText);
                searchText = inputText.getText().toString();
                dialog.dismiss();
                progress = ProgressDialog.show(context, "Please wait...", "Searching books for text '" + searchText + "' ..." , true);

                Thread t = new Thread() {
                    @Override
                    public void run(){
                        foundBooks = new ArrayList<>();

                        for (int i=0; i<allBooks.size(); i++){
                            BookInformation  bookItem = allBooks.get(i);
                            if(bookItem.getFilePath().contains(searchText) || bookItem.getAuthors().contains(searchText)){
                                foundBooks.add(bookItem);
                            }
                        }

                        Message msg = hSearch.obtainMessage();
                        hSearch.sendMessage(msg);
                    }
                };

                t.start();
            }
        });

        dialog.show();
    }

    private void openBookLibrary(){
        databaseBookPath = getApplication().getFilesDir() + "/" + "book_library";
        bookLibrary = SQLiteDatabase.openDatabase(databaseBookPath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
        bookLibrary.beginTransaction();
        bookLibrary.execSQL("create table if not exists books "
                + " (id integer PRIMARY KEY autoincrement, name text, authors text, format text, path text, " +
                "recent_read integer, percent_read integer, current_page integer, total_page integer, date_read integer); ");
        bookLibrary.setTransactionSuccessful();
        bookLibrary.endTransaction();
    }

    private void showOpenFileLayout(boolean isReturn){
        //Disable viewListBooks
        listBookLayout.setVisibility(View.GONE);
        gridBookLayout.setVisibility(View.GONE);

        //Show option about choose file or directory
        openBooksLayout.setVisibility(View.VISIBLE);
        openBooksLayout.setBackgroundColor(listViewColor);

        //Load list_book_layout
        if(viewOptionOpenFile != null && isReturn){
            viewOptionOpenFile.setVisibility(View.VISIBLE);
        }else {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            viewOptionOpenFile = inflater.inflate(R.layout.open_file_layout, openBooksLayout, true);
        }

        Button btnChooseFile = (Button)viewOptionOpenFile.findViewById(R.id.chooseFile);
        btnChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Not read folder
                isReadFolder = false;

                //View list book layout to show file  and handleChooseFileLayout
                listBookLayout.setVisibility(View.VISIBLE);
                handleChooseFileLayout.setVisibility(View.VISIBLE);

                //Disable openBooksLayout and handleChooseFileLayout
                openBooksLayout.setVisibility(View.GONE);

                //Show list file
                getDirectory(rootDirectory);

                showFileOptionButton(1);
            }
        });

        Button btnChooseFolder = (Button) viewOptionOpenFile.findViewById(R.id.chooseFolder);
        btnChooseFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Not read folder
                isReadFolder = true;

                //Disable openBooksLayout that containt 2 buttons: choose file and choose folder
                openBooksLayout.setVisibility(View.GONE);

                //View list book layout to show file and handleChooseFileLayout
                listBookLayout.setVisibility(View.VISIBLE);
                handleChooseFileLayout.setVisibility(View.VISIBLE);

                //Show list file
                getDirectory(rootDirectory);
                showFileOptionButton(2);
            }
        });
    }

    private  void showFileOptionButton(int type){
        //type = 1: choose file button call, type = 2: choose folder button call

        //Set margin bottom for list view, because, if not, it will be hid beside the bottom buttons.
        marginBottom.setMargins(0, 0, 0, 150); //left,top,right,bottom
        listBookLayout.setLayoutParams(marginBottom);

        Button btnBackDirectory = (Button) findViewById(R.id.btnBackDirectory);
        Button btnChooseThisFolder = (Button) findViewById(R.id.btnChooseThisFolder);
        Button btnExitChooseFile = (Button) findViewById(R.id.btnExitChooseFile);

        if (type == 1)
            btnChooseThisFolder.setVisibility(View.GONE);
        else{
            btnChooseThisFolder.setVisibility(View.VISIBLE);
            //Set click event for button choose folder
            if(btnChooseThisFolder != null) {
                btnChooseThisFolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Remove margin bottom for listbooks view
                        marginBottom.setMargins(0, 0, 0, 0); //left,top,right,bottom
                        listBookLayout.setLayoutParams(marginBottom);

                        readAllBooksInFolder();
                    }
                });
            }
        }

        if(btnBackDirectory != null) {
            btnBackDirectory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(filePath != null && filePath.get(0).equals("/") && atWhere != 4) {
                        atWhere = 4; //set app is at root directory of choose file
                        getDirectory(filePath.get(0));
                    }else if(atWhere == 4){
                        //Remove margin bottom for listbooks view
                        marginBottom.setMargins(0, 0, 0, 0); //left,top,right,bottom
                        listBookLayout.setLayoutParams(marginBottom);

                        handleChooseFileLayout.setVisibility(View.GONE);
                        showOpenFileLayout(true);
                        atWhere = 0;
                    }else if(atWhere == 1){
                        getDirectory(filePath.get(0));
                    }
                }
            });
        }

        if(btnExitChooseFile != null) {
            btnExitChooseFile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(recentBooks.size() != 0 && allBooks.size() != 0) {
                        //app is at list book
                        atWhere = 2;

                        //Disable open Books Layout and handleChooseFileLayout
                        openBooksLayout.setVisibility(View.GONE);
                        handleChooseFileLayout.setVisibility(View.GONE);

                        //Enable viewListBooks
                        listBookLayout.setVisibility(View.VISIBLE);

                        //Remove margin bottom for listbooks view
                        marginBottom.setMargins(0, 0, 0, 0); //left,top,right,bottom
                        listBookLayout.setLayoutParams(marginBottom);

                        //View book on screen
                        if (recentBooks.size() != 0) {
                            if (showGridView) {
                                viewBookInformationInScreenAsGrid(recentBooks);
                            } else {
                                viewBookInformationInScreenAsList(recentBooks);
                            }
                        } else {
                            if (showGridView) {
                                viewBookInformationInScreenAsGrid(allBooks);
                            } else {
                                viewBookInformationInScreenAsList(allBooks);
                            }
                        }
                    }else{
                        showOpenFileLayout(true);
                    }
                }
            });
        }
    }

    private void getDirectory(String dirPath) {
        lastFilePath = dirPath;
        fileName = new ArrayList<>();
        filePath = new ArrayList<>();
        fileImage = new ArrayList<>();
        File f = new File(dirPath);
        File[] files = f.listFiles();

        currentPath = dirPath;

        if(!dirPath.equals(rootDirectory))
        {
            filePath.add(f.getParent());
            fileName.add("../");
            fileImage.add(1);
        }else{
            atWhere = 4;
        }

        for(int i=0; i < files.length; i++)
        {
            File file = files[i];
            filePath.add(file.getPath());
            fileName.add(file.getName());

            if(file.isDirectory()) {
                fileImage.add(1);
            }else {
                fileImage.add(0);
            }
        }

        ListFileAdapter listFileAdapter = new ListFileAdapter(this, fileImage, fileName, listViewColor);

        listFile = (ListView)findViewById(R.id.listBooks);
        listFile.setAdapter(listFileAdapter);
        listFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            File file = new File(filePath.get(position));
            atWhere = 1; //app is at choose file
            if (file.isDirectory()){
                if(file.canRead()){
                    getDirectory(filePath.get(position));
                }
            }else{
                if(isReadFolder){
                    showDialogErrorReadFile("'" + file.getName() + "' is not a folder");
                }else{
                    readABookInFolder(file);
                }
            }
            }
        });
    }

    private void showDialogErrorReadFile(String message){
        new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_error)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    private void showDialogSuccessReadFile(String message){
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_success)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
    }

    private void showDialogConfirm(String message){
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_information)
                .setTitle("Confirm")
                .setMessage(message)
                .setPositiveButton("No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                .setNegativeButton("Yes",
                        new  DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                bookLibrary.close();
                                System.exit(0);
                            }
                        })
                .show();
    }

    private  void readABookInFolder(File file){
        String []filenameArray = file.getName().split("\\.");
        String extension = filenameArray[filenameArray.length-1];

        if(!extension.equals("epub") && !extension.equals("pdf")){
            showDialogErrorReadFile("Can't read '" + file.getName() + "'. Please select the file with .epub, .pdf extension");
        }else {
            BookInformation bo = null;

            //Check whether or not this book is opened
            if(extension.equals("epub")) {
                bo = readEpubInformation(file.getPath());
            }else if (extension.equals("pdf")){
                bo = readPDFInformation(file.getPath());
            }

            if(bo != null) {
                //If this books is not exists in library, we will insert to library
                if(!wasBookExitsInLibrary(bo.getFilePath())) {
                    insertABookWillBeRead(bo);
                }

                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("currentBookPath", bo.getFilePath());
                editor.commit();

                Intent intent;
                if (bo.getFormat().equals("epub"))
                    intent = new Intent(MainActivity.this, EpubContentActivity.class);
                else
                    intent = new Intent(MainActivity.this, PDFContentActivity.class);

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }else{
                showDialogErrorReadFile("Can't read '" + file.getName() + "', because it corrupted.");
            }
        }
    }

    private void readAllBooksInFolder(){
        String []folderPath = currentPath.split("/");
        String fol = currentPath;
        if(folderPath.length > 1)
            fol = folderPath[folderPath.length - 1];
        final String folderName = fol;

        hReadBooksInFolder = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(listBooksCanOpen == null || listBooksCanOpen.size() == 0){
                    showDialogErrorReadFile("'" + folderName + "' doesn't contain any file with .epub, " +
                            "pdf extension or already added to library");

                }else {
                    //Set listBookCantRead = null, because we already removed when read in folder before
                    listBooksCantRead = null;

                    hanldeAfterReadAllBooks(allBooks);
                    showDialogSuccessReadFile("Added " + listBooksCanOpen.size() + " books to library");
                }
                progress.dismiss();
            }
        };
        progress = ProgressDialog.show(context, "Please wait...", "Reading all books in folder '"+ folderName  +"'..." , true);

        Thread t = new Thread() {
            @Override
            public void run(){
                listBooksCanOpen = new ArrayList<>();

                for (int i=0; i<filePath.size(); i++) {
                    File file = new File(filePath.get(i));

                    if (!file.isDirectory()) {
                        String[] filenameArray = file.getName().split("\\.");
                        String extension = filenameArray[filenameArray.length - 1];
                        BookInformation bo = null;

                        if (extension.equals("epub") && !wasBookExitsInLibrary(file.getPath())){
                            bo = readEpubInformation(file.getPath());
                            if(bo != null)
                                listBooksCanOpen.add(bo);
                        }else if(extension.equals("pdf") && !wasBookExitsInLibrary(file.getPath())){
                            bo = readPDFInformation(file.getPath());
                            if(bo != null)
                                listBooksCanOpen.add(bo);
                        }
                    }
                }

                if(listBooksCanOpen.size() != 0){
                    insertBooks(listBooksCanOpen);

                    //Add to current allBooks
                    for (BookInformation bo: listBooksCanOpen){
                        allBooks.add(bo);
                    }
                }

                Message msg = hReadBooksInFolder.obtainMessage();
                hReadBooksInFolder.sendMessage(msg);
            }
        };

        t.start();
    }

    private BookInformation readEpubInformation(String path){
        try {
            BookInformation bookInfo = new BookInformation();
            InputStream epubInputStream = new FileInputStream(path);
            Book book = (new EpubReader()).readEpub(epubInputStream);

            bookInfo.setName(book.getTitle());
            bookInfo.setAuthors(book.getMetadata().getAuthors().toString());
            bookInfo.setFormat("epub");
            bookInfo.setCoverImage(BitmapFactory.decodeStream(book.getCoverImage().getInputStream()));
            bookInfo.setFilePath(path);
            bookInfo.setPrecentRead(0);
            bookInfo.setCurrentPage(0);
            bookInfo.setTotalPage(0);

            epubInputStream.close();
            return bookInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }

    private BookInformation readPDFInformation(String path){
        File file = new File(path);
        try {
            BookInformation bookInfo = new BookInformation();
            PdfReader reader = new PdfReader(file.getAbsolutePath());
            bookInfo.setFilePath(path);
            bookInfo.setName(reader.getInfo().get("Title"));

            if(bookInfo.getName() == null || bookInfo.getName() == ""){
                String []temp = path.split("\\/");
                bookInfo.setName(temp[temp.length -1]);
            }

            bookInfo.setAuthors(reader.getInfo().get("Author"));
            bookInfo.setFormat("pdf");
            bookInfo.setPrecentRead(0);
            bookInfo.setCurrentPage(0);

            ParcelFileDescriptor mFileDescriptor = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer mPdfRenderer = new PdfRenderer(mFileDescriptor);
            PdfRenderer.Page mCurrentPage = mPdfRenderer.openPage(0);
            Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(), Bitmap.Config.ARGB_8888);
            mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            bookInfo.setCoverImage(bitmap);
            bookInfo.setTotalPage(mPdfRenderer.getPageCount());

            mCurrentPage.close();
            mPdfRenderer.close();
            mFileDescriptor.close();
            reader.close();

            return bookInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }

    }

    private boolean wasBookExitsInLibrary(String path){
        for (BookInformation book: allBooks){
            String bPath = book.getFilePath();
            if(bPath.equals(path)){
                return true;
            }
        }
        return false;
    }

    private ArrayList<BookInformation> readRecentBooksInLibrary(){
        ArrayList<BookInformation> books = new ArrayList<>();
        String sql = "select * from books where recent_read = 1 order by date_read DESC";
        listBooksCantRead = new ArrayList<>();
        try {
            Cursor cur = bookLibrary.rawQuery(sql, null);

            cur.moveToPosition(-1);
            while (cur.moveToNext()) {
                BookInformation bf = new BookInformation();
                bf.setId(cur.getInt(0));
                bf.setName(cur.getString(1));
                bf.setAuthors(cur.getString(2));
                bf.setFormat(cur.getString(3));
                bf.setFilePath(cur.getString(4));
                bf.setPrecentRead(cur.getInt(6));
                bf.setCurrentPage(cur.getInt(7));
                bf.setTotalPage(cur.getInt(8));
                bf.setDateRead(cur.getInt(9));
                Bitmap bitmap = null;

                if(bf.getFormat().equals("epub")) {
                    bitmap = getEpubBookCoverImage(bf.getFilePath());
                }else if(bf.getFormat().equals("pdf")) {
                    bitmap = getPDFBookCoverImage(bf.getFilePath());
                }

                if(bitmap != null) {
                    bf.setCoverImage(bitmap);
                    books.add(bf);
                }else{
                    listBooksCantRead.add(bf.getId());
                }
            }
            return books;
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    private void readAllBooksInLibrary(){
        listBooksCantRead = new ArrayList<>();

        hLoadALlBooks = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                hanldeAfterReadAllBooks(allBooks);
                Toast.makeText(context, "Show all books in library...", Toast.LENGTH_LONG).show();
                progress.dismiss();
            }
        };
        progress = ProgressDialog.show(context, "Please wait...", "Reading all books in library..." , true);

        Thread t = new Thread() {
            @Override
            public void run(){
                String sql = "select * from books";
                try {
                    Cursor cur = bookLibrary.rawQuery(sql, null);

                    cur.moveToPosition(-1);
                    while (cur.moveToNext()) {
                        BookInformation bf = new BookInformation();
                        bf.setId(cur.getInt(0));
                        bf.setName(cur.getString(1));
                        bf.setAuthors(cur.getString(2));
                        bf.setFormat(cur.getString(3));
                        bf.setFilePath(cur.getString(4));
                        bf.setPrecentRead(cur.getInt(6));
                        bf.setCurrentPage(cur.getInt(7));
                        bf.setTotalPage(cur.getInt(8));
                        bf.setDateRead(cur.getInt(9));
                        Bitmap bitmap = null;

                        if(bf.getFormat().equals("epub")) {
                            bitmap = getEpubBookCoverImage(bf.getFilePath());
                        }else if(bf.getFormat().equals("pdf")) {
                            bitmap = getPDFBookCoverImage(bf.getFilePath());
                        }

                        if(bitmap != null) {
                            bf.setCoverImage(bitmap);
                            allBooks.add(bf);
                        }else{
                            addBookCantRead(bf.getId());
                        }
                    }

                    Message msg = hLoadALlBooks.obtainMessage();
                    hLoadALlBooks.sendMessage(msg);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        };
        t.start();
    }

    private synchronized void addBookCantRead(int bookId){
        listBooksCantRead.add(bookId);
    }

    private Bitmap getEpubBookCoverImage(String bookPath){
        try {
            InputStream epubInputStream = new FileInputStream(bookPath);
            Book book = (new EpubReader()).readEpub(epubInputStream);
            Bitmap bitmap = BitmapFactory.decodeStream(book.getCoverImage().getInputStream());
            epubInputStream.close();
            return bitmap;
        } catch (Exception e) {
            Log.e("error", "Can't read '" + bookPath + "'");
            return  null;
        }
    }

    private Bitmap getPDFBookCoverImage(String bookPath){
        File file = new File(bookPath);
        try {
            ParcelFileDescriptor mFileDescriptor = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer mPdfRenderer = new PdfRenderer(mFileDescriptor);
            PdfRenderer.Page mCurrentPage = mPdfRenderer.openPage(0);
            Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(), Bitmap.Config.ARGB_8888);
            mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            mCurrentPage.close();
            mPdfRenderer.close();
            mFileDescriptor.close();

            return bitmap;
        } catch (Exception e) {
            Log.e("error", "Can't read '" + bookPath + "'");
            return  null;
        }
    }

    private void handleAfterReadRecentBook(ArrayList<BookInformation> booksOpened){
        if(booksOpened.size() == 0){
            //Load book path in file
            readAllBooksInLibrary();
        }else {
            //app is at list book
            atWhere = 2;

            //Disable open Books Layout and handleChooseFileLayout
            openBooksLayout.setVisibility(View.GONE);
            handleChooseFileLayout.setVisibility(View.GONE);

            //Set isAllBooks = false; to check show grid view for recent book or all book?
            isAllBooks = false;

            //View book on screen
            if(showGridView){
                viewBookInformationInScreenAsGrid(booksOpened);
            }else {
                viewBookInformationInScreenAsList(booksOpened);
            }

            //Write book paths to list_book_path again. This paths read successfully
            if (listBooksCantRead != null && listBooksCantRead.size() > 0)
                deleteBooksCantRead();
        }
    }

    private void hanldeAfterReadAllBooks(ArrayList<BookInformation> booksOpened){
        if(booksOpened.size() == 0){
            showOpenFileLayout(false);
        }else{
            //app is at list book
            atWhere = 2;

            //Disable open Books Layout and handleChooseFileLayout
            openBooksLayout.setVisibility(View.GONE);
            handleChooseFileLayout.setVisibility(View.GONE);

            //Set isAllBooks = true; to check show grid view for recent book or all book?
            isAllBooks = true;

            if(showGridView){
                viewBookInformationInScreenAsGrid(booksOpened);
            }else {
                viewBookInformationInScreenAsList(booksOpened);
            }

            //Write book paths to list_book_path again. This paths read successfully
            if(listBooksCantRead != null && listBooksCantRead.size() > 0)
                deleteBooksCantRead();
        }
    }

    private void viewBookInformationInScreenAsList(final ArrayList<BookInformation> books){
        //type: 1 loadAllBooks calls, 2 loadAllRecentReadBook calls

        //Hide view book as grid and show view book as list
        gridBookLayout.setVisibility(View.GONE);
        listBookLayout.setVisibility(View.VISIBLE);

        listBookAdapter = new ListBookAdapter(this, books, listViewColor);
        listBookLayout.setAdapter(listBookAdapter);
        listBookLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BookInformation bo = books.get(position);

                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("currentBookPath", bo.getFilePath());
                editor.commit();

                if(!wasExistInRecentRead(bo.getId())) {
                    updateBookRecentRead(bo.getId());
                }

                Intent intent;
                if(bo.getFormat().equals("epub"))
                    intent = new Intent(MainActivity.this, EpubContentActivity.class);
                else
                    intent = new Intent(MainActivity.this, PDFContentActivity.class);

                bookLibrary.close();
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void viewBookInformationInScreenAsGrid(final ArrayList<BookInformation> books){
        //type: 1 loadAllBooks calls, 2 loadAllRecentReadBook calls

        ArrayList<Bitmap> bookImages = new ArrayList<>();
        for (BookInformation b: books)
            bookImages.add(b.getCoverImage());

        //Hide view book as list and show view book as grid
        listBookLayout.setVisibility(View.GONE);
        gridBookLayout.setVisibility(View.VISIBLE);
        gridBookLayout.setBackgroundColor(listViewColor);

        gridBookLayout.setAdapter(new GridBookAdapter(context, bookImages));
        gridBookLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id){
                BookInformation bo = books.get(position);

                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("currentBookPath", bo.getFilePath());
                editor.commit();

                if(!wasExistInRecentRead(bo.getId())) {
                    updateBookRecentRead(bo.getId());
                }

                Intent intent;
                if(bo.getFormat().equals("epub"))
                    intent = new Intent(MainActivity.this, EpubContentActivity.class);
                else
                    intent = new Intent(MainActivity.this, PDFContentActivity.class);

                bookLibrary.close();
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    public boolean wasExistInRecentRead(int bookId){
        if(recentBooks != null && recentBooks.size() != 0){
            for(BookInformation b: recentBooks) {
                if(b.getId() == bookId){
                        return true;
                }
            }
        }

        return false;
    }

    private void deleteBooksCantRead(){
        for (int id: listBooksCantRead){
            String sql = "delete from books where id = " + id;
            bookLibrary.execSQL(sql);
        }
    }

    private void updateBookRecentRead(int bookId){
        //Because total recent books is 10, so if current recent books
        // is lager than 10, remove an oldest book read
        if(recentBooks.size() >= 10){
            int id = recentBooks.get(recentBooks.size() - 1).getId();
            String sql = "update books set recent_read = 0 where id = " + id;
            bookLibrary.execSQL(sql);
        }

        //Update book recent read
        Date today = new Date();
        String sql = "update books set recent_read=1, date_read="+ today.getTime() +" where id = " + bookId;
        try {
            bookLibrary.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertABookWillBeRead(BookInformation book){
        ContentValues cValues = new ContentValues();
        bookLibrary.beginTransaction();
        cValues.put("name", book.getName());
        cValues.put("authors", book.getAuthors());
        cValues.put("format", book.getFormat());
        cValues.put("path", book.getFilePath());
        cValues.put("recent_read", 1);
        cValues.put("percent_read", 0);
        cValues.put("current_page", 0);
        cValues.put("total_page", book.getTotalPage());
        bookLibrary.insert("books", null, cValues);
        bookLibrary.setTransactionSuccessful();
        bookLibrary.endTransaction();
    }

    private void insertBooks(ArrayList<BookInformation> books){
        ContentValues cValues = new ContentValues();
        bookLibrary.beginTransaction();
        for (BookInformation book: books){
            cValues.put("name", book.getName());
            cValues.put("authors", book.getAuthors());
            cValues.put("format", book.getFormat());
            cValues.put("path", book.getFilePath());
            cValues.put("recent_read", 0);
            cValues.put("percent_read", 0);
            cValues.put("current_page", 0);
            cValues.put("total_page", book.getTotalPage());
            bookLibrary.insert("books", null, cValues);
        }
        bookLibrary.setTransactionSuccessful();
        bookLibrary.endTransaction();
    }

}
