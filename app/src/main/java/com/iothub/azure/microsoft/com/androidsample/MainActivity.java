package com.iothub.azure.microsoft.com.androidsample;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private String connString = ""; //
    private String deviceId = "";
    private double temperature = 35;
    private double humidity;
    private String userName = "";

    private Button startButton;
    private Button stopButton;

    private boolean flag = true;

    SharedPreferences myPrefs;
    IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

    DeviceClient client;
    int readData_idx = 1;

    TelephonyManager telephonyManager;

    private SensorManager sensorManager;
    private Sensor sensor_temp, sensor_accelero;

    private String imei = "";
    private float x, y, z;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        sensor_temp     = sensorManager.getSensorList(Sensor.TYPE_AMBIENT_TEMPERATURE).get(0);
        sensor_accelero = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);

        startButton = (Button) findViewById(R.id.btnSendMessage);
        stopButton = (Button) findViewById(R.id.btnStopMessage);

        stopButton.setEnabled(false);
        startButton.setEnabled(false);

        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
//        try {
//            SendMessage();
//        } catch (Exception e2)
//        {
//            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
//        }

        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);


        myPrefs = getApplicationContext().getSharedPreferences("myPrefs", 0);





/*
        int MY_PERMISSIONS_REQUEST_READ_CONTACTS =0;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
*/


        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_PHONE_STATE},
                1);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        imei = telephonyManager.getDeviceId();
        userName = myPrefs.getString("user", null);

        System.out.println("From Memory :"+ userName);

        if (userName==null) {
            showDialog();
        }
        else {
            start();
        }


    }


    public void start() {


        deviceId = userName +"-"+ imei;

        if (isNetworkAvailable()) {
            try {
                register();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Please connect to Internet", Toast.LENGTH_SHORT);

                }
            });

        }


    }


    public void showDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //before

        dialog.setContentView(R.layout.dialoguserinput);
        //dialog.setTitle("Enter Your Name:");



        // set the custom dialog components - text, image and button
        final EditText text = (EditText) dialog.findViewById(R.id.userInput);


        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                userName = text.getText().toString();
                System.out.println(userName);
//                    deviceId = userName+deviceId;
//                    register();


                SharedPreferences.Editor myPrefsEditor = myPrefs.edit();

                myPrefsEditor.putString("user", userName);
                myPrefsEditor.commit();

                text.setText("");
                start();
                dialog.dismiss();

            }
        });

        dialog.show();
    }

    public void register() throws IOException {

        new RegisterClient().execute();

    }

    public void SendMessage(View view) throws URISyntaxException, IOException
    {
        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        flag=true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        MyClientTask myClientTask = new MyClientTask();
        myClientTask.execute();

    }






    public void btnReceiveOnClick(View v) throws URISyntaxException, IOException
    {
        Button button = (Button) v;

        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);

        if (protocol == IotHubClientProtocol.MQTT)
        {
            MessageCallbackMqtt callback = new MessageCallbackMqtt();
            Counter counter = new Counter(0);
            client.setMessageCallback(callback, counter);
        } else
            {
            MessageCallback callback = new MessageCallback();
            Counter counter = new Counter(0);
            client.setMessageCallback(callback, counter);
        }

        try
        {
            client.open();
        } catch (Exception e2)
        {
            updateUI("Exception while opening IoTHub connection: " + e2.toString());
            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
        }

        try
        {
            Thread.sleep(2000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        client.closeNow();
    }

    public void clearResponse(View view) {
        this.textView.setText("");
    }

    public void StopMessage(View view) {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        flag=false;
    }


    // Our MQTT doesn't support abandon/reject, so we will only display the messaged received
    // from IoTHub and return COMPLETE
    class MessageCallbackMqtt implements com.microsoft.azure.sdk.iot.device.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Counter counter = (Counter) context;
            final String text = "Received message " + counter.toString()
                    + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET);
            System.out.println(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            updateUI(text);

            counter.increment();

            return IotHubMessageResult.COMPLETE;
        }
    }

    class EventCallback implements IotHubEventCallback
    {

        public void execute(final IotHubStatusCode status, Object context)
        {
            final Integer i = (Integer) context;
            System.out.println("IoT Hub responded to message " + i.toString()
                    + " with status " + status.name());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.append("IoT Hub responded to message " + i.toString()
                            + " with status " + status.name() +"\n\n");

                }
            });
        }
    }
    public void updateUI(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append(message +"\n");

            }
        });
    }

    static class MessageCallback implements com.microsoft.azure.sdk.iot.device.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Counter counter = (Counter) context;
            System.out.println(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));


            int switchVal = counter.get() % 3;
            IotHubMessageResult res;
            switch (switchVal)
            {
                case 0:
                    res = IotHubMessageResult.COMPLETE;
                    break;
                case 1:
                    res = IotHubMessageResult.ABANDON;
                    break;
                case 2:
                    res = IotHubMessageResult.REJECT;
                    break;
                default:
                    // should never happen.
                    throw new IllegalStateException("Invalid message result specified.");
            }

            System.out.println("Responding to message " + counter.toString() + " with " + res.name());

            counter.increment();

            return res;
        }
    }

    /**
     * Used as a counter in the message callback.
     */
    static class Counter
    {
        int num;

        Counter(int num) {
            this.num = num;
        }

        int get() {
            return this.num;
        }

        void increment() {
            this.num++;
        }

        @Override
        public String toString() {
            return Integer.toString(this.num);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //sensorManager.registerListener(temp_listener, sensor_temp, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(accelero_listener, sensor_accelero, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        //sensorManager.unregisterListener(temp_listener);
        sensorManager.unregisterListener(accelero_listener);
        super.onStop();
    }

/*    private SensorEventListener temp_listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {}
        @Override
        public void onSensorChanged(SensorEvent event) {
            temperature = event.values[0];
        }
    };*/

    private SensorEventListener accelero_listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {}
        @Override
        public void onSensorChanged(SensorEvent event) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
        }
    };





    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }



    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        String msgStr="";
        @Override
        protected Void doInBackground(Void... arg0) {

            try {

                int i=0;
                while (flag) {

                    i++;

                    try
                    {
                        client.open();
                    } catch (Exception e2)
                    {
                        System.err.println("Exception while opening IoTHub connection: " + e2.toString());
                    }

                        temperature = 20.0 + Math.random() * 10;
                        humidity = 30.0 + Math.random() * 20;
                        String messageId = java.util.UUID.randomUUID().toString();
                        String msgStr  = "{\"deviceId\":\"" + deviceId + "\"," +
                                "\"messageId\": \"" + messageId +  "\", \"x\" : " + Float.toString(x) + ",\"y\" : " + Float.toString(y) + ",\"z\" : " + Float.toString(z) + ", \"readData_idx\" : " + Float.toString(readData_idx) +"}";

                    //"temperature" = " + Double.toString(temperature) + ",

                        //"temperature = " + Double.toString(temperature) + "\nx = " + Float.toString(x) + "\ny = " + Float.toString(y) + "\nz = " + Float.toString(z) + "\nreadData_idx = " + Float.toString(readData_idx);
                        //final String msgStr = "{\"deviceId\":\"" + deviceId + "\",\"messageId\":" + i + ",\"temperature\":" + temperature + ",\"humidity\":" + humidity + "}";
                        try
                        {
                            Message msg = new Message(msgStr);
                            msg.setProperty("temperatureAlert", temperature > 28 ? "true" : "false");
                            msg.setMessageId(messageId);
                            System.out.println(msgStr);

                            String msgs = "x = " +x +"\n" + "y = " +y +"\n"+"z = " +z +"\n";

                            updateUI(msgs);


                            EventCallback eventCallback = new EventCallback();
                            client.sendEventAsync(msg, eventCallback, i);
                        } catch (Exception e)
                        {
//                            updateUI("Exception while sending event: " + e.getMessage());
                            System.err.println("Exception while sending event: " + e.getMessage());
                        }
                        try
                        {
                            Thread.sleep(1000);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }


                    client.closeNow();


                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            textView.append(msgStr +"\n");
            super.onPostExecute(result);
        }
    }



    public class RegisterClient extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... arg0) {


            String primaryKey = "";




            HttpClient httpClient= new DefaultHttpClient();
            HttpGet request = new HttpGet("http://fintech.xlabs.one:8080/azure/registerDevice/"+deviceId);
            HttpResponse response = null;
            try {
                response = httpClient.execute(request);
            } catch (IOException e) {
                e.printStackTrace();
            }

// Get the response
            BufferedReader rd = null;
            try {
                rd = new BufferedReader
                        (new InputStreamReader(
                                response.getEntity().getContent()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            String line = "";
            try {
                while ((line = rd.readLine()) != null) {


                    primaryKey = line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            if(primaryKey.contains("error") || primaryKey.contains("message")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"The device is not connected to the Hub",Toast.LENGTH_SHORT);
                    }
                });


            }

            else {

                //connString = "HostName=TechDay.azure-devices.net;DeviceId=" + deviceId + ";SharedAccessKey=" + primaryKey;
                connString = "HostName=IoT-Hub-EIM-CMB.azure-devices.net;DeviceId=" + deviceId + ";SharedAccessKey=" + primaryKey;
                System.out.println(connString);
                try {
                    client = new DeviceClient(connString, protocol);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startButton.setEnabled(true);
                        }
                    });


                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


}
