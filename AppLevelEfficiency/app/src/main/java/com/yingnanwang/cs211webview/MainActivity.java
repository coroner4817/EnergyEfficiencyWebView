package com.yingnanwang.cs211webview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText mUrl;
    private Button mBtnGo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUrl=(EditText)findViewById(R.id.edittext_url);
        mBtnGo=(Button)findViewById(R.id.btn_go);


        mUrl.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if((event.getAction()==KeyEvent.ACTION_DOWN)&&(keyCode==KeyEvent.KEYCODE_ENTER)){
                    GoToWebView();
                }

                return false;
            }
        });

        mBtnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoToWebView();
            }
        });
    }

    private void GoToWebView()
    {
        if (mUrl.getText().toString().matches("")) {
            Toast.makeText(MainActivity.this, "Please input url", Toast.LENGTH_SHORT).show();
        } else {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(MainActivity.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(mUrl.getWindowToken(), 0);

            WebViewActivity.actionStart(MainActivity.this, "http://" + mUrl.getText().toString());
        }
    }
}
