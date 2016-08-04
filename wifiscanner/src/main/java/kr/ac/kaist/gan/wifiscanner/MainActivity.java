package kr.ac.kaist.gan.wifiscanner;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private WifiManager wifiManager;
    private BroadcastReceiver receiver;

    public TextView textStatus;
    public Button btnScan;
    public EditText txtEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up UI
        textStatus = (TextView) findViewById(R.id.textStatus);
        btnScan = (Button) findViewById(R.id.btnScan);
        txtEdit = (EditText) findViewById(R.id.txtEdit);
        btnScan.setOnClickListener(this);

        // Setup Wifi
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        // Get Wifi status
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        textStatus.append("\n\nWifi Status: "+ wifiInfo.toString());

        // List available networks
        List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration config : wifiConfigs){
            textStatus.append("\n\n" + config.toString());
        }

        // Register Broadcast Receiver
        if (receiver == null){
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    List<ScanResult> results = wifiManager.getScanResults();
                    ScanResult bestSignal = null;
                    String message = String.format("%s networks found.\n", results.size());

                    Log.d("wifiscanner","onReceive() message: "+ message);

                    for (ScanResult result : results){
                        if (bestSignal == null || WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0){
                            bestSignal = result;
                        }

                        String strStrong = String.format("%s is detected.\n", result.SSID);
                        message += strStrong;
                        Log.d("wifiscanner","AP: " + strStrong);
                    }

                    String msg;
                    if(bestSignal == null){
                        msg = "There is no available AP.";
                    }else{
                        msg = String.format("%s is the strongest.", bestSignal.SSID);
                    }
                    Toast.makeText(MainActivity.this, "msg", Toast.LENGTH_LONG).show();

                    message += msg;
                    print(message);
                }
            };
        }

        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        Log.d("wifiScanner", "onCreate()");

        // Auto made part(by selecting default activity)
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onStop(){
        unregisterReceiver(receiver);
        super.onStop();
    }

    public void onClick(View view){
        Toast.makeText(this, "On Click Clicked. Toast to that!!", Toast.LENGTH_LONG).show();
        if (view.getId() == R.id.btnScan){
            Log.d("wifiScanner","onClick() wifi.startScan()");
            wifiManager.startScan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // print function
    public void print(String string){
        String str = txtEdit.getText().toString();
        if(str.length() > 0){
            str += "\n\n";
        }
        str += string;
        txtEdit.setText(str);

    }
}
