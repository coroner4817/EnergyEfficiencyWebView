# EnergyEfficiencyWebView

EnergyEfficiencyWebView is an Android WebView API which can provide flexibility for developer to reconstruct the web page of computer version to the mobile version and add what they want to show to the user.  

<centering>
<img src="https://github.com/coroner4817/EnergyEfficiencyWebView/raw/master/demo/demogif.gif" width="220" height="400"/>
</centering>

## Requirements
  Android SDK 15+

## Usage
  * Add all dependency file in the EnergyEfficiencyWebView (Jsoup jar, class, assets folder)  
  
  * Add the EnergyEfficiencyWebView in to the layout file of the Activity  
    ```  
      <com.yingnanwang.energyefficiencywebview.EnergyEfficiencyWebView  
        android:id="@+id/webview"  
        android:layout_width="match_parent"  
        android:layout_height="match_parent"/>  
    ```  
    
  * In the Activity, add the following set up to the Activity <code>onCreate</code> method:  
    ```  
        EnergyEfficiencyWebView mWebView = (EnergyEfficiencyWebView) findViewById(R.id.webview);
        
        mWebView.setSimpleModeOn(true);
        mWebView.setCharset("UTF-8");
        mWebView.setLoadCSS(false);
        mWebView.setLoadJS(false);
        mWebView.setReplacedCSS(null);
        mWebView.setReplacedJS(null);
        mWebView.setInsertedJS(null);
        mWebView.setNightModeOn(false);
        mWebView.setLoadFooter(false);
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
    ```
    
  * Override the <code>CustomHandleHTML</code> in the Activity to add custom handle mechanism of the HTML:  
    ```  
      @Override
      public Document CustomHandleHTML(Document in) {
          // Do something to DOM with Jsoup lib
          return in;
      }
    ``` 
  * Put you replace and insert CSS and JS file into the assets folder

## Explanation
  * ```setSimpleModeOn``` is ture then only the HTML will be loaded, the css, js and img will be ignored
  * ```setCharset``` is used to set the decode charset, default is ```UTF-8```
  * ```setLoadCSS``` is true then the WebView will load the origin css from the url
  * ```setLoadJS``` is true then the WebView will load the origin js from teh url
  * ```setReplacedCSS``` when setLoadCSS is false and ```setReplacedCSS``` is set to a filename in the assets folder, the WebView will load the custom pre-define css
  * ```setReplacedJS``` is works the same as above
  * ```setInsertedJS``` is set to the JavaScript filename in the assets folder. The js should be wrapped with:  
    ```
    javascript:(
     //your js function code
     )()
    ```
    to let it run immediately
  * ```setNightModeOn``` is true then the WebView will change to night mode (background is black and strings are white)
  * ```setLoadFooter``` is false then the footer of the web page won't be loaded
  
  * Our WebView also have the callback function ```onProgressChanged``` when the progress is changed, use same as the default WebView 
  * Beyond all the setting above, you can also add your custom modification you want to show to the user by overriding ```CustomHandleHTML```. You should using the Java Jsoup HTML parser Library which is included in the project and follow the document from [Jsoup](http://jsoup.org/)
  
## Remaining work
  * Cannot handle the redirection issue in some login system
  * For some big web site, the replacecss and js might not work properly
  * Add into the Maven center for easier usage
  
## Reference
  * [AdvancedWebView](https://github.com/delight-im/Android-AdvancedWebView)  
  
## This is final project for UCLA CS211 in 2016 Winter
  
  
