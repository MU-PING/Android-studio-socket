package com.example.socketserver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    EditText name;
    Button connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_setting();
        init_callback();
    }
    private void init_setting(){
        // R class 是 aapt 工具自動生成的 Class，它通過資源ID來取得資源。
        // https://reurl.cc/jgol3p

        name = (EditText) findViewById(R.id.name_edit_text);
        connect = (Button)findViewById(R.id.connect_button);
    }

    private void init_callback(){
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString("name", name.getText().toString());

                //初始化Intent物件
                Intent it = new Intent();

                //傳遞參數
                it.putExtras(bundle);

                //從MainActivity 到 ServerActivity
                it.setClass(MainActivity.this, ServerActivity.class); //轉移頁面

                startActivity(it);
            }
        });
    }
}