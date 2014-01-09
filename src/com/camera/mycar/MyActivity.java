package com.camera.mycar;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;
import com.example.MyCar.R;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MyActivity extends Activity {

    private static final int REQUEST_SHUTDOWN = 0;

    private int width = 640;
    private int height = 480;

    private int ip_ad1 = 172;
    private int ip_ad2 = 20;
    private int ip_ad3 = 10;
    private int ip_ad4 = 11;

    private String ip;

    private GpsInfo currentGpsInfo;
    private int currentSpeedLimit = 0;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        StringBuilder sb = new StringBuilder();

        String s_dot = ".";

        String s_http = "http://";
        sb.append(s_http);
        sb.append(ip_ad1);
        sb.append(s_dot);
        sb.append(ip_ad2);
        sb.append(s_dot);
        sb.append(ip_ad3);
        sb.append(s_dot);
        sb.append(ip_ad4);

        ip = new String(sb);

        WebView webViewLeft = (WebView)findViewById(R.id.webViewLeft);
        webViewLeft.loadUrl(ip + "/leftCam");

        WebView webViewRight = (WebView)findViewById(R.id.webViewRight);
        webViewRight.loadUrl(ip + "/rightCam");

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

    public class DoUpdateGpsInfo extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... v) {

            // speed from gps
            try {
                URL thisUrl = new URL(ip + "/geogpsd");
                Gson gson = new Gson();
                currentGpsInfo = gson.fromJson(IOUtils.toString(thisUrl), GpsInfo.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (currentGpsInfo != null) {
                StringBuffer response = new StringBuffer("");

                // speed limit
                URL url = null;
                String latlngString= Double.toString(currentGpsInfo.getLatitude()) + "," + Double.toString(currentGpsInfo.getLongitude());

                try {
                    url = new URL("http://www.wikispeedia.org/speedlimit/geo.php" + "?latlng=" + latlngString);
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
