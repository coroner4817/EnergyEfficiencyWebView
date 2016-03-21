package com.yingnanwang.energyefficiencywebview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jsoup.nodes.Document;


public class MainActivity extends AppCompatActivity implements EnergyEfficiencyWebView.Listener{

    private EnergyEfficiencyWebView mWebView;
    private ProgressBar mPB;
    private int count0=0;
    private int count1=0;
    private int count2=0;
    private int count3=0;
    private int count4=0;
    private int count5=0;
    private int count6=0;
//        private String url = "https://en.m.wikipedia.org/wiki/Main_Page ";
    private String url = "http://www.cs.ucla.edu/";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPB = (ProgressBar)findViewById(R.id.progress);
        mPB.setVisibility(View.VISIBLE);

        getSupportActionBar().setTitle("Default WebView");

        mWebView = (EnergyEfficiencyWebView) findViewById(R.id.webview);

//        mWebView.setSimpleModeOn(true);
//        mWebView.setCharset("UTF-8");
//        mWebView.setLoadCSS(false);
//        mWebView.setLoadJS(false);
//        mWebView.setReplacedCSS(null);
//        mWebView.setReplacedJS(null);
//        mWebView.setInsertedJS(null);
//        mWebView.setNightModeOn(false);
//        mWebView.setLoadFooter(false);

        mWebView.setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100) {
                    mPB.setVisibility(View.VISIBLE);
                }
                mPB.setProgress(progress);
                if (progress == 100) {
                    mPB.setProgress(0);
                }
            }
        });

        mWebView.loadUrl(url);
    }

    @Override
    public Document CustomHandleHTML(Document in) {
        // Do something to DOM with Jsoup lib
        return in;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mWebView.stopLoading();
        mWebView.clearCache(true);
        switch (item.getItemId()){
            case R.id.simple:
                if(count0%2==0){
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Simple Mode On");
                    Toast.makeText(MainActivity.this, "Simple Mode On", Toast.LENGTH_SHORT).show();
                }else{
                    mWebView.setSimpleModeOn(false);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Simple Mode Off");
                    Toast.makeText(MainActivity.this, "Simple Mode Off", Toast.LENGTH_SHORT).show();
                }
                count0++;
                mWebView.loadUrl(url);
//                mWebView.reload();
                return true;
            case R.id.loadcss:
                if(count1%2==0){
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(true);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Load Remote CSS");
                    Toast.makeText(MainActivity.this, "Load CSS", Toast.LENGTH_SHORT).show();
                }else{
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Ignore Remote CSS");
                    Toast.makeText(MainActivity.this, "Ignore CSS", Toast.LENGTH_SHORT).show();
                }
                count1++;
                mWebView.loadUrl(url);
//                mWebView.reload();
                return true;
            case R.id.replacecss:
                if(count2%2==0){
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS("replace.css");
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Replace CSS");
                    Toast.makeText(MainActivity.this, "Replace CSS", Toast.LENGTH_SHORT).show();
                }else{
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("No Replace CSS");
                    Toast.makeText(MainActivity.this, "No Replace CSS", Toast.LENGTH_SHORT).show();
                }
                count2++;
                mWebView.loadUrl(url);
//                mWebView.reload();
                return true;
            case R.id.loadjs:
                if(count3%2==0){
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(true);
                    mWebView.setLoadJS(true);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Load Remote JS and CSS");
                    Toast.makeText(MainActivity.this, "Load JS", Toast.LENGTH_SHORT).show();
                }else{
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Ignore Remote JS");
                    Toast.makeText(MainActivity.this, "Ignore JS", Toast.LENGTH_SHORT).show();
                }
                count3++;
                mWebView.loadUrl(url);
//                mWebView.reload();
                return true;
            case R.id.replacejs:
                if(count4%2==0){
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS("replace.js");
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Replace JS");
                    Toast.makeText(MainActivity.this, "Replace JS", Toast.LENGTH_SHORT).show();
                }else{
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("No Replace JS");
                    Toast.makeText(MainActivity.this, "No Replace JS", Toast.LENGTH_SHORT).show();
                }
                count4++;
                mWebView.loadUrl(url);
//                mWebView.reload();
                return true;
            case R.id.insertjs:
                if(count5%2==0){
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS("insert.js");
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Insert JS");
                    Toast.makeText(MainActivity.this, "Insert Custom JS", Toast.LENGTH_SHORT).show();
                }else{
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("No Insert JS");
                    Toast.makeText(MainActivity.this, "No Insert JS", Toast.LENGTH_SHORT).show();
                }
                count5++;
                mWebView.loadUrl(url);
//                mWebView.reload();
                return true;
            case R.id.nightmode:
                if(count6%2==0){
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(true);
                    getSupportActionBar().setTitle("Night Mode On");
                    Toast.makeText(MainActivity.this, "Night Mode On", Toast.LENGTH_SHORT).show();
                }else{
                    mWebView.setSimpleModeOn(true);
                    mWebView.setLoadCSS(false);
                    mWebView.setLoadJS(false);
                    mWebView.setReplacedCSS(null);
                    mWebView.setReplacedJS(null);
                    mWebView.setInsertedJS(null);
                    mWebView.setNightModeOn(false);
                    getSupportActionBar().setTitle("Night Mode Off");
                    Toast.makeText(MainActivity.this, "Night Mode Off", Toast.LENGTH_SHORT).show();
                }
                count6++;
                mWebView.loadUrl(url);
//                mWebView.reload();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
