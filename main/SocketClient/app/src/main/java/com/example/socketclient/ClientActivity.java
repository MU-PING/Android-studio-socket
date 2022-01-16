package com.example.socketclient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;


public class ClientActivity extends AppCompatActivity { //主執行緒，也稱UI執行緒，負責更新UI介面，禁止存取網路

    private Button leave_btn;
    private Button send_btn;
    private EditText input_edit_text;
    private TextView content_text;
    private TextView hello_text;
    private ClientThread clientThread;
    private Socket socket;
    private String name;
    private String ip;
    private int port;

    private  BufferedReader read;
    private  DataOutputStream write;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        init_setting();
        init_callback();
        init_client();
    }

    private void init_setting(){
        leave_btn=(Button)findViewById(R.id.leave_btn);
        send_btn=(Button)findViewById(R.id.send_btn);
        input_edit_text=(EditText)findViewById(R.id.input_edit_text);
        content_text=(TextView)findViewById(R.id.content_text);
        hello_text=(TextView)findViewById(R.id.hello_text);
        content_text.setMovementMethod(ScrollingMovementMethod.getInstance()); // 更新文字時自動滾動到最後一行
    }

    private void init_callback(){
        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                senddata(input_edit_text.getText().toString());
                input_edit_text.setText("");
            }
        });
        leave_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnect();
            }
        });
    }

    // 初始化client
    private void init_client() {

        //讀取client的登入資訊：name ip port
        Intent it = this.getIntent();
        if (it != null) {

            Bundle bundle = it.getExtras();

            if (bundle != null) {

                name = bundle.getString("name");
                ip = bundle.getString("ip");
                port = Integer.parseInt(bundle.getString("port"));

                if (name != null && !name.equals("")) {
                    hello_text.setText("Hi! " + name);
                }
            }
        }

        //啟動client
        clientThread = new ClientThread();
        clientThread.start();

    }
    private void connect(String name){ //連線上時通知server，僅執行一次

        //啟動 SendNameThread
        Connect connect = new Connect(name);
        connect.start();

    }
    private void disconnect(){ // leave主動關閉socket

        try {
            socket.close();
            finish();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void senddata(String msg){ //傳送訊息給server，可執行很多次

        //啟動 SendDataThread
        SendDataThread sendDataThread = new SendDataThread(msg);
        sendDataThread.start();
    }

    class ClientThread extends Thread{ //一般執行緒，可執行所有雜事，可存取網路，禁止更新UI介面

        String input;
        String output;

        @Override
        public void run() {
            try{
                runOnUiThread(() -> hello_text.setText("Connecting..."));

                socket = new Socket(ip, port);

                runOnUiThread(() -> {
                    content_text.append("Hi! " + name + "\n");
                    hello_text.setText("Hi! " + name);
                });

                read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                write = new DataOutputStream(socket.getOutputStream());
                connect(name);

                // readLine() 沒有新訊息就會卡住，直到有新訊息或連線中斷
                // 關閉「本地socket」會導致本地read.readLine()報錯；關閉「遠端socket」，本地read.readLine()會回傳null
                while ((input=read.readLine())!=null) { //離開，就是關閉的意思

                    JSONObject json_ob = new JSONObject(input);
                    String get_name = json_ob.getString("name");
                    String get_msg = json_ob.getString("msg");
                    output = get_name + ":" + get_msg + " from " + get_name + "\n";

                    runOnUiThread(() -> content_text.append(output));
                }

                //leave可以關閉socket；或是透過server關閉socket
                finish();
            }
            catch(IOException | JSONException e){
                e.printStackTrace();
            }
        }
    }
    class Connect extends Thread{

        String name;

        public Connect(String n){
            name = n;
        }
        @Override
        public void run() {
            try{
                Map map=new HashMap();
                map.put("name", name);

                JSONObject value = new JSONObject(map);
                byte[] jsonByte = (value.toString()+"\n").getBytes();

                write.write(jsonByte);
                write.flush();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    class SendDataThread extends Thread{

        String msg;

        public SendDataThread(String m){
            msg = m;
        }

        @Override
        public void run() {
            try{
                Map map=new HashMap();
                map.put("msg", msg);

                JSONObject value = new JSONObject(map);
                byte[] jsonByte = (value.toString()+"\n").getBytes();

                write.write(jsonByte);
                write.flush();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }


}

