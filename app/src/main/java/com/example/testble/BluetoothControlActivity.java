package com.example.testble;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;

public class BluetoothControlActivity extends Activity implements SensorEventListener,LocationListener {
    private final static String TAG = BluetoothControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private HolloBluetooth mble;
    private Context context;

    private ScrollView scrollView;

    private Handler mHandler;
    public String received;
    public String A3Value;
    public String A0Value;
    public String A2Value;
    public String A1Value;
    private static final int MSG_DATA_CHANGE = 0x11;

    private SeekBar slider;
    float slide1Value = 0.0f;

    TextView A0Input;
    TextView A1Input;
    TextView A2Input;
    TextView A3Input;

    TextView accelX;
    TextView accelY;

    TextView lati;
    TextView longi;

    double longitude;
    double latitude;




    public String toSend;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    StringBuilder output = new StringBuilder();

    private PdUiDispatcher dispatcher;

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(44100, 1, 2, 8, true);

        dispatcher = new PdUiDispatcher();

        PdBase.setReceiver(dispatcher);

        dispatcher.addListener("ble",receiver);
        PdBase.subscribe("ble");

        dispatcher.addListener("AccelXValue",receiver);
        PdBase.subscribe("AccelXValue");

        dispatcher.addListener("AccelYValue",receiver);
        PdBase.subscribe("AccelYValue");

        dispatcher.addListener("LatValue",receiver);
        PdBase.subscribe("LatValue");

        dispatcher.addListener("LongValue",receiver);
        PdBase.subscribe("LongValue");

    }


    public void sendPatchData(String receive, String value) {

        sendFloatPD(receive, Float.parseFloat(value));

        Log.e(receive, value);

    }

    public void sendFloatPD(String receiver, Float value)
    {
        PdBase.sendFloat(receiver, value);
    }

    public void sendBangPD(String receiver)
    {
        PdBase.sendBang(receiver);
    }


    private void loadPDPatch(String patchName) throws IOException {
        File dir = getFilesDir();
        try {
            IoUtils.extractZipResource(getResources().openRawResource(R.raw.synth), dir, true);
            File pdPatch = new File(dir, patchName);
            PdBase.openPatch(pdPatch.getAbsolutePath());
        } catch (IOException e) {

        }
    }




        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            Sensor mySensor = sensorEvent.sensor;

            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {

                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];

                sendFloatPD("accelX", x);
                accelX.setText(String.valueOf(x));
                sendFloatPD("accelY", y);
                accelY.setText(String.valueOf(y));

            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        @Override
        public void onLocationChanged(Location location) {

            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Log.i("MyLocation", Double.toString(latitude) + " " + Double.toString(longitude));
            sendFloatPD("latitude",Float.parseFloat(Double.toString(latitude)));
            lati.setText(String.valueOf(latitude));
            sendFloatPD("longitude",Float.parseFloat(Double.toString(longitude)));

            longi.setText(String.valueOf(longitude));

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        A0Input = (TextView) findViewById(R.id.A0Input);
        A1Input = (TextView) findViewById(R.id.A1Input);
        A2Input = (TextView) findViewById(R.id.A2Input);
        A3Input = (TextView) findViewById(R.id.A3Input);

        accelX = (TextView) findViewById(R.id.AccelXValue);
        accelY = (TextView) findViewById(R.id.AccelYValue);

        lati = (TextView) findViewById(R.id.LatValue);
        longi = (TextView) findViewById(R.id.LongValue);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1,this);


        slider = (SeekBar) findViewById(R.id.slider);

        slider.setOnSeekBarChangeListener(

                new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)  {
                        slide1Value = progress / 100.0f;
                        sendFloatPD("slider", slide1Value);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });





        Switch onOffSwitch = (Switch) findViewById(R.id.onOffSwitch);
        Switch AonOffSwitch = (Switch) findViewById(R.id.A_onOff);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        context = this;

        mble = HolloBluetooth.getInstance(getApplicationContext());


        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        int color = msg.arg1;
                        String strData = (String) msg.obj;
                        SpannableStringBuilder builder = new SpannableStringBuilder(strData);

                        //ForegroundColorSpan ，BackgroundColorSpan
                        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
                        String string;
                        int num;
                        switch (color) {
                            case Color.BLUE: //send

                                builder.setSpan(colorSpan, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.RED:    //error
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.BLACK: //tips
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;

                            default: //receive
                                addLogText(strData, Color.BLACK, strData.length());


                                    for (int i = 0; i < strData.length(); i++) {
                                        if (strData.charAt(i) == 'A' || strData.charAt(i) == 'B' || strData.charAt(i) == 'C' || strData.charAt(i) == 'D') {
                                            received = output.toString();
                                            sensorParse();
                                            output.delete(0,output.length());
                                            output.append(strData.charAt(i));

                                        } else {
                                           output.append(strData.charAt(i));
                                        }
                                    }

                                break;
                        }

                        break;

                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };

               new Handler().post(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++) {
                    if (mble.connectDevice(mDeviceAddress, bleCallBack))
                        break;

                    try {
                        Thread.sleep(10, 0);
                    } catch (Exception e) {

                    }
                }
                if (i == 5) {

                    return;
                }

                try {
                    Thread.sleep(10, 0);
                } catch (Exception e) {

                }


                if (mble.wakeUpBle()) {

                } else {

                }

            }
        });

        try {
            initPD();
            loadPDPatch("synth.pd"); // This is the name of the patch in the zip

            new Handler().post(new Runnable() {
                @Override
                public void run() {

                    if (!mble.sendData("start")) {
                    }

                }
            });


        } catch (IOException e) {
            finish();
        }

        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                float val = (isChecked) ?  1.0f : 0.0f; // value = (get value of isChecked, if true val = 1.0f, if false val = 0.0f)
                sendFloatPD("onOff", val); //send value to patch, receiveEvent names onOff

            }
        });

        AonOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                float val = (isChecked) ?  1.0f : 0.0f; // value = (get value of isChecked, if true val = 1.0f, if false val = 0.0f)
                sendFloatPD("a_onOff", val); //send value to patch, receiveEvent names onOff

            }
        });

    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        return super.onMenuItemSelected(featureId, item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.menu_refresh).setActionView(null);

        return super.onCreateOptionsMenu(menu);
    }

    void sensorParse() {
        if (received.length() > 0) {
            if (received.charAt(0) == 'A') {
                A0Value = received.substring(1);
                Log.i("A0", A0Value);
                A0Input.setText(A0Value);
                sendPatchData("a_input_0", A0Value);
            } else if (received.charAt(0) == 'B') {
                A1Value = received.substring(1);
                Log.i("A1", A1Value);
                A1Input.setText(A1Value);
                sendPatchData("a_input_1", A1Value);
            } else if (received.charAt(0) == 'C') {
                A2Value = received.substring(1);
                Log.i("A2", A2Value);
                A2Input.setText(A2Value);
                sendPatchData("a_input_2", A2Value);
            } else if (received.charAt(0) == 'D') {
                A3Value = received.substring(1);
                Log.i("A3", A3Value);
                A3Input.setText(A3Value);
                sendPatchData("a_input_3", A3Value);
            }

        }
    }

    void addLogText(final String log, final int color, int byteLen) {
        Message message = new Message();
        message.what = MSG_DATA_CHANGE;
        message.arg1 = color;
        message.arg2 = byteLen;
        message.obj = log;
        mHandler.sendMessage(message);
    }

    HolloBluetooth.OnHolloBluetoothCallBack bleCallBack = new HolloBluetooth.OnHolloBluetoothCallBack() {

        @Override
        public void OnHolloBluetoothState(int state) {
            if (state == HolloBluetooth.HOLLO_BLE_DISCONNECTED) {
                onBackPressed();
            }
        }

        @Override
        public void OnReceiveData(byte[] recvData) {
            addLogText(ConvertData.bytesToHexString(recvData, false), Color.rgb(139, 0, 255), recvData.length);


        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PdAudio.startAudio(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PdAudio.stopAudio();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mble.disconnectDevice();
        Log.d(TAG, "destroy");
        mble.disconnectLocalDevice();
        Log.d(TAG, "destroyed");
    }


    private PdReceiver receiver = new PdReceiver() {

        private void pdPost(final String msg) {
            Log.e("RECEIVED:", msg);



                    while (!mble.sendData(msg)) {
                        //  Log.e("BLEWRITE","ERROR");
                    }

                    sendFloatPD("stop", 1.0f);

                }




        @Override
        public void print(String s) {
            Log.i("PRINT",s);
        }

        @Override
        public void receiveBang(String source) {
            //pdPost("bang");
        }

        @Override
        public void receiveFloat(String source, float x) {
            if(source.equals("AccelX"))
            {
                accelX.setText(Float.toString(x));
            }

            else if(source.equals("AccelY"))
            {
                accelY.setText(Float.toString(x));
            }

            else if(source.equals("LatV"))
            {
                lati.setText(Float.toString(x));
            }

            else if(source.equals("LongV"))
            {
                longi.setText(Float.toString(x));
            }
        }

        @Override
        public void receiveList(String source, Object... args) {

        }

        @Override
        public void receiveMessage(String source, String symbol, Object... args) {
            //  pdPost("list: " + Arrays.toString(args));
            toSend  = symbol+",";
            for(int i = 0; i < args.length;i++) {
                toSend += args[i].toString();
                if(i != args.length - 1) {
                    toSend += ",";
                }
                else
                {
                    toSend += ";";
                }
            }
            toSend = toSend.replace(".0","");
            sendFloatPD("start", 1.0f);
            pdPost(toSend);

        }

        @Override
        public void receiveSymbol(String source, String symbol) {
            //pdPost("symbol: " + symbol);
        }
    };

}
