package com.ctt535.start.shambook;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PDFContentActivity extends AppCompatActivity {
    static final String PREFS_NAME = "MyPrefsFile";

    private Context context;

    private DrawerLayout tableOfChapters;
    private ListView listPdfPages;
    private ListView leftDrawer;
    private WebView webviewContent;

    private ParcelFileDescriptor mFileDescriptor;
    private PdfRenderer mPdfRenderer;
    private PdfRenderer.Page mCurrentPage;
    private ListPDFViewAdapter lPDFViewAdapter;

    private ImageButton turnBackBtn;
    private ImageButton tableContentBtn;
    private ImageButton settingBtn;

    private BookInformation currentBookInfo;
    private ArrayAdapter chapterAdapter;
    private ArrayAdapter settingAdapter;

    private Bitmap []listPages;

    ArrayList<String> listSettingText;
    ArrayList<Integer> listSettingImage;

    private int newUiOptions;
    private int numPages;
    private int nextPageIndex = 0;
    private int prePageIndex = 0;
    private int screenWidth;
    private int screenHeight;

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

        setContentView(R.layout.activity_book_content);
        context =this;

        //Get display size
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        //Hide webviewContent, because file that is reading is pdf
        webviewContent = (WebView) findViewById(R.id.webviewContent);
        webviewContent.setVisibility(View.GONE);

        //Set function for image button in leftview
        setFunctionForLeftImageButton();

        //Get book path store in the previous activity
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String currentBookPath = settings.getString("currentBookPath", "");

        //Find ids of leftview layout
        tableOfChapters = (DrawerLayout) findViewById(R.id.tableOfChapters);
        leftDrawer = (ListView) findViewById(R.id.listChapter);

        //Show pdf content(pages)
        showBookContent(currentBookPath);

        //Initial list setting
        initSettingList();
    }

    @Override
    public void onBackPressed(){
        //((PDFPagerAdapter)pdfViewPager.getAdapter()).close();
        try {
            mFileDescriptor.close();
            mPdfRenderer.close();
        }catch (Exception e){
            Log.e("mFileDescriptor Error", "Can't close mFileDescriptor");
        }

        Intent intent = new Intent(PDFContentActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private BookInformation readBookInformation(PdfReader pdfReader, String currentBookPath){
        BookInformation bookInfo = new BookInformation();

        bookInfo.filePath = currentBookPath;
        bookInfo.title = pdfReader.getInfo().get("Title");
        if(bookInfo.title.isEmpty()){
            String []temp = currentBookPath.split("\\/");
            bookInfo.title = temp[temp.length -1];
        }

        bookInfo.authors = pdfReader.getInfo().get("Author");
        bookInfo.authors = pdfReader.getInfo().get("Author");
        bookInfo.format = "pdf";
        bookInfo.precentRead = 0;

        return bookInfo;
    }

    private Bitmap readPageImage(int index) {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }

        mCurrentPage = mPdfRenderer.openPage(index);
        Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);

        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        return bitmap;
    }

    private void showBookContent(String currentBookPath){
        try {
            File file = new File(currentBookPath);
            PdfReader pdfReader = new PdfReader(file.getAbsolutePath());
            currentBookInfo = readBookInformation(pdfReader, currentBookPath);

            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            mPdfRenderer = new PdfRenderer(mFileDescriptor);

            numPages = mPdfRenderer.getPageCount();
            listPages = new Bitmap[numPages];

            if (numPages <= 20){
                while (nextPageIndex < numPages){
                    listPages[nextPageIndex] = readPageImage(nextPageIndex);
                    nextPageIndex ++;
                }
            }else{
                while (nextPageIndex < 20){
                    listPages[nextPageIndex] = readPageImage(nextPageIndex);
                    nextPageIndex ++;
                }
            }

            lPDFViewAdapter = new ListPDFViewAdapter(this, listPages);
            final Bitmap nullBitmap = null;
            listPdfPages = (ListView)findViewById(R.id.list_pdf_pages);
            listPdfPages.setAdapter(lPDFViewAdapter);
            listPdfPages.setOnScrollListener(new AbsListView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if(numPages - nextPageIndex > 0 && nextPageIndex - firstVisibleItem <= 15 ) {
                        listPages[prePageIndex] = null;
                        listPages[nextPageIndex] = readPageImage(nextPageIndex);
                        prePageIndex ++;
                        nextPageIndex++;
                        lPDFViewAdapter.notifyDataSetChanged();
                    }else if(prePageIndex > 0  && firstVisibleItem - prePageIndex <= 15){
                        nextPageIndex--;
                        prePageIndex --;
                        listPages[nextPageIndex] = null;
                        listPages[prePageIndex] = readPageImage(prePageIndex);
                        lPDFViewAdapter.notifyDataSetChanged();
                    }
                }
            });

            pdfReader.close();
        }catch (IOException e){
            Log.e("Error", e.getMessage());
        }
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
                //showTableContent();
            }
        });

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingList();
            }
        });
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

    private void showSettingList(){
        settingAdapter = new ListAppFeaturesAdapter(this, listSettingImage, listSettingText);
        leftDrawer.setAdapter(settingAdapter);
        leftDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                tableOfChapters.closeDrawers();
                if(position == 3){
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
}
