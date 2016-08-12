package kr.ac.kaist.gan.zonedetector;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //default proximity uuid of the RECO
    //Reco 비콘의 정해진 UUID. 모든 레코 비콘이 같은 값을 가지고 있다.
    public static final String RECO_UUID = "24DDF411-8CF1-440C-87CD-E368DAF9C93E";

    //public static final String RECO_REG_TIME = "registration_reco_time";

    /***
     *
     * True : Scan RECO beacons only
     * False : Scan all beacons
     * It will be used when the instance of RECOBeaconManager is created.
     * true일 경우 레코 비콘만 스캔하며, false일 경우 모든 비콘을 스캔합니다.
     * RECOBeaconManager 객체 생성 시 사용합니다.
     **/
    public static final boolean SCAN_RECO_ONLY = true;

    /**
     * ENABLE_BACKGROUND_RANGING_TIMEOUT:
     *
     * If true, the application stops to range beacons in the entered region automatically in 10 seconds (background),
     * otherwise it continues to range beacons. (It affects the battery consumption.)
     * It will be used when the instance of RECOBeaconManager is created.
     *
     * 백그라운드 ranging timeout을 설정합니다.
     * true일 경우, 백그라운드에서 입장한 region에서 ranging이 실행 되었을 때, 10초 후 자동으로 정지합니다.
     * false일 경우, 계속 ranging을 실행합니다. (배터리 소모율에 영향을 끼칩니다.)
     * RECOBeaconManager 객체 생성 시 사용합니다.
     */
    public static final boolean ENABLE_BACKGROUND_RANGING_TIMEOUT = false;

    /**
     * DISCONTINUOUS_SCAN:
     *
     * There is a known android bug that some android devices scan BLE devices only once.
     * (link: http://code.google.com/p/android/issues/detail?id=65863)
     * To resolve the bug in our SDK, you can use setDiscontinuousScan() method of the RECOBeaconManager.
     * This method is to set whether the device scans BLE devices continuously or discontinuously.
     * The default is set as FALSE. Please set TRUE only for specific devices.
     *
     * 일부 안드로이드 기기에서 BLE 장치들을 스캔할 때, 한 번만 스캔 후 스캔하지 않는 버그(참고: http://code.google.com/p/android/issues/detail?id=65863)가 있습니다.
     * 해당 버그를 SDK에서 해결하기 위해, RECOBeaconManager에 setDiscontinuousScan() 메소드를 이용할 수 있습니다.
     * 해당 메소드는 기기에서 BLE 장치들을 스캔할 때(즉, ranging 시에), 연속적으로 계속 스캔할 것인지, 불연속적으로 스캔할 것인지 설정하는 것입니다.
     * 기본 값은 FALSE로 설정되어 있으며, 특정 장치에 대해 TRUE로 설정하시길 권장합니다.
     */
    public static final boolean DISCONTINUOUS_SCAN = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION = 10;

    //manually added for wifi connection
    private static final int REQUEST_ENABLE_WIFI = 2;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLayout = findViewById(R.id.mainLayout);

        //If a user device turns off bluetooth, request to turn it on.
        //사용자가 블루투스를 켜도록 요청합니다.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }

        if(!checkWifiNetwork()){
            AlertDialog.Builder wifiDialog = new AlertDialog.Builder(MainActivity.this);
            wifiDialog.setTitle("Network Data Alert");
            //OK button event
            wifiDialog.setMessage("와이파이를 사용하고 있지 않습니다. 요금이 발생할 수 있습니다.");
            wifiDialog.setPositiveButton("OK",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    WifiManager wifiManager = (WifiManager) MainActivity.this.getSystemService(Context.WIFI_SERVICE);
                    wifiManager.setWifiEnabled(true);
                    Toast.makeText(MainActivity.this, "Wi-fi turned on", Toast.LENGTH_SHORT).show();
                }
            });
            //Cancel button event
            wifiDialog.setNegativeButton("Cancel",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    Toast.makeText(MainActivity.this, "Program terminated",Toast.LENGTH_SHORT).show();
                    MainActivity.this.finish();
                }
            });
            wifiDialog.show();
        }

        /**
         * In order to use RECO SDK for Android API 23 (Marshmallow) or higher,
         * the location permission (ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION) is required.
         * Please refer to the following permission guide and sample code provided by Google.
         * 안드로이드 API 23 (마시멜로우)이상 버전부터, 정상적으로 RECO SDK를 사용하기 위해서는
         * 위치 권한 (ACCESS_COARSE_LOCATION 혹은 ACCESS_FINE_LOCATION)을 요청해야 합니다.
         * 권한 요청의 경우, 구글에서 제공하는 가이드를 참고하시기 바랍니다.
         * http://www.google.com/design/spec/patterns/permissions.html
         * https://github.com/googlesamples/android-RuntimePermissions
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.i("MainActivity", "The location permission (ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION) is not granted.");
                this.requestLocationPermission();
            } else {
                Log.i("MainActivity", "The location permission (ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION) is already granted.");
            }
        }

        /**
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

         ***/
    }

    /***
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
    ****/

    /**
     * If the request to turn on bluetooth is denied, the app will be finished.
     * 사용자가 블루투스 요청을 허용하지 않았을 경우, 어플리케이션은 종료됩니다.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            //If the request to turn on bluetooth is denied, the app will be finished.
            //사용자가 블루투스 요청을 허용하지 않았을 경우, 어플리케이션은 종료됩니다.
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /***
     * 위치정보 사용에 대한 퍼미션 허용 or 차단에 따른 결과를 실행하고 보여줍니다.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case REQUEST_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(mLayout, R.string.location_permission_granted, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(mLayout, R.string.location_permission_not_granted, Snackbar.LENGTH_LONG).show();
                }
            }
            default:
                break;
        }
    }

    /**
     * 메인 액티비티가 resume 되었을 때 어떤 서비스가 동작중인지 파악하여 버튼의 상태를 변경합니다.
     * 마지막 zone detecting service에 대해서만 추가되었고, 나머지는 예제 그대로입니다.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if(this.isBackgroundMonitoringServiceRunning(this)) {
            ToggleButton toggle = (ToggleButton)findViewById(R.id.backgroundMonitoringToggleButton);
            toggle.setChecked(true);
        }

        if(this.isBackgroundRangingServiceRunning(this)) {
            ToggleButton toggle = (ToggleButton)findViewById(R.id.backgroundRangingToggleButton);
            toggle.setChecked(true);
        }

        //added manually for check background detecting service is running when returning to main activity
        if(this.isBackgroundDetectingServiceRunning(this)) {
            ToggleButton toggle = (ToggleButton)findViewById(R.id.zoneDetectingToggleButton);
            toggle.setChecked(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * In order to use RECO SDK for Android API 23 (Marshmallow) or higher,
     * the location permission (ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION) is required.
     *
     * This sample project requests "ACCESS_COARSE_LOCATION" permission only,
     * but you may request "ACCESS_FINE_LOCATION" permission depending on your application.
     *
     * "ACCESS_COARSE_LOCATION" permission is recommended.
     *
     * 안드로이드 API 23 (마시멜로우)이상 버전부터, 정상적으로 RECO SDK를 사용하기 위해서는
     * 위치 권한 (ACCESS_COARSE_LOCATION 혹은 ACCESS_FINE_LOCATION)을 요청해야 합니다.
     *
     * 본 샘플 프로젝트에서는 "ACCESS_COARSE_LOCATION"을 요청하지만, 필요에 따라 "ACCESS_FINE_LOCATION"을 요청할 수 있습니다.
     *
     * 당사에서는 ACCESS_COARSE_LOCATION 권한을 권장합니다.
     **/
    private void requestLocationPermission() {
        if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            return;
        }

        Snackbar.make(mLayout, R.string.location_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                    }
                })
                .show();
    }

    public void onMonitoringToggleButtonClicked(View v) {
        ToggleButton toggle = (ToggleButton) v;
        if (toggle.isChecked()) {
            Log.i("MainActivity", "onMonitoringToggleButtonClicked off to on");
            Intent intent = new Intent(this, RecoBackgroundMonitoringService.class);
            startService(intent);
        } else {
            Log.i("MainActivity", "onMonitoringToggleButtonClicked on to off");
            stopService(new Intent(this, RecoBackgroundMonitoringService.class));
        }
    }

    public void onRangingToggleButtonClicked(View v) {
        ToggleButton toggle = (ToggleButton)v;
        if(toggle.isChecked()) {
            Log.i("MainActivity", "onRangingToggleButtonClicked off to on");
            Intent intent = new Intent(this, RecoBackgroundRangingService.class);
            startService(intent);
        } else {
            Log.i("MainActivity", "onRangingToggleButtonClicked on to off");
            stopService(new Intent(this, RecoBackgroundRangingService.class));
        }
    }

    /**
     * 위의 함수를 참조하여 만든 (백그라운드)zone detecting service 의 시작 혹은 정지하는 버튼의
     * 실행을 담당하는 함수입니다.
     * added manually for onclick triggering for Background Zone Detecting Button
     */
    public void onDetectingToggleButtonClicked(View v) {
        ToggleButton toggle = (ToggleButton)v;
        if(toggle.isChecked()) {
            Log.i("MainActivity", "onDetectingToggleButtonClicked off to on");
            Intent intent = new Intent(this, ZoneDetectingService.class);
            startService(intent);
        } else {
            Log.i("MainActivity", "onRangingToggleButtonClicked on to off");
            stopService(new Intent(this, ZoneDetectingService.class));
        }
    }

    /**
     * 각 버튼이 클릭되었을 시에 어떤 액티비티가 실행될 지를 결정하여 Intent를 통해 실행합니다.
     * (화면 전환이 일어나는 activity의 경우에만 해당사항이 있습니다. background 서비스는 상관 없음)
     */
    public void onButtonClicked(View v) {
        Button btn = (Button)v;
        if(btn.getId() == R.id.monitoringButton) {
            final Intent intent = new Intent(this, RecoMonitoringActivity.class);
            startActivity(intent);
        } else if(btn.getId() == R.id.rangingButton) {
            final Intent intent = new Intent(this, RecoRangingActivity.class);
            startActivity(intent);
        } else {
            //added manually for real time zone detecting activity
            final Intent intent = new Intent(this, ZoneDetectingActivity.class);
            startActivity(intent);
        }
    }

    private boolean isBackgroundMonitoringServiceRunning(Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo runningService : am.getRunningServices(Integer.MAX_VALUE)) {
            if(RecoBackgroundMonitoringService.class.getName().equals(runningService.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBackgroundRangingServiceRunning(Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo runningService : am.getRunningServices(Integer.MAX_VALUE)) {
            if(RecoBackgroundRangingService.class.getName().equals(runningService.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 위의 예제 함수들을 따라 zone detecting service가 돌고 있는지를 확인하기 위해 만들어진 함수입니다.
     * 크게 바뀔 부분은 없습니다.
     * description : manually made for checking background zone detecting service is running
     */
    private boolean isBackgroundDetectingServiceRunning(Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo runningService : am.getRunningServices(Integer.MAX_VALUE)) {
            if(ZoneDetectingService.class.getName().equals(runningService.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 현재 네트워크 연결이 WIFi인지 확인하여 그 값을 boolean으로 리턴
     */
    private boolean checkWifiNetwork(){
        ConnectivityManager connMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        return (netInfo.getType() == ConnectivityManager.TYPE_WIFI);
    }
}
