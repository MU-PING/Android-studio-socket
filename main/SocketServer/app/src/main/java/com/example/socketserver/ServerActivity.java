package com.example.socketserver;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ServerActivity extends AppCompatActivity {

    // UI元件
    private Button leave_btn;
    private Button send_btn;
    private EditText input_edit_text;
    private TextView content_text;
    private TextView member_text;
    private TextView hello_text;
    private String server_name;

    // Socket
    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private final int server_port = 7100;
    private ArrayList<Socket> clients = new ArrayList<Socket>();
    private ArrayList<ClientThread> clients_threads = new ArrayList<ClientThread>();
    private ArrayList<String> clients_names = new ArrayList<String>();
    private boolean isServerOpen = false;                                                   //server是否建立

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        init_setting();
        init_callback();
        init_server();
    }

    // 初始化UI設定
    private void init_setting() {
        leave_btn = (Button) findViewById(R.id.leave_btn);
        send_btn = (Button) findViewById(R.id.send_btn);
        input_edit_text = (EditText) findViewById(R.id.input_edit_text);
        content_text = (TextView) findViewById(R.id.content_text);
        member_text = (TextView) findViewById(R.id.member_text);
        hello_text = (TextView) findViewById(R.id.hello_text);
        content_text.setMovementMethod(ScrollingMovementMethod.getInstance()); // 更新文字時自動滾動到最後一行
        member_text.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    // 初始化事件
    private void init_callback() {
        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                broadcast(server_name, input_edit_text.getText().toString());
                input_edit_text.setText("");
            }
        });
        leave_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // 初始化server
    private void init_server() {

        //讀取server的登入資訊：name
        Intent it = this.getIntent();
        if (it != null) {

            Bundle bundle = it.getExtras();

            if (bundle != null) {

                server_name = bundle.getString("name");

                if (server_name != null && !server_name.equals("")) {
                    hello_text.setText("Hi! " + server_name);
                }
            }
        }

        //啟動server
        serverThread = new ServerThread();
        serverThread.start();
    }

    //取得本地IP位置
    private String getLocalIpAddress() {
        try {
            // 遍歷Collection物件的傳統方式
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()){

                        return inetAddress.getHostAddress();
                    }
                }
            }
        }catch (SocketException ex){
            Log.v("debug",ex.toString());
        }
        return null;
    }

    //加入client
    private void addClient(Socket socket) {

        //啟動 ClientThread
        ClientThread clientThread = new ClientThread(socket);
        clients.add(socket);
        clients_threads.add(clientThread);
        clients_names.add(clientThread.client_name);
        update_member();
        clientThread.start();

        broadcast(server_name,"Welcome "+clientThread.client_name+" join us.");

    }

    //廣播訊息
    private void broadcast(String name, String msg) {

        //啟動 BroadcastThread
        BroadcastThread broadcastThread = new BroadcastThread(name, msg);
        broadcastThread.start();

    }

    //更新聊天室成員
    private void update_member() {
        String output = "";
        String[] clients = new String[clients_names.size()];
        clients_names.toArray(clients);

        //處理還在線上的客戶端
        for (String client : clients_names ) {
            output += client+"\n";
        }

        String finalOutput = output;
        runOnUiThread(() -> member_text.setText(finalOutput));
    }

    //關閉server
    @Override
    public void finish() {
        Socket[] clientArrays = new Socket[clients.size()];
        clients.toArray(clientArrays);

        ClientThread[] threadsArrays = new ClientThread[clients_threads.size()];
        clients_threads.toArray(threadsArrays);

        for (Socket socket : clientArrays) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (ClientThread thread : threadsArrays) {
            if (thread != null) {
                thread.interrupt();
            }
        }
        try {
            serverThread.interrupt();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clients.clear();
        clients_names.clear();
        super.finish();
    }

    //Server執行緒
    class ServerThread extends Thread {

        @Override
        public void run() {
            try {
                runOnUiThread(() -> content_text.append("Connecting...\n"));
                serverSocket = new ServerSocket(server_port); //建立socket server
                runOnUiThread(() -> content_text.append("Server started.("+getLocalIpAddress()+")\n"));

                //等待client得加入，直到server關閉
                while (!serverSocket.isClosed()){
                    try {
                        Socket socket = serverSocket.accept();
                        addClient(socket);

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    // Client執行緒
    class ClientThread extends Thread {
        Socket socket;
        String client_name;
        BufferedReader read;

        public ClientThread(Socket m){
            socket = m;

            try {
                read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String JSONObj = read.readLine();
                JSONObject json_ob=new JSONObject(JSONObj);
                client_name = json_ob.getString("name");
            }
            catch (Exception e){

            }
        }

        @Override
        public void run() {
            String JSONObj;
            String msg;

            try {
                //收到訊息後的處理，使用者可能輸入很多次
                while ((JSONObj= read.readLine()) != null) {
                    JSONObject json_ob = new JSONObject(JSONObj);
                    msg = json_ob.getString("msg");

                    //if (isConnect==false){break;}
                    broadcast(client_name, msg);
                }

                //跳出迴圈表示thread被關閉
                clients.remove(socket);
                clients_names.remove(client_name);
                update_member();
            }
            catch (Exception e){

            }
        }
    }

    // Broadcast執行緒
    class BroadcastThread extends Thread {
        String name;
        String msg;

        public BroadcastThread(String n, String m) {
            name = n;
            msg = m;
        }

        @Override
        public void run() {

            //取出所有連線的client
            Socket[] clientArrays =new Socket[clients.size()];
            clients.toArray(clientArrays);

            //將訊息傳給沒斷線的client
            for (Socket socket : clientArrays ) {
                if(socket!=null){
                    try {
                        //打包訊息
                        Map map=new HashMap();
                        map.put("name", name);
                        map.put("msg", msg);
                        JSONObject value = new JSONObject(map);
                        byte[] jsonByte = (value.toString()+"\n").getBytes();

                        //傳輸訊息
                        DataOutputStream write = new DataOutputStream(socket.getOutputStream());
                        write.write(jsonByte);
                        write.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //廣播的同時更新聊天室
            runOnUiThread(() -> content_text.append(name + "：" + msg + "\t from:" + name + "\n"));
        }
    }
}
