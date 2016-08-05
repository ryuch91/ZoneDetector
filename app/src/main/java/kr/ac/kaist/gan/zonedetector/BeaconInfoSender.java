package kr.ac.kaist.gan.zonedetector;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by iDB_ADB on 2016-08-05.
 */
public class BeaconInfoSender {
    //send Json data to server using POST method
    public String sendJsonDataToServer(String JsonMsg, String ServerURL){
        OutputStream outputStream;
        BufferedWriter bufferedWriter;
        InputStream inputStream;
        ByteArrayOutputStream baos;
        HttpURLConnection httpURLConn;
        String response = "";

        try{
            URL url = new URL(ServerURL);
            httpURLConn = (HttpURLConnection)url.openConnection();
            Log.d("BeaconInfoSender", "sendJsonDataToServer() - " + "httpURLConnection status is = " + httpURLConn);
            httpURLConn.setConnectTimeout(5 * 1000);
            httpURLConn.setReadTimeout(5 * 1000);
            httpURLConn.setRequestMethod("POST");
            httpURLConn.setRequestProperty("Cache-Control", "no-cache");
            httpURLConn.setRequestProperty("Content-Type", "application/json");
            httpURLConn.setRequestProperty("Accept", "application/json");
            httpURLConn.setDoOutput(true);
            httpURLConn.setDoInput(true);

            //Log.d("ZoneDetectingService","sendJsonDataToServer() - " + "response code is : " + httpURLConn.getResponseCode());

            outputStream = httpURLConn.getOutputStream();
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            bufferedWriter.write(JsonMsg);
            bufferedWriter.flush();
            bufferedWriter.close();
            outputStream.close();
            //outputStream.flush();

            httpURLConn.connect();

            int responseCode = httpURLConn.getResponseCode();
            Log.d("BeaconInfoSender","sendJsonDataToServer() - " + "response code is : " + responseCode);
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
                Log.i("BeaconInfoSender","sendJsonDataToServer() - DATA response = " + response);
            }

            httpURLConn.disconnect();

        }catch(MalformedURLException e){
            e.printStackTrace();
            return null;
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }catch(Exception e){
            Log.e("BeaconInfoSender","sendJsonDataToServer()");
            e.printStackTrace();
            return null;
        }
        return response;
    }
}
