package com.example.joohonga.microdust;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SeachingAddressActivity extends AppCompatActivity {

    private WebView webView;
    private TextView txt_address;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seaching_address);

        txt_address = findViewById(R.id.txt_address);

        // WebView 초기화
        init_webView();

        // 핸들러를 통한 JavaScript 이벤트 반응
        handler = new Handler();
    }
    public void init_webView() {
        // WebView 설정
        webView = (WebView) findViewById(R.id.webView_address);

        // JavaScript 허용
        webView.getSettings().setJavaScriptEnabled(true);

        // JavaScript의 window.open 허용
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);


        // JavaScript이벤트에 대응할 함수를 정의 한 클래스를 붙여줌
        webView.addJavascriptInterface(new AndroidBridge(), "TestApp");

        // web client 를 chrome 으로 설정
        webView.setWebChromeClient(new WebChromeClient());

        // webview url load. php 파일 주소
        webView.loadUrl("http://10.0.2.2:8888/searching_add.php");

    }


    private class AndroidBridge {
        @JavascriptInterface
        public void setAddress(final String arg1, final String arg2, final String arg3) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    txt_address.setText(String.format("(%s) %s %s", arg1, arg2, arg3));
                    String address = arg2;
                    List<String> addresses = readAddressesFromSP();
                    addresses.add(address);
                    saveToSP(convertToString(addresses));
                    finish();
                }
            });
        }
    }

    private void saveToSP(String add){
        SharedPreferences sharedPreferences = getSharedPreferences("addresses",MODE_PRIVATE);

        //저장을 하기위해 editor를 이용하여 값을 저장시켜준다.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("add",add); // key, value를 이용하여 저장하는 형태

        //최종 커밋
        editor.commit();
    }

    private List<String> readAddressesFromSP(){
        SharedPreferences sp = this.getSharedPreferences("addresses", Context.MODE_PRIVATE);
        String addresses = sp.getString("add", "");
        if(!addresses.equals("")){
            List<String> addes = new ArrayList<>();
            for(String a : addresses.split(" /// ")){
                addes.add(a);
            }
            return addes;
        }
        else
            return new ArrayList<String>();

    }
    private String convertToString(List<String> addresses){
        String toReturn = "";
        for(String add : addresses){
            if(toReturn.isEmpty()){
                toReturn += add;
            }
            else
                toReturn += " /// "+add;
        }
        return toReturn;
    }
}

