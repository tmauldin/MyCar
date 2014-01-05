package com.camera.simplemjpeg;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import com.example.MyCar.R;
import com.google.gson.Gson;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MyActivity extends Activity {

    private static final boolean DEBUG=false;
    private static final String TAG = "MJPEG";

    private static final int REQUEST_SHUTDOWN = 0;

    private MjpegView mv = null;
    String URL;

    private int width = 640;
    private int height = 480;

    private int ip_ad1 = 192;
    private int ip_ad2 = 168;
    private int ip_ad3 = 8;
    private int ip_ad4 = 116;
    private int ip_port = 8080;
    private String ip_command = "?action=stream";
    private boolean suspending = false;

    private String ip;

    private GpsInfo currentGpsInfo;
    private int currentSpeedLimit = 0;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        StringBuilder sb = new StringBuilder();

        String s_dot = ".";
        String s_colon = ":";
        String s_slash = "/";

        sb.append(ip_ad1);
        sb.append(s_dot);
        sb.append(ip_ad2);
        sb.append(s_dot);
        sb.append(ip_ad3);
        sb.append(s_dot);
        sb.append(ip_ad4);

        ip = new String(sb);

        String s_http = "http://";
        sb.insert(0, s_http);
        sb.append(s_colon);
        sb.append(ip_port);
        sb.append(s_slash);
        sb.append(ip_command);
        URL = new String(sb);

        setContentView(R.layout.main);
        mv = (MjpegView) findViewById(R.id.videoViewLeft);
        if(mv != null){
            mv.setResolution(width, height);
        }
        //new DoStartGps().execute();
        new DoRead().execute(URL);
        Timer t = new Timer();
        TimerTask gpsTask = new TimerTask() {
            public void run() {
                new DoUpdateGpsInfo().execute();
            }
        };
        t.schedule(gpsTask, 15000, 30000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shutdown:
                Intent shutdownIntent = new Intent(MyActivity.this, PiShutdownActivity.class);
                shutdownIntent.putExtra("width", width);
                shutdownIntent.putExtra("height", height);
                shutdownIntent.putExtra("ip", ip);
                startActivityForResult(shutdownIntent, REQUEST_SHUTDOWN);
                return true;
            case R.id.restart:
                new RestartApp().execute();
                return true;
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SHUTDOWN:
                if (resultCode == Activity.RESULT_OK) {
                    new RestartApp().execute();
                }
                break;
        }
    }

    public void onResume() {
        if(DEBUG) Log.d(TAG,"onResume()");
        super.onResume();
        if(mv!=null){
            if(suspending){
                new DoRead().execute(URL);
                suspending = false;
            }
        }
    }

    public void onStart() {
        if(DEBUG) Log.d(TAG,"onStart()");
        super.onStart();
    }
    public void onPause() {
        if(DEBUG) Log.d(TAG,"onPause()");
        super.onPause();
        if(mv!=null){
            if(mv.isStreaming()){
                mv.stopPlayback();
                suspending = true;
            }
        }
    }
    public void onStop() {
        if(DEBUG) Log.d(TAG,"onStop()");
        super.onStop();
    }

    public void onDestroy() {
        if(DEBUG) Log.d(TAG,"onDestroy()");

        if(mv!=null){
            mv.freeCameraMemory();
        }

        super.onDestroy();
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {

        protected MjpegInputStream doInBackground(String... url) {
            org.apache.http.HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5 * 1000);
            Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if(res.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            if(result!=null) result.setSkip(1);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(false);
        }
    }

    public class DoUpdateGpsInfo extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... v) {

            // speed from gps
            try {
                URL thisUrl = new URL("http://" + ip + ":8000/nmea.json");
                Gson gson = new Gson();
                currentGpsInfo = gson.fromJson(IOUtils.toString(thisUrl), GpsInfo.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            StringBuffer response = new StringBuffer("");

            // speed limit
            URL url = null;
            String latlngString= Double.toString(currentGpsInfo.getLatitude()) + "," + Double.toString(currentGpsInfo.getLongitude());

            try {
                url = new URL("http://www.wikispeedia.org/speedlimit/geo.php" +
                        "?latlng=" + latlngString);
            } catch (MalformedURLException e) {}

            currentSpeedLimit = 0;
            if (url != null) {
                try {
                    HttpURLConnection urlConn = (HttpURLConnection) url
                            .openConnection();

                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            urlConn.getInputStream()));
                    String inputLine;
                    int amount=0;
                    while (((amount++)<500) && ((inputLine = in.readLine()) != null)) {
                        response.append( inputLine );
                    }
                    in.close();
                    urlConn.disconnect();

                } catch (IOException e) {
                    System.out.println(e.toString());
                }
            }

            int nlen= response.length();
            if(nlen>0) {
                int n,m,max;
                n= response.indexOf("mph");
                m= response.indexOf("kph");
                max= m>n ? m : n;
                if ( max > 0) {
                    CharSequence stuff;
                    stuff= response.subSequence(0, max-1);
                    String str= stuff.toString();

                    try {
                        currentSpeedLimit= Integer.parseInt(str.trim());
                    } catch ( NumberFormatException e ) { }
                }
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            if (currentGpsInfo != null) {
                // speed
                TextView currentSpeed = (TextView)findViewById(R.id.currentSpeed);
                double mph = currentGpsInfo.getSpeed() * 0.621371;
                currentSpeed.setText("Current Speed: " + (int)mph + " mph");

                // speed limit
                TextView speedLimitTextView = (TextView)findViewById(R.id.currentSpeedLimit);
                if (currentSpeedLimit != 0) {
                    speedLimitTextView.setText("Current Speed Limit: " + currentSpeedLimit + " mph");
                }
                else {
                    speedLimitTextView.setText("Speed Limit Unavailable");
                }
            }
        }
    }

    public class DoStartGps extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... v) {
            JSch jsch = new JSch();
            String user ="pi";
            try{
                Session session = jsch.getSession(user, ip, 22);
                session.setPassword("84Lauren84");
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                String command = "sudo killall gpsd; sudo gpsd /dev/ttyUSB0 -F /var/run/gpsd.sock; python /home/pi/geogpsd.py & cd /tmp && python -m SimpleHTTPServer;";

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
                System.out.println(e);
            }
            return null;
        }
    }

    public class RestartApp extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... v) {
            MyActivity.this.finish();
            return null;
        }

        protected void onPostExecute(Void v) {
            startActivity((new Intent(MyActivity.this, MyActivity.class)));
        }
    }
}
