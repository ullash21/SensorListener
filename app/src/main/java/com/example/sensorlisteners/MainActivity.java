package com.example.sensorlisteners;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/*
* To dos
* make factor, shortest time interval for all variables
* UI design for start and stop measurement procedures
* Go through enumerate in java
* See if can get name or value info from Sensor API
* Fix permissions issue, error: need explicit permission else deny access on write
* Synchronise through flags or time intervals
* go through sensorSurvey app to understand how sensor infos like names are collected
* Understand UI implementation of Sensor survey app
* */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String time=java.time.LocalTime.now()+"";
    // Sensors
    private Sensor mSensorProximity;
    private Sensor mSensorLight;
    private Sensor  sensors[];
    private SensorManager mSensorManager;

    // TextViews to display current sensor values
    private TextView mTextSensorLight;
    private TextView mTextSensorProximity;
    boolean dirsMade=false;
    boolean writeEnabled=false;
    FileOutputStream f;
    //Sensor value type handler variables
    private String header[]= {"TimeStamp", "AccX", "AccY","AccZ","GyX","GyY","GyZ","Pressure","GravX",
        "GravY","GravZ","linAccX","linAccY", "linAccZ", "RotVecX","RotVecY","RotVecZ","RotVecAngle",
        "RotVecAccuracy","Humidity","Temp", "GameRotVectorX", "GameRotVectorY","GameRotVectorZ",
        "GameRotVectorAngle", "GameRotVectorAccuracy","GyUncalAngSpeedX","GyUncalAngSpeedY",
        "GyUncalAngSpeedZ", "GyUncaldriftX","GyUncaldriftY","GyUncaldriftZ","PoseX","PoseY","PoseZ",
        "PoseAngle", "PoseTransX","PoseTransY"," PoseTransZ","PoseDelRotX","PoseDelRotY","PoseDelRotZ",
        "PoseDelRotAngle", "PoseDelTransX","PoseDelTransY","PoseDelTransZ","PoseSeqNum",
        "StationaryDetect","MotionDetect", "AccUncalX","AccUncalY","AccUncalZ"," AccUncalBX",
        "AccUncalBY","AccUncalBZ"};
    private int types[]={
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            Sensor.TYPE_POSE_6DOF,
            Sensor.TYPE_STATIONARY_DETECT,
            Sensor.TYPE_MOTION_DETECT,
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED};
    int valueLengths[]={3,3,1,3,3,5,1,1,5,6,15,1,1,6};// order of sensor values with types start from index 1
    float values[];
    boolean headerWritten=false;
    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextSensorLight = (TextView) findViewById(R.id.label_light);
        mTextSensorProximity = (TextView) findViewById(R.id.label_proximity);
        mTextSensorLight = (TextView) findViewById(R.id.label_light);
        writeEnabled=checkPermissions();
        String sensor_error = getResources().getString(R.string.error_no_sensor);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors= new Sensor[14];
        mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        for(int i=0;i<sensors.length;i++)
            sensors[i]=mSensorManager.getDefaultSensor(types[i]);

        if (mSensorLight == null) {
            mTextSensorLight.setText(sensor_error);
        }
        if (mSensorProximity == null) {
            mTextSensorProximity.setText(sensor_error);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(writeEnabled) {
            String csvData = transformCurrentValue(event.values, event.sensor.getType(), event.timestamp);
            csvWrite(csvData);
        }
    }
    private String transformCurrentValue(float[] currentValue,int sensorType,long timestamp){
        String data="";
        if(headerWritten==false) {
            for (String s : header)
                data += s + ",";
            data = data.substring(0, data.length() - 1) + "\n";
            headerWritten=true;
        }
        data+=timestamp+",";
        for(int i=0;i<types.length;i++) {
            if (sensorType == types[i])
                for (int j = 0; j < currentValue.length; j++)
                    data += currentValue[j] + ",";
            else {
                if (sensors[i] == null)
                    for (int j = 0; j < valueLengths[i]; j++)
                        data += "null,";
                else
                    for(int j=0;j<valueLengths[i];j++)
                        data+="-,";
            }
        }
        data = data.substring(0, data.length() - 1) + "\n";
        Log.d("DATA ", data);
        return data;
    }
    private void csvWrite(String s){
        try {

            File sdCard = getExternalFilesDir (Environment.DIRECTORY_DOCUMENTS);
            File dir = new File(sdCard.getAbsolutePath() + "/csv");
            if(!dir.exists())
                dirsMade = dir.mkdir();
            System.out.println(dirsMade+"");
            Log.v("CSV", dirsMade+"");
            Log.v("path",sdCard.getAbsolutePath()+"/csv");
            File file = new File(dir, time+"output.csv");
            f = new FileOutputStream(file, true);
            try {
                f.write(s.getBytes());
                f.flush();
                f.close();
                Log.v("wrote","success");
                //Toast.makeText(getBaseContext(), "Data saved", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    private boolean checkPermissions() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            Toast.makeText(getBaseContext(), "Permission is already granted", Toast.LENGTH_LONG).show();
            Log.v("write","granted");
            return true;
        }
        else{
            Log.v("write","denied");
            return false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    @Override
    protected void onStart() {
        super.onStart();

        for(int i=0;i<sensors.length;i++)
            if (sensors[i] != null)
                mSensorManager.registerListener(this, sensors[i],
                        SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onStop() {
        super.onStop();
        try {
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSensorManager.unregisterListener(this);
    }
}
class MySensor{

    //A data structure to hold up and access sensor information
    private final int type;
    private int values[];
    private String[] headers;
    private boolean stale;// determined by if the value is fetched or not
    private long updateTime;//latest time when the sensor was updated
    private final boolean pseudo;// whether hardware or software sensor
    private long shortestInterval;

    public MySensor(int type,int [] values,String[] headers,boolean pseudo) {
        this.type = type;
        this.headers = headers;
        this.values = values;
        stale=true;
        updateTime = 0;
        shortestInterval=Long.MAX_VALUE;
        this.pseudo=pseudo;
        assert headers.length==values.length;
    }
    public MySensor(int type,int [] values,String[] headers) {
        this.type = type;
        this.headers = headers;
        this.values = values;
        stale=true;
        updateTime = 0;
        shortestInterval=Long.MAX_VALUE;
        this.pseudo=true;
        assert headers.length==values.length;
    }

    public boolean isPseudo() {
        return pseudo;
    }
    public int getLength(){
        return headers.length;
    }

    public void update(int[] values,long updateTime){
        stale=false;
        this.values=values;
        if(updateTime-this.updateTime<shortestInterval) {
            shortestInterval=updateTime-this.updateTime;
        }
        this.updateTime=updateTime;
    }

    public long getShortestInterval() {
        return shortestInterval;
    }

    public double getFrequency() {//Max frequency for sensor
        return Math.pow(10,9)/shortestInterval;
    }

    public String[] getHeaders(){
        return this.headers;
    }

    public int getType() {
        return type;
    }

    public int[] getValues() {
        stale=true;
        return values;
    }

    public boolean isStale() {
        return stale;
    }

}