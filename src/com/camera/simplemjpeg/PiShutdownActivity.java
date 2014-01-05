package com.camera.simplemjpeg;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import com.example.MyCar.R;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;


public class PiShutdownActivity extends Activity {

    int width = 640;
    int height = 480;

    String ip = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pishutdown);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            width = extras.getInt("width", width);
            height = extras.getInt("height", height);
            ip = extras.getString("ip", ip);
        }

        new DoPiShutdown().execute(ip);
    }

    public class DoPiShutdown extends AsyncTask<String, Void, Long> {

        protected Long doInBackground(String... url) {
            try{
                JSch jsch = new JSch();
                String user ="pi";
                Session session = jsch.getSession(user, ip, 22);
                session.setPassword("84Lauren84");
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                String command = "sudo shutdown -h now";
                //String command = "sudo reboot";

                Channel channel=session.openChannel("exec");
                ((ChannelExec)channel).setCommand(command);

                channel.setInputStream(null);

                InputStream in=channel.getInputStream();
                channel.connect();

                byte[] tmp=new byte[1024];
                while(true){
                    while(in.available()>0){
                        int i=in.read(tmp, 0, 1024);
                        if(i<0)break;
                    }
                    if(channel.isClosed()){
                        break;
                    }
                    try{Thread.sleep(1000);}catch(Exception ee){}
                }
                if(!channel.isClosed()){
                    channel.disconnect();
                    session.disconnect();
                }
            } catch(Exception e){
                String x = "";
            }
            return null;
        }

        protected void onPostExecute(Long result) {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}