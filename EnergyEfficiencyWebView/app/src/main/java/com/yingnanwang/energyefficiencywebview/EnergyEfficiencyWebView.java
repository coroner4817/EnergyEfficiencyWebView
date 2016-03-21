package com.yingnanwang.energyefficiencywebview;


import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ViewGroup;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.os.Environment;
import android.webkit.CookieManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.HashMap;
import android.net.http.SslError;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.os.Message;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebStorage.QuotaUpdater;
import android.app.Fragment;
import android.util.Base64;
import android.os.Build;
import android.webkit.DownloadListener;
import android.graphics.Bitmap;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.widget.ProgressBar;

import java.util.MissingResourceException;
import java.util.Locale;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jsoup.*;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;


// YW - This AdvancedWebView can be used in both Activity and the Fragment
// I think the developer wants to write the WebView into an Activity-like component instead just a component
// Managed the webview like an activity/fragment

// YW - avoid the compiler show deprecation warning
@SuppressWarnings("deprecation")
public class EnergyEfficiencyWebView extends WebView {

    private static final String TAG = "EnergyEfficiencyWebView";

    // YW - AdvancedWebView.Listener
    // this interface is implemented as below and should be set to the activity who use this webview
    // This can be overrided by the developer when using this class
    // https://github.com/CS211WebView/Android-AdvancedWebView
    public interface Listener {
        Document CustomHandleHTML(Document in);
    }

    public static final String PACKAGE_NAME_DOWNLOAD_MANAGER = "com.android.providers.downloads";
    protected static final int REQUEST_CODE_FILE_PICKER = 51426;
    protected static final String DATABASES_SUB_FOLDER = "/databases";
    protected static final String LANGUAGE_DEFAULT_ISO3 = "eng";
    protected static final String CHARSET_DEFAULT = "UTF-8";
    protected static final String[] ALTERNATIVE_BROWSERS = new String[] { "org.mozilla.firefox", "com.android.chrome", "com.opera.browser", "org.mozilla.firefox_beta", "com.chrome.beta", "com.opera.browser.beta" };

    // YW - WeakReference make sure the object in the *cache* can be collected by GC when useless while the program is running to avoid memory leakage
    // Without using WeakReference the object in *cache* only collected after the program has finished
    // (http://www.tuicool.com/articles/imyueq)
    protected WeakReference<Activity> mActivity;
    protected WeakReference<Fragment> mFragment;
    protected Listener mListener;
    protected final List<String> mPermittedHostnames = new LinkedList<String>();
    /** File upload callback for platform versions prior to Android 5.0 */
    protected ValueCallback<Uri> mFileUploadCallbackFirst;
    /** File upload callback for Android 5.0+ */
    protected ValueCallback<Uri[]> mFileUploadCallbackSecond;
    protected long mLastError;
    protected String mLanguageIso3;
    protected int mRequestCodeFilePicker = REQUEST_CODE_FILE_PICKER;
    protected WebViewClient mCustomWebViewClient;
    protected WebChromeClient mCustomWebChromeClient;
    protected boolean mGeolocationEnabled;
    protected String mUploadableFileTypes = "*/*";
    protected final Map<String, String> mHttpHeaders = new HashMap<String, String>();
    private Boolean SimpleModeOn = false;
    private Boolean userSetloadImg = false;
    private Boolean userSetloadFooter = false;
    private Boolean userSetloadCSS = false;
    private Boolean userSetloadJS = false;
    private String charset = "UTF-8";
    private String customCSS = null;
    private String customJS = null;
    private String insertedJS = null;
    private String insertedCSS = null;
    private Boolean nightModeOn = false;

    // YW - constructor
    public EnergyEfficiencyWebView(Context context) {
        super(context);
        init(context);
    }

    public EnergyEfficiencyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EnergyEfficiencyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // YW - pass the activity/fragment (context) to the webview class
    public void setListener(final Activity activity, final Listener listener) {
        setListener(activity, listener, REQUEST_CODE_FILE_PICKER);
    }

    public void setListener(final Activity activity, final Listener listener, final int requestCodeFilePicker) {
        if (activity != null) {
            mActivity = new WeakReference<Activity>(activity);
        }
        else {
            mActivity = null;
        }
        setListener(listener, requestCodeFilePicker);
    }

    public void setListener(final Fragment fragment, final Listener listener) {
        setListener(fragment, listener, REQUEST_CODE_FILE_PICKER);
    }

    public void setListener(final Fragment fragment, final Listener listener, final int requestCodeFilePicker) {
        if (fragment != null) {
            mFragment = new WeakReference<Fragment>(fragment);
        }
        else {
            mFragment = null;
        }
        setListener(listener, requestCodeFilePicker);
    }

    protected void setListener(final Listener listener, final int requestCodeFilePicker) {
        mListener = listener;
        mRequestCodeFilePicker = requestCodeFilePicker;
    }

    // YW - set WebViewClient and WebChromeClient
    // WebViewClient is mainly for webview status management
    // WebChromeClient is mainly for JS process
    // http://blog.csdn.net/jackyhuangch/article/details/8310033
    @Override
    public void setWebViewClient(final WebViewClient client) {
        mCustomWebViewClient = client;
    }

    @Override
    public void setWebChromeClient(final WebChromeClient client) {
        mCustomWebChromeClient = client;
    }

    // YW - suppress the warning of SetJavaScriptEnabled
    // Start the HTML5's Geolocation API to collect users' location information
    @SuppressLint("SetJavaScriptEnabled")
    public void setGeolocationEnabled(final boolean enabled) {
        if (enabled) {
            // YW - getSettings() is from the WebView class
            getSettings().setJavaScriptEnabled(true);
            getSettings().setGeolocationEnabled(true);
            setGeolocationDatabasePath();
        }

        mGeolocationEnabled = enabled;
    }

    // YW - suppress the waring of using the new API which are introduced after the minSdkVersion set by this app
    // Set up the directory to store the database for Geolocation
    @SuppressLint("NewApi")
    protected void setGeolocationDatabasePath() {
        final Activity activity;

        // YW - mFragment.get() is to make sure the WeakReference haven't been collected by the GC
        if (mFragment != null && mFragment.get() != null && Build.VERSION.SDK_INT >= 11 && mFragment.get().getActivity() != null) {
            activity = mFragment.get().getActivity();
        }
        else if (mActivity != null && mActivity.get() != null) {
            activity = mActivity.get();
        }
        else {
            return;
        }
        // YW - set the database to be at the /data/ folder which will be deleted when the app is uninstalled
        getSettings().setGeolocationDatabasePath(activity.getFilesDir().getPath());
    }

    public void setUploadableFileTypes(final String mimeType) {
        mUploadableFileTypes = mimeType;
    }

    // YW - this part is to enable the WebView to mimic the activity's functions

    // YW - SuppressLint is to avoid the *Android* library using warning shown to the developer
    // SuppressWarnings is to avoid warning to the *Java compiler*
    // The difference between resumeTimers/pauseTimers and super.onResume()/super.onPause()
    // To pause flash you have to call hidden methods WebView.onPause() / WebView.onResume().
    // To pause WebViewCoreThread you use WebView.pauseTimers() / WebView.resumeTimers().

    // onResume is called when the activity is viewable
    @SuppressLint("NewApi")
    @SuppressWarnings("all")
    public void onResume() {
        if (Build.VERSION.SDK_INT >= 11) {
            super.onResume();
        }
        resumeTimers();
    }

    // YW - onPause is called when the activity is not viewable
    @SuppressLint("NewApi")
    @SuppressWarnings("all")
    public void onPause() {
        pauseTimers();
        if (Build.VERSION.SDK_INT >= 11) {
            super.onPause();
        }
    }

    // YW - destroy() is the default method to collect WebView
    // Here I think the developer wants to write the WebView into an Activity-like component instead just a component
    public void onDestroy() {
        // try to remove this view from its parent first
        try {
            ((ViewGroup) getParent()).removeView(this);
        }
        catch (Exception e) { }

        // then try to remove all child views from this view
        try {
            removeAllViews();
        }
        catch (Exception e) { }

        // and finally destroy this view
        destroy();
    }

    // YW - onActivityResult is for file picker. This method will call after the user have select the file for upload to the website (like user icon)
    // after the user has selected the file to upload the file's uri will save at mFileUploadCallbackFirst if sdk<21 and mFileUploadCallbackSecond if sdk>21
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        // Check which request we're responding to
        if (requestCode == mRequestCodeFilePicker) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    if (mFileUploadCallbackFirst != null) {
                        // YW - must set this method to finish the file selection
                        // the data of the file is in intent.getData()
                        mFileUploadCallbackFirst.onReceiveValue(intent.getData());
                        mFileUploadCallbackFirst = null;
                    }
                    else if (mFileUploadCallbackSecond != null) {
                        Uri[] dataUris;
                        try {
                            dataUris = new Uri[] { Uri.parse(intent.getDataString()) };
                        }
                        catch (Exception e) {
                            dataUris = null;
                        }

                        mFileUploadCallbackSecond.onReceiveValue(dataUris);
                        mFileUploadCallbackSecond = null;
                    }
                }
            }
            // if request is not successful then return null
            else {
                if (mFileUploadCallbackFirst != null) {
                    mFileUploadCallbackFirst.onReceiveValue(null);
                    mFileUploadCallbackFirst = null;
                }
                else if (mFileUploadCallbackSecond != null) {
                    mFileUploadCallbackSecond.onReceiveValue(null);
                    mFileUploadCallbackSecond = null;
                }
            }
        }
    }

    public boolean onBackPressed() {
        if (canGoBack()) {
            goBack();
            return false;
        }
        else {
            return true;
        }
    }

    // YW - add HTML header to the HTML string received
    public void addHttpHeader(final String name, final String value) {
        mHttpHeaders.put(name, value);
    }


    public void removeHttpHeader(final String name) {
        mHttpHeaders.remove(name);
    }

    // YW - Set the allowed url into a List
    public void addPermittedHostname(String hostname) {
        mPermittedHostnames.add(hostname);
    }

    public void addPermittedHostnames(Collection<? extends String> collection) {
        mPermittedHostnames.addAll(collection);
    }

    public List<String> getPermittedHostnames() {
        return mPermittedHostnames;
    }

    public void removePermittedHostname(String hostname) {
        mPermittedHostnames.remove(hostname);
    }

    public void clearPermittedHostnames() {
        mPermittedHostnames.clear();
    }

    // YW - This is to set whether the JavaScript can access to the file in the user's phone. Introduced since sdk 16
    @SuppressLint("NewApi")
    protected static void setAllowAccessFromFileUrls(final WebSettings webSettings, final boolean allowed) {
        if (Build.VERSION.SDK_INT >= 16) {
            webSettings.setAllowFileAccessFromFileURLs(allowed);
            webSettings.setAllowUniversalAccessFromFileURLs(allowed);
        }
    }

    // YW - Sets whether the application's WebView instances should send and accept cookies.
    @SuppressWarnings("static-method")
    public void setCookiesEnabled(final boolean enabled) {
        CookieManager.getInstance().setAcceptCookie(enabled);
    }

    // YW - Sets whether the WebView should allow third party cookies to be set.
    // Apps sdk 21 or later default to disallowing third party cookies.
    @SuppressLint("NewApi")
    public void setThirdPartyCookiesEnabled(final boolean enabled) {
        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, enabled);
        }
    }

    // YW - Configures the WebView's behavior when a secure origin attempts to load a resource from an insecure origin.
    public void setMixedContentAllowed(final boolean allowed) {
        setMixedContentAllowed(getSettings(), allowed);
    }

    @SuppressWarnings("static-method")
    @SuppressLint("NewApi")
    protected void setMixedContentAllowed(final WebSettings webSettings, final boolean allowed) {
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(allowed ? WebSettings.MIXED_CONTENT_ALWAYS_ALLOW : WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onBackKeyUp");
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (this.canGoBack()) {
                        this.goBack();
                    } else {
                        onDestroy();
                    }
                    return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }


    private static class MyTaskParams {

        String u;
        Boolean loadImg;
        Boolean loadFooter;
        Boolean loadCSS;
        Boolean loadJS;

        MyTaskParams(String u, Boolean loadImg, Boolean loadFooter, Boolean loadCSS, Boolean loadJS) {
            this.u=u;
            this.loadImg=loadImg;
            this.loadFooter=loadFooter;
            this.loadCSS=loadCSS;
            this.loadJS=loadJS;
        }
    }

    public void setSimpleModeOn(Boolean t){
        this.SimpleModeOn = t;
    }

    public void setLoadImage(Boolean t){
        this.userSetloadImg = t;
    }

    public void setLoadFooter(Boolean t){
        this.userSetloadFooter = t;
    }

    public void setLoadCSS(Boolean t){
        this.userSetloadCSS = t;
    }

    public void setLoadJS(Boolean t){
        this.userSetloadJS = t;
    }

    public void setCharset(String t){
        this.charset = t;
    }

    public void setReplacedCSS(String filename){
        this.customCSS = filename;
    }

    public void setInsertedJS(String filename){
        this.insertedJS=filename;
    }

    public void setReplacedJS(String JS) { this.customJS=JS; }

    public void setNightModeOn(Boolean t){
        this.nightModeOn=t;
    }

    public void setInsertedCSS(String filename){
        this.insertedCSS=filename;
    }

    class MyJavaScriptInterface {

        private Context ctx;

        MyJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void showHTML(String html) {
            new AlertDialog.Builder(ctx).setTitle("HTML").setMessage(html)
                    .setPositiveButton(android.R.string.ok, null).setCancelable(false).create().show();
        }

    }

    // YW - Initialized when construct
    @SuppressLint({ "SetJavaScriptEnabled" })
    protected void init(final Context context) {

        Log.e(TAG, "Build.VERSION.SDK_INT "+Build.VERSION.SDK_INT);

        if (context instanceof Activity) {
            mActivity = new WeakReference<Activity>((Activity) context);
        }

        // YW - get user language
        mLanguageIso3 = getLanguageIso3();
        // YW - enable view focus
        setFocusable(true);
        setFocusableInTouchMode(true);
        // YW - whether the saving of this view's state is enabled.
        setSaveEnabled(true);
        // YW - get file directory
        final String filesDir = context.getFilesDir().getPath();
        final String databaseDir = filesDir.substring(0, filesDir.lastIndexOf("/")) + DATABASES_SUB_FOLDER;


        // YW - configure the WebView set up
        this.setFitsSystemWindows(true);
        this.clearCache(true);
//        this.addJavascriptInterface(new MyJavaScriptInterface(context), "HtmlViewer");

        final WebSettings webSettings = getSettings();
        webSettings.setAllowFileAccess(false); // set cannot access to file
        webSettings.setBuiltInZoomControls(true); // don't allow built-in zoom mechanism
        webSettings.setJavaScriptEnabled(true); //enable javascript
        webSettings.setDomStorageEnabled(true); // enable storage
        webSettings.setDatabaseEnabled(true); // enable database storage
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setSavePassword(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT < 18) {
            webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH); // webview thread priorities set to high
        }
        if (Build.VERSION.SDK_INT < 19) {
            webSettings.setDatabasePath(databaseDir);
        }

        setAllowAccessFromFileUrls(webSettings, false); // avoid JavaScript access to file
        setMixedContentAllowed(webSettings, true); // actuall set this true is strongly discourage by android document
        setThirdPartyCookiesEnabled(true);


        // YW - setWebViewClient mainly for webview status management


        super.setWebViewClient(new WebViewClient() {
            // YW - implement the AdvancedWebView.Listener interface
            // If developer has override the interface then use the overrided interface. This override is a directly override
            // Developer can also use the traditional way to override the WebViewClient's interface by using setWebViewClient of this AdvancedWebView class (up)
            // So these too kind of override mechanism should not be implemented at the same time
            // The directly override is shown as https://github.com/CS211WebView/Android-AdvancedWebView
            // We should use the traditional setWebViewClient way to override which give us more to override

            private Boolean pageFinished = true;

            // YW - called when page start
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                Log.e(TAG, "Go to new Link");
                Log.d(TAG, "onPageStarted " + url + " " + pageFinished);
//                Log.e("PageSetting", "SimpleModeOn: " + SimpleModeOn + " userSetloadCSS: " + userSetloadCSS + " userSetloadJS: " + userSetloadJS + " ReplaceCSS: " + customCSS + " ReplaceJS: " + customJS + " insertedJS: " + insertedJS + " nightModeOn: " + nightModeOn);

                if (!hasError()) {

                }

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onPageStarted(view, url, favicon); // use method overrided in tranditional way by user
                }

                if (SimpleModeOn) {
//                    Log.e("PageSetting","Get into the mode"+url);

                    if (!isData(url)) {
                        pageFinished = false;
                        stopLoading();
                    }

                    if (!pageFinished) {

                        MyTaskParams para = new MyTaskParams(url, userSetloadImg, userSetloadFooter, userSetloadCSS, userSetloadJS);

                        AsyncTask<MyTaskParams, Integer, Document> htmlRequest = new AsyncTask<MyTaskParams, Integer, Document>() {

                            private MyTaskParams myPara;

                            private Document HandleHTML(Document doc) {

                                String absLinks;
                                try {
                                    Elements as = doc.select("a");
                                    for (Element a : as) {
                                        absLinks = a.attr("abs:href");
                                        a.attr("href", absLinks);
                                    }

                                    Elements forms = doc.select("form[action]");
                                    for (Element form : forms) {
                                        absLinks = form.attr("abs:action");
                                        form.attr("action", absLinks);
                                    }

//                                    System.out.println(doc.html());

                                    if (!myPara.loadImg) {

                                        Elements icos = doc.select(" link[href~=(.*)\\.(ico)$] ");
                                        for (Element ico : icos) {
                                            ico.remove();
                                        }

                                        Elements media = doc.select("img[src]");
                                        for (Element src : media) {
                                            src.select("img").remove();
                                        }

                                        Elements media2 = doc.select("[data-src]");
                                        for (Element src : media2) {
                                            src.remove();
                                        }
                                    } else {
                                        //TODO download the image and add to html

                                    }


                                    if (!myPara.loadFooter) {
                                        Elements footers = doc.select("div#footer");
                                        for (Element fo : footers) {
                                            fo.remove();
                                        }
                                    }

                                    publishProgress(70);

                                    if (myPara.loadCSS) {
                                        // load css
                                        Elements imports = doc.select("link[href]");
                                        for (Element link : imports) {
                                            // get css
                                            if (link.attr("rel").equals("stylesheet")) {
                                                HttpURLConnection connection = null;
                                                BufferedReader reader = null;
                                                StringBuilder response = new StringBuilder();
                                                try {
                                                    URL url = new URL(link.attr("abs:href"));
                                                    connection = (HttpURLConnection) url.openConnection();
                                                    connection.setRequestMethod("GET");
                                                    connection.setConnectTimeout(8000);
                                                    connection.setReadTimeout(8000);
                                                    InputStream in = connection.getInputStream();
                                                    reader = new BufferedReader(new InputStreamReader(in));
                                                    String line;
                                                    while ((line = reader.readLine()) != null) {
                                                        response.append(line);
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                } finally {
                                                    if (connection != null) {
                                                        connection.disconnect();
                                                    }
                                                    if (reader != null) {
                                                        reader.close();
                                                    }
                                                }
                                                Attribute a = new Attribute("type", "text/css");
                                                Attributes adds = new Attributes();
                                                adds.put(a);
                                                Element forReplace = new Element(Tag.valueOf("style"), "", adds);
                                                forReplace.html(response.toString());

                                                link.replaceWith(forReplace);
                                            }
                                        }
                                    } else {
                                        Elements imports = doc.select("link[href]");
                                        for (Element link : imports) {
                                            if (link.attr("rel").equals("stylesheet")) {
                                                if (customCSS == null) {
                                                    link.remove();
                                                } else {
                                                    BufferedReader reader = null;
                                                    StringBuilder response = new StringBuilder();
                                                    try {
                                                        reader = new BufferedReader(
                                                                new InputStreamReader(context.getAssets().open(customCSS)));
                                                        String mLine;
                                                        while ((mLine = reader.readLine()) != null) {
                                                            response.append(mLine);
                                                        }
                                                    } catch (IOException e) {
                                                        //log the exception
                                                        e.printStackTrace();
                                                    } finally {
                                                        if (reader != null) {
                                                            try {
                                                                reader.close();
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                    Attribute a = new Attribute("type", "text/css");
                                                    Attributes adds = new Attributes();
                                                    adds.put(a);
                                                    Element forReplace = new Element(Tag.valueOf("style"), "", adds);
                                                    forReplace.html(response.toString());

                                                    link.replaceWith(forReplace);
                                                }
                                            }
                                        }
                                    }


                                    if (myPara.loadJS) {
                                        Elements scripts = doc.select("script[src]");
                                        for (Element script : scripts) {
                                            // get js
                                            HttpURLConnection connection = null;
                                            BufferedReader reader = null;
                                            StringBuilder response = new StringBuilder();
                                            try {
                                                URL url = new URL(script.attr("abs:src"));
                                                connection = (HttpURLConnection) url.openConnection();
                                                connection.setRequestMethod("GET");
                                                connection.setConnectTimeout(8000);
                                                connection.setReadTimeout(8000);
                                                InputStream in = connection.getInputStream();
                                                reader = new BufferedReader(new InputStreamReader(in));
                                                String line;
                                                while ((line = reader.readLine()) != null) {
                                                    response.append(line);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            } finally {
                                                if (connection != null) {
                                                    connection.disconnect();
                                                }
                                                if (reader != null) {
                                                    reader.close();
                                                }
                                            }

                                            Element forReplace = new Element(Tag.valueOf("script"), "");
                                            forReplace.html(response.toString());

                                            script.replaceWith(forReplace);
                                        }
                                    } else {
                                        Elements scripts = doc.select("script[src]");
                                        for (Element script : scripts) {
                                            if (customJS == null) {
                                                script.remove();
                                            } else {
                                                BufferedReader reader = null;
                                                StringBuilder response = new StringBuilder();
                                                try {
                                                    reader = new BufferedReader(
                                                            new InputStreamReader(context.getAssets().open(customJS)));
                                                    String mLine;
                                                    while ((mLine = reader.readLine()) != null) {
                                                        response.append(mLine);
                                                    }
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                } finally {
                                                    if (reader != null) {
                                                        try {
                                                            reader.close();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }
                                                Element forReplace = new Element(Tag.valueOf("script"), "");
                                                forReplace.html(response.toString());

                                                script.replaceWith(forReplace);
                                            }
                                        }
                                    }

                                    publishProgress(90);


                                    if (insertedCSS != null) {

                                        BufferedReader reader = null;
                                        StringBuilder response = new StringBuilder();
                                        try {
                                            reader = new BufferedReader(
                                                    new InputStreamReader(context.getAssets().open(insertedCSS)));
                                            String mLine;
                                            while ((mLine = reader.readLine()) != null) {
                                                response.append(mLine);
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } finally {
                                            if (reader != null) {
                                                try {
                                                    reader.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                        Attribute a = new Attribute("type", "text/css");
                                        Attributes adds = new Attributes();
                                        adds.put(a);

                                        Element forInsertCSS = new Element(Tag.valueOf("style"), "", adds);
                                        forInsertCSS.html(response.toString());
                                    }

                                    if (insertedJS != null) {
                                        BufferedReader reader = null;
                                        StringBuilder response = new StringBuilder();
                                        try {
                                            reader = new BufferedReader(
                                                    new InputStreamReader(context.getAssets().open(insertedJS)));
                                            String mLine;
                                            while ((mLine = reader.readLine()) != null) {
                                                response.append(mLine);
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } finally {
                                            if (reader != null) {
                                                try {
                                                    reader.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }

                                        Element forInsertJS = new Element(Tag.valueOf("script"), "");
                                        forInsertJS.html(response.toString());

                                        doc.body().append(forInsertJS.toString());
                                    }


                                    if (nightModeOn) {
                                        doc.body().append("<script> javascript:(function() { \n" +
                                                "                    var tags = document.getElementsByTagName('*');\n" +
                                                "                    var i = tags.length;\n" +
                                                "                    while ( i-- ) {\n" +
                                                "                    tags[i].style.backgroundColor = 'black';\n" +
                                                "                    tags[i].style.color = 'white';\n" +
                                                "                    }\n" +
                                                "                    })() </script>");
                                    }
                                    publishProgress(100);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                return doc;
                            }

                            @Override
                            protected Document doInBackground(MyTaskParams... params) {
                                myPara = params[0];
                                Document doc = null;
                                publishProgress(5);
                                try {
                                    doc = Jsoup.connect(myPara.u).get();
//                                    doc = Jsoup.parse(new URL(myPara.u).openStream(), charset, myPara.u);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                publishProgress(50);
                                return HandleHTML(doc);
                            }

                            @Override
                            protected void onProgressUpdate(Integer... values) {

                                if (mCustomWebChromeClient != null) {
                                    mCustomWebChromeClient.onProgressChanged(EnergyEfficiencyWebView.this, values[0]);
                                }

                            }

                            @Override
                            protected void onPostExecute(Document s) {
                                // run on ui thread
                                try {
//                                    System.out.println(s.html());
//                                    System.out.println("---------------------------------------------------------------------------");
                                    Document re = null;
                                    if (mListener != null) {
                                        re = mListener.CustomHandleHTML(s);
                                    } else {
                                        re = s;
                                    }

                                    pageFinished = true;
                                    loadData(re.html(), null, null);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.execute(para);
                    }
                }

            }

            @Override
            public void onPageFinished(WebView view, String url) {

                Log.d(TAG, "onPageFinished");

                if (!hasError()) {
//                    if (mListener != null) {
//                        mListener.onPageFinished(url);
//                    }
                }

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onPageFinished(view, url);
                }

            }

            // YW - deprecated
            // use onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                if (Build.VERSION.SDK_INT < 23) {
                    Log.d(TAG, "onReceivedError, deprecated");

                    setLastError(); // Set up error time

//                    if (mListener != null) {
//                        mListener.onPageError(errorCode, description, failingUrl);
//                    }

                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient.onReceivedError(view, errorCode, description, failingUrl);
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {

                if (Build.VERSION.SDK_INT >= 23) {

                    Log.d(TAG, "onReceivedError");

                    setLastError(); // Set up error time

                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient.onReceivedError(view, request, error);
                    } else {
                        super.onReceivedError(view, request, error);
                    }
                }

            }

            // YW - this is called when the url is just start loading
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.d(TAG, "shouldOverrideUrlLoading");

                if (isHostnameAllowed(url)) {
                    if (mCustomWebViewClient != null) {
                        return mCustomWebViewClient.shouldOverrideUrlLoading(view, url);
                    } else {
                        return false;
                    }
                } else {
//                    if (mListener != null) {
//                        mListener.onExternalPageRequest(url); // Override this method to show some alert when user is trying to access some url not allowed
//                    }

                    return true;
                }
            }

            // YW - called when every time the resource is loading, such as every image
            @Override
            public void onLoadResource(WebView view, String url) {

                Log.d(TAG, "onLoadResource " + url);

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onLoadResource(view, url);
                } else {
                    super.onLoadResource(view, url);
                }
            }

            // YW - This method allow save cache at local instead of request through Internet to reduce the loading speed
            // Notify the host application of a resource request and allow the local storage to return the data
            // deprecated
            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {


                if ((Build.VERSION.SDK_INT >= 11) && (Build.VERSION.SDK_INT <= 20)) {

                    Log.d(TAG, "shouldInterceptRequest, API<21");

//                    Log.d(TAG, "shouldInterceptRequest deprecated called and do something");

                    if (mCustomWebViewClient != null) {
                        return mCustomWebViewClient.shouldInterceptRequest(view, url);
                    } else {
                        return super.shouldInterceptRequest(view, url);
                    }
                } else {
//                    Log.d(TAG, "shouldInterceptRequest deprecated called but do nothing");

                    return null;
                }
            }

            // YW - when API > 21 should use this
            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {


                if (Build.VERSION.SDK_INT >= 21) {

                    Log.d(TAG, "shouldInterceptRequest");
//                    Log.d(TAG, "shouldInterceptRequest not deprecated called and do something");


                    if (mCustomWebViewClient != null) {
                        return mCustomWebViewClient.shouldInterceptRequest(view, request);
                    } else {
//                        if (SimpleModeOn && !isData(request.getUrl().toString())) {
//                            Log.e("Not DATA: should Stop","return null");
//                            return null;
//                        } else {
//                            return super.shouldInterceptRequest(view, request);
//                        }
                        return super.shouldInterceptRequest(view, request);
                    }
                } else {

//                    Log.d(TAG, "shouldInterceptRequest not deprecated called but do nothing");
                    return null;
                }
            }

            // YW - As the host application if the browser should resend data as the requested page was a result of a POST.
            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {

                Log.d(TAG, "onFormResubmission");

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onFormResubmission(view, dontResend, resend);
                } else {
                    super.onFormResubmission(view, dontResend, resend);
                }
            }

            // YW - Notify the host application to update its visited links database.
            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {

                Log.d(TAG, "doUpdateVisitedHistory");

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.doUpdateVisitedHistory(view, url, isReload);
                } else {
                    super.doUpdateVisitedHistory(view, url, isReload);
                }
            }

            // YW - Notify the host application that an SSL error occurred while loading a resource.
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {

                Log.d(TAG, "onReceivedSslError");

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onReceivedSslError(view, handler, error);
                } else {
                    super.onReceivedSslError(view, handler, error);
                }
            }

            // YW - Notify the host application to handle a SSL client certificate request.
            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {

                Log.d(TAG, "onReceivedClientCertRequest");

                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient.onReceivedClientCertRequest(view, request);
                    } else {
                        super.onReceivedClientCertRequest(view, request);
                    }
                }
            }

            // YW - Notifies the host application that the WebView received an HTTP authentication request
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

                Log.d(TAG, "onReceivedHttpAuthRequest");

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
                } else {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm);
                }
            }

            // YW - Give the host application a chance to handle the key event synchronously.
            @Override
            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {

                Log.d(TAG, "shouldOverrideKeyEvent");

                if (mCustomWebViewClient != null) {
                    return mCustomWebViewClient.shouldOverrideKeyEvent(view, event);
                } else {
                    return super.shouldOverrideKeyEvent(view, event);
                }
            }

            // YW - Notify the host application that a key was not handled by the WebView.
            // deprecated
            @Override
            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {

                Log.d(TAG, "onUnhandledKeyEvent, deprecated");

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onUnhandledKeyEvent(view, event);
                } else {
                    super.onUnhandledKeyEvent(view, event);
                }
            }

            // YW - Notify the host application that a input event was not handled by the WebView.
            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onUnhandledInputEvent(WebView view, InputEvent event) {

                if (Build.VERSION.SDK_INT >= 21) {

                    Log.d(TAG, "onUnhandledInputEvent");

                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient.onUnhandledInputEvent(view, event);
                    } else {
                        super.onUnhandledInputEvent(view, event);
                    }
                }
            }

            // YW - Notify the host application that the scale applied to the WebView has changed.
            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {

                Log.d(TAG, "onScaleChanged");

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onScaleChanged(view, oldScale, newScale);
                } else {
                    super.onScaleChanged(view, oldScale, newScale);
                }
            }

            // YW - Notify the host application that a request to automatically log in the user has been processed.
            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {

                if (Build.VERSION.SDK_INT >= 12) {

                    Log.d(TAG, "onReceivedLoginRequest");
                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient.onReceivedLoginRequest(view, realm, account, args);
                    } else {
                        super.onReceivedLoginRequest(view, realm, account, args);
                    }
                }
            }

        });


        // YW - setWebChromeClient mainly for JS process


        super.setWebChromeClient(new WebChromeClient() {

            // file upload callback (Android 2.2 (API level 8) -- Android 2.3 (API level 10)) (hidden method)
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, null);
            }

            // file upload callback (Android 3.0 (API level 11) -- Android 4.0 (API level 15)) (hidden method)
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                openFileChooser(uploadMsg, acceptType, null);
            }

            // file upload callback (Android 4.1 (API level 16) -- Android 4.3 (API level 18)) (hidden method)
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileInput(uploadMsg, null);
            }

            // file upload callback (Android 5.0 (API level 21) -- current) (public method)
            @SuppressWarnings("all")
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {

                Log.i(TAG, "onShowFileChooser");

                openFileInput(null, filePathCallback);
                return true;
            }

            // YW - Tell the host application the current progress of loading a page.
            @Override
            public void onProgressChanged(WebView view, int newProgress) {

                Log.i(TAG, "onProgressChanged");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onProgressChanged(view, newProgress);
                } else {
                    super.onProgressChanged(view, newProgress);
                }
            }

            // YW - Notify the host application of a change in the document title.
            @Override
            public void onReceivedTitle(WebView view, String title) {

                Log.i(TAG, "onReceivedTitle");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onReceivedTitle(view, title);
                } else {
                    super.onReceivedTitle(view, title);
                }
            }

            // YW - Notify the host application of a new favicon for the current page.
            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {

                Log.i(TAG, "onReceivedIcon");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onReceivedIcon(view, icon);
                } else {
                    super.onReceivedIcon(view, icon);
                }
            }

            // YW - Notify the host application of the url for an apple-touch-icon.
            @Override
            public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {

                Log.i(TAG, "onReceivedTouchIconUrl");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
                } else {
                    super.onReceivedTouchIconUrl(view, url, precomposed);
                }
            }

            // YW - Notify the host application that the current page has entered full screen mode.
            // The host application must show the custom View which contains the web contents — video or other HTML content — in full screen mode.
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {

                Log.i(TAG, "onShowCustomView");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onShowCustomView(view, callback);
                } else {
                    super.onShowCustomView(view, callback);
                }
            }

            // YW - Notify the host application that the current page would like to show a custom View in a particular orientation.
            // deprecated
            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {


                if ((Build.VERSION.SDK_INT >= 14) && (Build.VERSION.SDK_INT < 18)) {

                    Log.i(TAG, "onShowCustomView, deprecated");

                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
                    } else {
                        super.onShowCustomView(view, requestedOrientation, callback);
                    }
                }
            }

            // YW - Notify the host application that the current page has exited full screen mode.
            @Override
            public void onHideCustomView() {

                Log.i(TAG, "onHideCustomView");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onHideCustomView();
                } else {
                    super.onHideCustomView();
                }
            }

            // YW - Request the host application to create a new window.
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

                Log.i(TAG, "onCreateWindow");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
                } else {
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
                }
            }

            // YW - Request display and focus for this WebView.
            @Override
            public void onRequestFocus(WebView view) {

                Log.i(TAG, "onRequestFocus");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onRequestFocus(view);
                } else {
                    super.onRequestFocus(view);
                }
            }

            // YW - Notify the host application to close the given WebView and remove it from the view system if necessary.
            @Override
            public void onCloseWindow(WebView window) {

                Log.i(TAG, "onCloseWindow");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onCloseWindow(window);
                } else {
                    super.onCloseWindow(window);
                }
            }

            // YW - Tell the client to display a javascript alert dialog.
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {

                Log.i(TAG, "onJsAlert");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsAlert(view, url, message, result);
                } else {
                    return super.onJsAlert(view, url, message, result);
                }
            }

            // YW - Tell the client to display a confirm dialog to the user.
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {

                Log.i(TAG, "onJsConfirm");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsConfirm(view, url, message, result);
                } else {
                    return super.onJsConfirm(view, url, message, result);
                }
            }

            // YW - Tell the client to display a prompt dialog to the user.
            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {

                Log.i(TAG, "onJsPrompt");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsPrompt(view, url, message, defaultValue, result);
                } else {
                    return super.onJsPrompt(view, url, message, defaultValue, result);
                }
            }

            // YW - Tell the client to display a dialog to confirm navigation away from the current page.
            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {

                Log.i(TAG, "onJsBeforeUnload");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsBeforeUnload(view, url, message, result);
                } else {
                    return super.onJsBeforeUnload(view, url, message, result);
                }
            }

            // YW - Notify the host application that web content from the specified origin is attempting to use the Geolocation API
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {

                Log.i(TAG, "onGeolocationPermissionsShowPrompt");

                if (mGeolocationEnabled) {
                    callback.invoke(origin, true, false);
                } else {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
                    } else {
                        super.onGeolocationPermissionsShowPrompt(origin, callback);
                    }
                }
            }

            // YW - Notify the host application that a request for Geolocation permissions, made with a previous call to onGeolocationPermissionsShowPrompt() has been canceled.
            @Override
            public void onGeolocationPermissionsHidePrompt() {

                Log.i(TAG, "onGeolocationPermissionsHidePrompt");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onGeolocationPermissionsHidePrompt();
                } else {
                    super.onGeolocationPermissionsHidePrompt();
                }
            }

            // YW - Notify the host application that web content is requesting permission to access the specified resources and the permission currently isn't granted or denied.
            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onPermissionRequest(PermissionRequest request) {

                Log.i(TAG, "onPermissionRequest");

                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient.onPermissionRequest(request);
                    } else {
                        super.onPermissionRequest(request);
                    }
                }
            }

            // YW - Notify the host application that the given permission request has been canceled. Any related UI should therefore be hidden.
            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onPermissionRequestCanceled(PermissionRequest request) {

                Log.i(TAG, "onPermissionRequestCanceled");

                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient.onPermissionRequestCanceled(request);
                    } else {
                        super.onPermissionRequestCanceled(request);
                    }
                }
            }

            // YW - Tell the client that a JavaScript execution timeout has occured.
            // deprecated
            @Override
            public boolean onJsTimeout() {

                Log.i(TAG, "onJsTimeout, deprecated");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsTimeout();
                } else {
                    return super.onJsTimeout();
                }
            }


            // YW - Report a JavaScript console message to the host application.
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {

                Log.i(TAG, "onConsoleMessage");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onConsoleMessage(consoleMessage);
                } else {
                    return super.onConsoleMessage(consoleMessage);
                }
            }

            // YW - When not playing, video elements are represented by a 'poster' image.
            @Override
            public Bitmap getDefaultVideoPoster() {

                Log.i(TAG, "getDefaultVideoPoster");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.getDefaultVideoPoster();
                } else {
                    return super.getDefaultVideoPoster();
                }
            }

            // YW - Obtains a View to be displayed while buffering of full screen video is taking place.
            @Override
            public View getVideoLoadingProgressView() {

                Log.i(TAG, "getVideoLoadingProgressView");

                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.getVideoLoadingProgressView();
                } else {
                    return super.getVideoLoadingProgressView();
                }
            }

            // YW - Obtains a list of all visited history items, used for link coloring
            @Override
            public void getVisitedHistory(ValueCallback<String[]> callback) {

                Log.i(TAG, "getVisitedHistory");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.getVisitedHistory(callback);
                } else {
                    super.getVisitedHistory(callback);
                }
            }

            // YW - Tell the client that the quota has been exceeded for the Web SQL Database API for a particular origin and request a new quota.
            // deprecated
            @Override
            public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long estimatedDatabaseSize, long totalQuota, QuotaUpdater quotaUpdater) {

                Log.i(TAG, "onExceededDatabaseQuota, deprecated");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
                } else {
                    super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
                }
            }

            // YW - Notify the host application that the Application Cache has reached the maximum size.
            // deprecated
            @Override
            public void onReachedMaxAppCacheSize(long requiredStorage, long quota, QuotaUpdater quotaUpdater) {

                Log.i(TAG, "onReachedMaxAppCacheSize, deprecated");

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
                } else {
                    super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
                }
            }

        });


        // YW - setDownloadListener

        setDownloadListener(new DownloadListener() {

            // YW - Notify the host application that a file should be downloaded
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
//                if (mListener != null) {
//                    mListener.onDownloadRequested(url, userAgent, contentDisposition, mimetype, contentLength);
//
//                    Log.i(TAG, "onDownloadRequested");
//                }
            }

        });
    }

    private Boolean isUrl(String url){

        if(url.substring(0,4).equals("http") || url.substring(0,5).equals("https")){
            return true;
        }else{
            return false;
        }
    }

    private Boolean isData(String url){

        if(url.substring(0,4).equals("data")){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    public void stopLoading() {
        Log.e(TAG, "stopLoading");
        super.stopLoading();
    }

    @Override
    public void loadData(String data, String mimeType, String encoding) {
        super.loadData(data, mimeType, encoding);
    }

    // YW - loadUrl with the header added by developer
    @Override
    public void loadUrl(final String url, Map<String, String> additionalHttpHeaders) {
        if (additionalHttpHeaders == null) {
            additionalHttpHeaders = mHttpHeaders;
        }
        else if (mHttpHeaders.size() > 0) {
            additionalHttpHeaders.putAll(mHttpHeaders);
        }

        // YW - super.loadUrl is the extended webview class method
        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void loadUrl(final String url) {
        if (mHttpHeaders.size() > 0) {
            super.loadUrl(url, mHttpHeaders);
        }
        else {
            super.loadUrl(url);
        }
    }

    public void loadUrl(String url, final boolean preventCaching) {
        if (preventCaching) {
            url = makeUrlUnique(url);
        }

        loadUrl(url);
    }

    // YW - add system time at the back of the url to avoid webview load from cache
    public void loadUrl(String url, final boolean preventCaching, final Map<String,String> additionalHttpHeaders) {
        if (preventCaching) {
            url = makeUrlUnique(url);
        }

        loadUrl(url, additionalHttpHeaders);
    }

    protected static String makeUrlUnique(final String url) {
        StringBuilder unique = new StringBuilder();
        unique.append(url);

        if (url.contains("?")) {
            unique.append('&');
        }
        else {
            if (url.lastIndexOf('/') <= 7) {
                unique.append('/');
            }
            unique.append('?');
        }

        unique.append(System.currentTimeMillis());
        unique.append('=');
        unique.append(1);

        return unique.toString();
    }

    // YW - check if the url is allowed to access (safety)
    protected boolean isHostnameAllowed(String url) {
        if (mPermittedHostnames.size() == 0) {
            return true;
        }

        url = url.replace("http://", "");
        url = url.replace("https://", "");

        for (String hostname : mPermittedHostnames) {
            if (url.startsWith(hostname)) {
                return true;
            }
        }

        return false;
    }

    // YW - set the time of receiving last error
    protected void setLastError() {
        mLastError = System.currentTimeMillis();
    }

    // YW - Define the error period to be 500ms after the laster error
    // If the current time is inside this range , still consider error currently
    protected boolean hasError() {
        return (mLastError + 500) >= System.currentTimeMillis();
    }

    // YW - get the current system language
    protected static String getLanguageIso3() {
        try {
            return Locale.getDefault().getISO3Language().toLowerCase(Locale.US);
        }
        catch (MissingResourceException e) {
            return LANGUAGE_DEFAULT_ISO3;
        }
    }

    // YW - the file which will be sent is storage in the system and encode with the specific system language
    // So before send, should be decode to binary stream and at the receiver side, should encode with language to specific file type (jpg)
    protected String getFileUploadPromptLabel() {
        try {
            if (mLanguageIso3.equals("zho")) return decodeBase64("6YCJ5oup5LiA5Liq5paH5Lu2");
            else if (mLanguageIso3.equals("spa")) return decodeBase64("RWxpamEgdW4gYXJjaGl2bw==");
            else if (mLanguageIso3.equals("hin")) return decodeBase64("4KSP4KSVIOCkq+CkvOCkvuCkh+CksiDgpJrgpYHgpKjgpYfgpII=");
            else if (mLanguageIso3.equals("ben")) return decodeBase64("4KaP4KaV4Kaf4Ka/IOCmq+CmvuCmh+CmsiDgpqjgpr/gprDgp43gpqzgpr7gpprgpqg=");
            else if (mLanguageIso3.equals("ara")) return decodeBase64("2KfYrtiq2YrYp9ixINmF2YTZgSDZiNin2K3Yrw==");
            else if (mLanguageIso3.equals("por")) return decodeBase64("RXNjb2xoYSB1bSBhcnF1aXZv");
            else if (mLanguageIso3.equals("rus")) return decodeBase64("0JLRi9Cx0LXRgNC40YLQtSDQvtC00LjQvSDRhNCw0LnQuw==");
            else if (mLanguageIso3.equals("jpn")) return decodeBase64("MeODleOCoeOCpOODq+OCkumBuOaKnuOBl+OBpuOBj+OBoOOBleOBhA==");
            else if (mLanguageIso3.equals("pan")) return decodeBase64("4KiH4Kmx4KiVIOCoq+CovuCoh+CosiDgqJrgqYHgqKPgqYs=");
            else if (mLanguageIso3.equals("deu")) return decodeBase64("V8OkaGxlIGVpbmUgRGF0ZWk=");
            else if (mLanguageIso3.equals("jav")) return decodeBase64("UGlsaWggc2lqaSBiZXJrYXM=");
            else if (mLanguageIso3.equals("msa")) return decodeBase64("UGlsaWggc2F0dSBmYWls");
            else if (mLanguageIso3.equals("tel")) return decodeBase64("4LCS4LCVIOCwq+CxhuCxluCwsuCxjeCwqOCxgSDgsI7gsILgsJrgsYHgsJXgsYvgsILgsKHgsL8=");
            else if (mLanguageIso3.equals("vie")) return decodeBase64("Q2jhu41uIG3hu5l0IHThuq1wIHRpbg==");
            else if (mLanguageIso3.equals("kor")) return decodeBase64("7ZWY64KY7J2YIO2MjOydvOydhCDshKDtg50=");
            else if (mLanguageIso3.equals("fra")) return decodeBase64("Q2hvaXNpc3NleiB1biBmaWNoaWVy");
            else if (mLanguageIso3.equals("mar")) return decodeBase64("4KSr4KS+4KSH4KSyIOCkqOCkv+CkteCkoeCkvg==");
            else if (mLanguageIso3.equals("tam")) return decodeBase64("4K6S4K6w4K+BIOCuleCvh+CuvuCuquCvjeCuquCviCDgrqTgr4fgrrDgr43grrXgr4E=");
            else if (mLanguageIso3.equals("urd")) return decodeBase64("2KfbjNqpINmB2KfYptmEINmF24zauiDYs9uSINin2YbYqtiu2KfYqCDaqdix24zaug==");
            else if (mLanguageIso3.equals("fas")) return decodeBase64("2LHYpyDYp9mG2KrYrtin2Kgg2qnZhtuM2K8g24zaqSDZgdin24zZhA==");
            else if (mLanguageIso3.equals("tur")) return decodeBase64("QmlyIGRvc3lhIHNlw6dpbg==");
            else if (mLanguageIso3.equals("ita")) return decodeBase64("U2NlZ2xpIHVuIGZpbGU=");
            else if (mLanguageIso3.equals("tha")) return decodeBase64("4LmA4Lil4Li34Lit4LiB4LmE4Lif4Lil4LmM4Lir4LiZ4Li24LmI4LiH");
            else if (mLanguageIso3.equals("guj")) return decodeBase64("4KqP4KqVIOCqq+CqvuCqh+CqsuCqqOCrhyDgqqrgqrjgqoLgqqY=");
        }
        catch (Exception e) { }

        // return English translation by default
        return "Choose a file";
    }

    // YW - decode the file with base64, a common coding mechanism to transmit on Internet
    protected static String decodeBase64(final String base64) throws IllegalArgumentException, UnsupportedEncodingException {
        final byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return new String(bytes, CHARSET_DEFAULT);
    }

    // YW - fileUploadCallbackFirst for api<5.0, fileUploadCallbackSecond for api>5.0. To use one mode just set the other to null
    @SuppressLint("NewApi")
    protected void openFileInput(final ValueCallback<Uri> fileUploadCallbackFirst, final ValueCallback<Uri[]> fileUploadCallbackSecond) {
        if (mFileUploadCallbackFirst != null) {
            mFileUploadCallbackFirst.onReceiveValue(null);
        }
        mFileUploadCallbackFirst = fileUploadCallbackFirst;

        if (mFileUploadCallbackSecond != null) {
            mFileUploadCallbackSecond.onReceiveValue(null);
        }
        mFileUploadCallbackSecond = fileUploadCallbackSecond;

        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mUploadableFileTypes);

        // YW - Let user select file and trigger onActivityResult
        if (mFragment != null && mFragment.get() != null && Build.VERSION.SDK_INT >= 11) {
            mFragment.get().startActivityForResult(Intent.createChooser(i, getFileUploadPromptLabel()), mRequestCodeFilePicker);
        }
        else if (mActivity != null && mActivity.get() != null) {
            mActivity.get().startActivityForResult(Intent.createChooser(i, getFileUploadPromptLabel()), mRequestCodeFilePicker);
        }
    }


    public static boolean isFileUploadAvailable() {
        return isFileUploadAvailable(false);
    }


    public static boolean isFileUploadAvailable(final boolean needsCorrectMimeType) {
        if (Build.VERSION.SDK_INT == 19) {
            final String platformVersion = (Build.VERSION.RELEASE == null) ? "" : Build.VERSION.RELEASE;

            return !needsCorrectMimeType && (platformVersion.startsWith("4.4.3") || platformVersion.startsWith("4.4.4"));
        }
        else {
            return true;
        }
    }



    // YW - Handle download file in the class which using this AdvancedWebView
    @SuppressLint("NewApi")
    public static boolean handleDownload(final Context context, final String fromUrl, final String toFilename) {
        if (Build.VERSION.SDK_INT < 9) {
            throw new RuntimeException("Method requires API level 9 or above");
        }

        final Request request = new Request(Uri.parse(fromUrl));
        if (Build.VERSION.SDK_INT >= 11) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, toFilename);

        final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            try {
                dm.enqueue(request);
            }
            catch (SecurityException e) {
                if (Build.VERSION.SDK_INT >= 11) {
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                }
                dm.enqueue(request);
            }

            return true;
        }
        // if the download manager app has been disabled on the device
        catch (IllegalArgumentException e) {
            // show the settings screen where the user can enable the download manager app again
            openAppSettings(context, EnergyEfficiencyWebView.PACKAGE_NAME_DOWNLOAD_MANAGER);

            return false;
        }
    }

    // YW - open the setting in Android system
    @SuppressLint("NewApi")
    private static boolean openAppSettings(final Context context, final String packageName) {
        if (Build.VERSION.SDK_INT < 9) {
            throw new RuntimeException("Method requires API level 9 or above");
        }

        try {
            final Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static class Browsers {

        private static String mAlternativePackage;


        public static boolean hasAlternative(final Context context) {
            return getAlternative(context) != null;
        }


        public static String getAlternative(final Context context) {
            if (mAlternativePackage != null) {
                return mAlternativePackage;
            }

            final List<String> alternativeBrowsers = Arrays.asList(ALTERNATIVE_BROWSERS);
            final List<ApplicationInfo> apps = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo app : apps) {
                if (!app.enabled) {
                    continue;
                }

                if (alternativeBrowsers.contains(app.packageName)) {
                    mAlternativePackage = app.packageName;

                    return app.packageName;
                }
            }

            return null;
        }


        public static void openUrl(final Activity context, final String url) {
            openUrl(context, url, false);
        }


        public static void openUrl(final Activity context, final String url, final boolean withoutTransition) {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage(getAlternative(context));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

            if (withoutTransition) {
                context.overridePendingTransition(0, 0);
            }
        }

    }

}
