package kr.ac.kaist.gan.zonedetector;

import android.os.AsyncTask;

/**
 * Created by iDB_ADB on 2016-08-05.
 * It uses asynctask to do some network related job.
 * It sends jsonMsg to server and get response message and return it.
 * Input : jsonMsg
 * Output : response from server
 */
public class BeaconInfoSendingTask extends AsyncTask<String,Void,String> {
    private String serverURL = "http://143.248.55.143:8000";
    private String jsonMsg = null;
    private String response = null;

    @Override
    public String doInBackground(String... params){
        BeaconInfoSender sender = new BeaconInfoSender();
        jsonMsg = params[0];
        response = sender.sendJsonDataToServer(jsonMsg, serverURL);

        return response;
    }
}
