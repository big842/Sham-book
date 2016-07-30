package com.ctt535.start.shambook;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_WRITE_STORAGE = 112;
    static final int REQUEST_READ_STORAGE = 113;
    static final String PREFS_NAME = "MyPrefsFile";

    private Context context;
    private ProgressDialog progress;
    private View viewOptionOpenFile = null;

    private ArrayList<BookInformation> recentBooks = new ArrayList<>();
    private ArrayList<BookInformation> allBooks = new ArrayList<>();
    private ArrayList<BookInformation> listRecentBookPath = null; //Save list path of books read recently
    private ArrayList<String> listBookPath = null; //Save list path of books
    private List<String> fileName = null;
    private List<String> filePath = null;
    private List<Integer> fileImage = null;

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
    private String rootDirectory="/";
    private String currentPath = "/";
    private String lastFilePath = "/";

    // 0: at open file, 1: at choose file in directory, 2: at list book,
    // 3: at read book, 4: at root directory of choose file
    private int atWhere = 0;

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

        //Ininit and load leftview
        initAppSettingList();
        loadLeftViewListAppFeatures();

        //Load book path in file
        ArrayList<BookInformation> paths = readRecentBookPathInFile("list_recent_book");

        if(paths == null){
            //Load book path in file
            ArrayList<String> paths1 = readBookPathInFile("list_book");

            if(paths1 == null){
                showOpenFileLayout(false);
            }else {
                isAllBooks = true;

                //Load load all Books in paths
                loadAllBooks(paths1, 0, "");
                Toast.makeText(context, "Loading all books...", Toast.LENGTH_LONG).show();
            }
        }else {
            //Load book and show list book
            loadAllRecentReadBook(paths);
            Toast.makeText(context, "Loading all books read recently...", Toast.LENGTH_LONG).show();
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
            if(recentBooks != null && allBooks != null)

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
            if (recentBooks!=null) {
                if(showGridView)
                    viewBookInformationInScreenAsGrid(recentBooks, 2);
                else
                    viewBookInformationInScreenAsList(recentBooks, 2);
            } else{
                if(showGridView)
                    viewBookInformationInScreenAsGrid(allBooks, 1);
                else
                    viewBookInformationInScreenAsList(allBooks, 1);
            }
        }else{
            showDialogConfirm("Are you sure you  want to exit Shame Book ?");
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
                        if (atWhere == 0 && listRecentBookPath == null) {
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
                                viewBookInformationInScreenAsGrid(recentBooks, 2);
                            }else {
                                viewBookInformationInScreenAsList(recentBooks, 2);
                            }
                        } else {
                            showDialogErrorReadFile("There aren't any books that read recently");
                        }
                        break;
                    case 1:
                        if (atWhere == 0 && allBooks == null) {
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
                                viewBookInformationInScreenAsGrid(allBooks, 1);
                            }else {
                                viewBookInformationInScreenAsList(allBooks, 1);
                            }
                        } else {
                            //Load book path in file
                            ArrayList<String> paths = readBookPathInFile("list_book");

                            if (paths == null) {
                                showOpenFileLayout(false);
                            } else {
                                //Load load all Books in paths
                                loadAllBooks(paths, 0, "");
                            }
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
                                viewBookInformationInScreenAsGrid(allBooks, 1);
                            else
                                viewBookInformationInScreenAsGrid(recentBooks, 2);
                        }else {
                            if(isAllBooks)
                                viewBookInformationInScreenAsList(allBooks, 1);
                            else
                                viewBookInformationInScreenAsList(recentBooks, 2);
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
                            .setTitle("About Sambook")
                            .setMessage("This application is used to read electronic books with the formats include: .epub, .pdf and " +
                                "developed by MinhDai-AnhTuan\n\n" +
                                "Email: 1353006@student.hcmus.edu.vn - 1353041@student.hcmus.edu.vn")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                        break;
                    case 8:
                        showDialogConfirm("Are you sure you  want to exit Shame Book ?");
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
                viewBookInformationInScreenAsList(allBooks, 1);
            else
                viewBookInformationInScreenAsList(recentBooks, 2);
        }else if(atWhere == 1){
            getDirectory(lastFilePath);
        }
        dialog.dismiss();
    }

    private void showDialogSearchText(){
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
                final String searchText = inputText.getText().toString();
                dialog.dismiss();
                progress = ProgressDialog.show(context, "Please wait...", "Searching books for text '" + searchText + "' ..." , true);
                progress.setCancelable(true);

                new Thread(new Runnable() {
                    @Override
                    public void run(){
                        // do the thing that takes a long time
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run(){
                                ArrayList<BookInformation> foundBooks = new ArrayList<>();

                                for (int i=0; i<allBooks.size(); i++){
                                    BookInformation  bookItem = allBooks.get(i);
                                    if(bookItem.filePath.contains(searchText) || bookItem.authors.contains(searchText)){
                                        foundBooks.add(bookItem);
                                    }
                                }

                                if(foundBooks.size() == 0){
                                    showDialogErrorReadFile("Can't find any books match with '" + searchText + "'");
                                }else{
                                    if(showGridView)
                                        viewBookInformationInScreenAsGrid(foundBooks, 1);
                                    else
                                        viewBookInformationInScreenAsList(foundBooks, 1);
                                    showDialogSuccessReadFile("Found " + foundBooks.size() + " books with name " + searchText);
                                }
                                progress.dismiss();
                            }
                        });
                    }
                }).start();
            }
        });

        dialog.show();
    }

    private  void showFileOptionButton(int type){
        //type = 1: choose file button call, type = 2: choose folder button call

        //Set margin bottom for list view, because, if not, it will be hid beside the bottom buttons.
        marginBottom.setMargins(0, 0, 0, 120); //left,top,right,bottom
        listBookLayout.setLayoutParams(marginBottom);

        Button btnBackDirectory = (Button) findViewById(R.id.btnBackDirectory);
        Button btnChooseThisFolder = (Button) findViewById(R.id.btnChooseThisFolder);
        Button btnExitChooseFile = (Button) findViewById(R.id.btnExitChooseFile);

        if (type == 1)
            btnChooseThisFolder.setVisibility(View.GONE);
        else{
            //Set click event for button choose folder
            if(btnChooseThisFolder != null) {
                btnChooseThisFolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Remove margin bottom for listbooks view
                        marginBottom.setMargins(0, 0, 0, 0); //left,top,right,bottom
                        listBookLayout.setLayoutParams(marginBottom);

                        readFileInFolder();
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
                    if(recentBooks != null && allBooks != null) {
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
                        if (recentBooks != null) {
                            if (showGridView) {
                                viewBookInformationInScreenAsGrid(recentBooks, 2);
                            } else {
                                viewBookInformationInScreenAsList(recentBooks, 2);
                            }
                        } else {
                            if (showGridView) {
                                viewBookInformationInScreenAsGrid(allBooks, 1);
                            } else {
                                viewBookInformationInScreenAsList(allBooks, 1);
                            }
                        }
                    }else{
                        showOpenFileLayout(true);
                    }
                }
            });
        }
    }

    private void showOpenFileLayout(boolean isReturn){
        //Disable viewListBooks
        listBookLayout.setVisibility(View.GONE);

        //Show option about choose file or directory
        openBooksLayout.setVisibility(View.VISIBLE);
        openBooksLayout.setBackgroundColor(listViewColor);

        //Load list_book_layout
        if(viewOptionOpenFile != null && isReturn){
            viewOptionOpenFile.setVisibility(View.VISIBLE);
        }else {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            viewOptionOpenFile = inflater.inflate(R.layout.open_file, openBooksLayout, true);
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
                    readBookFile(file);
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
                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                intent.addCategory(Intent.CATEGORY_HOME);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//***Change Here***
                                startActivity(intent);
                                finish();
                                System.exit(0);
                            }
                        })
                .show();
    }

    private boolean checkFileExited(String booksOpen, String fileContainList){
        ArrayList<String> listBooks = readBookPathInFile(fileContainList);
        if(listBooks == null)
            return false;

        for (String str1: listBooks){
            if(str1.equals(booksOpen))
                return true;
        }

        return false;
    }

    private  void readBookFile(File file){
        String []filenameArray = file.getName().split("\\.");
        String extension = filenameArray[filenameArray.length-1];
        if(!extension.equals("epub") && !extension.equals("pdf")){
            showDialogErrorReadFile("Can't read '" + file.getName() + "'. Please select the file with .epub, .pdf extension");
        }else {
            ArrayList<BookInformation> tempBook1 = new ArrayList<>();
            ArrayList<String> tempBook2= new ArrayList<>();
            BookInformation bo = new BookInformation();
            boolean isRead = false;

            bo.filePath = file.getPath();
            bo.precentRead = 0;
            String []splitPath =  bo.filePath.split("\\.");

            //Check whether or not this book is opened
            if(splitPath[splitPath.length - 1].toLowerCase().equals("epub")) {
                if (getEpubBookInformation(bo.filePath, 1) != null){
                    isRead = true;
                }
            }else if (splitPath[splitPath.length - 1].toLowerCase().equals("pdf")){
                if (getPDFBookInformation(bo.filePath, 1) != null) {
                    isRead = true;
                }
            }

            if(isRead) {
                if(checkFileExited(bo.filePath, "list_book") == true)
                    showDialogErrorReadFile("'" + bo.filePath + "' already added to library");
                else {
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("currentBookPath", file.getPath());
                    editor.commit();

                    //Write book path to file
                    tempBook1.add(bo);
                    writeBookRecentRead(tempBook1);
                    tempBook2.add(bo.filePath);
                    writeBookPathsToFile(tempBook2);

                    Intent intent;
                    if (splitPath[splitPath.length - 1].equals("epub"))
                        intent = new Intent(MainActivity.this, EpubContentActivity.class);
                    else
                        intent = new Intent(MainActivity.this, PDFContentActivity.class);

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }else{
                showDialogErrorReadFile("Can't read '" + bo.filePath + "'. Please select the file with .epub, .pdf extension");
            }
        }
    }

    private ArrayList<String> removeBookExited(ArrayList<String> booksOpen, String fileContainList){
        ArrayList<String> listBooks = readBookPathInFile(fileContainList);
        if(listBooks == null)
            return booksOpen;

        if(booksOpen.size() == 0)
            return null;

        for (String str1: listBooks){
            for (int i = 0; i< booksOpen.size(); i++){
                if(str1.equals(booksOpen.get(i))){
                    booksOpen.remove(i);
                }
            }
        }

        return booksOpen;
    }

    private void readFileInFolder(){
        ArrayList<String> listBookChoosed = new ArrayList<>();

        String []folderPath = currentPath.split("/");
        String folderName = currentPath;
        if(folderPath.length > 1)
            folderName = folderPath[folderPath.length - 1];

        for (int i=0; i<filePath.size(); i++) {
            File file = new File(filePath.get(i));

            if (!file.isDirectory()) {
                String[] filenameArray = file.getName().split("\\.");
                String extension = filenameArray[filenameArray.length - 1];

                if (extension.equals("epub") || extension.equals("pdf") ) {
                    listBookChoosed.add(file.getPath());
                }
            }
        }

        if(listBookChoosed.size() == 0)
            showDialogErrorReadFile("'" + folderName + "' doesn't contain any file with .epub, pdf extension");
        else {
            ArrayList<String> remainBooks = removeBookExited(listBookChoosed, "list_book");
            if (remainBooks.size() == 0) {
                showDialogErrorReadFile("'" + folderName + "' contains the books already added to library");
                return;
            }

            loadAllBooks(remainBooks, 1, folderName);
        }
    }

    private ArrayList<BookInformation> readRecentBookPathInFile(String fileContainList){
        try {
            File secondInputFile = new File(getFilesDir(), fileContainList);
            InputStream secondInputStream = new BufferedInputStream(new FileInputStream(secondInputFile));
            BufferedReader r = new BufferedReader(new InputStreamReader(secondInputStream));
            ArrayList<BookInformation> lsPath = new ArrayList<>();

            String line;
            while ((line = r.readLine()) != null) {
                BookInformation bf = new BookInformation();
                bf.filePath = line;

                line = r.readLine();
                bf.precentRead = Integer.parseInt(line);

                lsPath.add(bf);
            }
            r.close();
            secondInputStream.close();
            return lsPath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<String> readBookPathInFile(String fileContainList){
        try {
            File secondInputFile = new File(getFilesDir(), fileContainList);
            InputStream secondInputStream = new BufferedInputStream(new FileInputStream(secondInputFile));
            BufferedReader r = new BufferedReader(new InputStreamReader(secondInputStream));
            ArrayList<String> lsPath = new ArrayList<>();

            String line;
            while ((line = r.readLine()) != null) {
                lsPath.add(line);
            }
            r.close();
            secondInputStream.close();
            return lsPath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private BookInformation getEpubBookInformation(String value, int type){
        //type: 1 readAllBooks call, 2 readRecentBooks call
        try {
            BookInformation bookInfo = new BookInformation();

            // Find InputStream for book
            InputStream epubInputStream = new FileInputStream(value);

            // Load Book from inputStream
            Book book = (new EpubReader()).readEpub(epubInputStream);

            bookInfo.authors = book.getMetadata().getAuthors().toString();
            bookInfo.title = book.getTitle();
            bookInfo.coverImage = BitmapFactory.decodeStream(book.getCoverImage().getInputStream());
            bookInfo.filePath = value;
            bookInfo.type = book.getMetadata().getTypes().toString();
            bookInfo.publishers = book.getMetadata().getPublishers().toString();
            bookInfo.precentRead = 0;

            //Get book format
            String bformat = book.getMetadata().getFormat();
            if (bformat.toLowerCase().contains("epub"))
                bookInfo.format = "epub";

            if(type == 1)
                allBooks.add(bookInfo);
            else
                recentBooks.add(bookInfo);

            epubInputStream.close();
            return bookInfo;
        } catch (IOException e) {
            Log.e("error", "Can't read '" + value + "'");
            return  null;
        }
    }

    private BookInformation getPDFBookInformation(String value, int type){
        //type: 1 readAllBooks call, 2 readRecentBooks call
        File file = new File(value);
        try {
            BookInformation bookInfo = new BookInformation();

            PdfReader reader = new PdfReader(file.getAbsolutePath());
            bookInfo.filePath = value;
            bookInfo.title = reader.getInfo().get("Title");
            if(bookInfo.title.isEmpty()){
                String []temp = value.split("\\/");
                bookInfo.title = temp[temp.length -1];
            }

            bookInfo.authors = reader.getInfo().get("Author");
            bookInfo.authors = reader.getInfo().get("Author");
            bookInfo.format = "pdf";
            bookInfo.precentRead = 0;

            ParcelFileDescriptor mFileDescriptor = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer mPdfRenderer = new PdfRenderer(mFileDescriptor);
            PdfRenderer.Page mCurrentPage = mPdfRenderer.openPage(0);
            Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(), Bitmap.Config.ARGB_8888);
            mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            bookInfo.coverImage = bitmap;
            mCurrentPage.close();
            mPdfRenderer.close();
            mFileDescriptor.close();


            if(type == 1)
                allBooks.add(bookInfo);
            else
                recentBooks.add(bookInfo);
            reader.close();
            return bookInfo;
        } catch (Exception e) {
            Log.e("error", "Can't read '" + value + "'");
            return  null;
        }
    }

    private void handleAfterReadRecentBook(ArrayList<BookInformation> booksOpened){
        if(booksOpened.size() == 0){
            //Load book path in file
            ArrayList<String> paths = readBookPathInFile("list_book");

            //Load load all Books in paths
            loadAllBooks(paths, 0, "");
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
                viewBookInformationInScreenAsGrid(recentBooks, 2);
            }else {
                viewBookInformationInScreenAsList(recentBooks, 2);
            }

            //Write book paths to list_book_path again. This paths read successfully
            writeBookRecentRead(booksOpened);
        }
    }

    private void loadAllRecentReadBook(final ArrayList<BookInformation> paths){
        ArrayList<BookInformation> booksOpened = new ArrayList<>();

        for (int i=0; i<paths.size(); i++){
            String [] extension = paths.get(i).filePath.split("\\.");

            if(extension[extension.length - 1].toLowerCase().equals("epub")) {
                if (getEpubBookInformation(paths.get(i).filePath, 2) != null)
                    booksOpened.add(paths.get(i));
            }else if (extension[extension.length - 1].toLowerCase().equals("pdf")){
                if (getPDFBookInformation(paths.get(i).filePath, 2) != null)
                    booksOpened.add(paths.get(i));
            }
        }

        handleAfterReadRecentBook(booksOpened);
    }

    private void hanldeAfterReadAllBooks(ArrayList<String> booksOpened, int status, String fiName){
        if(status != 0){
            if (booksOpened.size() == 0) {
                showDialogErrorReadFile("'" + fiName + "' doesn't contain any file with .epub, .pdf extension");
            } else {
                //show information
                showDialogSuccessReadFile("Added " + booksOpened.size() + " books to library");
            }
        }else if(booksOpened.size() == 0){
            showOpenFileLayout(false);
        }

        if(booksOpened.size() != 0) {
            //app is at list book
            atWhere = 2;

            //Disable open Books Layout and handleChooseFileLayout
            openBooksLayout.setVisibility(View.GONE);
            handleChooseFileLayout.setVisibility(View.GONE);

            //Set isAllBooks = true; to check show grid view for recent book or all book?
            isAllBooks = true;

            if(showGridView){
                viewBookInformationInScreenAsGrid(allBooks, 1);
            }else {
                viewBookInformationInScreenAsList(allBooks, 1);
            }

            //Write book paths to list_book_path again. This paths read successfully
            writeBookPathsToFile(booksOpened);
        }
    }

    private void loadAllBooks(final ArrayList<String> paths, final int status, final String fiName){
        //Status = 1: Load all book in fiName folder(link in paths list), status = 0: Load all books in paths

        progress = ProgressDialog.show(context, "Please wait...", "Loading all books..." , true);
        progress.setCancelable(true);

        new Thread(new Runnable() {
            @Override
            public void run(){
                // do the thing that takes a long time
                runOnUiThread(new Runnable() {
                    @Override
                    public void run(){
                        ArrayList<String> booksOpened = new ArrayList<>();

                        for (int i=0; i<paths.size(); i++){
                            String [] extension = paths.get(i).split("\\.");
                            if(extension[extension.length - 1].toLowerCase().equals("epub")) {
                                if (getEpubBookInformation(paths.get(i), 1) != null)
                                    booksOpened.add(paths.get(i));
                            }else if (extension[extension.length - 1].toLowerCase().equals("pdf")){
                                if (getPDFBookInformation(paths.get(i), 1) != null)
                                    booksOpened.add(paths.get(i));
                            }
                        }

                        hanldeAfterReadAllBooks(booksOpened, status, fiName);
                        progress.dismiss();
                     }
                });
            }
        }).start();
    }

    private void viewBookInformationInScreenAsList(final ArrayList<BookInformation> books, final int type){
        //type: 1 loadAllBooks calls, 2 loadAllRecentReadBook calls

        //Hide view book as grid and show view book as list
        gridBookLayout.setVisibility(View.GONE);
        listBookLayout.setVisibility(View.VISIBLE);

        listBookAdapter = new ListBookAdapter(this, books, listViewColor, isAllBooks);
        listBookLayout.setAdapter(listBookAdapter);
        listBookLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("currentBookPath", books.get(position).filePath);
                editor.commit();

                if(type == 1) {
                    ArrayList<BookInformation> tempBook = new ArrayList<>();
                    BookInformation bo = new BookInformation();
                    bo.filePath = books.get(position).filePath;
                    bo.precentRead = 0;
                    tempBook.add(bo);
                    if(!wasExistInRecentRead(bo.filePath))
                        writeBookRecentRead(tempBook);
                }

                String []splitPath =  books.get(position).filePath.split("\\.");
                Intent intent;
                if(splitPath[splitPath.length-1].equals("epub"))
                    intent = new Intent(MainActivity.this, EpubContentActivity.class);
                else
                    intent = new Intent(MainActivity.this, PDFContentActivity.class);

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void viewBookInformationInScreenAsGrid(final ArrayList<BookInformation> books, final int type){
        //type: 1 loadAllBooks calls, 2 loadAllRecentReadBook calls

        ArrayList<Bitmap> bookImages = new ArrayList<>();
        for (BookInformation b: books)
            bookImages.add(b.coverImage);

        //Hide view book as list and show view book as grid
        listBookLayout.setVisibility(View.GONE);
        gridBookLayout.setVisibility(View.VISIBLE);
        gridBookLayout.setBackgroundColor(listViewColor);

        gridBookLayout.setAdapter(new GridBookAdapter(context, bookImages));
        gridBookLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id){
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("currentBookPath", books.get(position).filePath);
                editor.commit();

                if(type == 1) {
                    ArrayList<BookInformation> tempBook = new ArrayList<>();
                    BookInformation bo = new BookInformation();
                    bo.filePath = books.get(position).filePath;
                    bo.precentRead = 0;
                    tempBook.add(bo);
                    if(!wasExistInRecentRead(bo.filePath))
                        writeBookRecentRead(tempBook);
                }

                String []splitPath =  books.get(position).filePath.split("\\.");
                Intent intent;
                if(splitPath[splitPath.length-1].equals("epub"))
                    intent = new Intent(MainActivity.this, EpubContentActivity.class);
                else
                    intent = new Intent(MainActivity.this, PDFContentActivity.class);

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    public boolean wasExistInRecentRead(String bookPath){
        if(listRecentBookPath != null){
            for(BookInformation path: listRecentBookPath) {
                if(path.filePath != null){
                    if(path.filePath.equals(bookPath))
                        return true;
                }
            }
        }

        return false;
    }

    private void writeBookRecentRead(ArrayList<BookInformation> booksOpen){
        if(listRecentBookPath == null)
            listRecentBookPath = new ArrayList<>();
        else if (listRecentBookPath.size() > 9){
            while (listRecentBookPath.size() > 9){
                listRecentBookPath.remove(0);
            }
        }

        //Add list path of book opened to listBookPath
        for(BookInformation path: booksOpen) {
            listRecentBookPath.add(path);
        }
        String data = "";
        for (int i = 0; i< listRecentBookPath.size() - 1; i++) {
            data += listRecentBookPath.get(i).filePath +"\n";
            data += listRecentBookPath.get(i).precentRead +"\n";
        }
        data += listRecentBookPath.get(listRecentBookPath.size() - 1).filePath + "\n";
        data += listRecentBookPath.get(listRecentBookPath.size() - 1).precentRead;

        try {
            File secondFile = new File(getFilesDir(), "list_recent_book");
            secondFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(secondFile);

            fos.write(data.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeBookPathsToFile(ArrayList<String> booksOpen){
        if(listBookPath == null)
            listBookPath = new ArrayList<>();

        //Add list path of book opened to listBookPath
        for(String path: booksOpen) {
            listBookPath.add(path);
        }

        String data = "";
        for (int i = 0; i< listBookPath.size() - 1; i++) {
            data += listBookPath.get(i) +"\n";
        }
        data += listBookPath.get(listBookPath.size() - 1);

        try {
            File secondFile = new File(getFilesDir(), "list_book");
            secondFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(secondFile);

            fos.write(data.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
