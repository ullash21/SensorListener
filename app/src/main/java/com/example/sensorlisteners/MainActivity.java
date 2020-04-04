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
import java.time.Instant;
import java.util.ArrayList;

/*
* To dos
* make factor, shortest time interval for all variables
* UI design for start and stop measurement procedures
* Go through enumerate in java
* See if can get name or value info from Sensor API
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
    private String headers[]= {"TimeStamp", "AccX", "AccY","AccZ","GyX","GyY","GyZ","Pressure","GravX",
        "GravY","GravZ","linAccX","linAccY", "linAccZ", "RotVecX","RotVecY","RotVecZ","RotVecAngle",
        "RotVecAccuracy", "GameRotVectorX", "GameRotVectorY","GameRotVectorZ",
        "GameRotVectorAngle", "GameRotVectorAccuracy","GyUncalAngSpeedX","GyUncalAngSpeedY",
        "GyUncalAngSpeedZ", "GyUncaldriftX","GyUncaldriftY","GyUncaldriftZ","PoseX","PoseY","PoseZ",
        "PoseAngle", "PoseTransX","PoseTransY"," PoseTransZ","PoseDelRotX","PoseDelRotY","PoseDelRotZ",
        "PoseDelRotAngle", "PoseDelTransX","PoseDelTransY","PoseDelTransZ","PoseSeqNum",
        "AccUncalX","AccUncalY","AccUncalZ"," AccUncalBX", "AccUncalBY","AccUncalBZ","magFieldX",
        "magField","magFieldY","magFieldZ","magFieldUncalX","magFieldUncalY","magFieldUncalZ",
        "magFieldUncalBX","magFieldUncalBY","magFieldUncalBZ"};
    private int types[]={
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            Sensor.TYPE_POSE_6DOF,
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
    };
    int valueLengths[]={3,3,1,3,3,5,5,6,15,6,3,6};// order of sensor values with types start except TimeStamp
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
        sensors= new Sensor[12];
        mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        int headerIterator=1;;
        for(int i=0;i<sensors.length;i++) {
            sensors[i] = mSensorManager.getDefaultSensor(types[i]);

            String header[]=new String[valueLengths[i]];
            for(int j=0;j<valueLengths[i];j++) {
                header[j] = headers[headerIterator+j];
            }
            headerIterator+=valueLengths[i];
            if(sensors[i]!=null)
                MySensor.mySensors.add(new MySensor(sensors[i],header));
        }
        StringBuilder sensorText = new StringBuilder();
        sensorText.append(System.getProperty("line.separator"));
        for (Sensor currentSensor : sensors ) {
            if(currentSensor!= null)
                sensorText.append(currentSensor.getName()).append("\t"+currentSensor.isDynamicSensor()).append("\t"+currentSensor.getMinDelay()).append(
                        System.getProperty("line.separator"));
        }
        Log.i("sensors",sensorText.toString());
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
            //String csvData = transformCurrentValue(event.values, event.sensor.getType(), event.timestamp);
            MySensor mySensor=MySensor.getSensor(event.sensor.getType());
            mySensor.update(event.values);
            if(MySensor.writeReady()) {
                long time=System.nanoTime();
                String csvData=transformCurrentValue(event.timestamp);
                csvWrite(csvData);
                time=System.nanoTime()-time;
                Log.i("write time",time+"");
            }
        }
    }
    private String transformCurrentValue(long timestamp){
        String data="";
        if(headerWritten==false) {
            for(MySensor mySensor:MySensor.mySensors)
                for (String s : mySensor.getHeader())
                    data += s + ",";
            data = data.substring(0, data.length() - 1) + "\n";
            headerWritten=true;
        }
        data+=timestamp+",";
        /*
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
        */
        for(MySensor mySensor:MySensor.mySensors)
            for (Float value : mySensor.getValues())
                data += value + ",";
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
            Log.v("write","granted");
            return true;
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
    /*Todo: parse string to get name automatically*/


    private final int type;
    private float values[];
    private String[] header;
    private boolean stale;// determined by if the value is fetched or not
    //private long updateTime;//latest time when the sensor was updated
    private boolean pseudo;// whether hardware or software sensor
    private long shortestInterval;
    private static long longestShortestInterval=0;
    private String name;
    public static ArrayList<MySensor> mySensors=new ArrayList();

    public MySensor(Sensor sensor,String[] header) {
        this.type = sensor.getType();
        this.header = header;
        stale=true;
        shortestInterval=sensor.getMinDelay();
        if(shortestInterval>longestShortestInterval)
            longestShortestInterval=shortestInterval;
    }
    /*
    public MySensor(int type,float [] values,String[] headers,boolean pseudo) {
        this.type = type;
        this.header = header;
        this.values = values;
        stale=false;
        updateTime = 0;
        shortestInterval=Long.MAX_VALUE;
        this.pseudo=pseudo;
        assert header.length==values.length;
    }
    public MySensor(int type,float [] values,String[] header) {
        this.type = type;
        this.header = header;
        this.values = values;
        stale=false;
        updateTime = 0;
        shortestInterval=Long.MAX_VALUE;
        this.pseudo=true;
        assert header.length==values.length;
    }
    */
    public static boolean writeReady(){
        int ready=1;
        for(int i=0;i<mySensors.size();i++)
            ready*=mySensors.get(i).isStale()? 0:1;
        return ready==1;
    }
    public static MySensor getSensor(int type){
        for(int i=0;i<mySensors.size();i++)
            if(type==mySensors.get(i).type)
                return mySensors.get(i);
            return null;
    }
    public boolean isPseudo() {
        return pseudo;
    }
    public int getLength(){
        return header.length;
    }

    public void update(float[] values){
        stale=false;
        this.values=values;
        /*
        if(updateTime-this.updateTime<shortestInterval) {
            shortestInterval=updateTime-this.updateTime;
        }
        this.updateTime=updateTime;
         */
    }

    public long getShortestInterval() {
        return shortestInterval;
    }

    public double getFrequency() {//Max frequency for sensor
        return Math.pow(10,9)/shortestInterval;
    }

    public String[] getHeader(){
        return this.header;
    }

    public int getType() {
        return type;
    }

    public float[] getValues() {
        stale=true;
        return values;
    }

    public boolean isStale() {
        return stale;
    }

}