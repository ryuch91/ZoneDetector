package kr.ac.kaist.gan.zonedetector;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOBeaconRegionState;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECOMonitoringListener;
import com.perples.recosdk.RECORangingListener;
import com.perples.recosdk.RECOServiceConnectListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * Created by iDB_ADB on 2016-07-27.
 */
public class ZoneDetectingService extends Service implements RECORangingListener, RECOMonitoringListener, RECOServiceConnectListener {
    /**
     * We recommend 1 second for scanning, 10 seconds interval between scanning, and 60 seconds for region expiration time.
     * 1초 스캔, 10초 간격으로 스캔, 60초의 region expiration time은 당사 권장사항입니다.
     */
    private long mScanDuration = 1*1000L;
    private long mSleepDuration = 10*1000L;
    private long mRegionExpirationTime = 60*1000L;
    private int mNotificationID = 9999;

    private RECOBeaconManager mRecoManager;
    private ArrayList<RECOBeaconRegion> mRegions;

    private ArrayList<RECOBeacon> mRangedBeacons;

    @Override
    public void onCreate() {
        Log.i("ZoneDetectingService", "onCreate()");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("ZoneDetectingService", "onStartCommand");
        /**
         * Create an instance of RECOBeaconManager (to set scanning target and ranging timeout in the background.)
         * If you want to scan only RECO, and do not set ranging timeout in the backgournd, create an instance:
         * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
         * WARNING: False enableRangingTimeout will affect the battery consumption.
         *
         * RECOBeaconManager 인스턴스틀 생성합니다. (스캔 대상 및 백그라운드 ranging timeout 설정)
         * RECO만을 스캔하고, 백그라운드 ranging timeout을 설정하고 싶지 않으시다면, 다음과 같이 생성하시기 바랍니다.
         * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
         * 주의: enableRangingTimeout을 false로 설정 시, 배터리 소모량이 증가합니다.
         */
        mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), MainActivity.SCAN_RECO_ONLY, MainActivity.ENABLE_BACKGROUND_RANGING_TIMEOUT);
        this.bindRECOService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("ZoneDetectingService", "onDestroy()");
        this.tearDown();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i("ZoneDetectingService", "onTaskRemoved()");
        super.onTaskRemoved(rootIntent);
    }

    private void bindRECOService() {
        Log.i("ZoneDetectingService", "bindRECOService()");

        mRegions = new ArrayList<RECOBeaconRegion>();
        this.generateBeaconRegion();

        mRecoManager.setMonitoringListener(this);
        mRecoManager.setRangingListener(this);
        mRecoManager.bind(this);
    }

    private void generateBeaconRegion() {
        Log.i("ZoneDetectingService", "generateBeaconRegion()");

        RECOBeaconRegion recoRegion;

        recoRegion = new RECOBeaconRegion(MainActivity.RECO_UUID, "RECO Sample Region");
        recoRegion.setRegionExpirationTimeMillis(this.mRegionExpirationTime);
        mRegions.add(recoRegion);
    }

    private void startMonitoring() {
        Log.i("ZoneDetectingService", "startMonitoring()");

        mRecoManager.setScanPeriod(this.mScanDuration);
        mRecoManager.setSleepPeriod(this.mSleepDuration);

        for(RECOBeaconRegion region : mRegions) {
            try {
                mRecoManager.startMonitoringForRegion(region);
            } catch (RemoteException e) {
                Log.e("ZoneDetectingService", "RemoteException has occured while executing RECOManager.startMonitoringForRegion()");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.e("ZoneDetectingService", "NullPointerException has occured while executing RECOManager.startMonitoringForRegion()");
                e.printStackTrace();
            }
        }
    }

    private void stopMonitoring() {
        Log.i("ZoneDetectingService", "stopMonitoring()");

        for(RECOBeaconRegion region : mRegions) {
            try {
                mRecoManager.stopMonitoringForRegion(region);
            } catch (RemoteException e) {
                Log.e("ZoneDetectingService", "RemoteException has occured while executing RECOManager.stopMonitoringForRegion()");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.e("ZoneDetectingService", "NullPointerException has occured while executing RECOManager.stopMonitoringForRegion()");
                e.printStackTrace();
            }
        }
    }

    private void startRangingWithRegion(RECOBeaconRegion region) {
        Log.i("ZoneDetectingService", "startRangingWithRegion()");

        /**
         * There is a known android bug that some android devices scan BLE devices only once. (link: http://code.google.com/p/android/issues/detail?id=65863)
         * To resolve the bug in our SDK, you can use setDiscontinuousScan() method of the RECOBeaconManager.
         * This method is to set whether the device scans BLE devices continuously or discontinuously.
         * The default is set as FALSE. Please set TRUE only for specific devices.
         *
         * mRecoManager.setDiscontinuousScan(true);
         */

        try {
            mRecoManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.e("ZoneDetectingService", "RemoteException has occured while executing RECOManager.startRangingBeaconsInRegion()");
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.e("ZoneDetectingService", "NullPointerException has occured while executing RECOManager.startRangingBeaconsInRegion()");
            e.printStackTrace();
        }
    }

    private void stopRangingWithRegion(RECOBeaconRegion region) {
        Log.i("ZoneDetectingService", "stopRangingWithRegion()");

        try {
            mRecoManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.e("ZoneDetectingService", "RemoteException has occured while executing RECOManager.stopRangingBeaconsInRegion()");
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.e("ZoneDetectingService", "NullPointerException has occured while executing RECOManager.stopRangingBeaconsInRegion()");
            e.printStackTrace();
        }
    }

    private void tearDown() {
        Log.i("ZoneDetectingService", "tearDown()");
        this.stopMonitoring();

        try {
            mRecoManager.unbind();
        } catch (RemoteException e) {
            Log.e("ZoneDetectingService", "RemoteException has occured while executing unbind()");
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceConnect() {
        Log.i("ZoneDetectingService", "onServiceConnect()");
        this.startMonitoring();
        //Write the code when RECOBeaconManager is bound to RECOBeaconService
    }

    @Override
    public void didDetermineStateForRegion(RECOBeaconRegionState state, RECOBeaconRegion region) {
        Log.i("ZoneDetectingService", "didDetermineStateForRegion()");
        //Write the code when the state of the monitored region is changed


    }

    @Override
    public void didEnterRegion(RECOBeaconRegion region, Collection<RECOBeacon> beacons) {
        /**
         * For the first run, this callback method will not be called.
         * Please check the state of the region using didDetermineStateForRegion() callback method.
         *
         * 최초 실행시, 이 콜백 메소드는 호출되지 않습니다.
         * didDetermineStateForRegion() 콜백 메소드를 통해 region 상태를 확인할 수 있습니다.
         */

        //Get the region and found beacon list in the entered region
        Log.i("ZoneDetectingService", "didEnterRegion() - " + region.getUniqueIdentifier());
        this.popupNotification("Inside of " + region.getUniqueIdentifier());
        //Write the code when the device is enter the region

        this.startRangingWithRegion(region); //start ranging to get beacons inside of the region
        //from now, stop ranging after 10 seconds if the device is not exited
    }

    @Override
    public void didExitRegion(RECOBeaconRegion region) {
        /**
         * For the first run, this callback method will not be called.
         * Please check the state of the region using didDetermineStateForRegion() callback method.
         *
         * 최초 실행시, 이 콜백 메소드는 호출되지 않습니다.
         * didDetermineStateForRegion() 콜백 메소드를 통해 region 상태를 확인할 수 있습니다.
         */

        Log.i("ZoneDetectingService", "didExitRegion() - " + region.getUniqueIdentifier());
        this.popupNotification("Outside of " + region.getUniqueIdentifier());
        //Write the code when the device is exit the region

        this.stopRangingWithRegion(region); //stop ranging because the device is outside of the region from now
    }

    @Override
    public void didStartMonitoringForRegion(RECOBeaconRegion region) {
        Log.i("ZoneDetectingService", "didStartMonitoringForRegion() - " + region.getUniqueIdentifier());
        //Write the code when starting monitoring the region is started successfully

        this.startRangingWithRegion(region);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<RECOBeacon> beacons, RECOBeaconRegion region) {
        Log.i("ZoneDetectingService", "didRangeBeaconsInRegion() - " + region.getUniqueIdentifier() + " with " + beacons.size() + " beacons");
        //Write the code when the beacons inside of the region is received

        String serverURL = "http://143.248.55.143:8000";
        Log.d("ZoneDetectingService","didRangeBeaconsInRegion() - " + "we will connect to server : " + serverURL);
        mRangedBeacons = (ArrayList) beacons;
        String reqMsg;
        String responseMsg;

        reqMsg = makeJsonMsgfromBeacons(mRangedBeacons);
        responseMsg = sendJsonDataToServer(reqMsg, serverURL);

        popupNotification(responseMsg);
    }

    private void popupNotification(String msg) {
        Log.i("ZoneDetectingService", "popupNotification()");
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date());
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(msg + " " + currentTime)
                .setContentText(msg);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        builder.setStyle(inboxStyle);
        nm.notify(mNotificationID, builder.build());
        mNotificationID = (mNotificationID - 1) % 1000 + 9000;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //This method is not used
        return null;
    }

    @Override
    public void onServiceFail(RECOErrorCode errorCode) {
        //Write the code when the RECOBeaconService is failed.
        //See the RECOErrorCode in the documents.
        return;
    }

    @Override
    public void monitoringDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
        //Write the code when the RECOBeaconService is failed to monitor the region.
        //See the RECOErrorCode in the documents.
        return;
    }

    @Override
    public void rangingBeaconsDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
        //Write the code when the RECOBeaconService is failed to range beacons in the region.
        //See the RECOErrorCode in the documents.
        return;
    }

    //Make JsonMsg from beacon info
    public String makeJsonMsgfromBeacons(Collection<RECOBeacon> beacons){
        String resultMsg = "";
        JSONObject jsonObject = new JSONObject();
        JSONObject beaconObject = new JSONObject();
        JSONArray beaconArray = new JSONArray();

        if(beacons.size()>=4){
            try{
                jsonObject.put("data_size",4);
            }catch(JSONException e){
                e.printStackTrace();
            }
        }else{
            try{
                jsonObject.put("data_size",beacons.size());
            }catch(JSONException e){
                e.printStackTrace();
            }
        }

        if(beacons.isEmpty()){
            beaconArray = null;
        }else if(beacons.size()<4 && beacons.size()>0){
            int indicator = 1;
            for(RECOBeacon beacon : beacons){
                beaconObject = makeJsonObj(indicator, beacon.getMajor(), beacon.getMinor(), beacon.getRssi(), beacon.getAccuracy());
                beaconArray.put(beaconObject);
                indicator++;
            }
        }else if(beacons.size()>=4){
            int indicator = 1;
            for(RECOBeacon beacon : beacons){
                if(indicator==5)
                    break;
                beaconObject = makeJsonObj(indicator, beacon.getMajor(), beacon.getMinor(), beacon.getRssi(), beacon.getAccuracy());
                beaconArray.put(beaconObject);
                indicator++;
            }
        }else{
            beaconArray = null;
        }

        try {
            jsonObject.put("beacon_data", beaconArray);
            resultMsg = jsonObject.toString();
        }catch(JSONException e){
            e.printStackTrace();
        }

        return resultMsg;
    }

    //Make JsonObject from a beacon data
    public JSONObject makeJsonObj(int indicator, int major, int minor, int rssi, double accuracy){
        JSONObject retObj = new JSONObject();
        //JSONStringer jsonStringer = new JSONStringer();
        try {
            retObj.put("beacon_number", indicator); // 1 or 2,3,4
            retObj.put("major", major);
            retObj.put("minor", minor);
            retObj.put("rssi", rssi);
            retObj.put("accuracy", accuracy);
        }catch(JSONException e){
            e.printStackTrace();
        }
        return retObj;
    }

    //send Json data to server using POST method
    public String sendJsonDataToServer(String JsonMsg, String ServerURL){
        OutputStream outputStream;
        InputStream inputStream;
        ByteArrayOutputStream baos;
        HttpURLConnection httpURLConn;
        String response = "";

        try{
            URL url = new URL(ServerURL);
            httpURLConn = (HttpURLConnection)url.openConnection();
            Log.d("ZoneDetectingService","sendJsonDataToServer() - " + "httpURLConnection status is = " + httpURLConn);
            httpURLConn.setConnectTimeout(5 * 1000);
            httpURLConn.setReadTimeout(5 * 1000);
            httpURLConn.setRequestMethod("POST");
            httpURLConn.setRequestProperty("Cache-Control", "no-cache");
            httpURLConn.setRequestProperty("Content-Type", "application/json");
            httpURLConn.setRequestProperty("Accept", "application/json");
            httpURLConn.setDoOutput(true);
            httpURLConn.setDoInput(true);

            outputStream = httpURLConn.getOutputStream();
            outputStream.write(JsonMsg.getBytes());
            outputStream.flush();

            int responseCode = httpURLConn.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                inputStream = httpURLConn.getInputStream();
                baos = new ByteArrayOutputStream();
                byte[] byteBuffer = new byte[1024];
                byte[] byteData = null;
                int nLength = 0;
                while((nLength = inputStream.read(byteBuffer,0,byteBuffer.length)) != -1){
                    baos.write(byteBuffer,0,nLength);
                }
                byteData = baos.toByteArray();
                response = new String(byteData);
                Log.i("ZoneDetectingService","DATA response = " + response);
            }
        }catch(MalformedURLException e){
            e.printStackTrace();
            return null;
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }catch(Exception e){
            Log.e("ZoneDetectingService","sendJsonDataToServer()");
            return null;
        }
        return response;
    }
}
