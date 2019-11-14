package com.example.wateringmoodlight;

import android.support.annotation.IdRes;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.larswerkman.holocolorpicker.ColorPicker;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient mqttAndroidClient;

    LinearLayout WateringTab, MoodLightTab;
    TabLayout tabLayout;
    ColorPicker picker;
    FrameLayout ColorPickLayout;
    LinearLayout ColorMenu;
    RadioGroup radioGroup;
    Button ColorChangeBt;
    TextView ColorVal;
    ToggleButton PowerOnOff;
    ProgressBar sensorValBar;
    int sensorVal_int;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String hostip ="tailor.cloudmqtt.com";

        WateringTab = (LinearLayout) findViewById(R.id.WateringTab);
        MoodLightTab = (LinearLayout) findViewById(R.id.MoodLightTab);
        tabLayout = (TabLayout)findViewById(R.id.tabs);
        ColorPickLayout = (FrameLayout)findViewById(R.id.ColorPick);
        ColorMenu = (LinearLayout)findViewById(R.id.ColoerMenu);

        picker = (ColorPicker) findViewById(R.id.picker);

        ColorChangeBt = (Button) findViewById(R.id.ColorChangeBt);
        ColorVal = (TextView) findViewById(R.id.ColorVal);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup); radioGroup.setOnCheckedChangeListener(radioGroupButtonChangeListener);

        PowerOnOff = (ToggleButton) findViewById(R.id.PowerOnOff);
        Button button_on = (Button)findViewById(R.id.button_on);
        sensorValBar = (ProgressBar)findViewById(R.id.sensorProgress);

        final TextView textView1 = (TextView) findViewById(R.id.ArdunoInfo) ;

        mqttAndroidClient = new MqttAndroidClient(this,  "tcp://" + hostip + ":17298", MqttClient.generateClientId());

        // 2번째 파라메터 : 브로커의 ip 주소 , 3번째 파라메터 : client 의 id를 지정함 여기서는 paho 의 자동으로 id를 만들어주는것
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

        options.setUserName("username");
        options.setPassword("password".toCharArray());
        try {
            IMqttToken token = mqttAndroidClient.connect(options);    //mqtttoken 이라는것을 만들어 connect option을 달아줌

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mqttAndroidClient.setBufferOpts(getDisconnectedBufferOptions());    //연결에 성공한경우
                    Log.e("Connect_success", "Success");

                    try {
                        mqttAndroidClient.subscribe("Arduino/sensorVal", 0 );   //토픽 subscribe
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {   //연결에 실패한경우
                    Log.e("connect_fail", "Failure " + exception.toString());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        //물주기 버튼
        button_on.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    mqttAndroidClient.publish("Arduino/Commend", "Watering".getBytes(), 0 , false );
                    //버튼을 클릭하면 jmlee 라는 토픽으로 메시지를 보냄
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        mqttAndroidClient.setCallback(new MqttCallback() {  //클라이언트의 콜백을 처리하는부분
            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override

            public void messageArrived(String topic, MqttMessage message) throws Exception {    //모든 메시지가 올때 Callback method
                if (topic.equals("Arduino/sensorVal")){     //topic 별로 분기처리하여 작업을 수행할수도있음
                    String msg = new String(message.getPayload());
                    Log.e("arrive message : ", msg);
                    sensorVal_int = Integer.parseInt(msg);
                    sensorValBar.setProgress(1023-sensorVal_int);

                    textView1.setText("화분 습도\n"+String.valueOf(((float)(1023-sensorVal_int)/1023)*100)+"%");

                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                changeView(pos);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // do nothing
            }
        });
        ColorChangeBt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorVal.setTextColor(picker.getColor());
                picker.setOldCenterColor(picker.getColor());
                String col = Integer.toHexString(picker.getColor());
                ColorVal.setText(col);

                try {
                    mqttAndroidClient.publish("MoodLight/Color", col.getBytes(), 0 , false );
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
        PowerOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true){
                    ColorMenu.setVisibility(View.VISIBLE);

                    try {
                        mqttAndroidClient.publish("MoodLight/Power", "on".getBytes(), 0 , false );
                        //버튼을 클릭하면 jmlee 라는 토픽으로 메시지를 보냄
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    //무드등 on

                } else {
                    ColorMenu.setVisibility(View.INVISIBLE);
                    try {
                        mqttAndroidClient.publish("MoodLight/Power", "off".getBytes(), 0 , false );
                        //버튼을 클릭하면 jmlee 라는 토픽으로 메시지를 보냄
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    //무드등off
                }
            }
        });
    }

    private void changeView(int index) {

        switch (index) {
            case 0:
                WateringTab.setVisibility(View.VISIBLE);
                MoodLightTab.setVisibility(View.INVISIBLE);
                break;
            case 1:
                WateringTab.setVisibility(View.INVISIBLE);
                MoodLightTab.setVisibility(View.VISIBLE);
                break;

        }
    }
    RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
            if(i == R.id.CW){
                ColorPickLayout.setVisibility(View.VISIBLE);
                try {
                    mqttAndroidClient.publish("MoodLight/Mode", "ColorWipe".getBytes(), 0 , false );
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                //ColorWipe 모드
            } else if(i == R.id.TC){
                ColorPickLayout.setVisibility(View.VISIBLE);

                try {
                    mqttAndroidClient.publish("MoodLight/Mode", "TheaterChase".getBytes(), 0 , false );
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                //TheaterChase
            } else if (i == R.id.RB){
                ColorPickLayout.setVisibility(View.INVISIBLE);

                try {
                    mqttAndroidClient.publish("MoodLight/Mode", "Rainbow".getBytes(), 0 , false );
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                //Rainbow
            }
        }
    };

    private DisconnectedBufferOptions getDisconnectedBufferOptions() {

        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();

        disconnectedBufferOptions.setBufferEnabled(true);

        disconnectedBufferOptions.setBufferSize(100);

        disconnectedBufferOptions.setPersistBuffer(true);

        disconnectedBufferOptions.setDeleteOldestMessages(false);

        return disconnectedBufferOptions;

    }



    private MqttConnectOptions getMqttConnectionOption() {

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

        mqttConnectOptions.setCleanSession(false);

        mqttConnectOptions.setAutomaticReconnect(true);

        mqttConnectOptions.setWill("aaa", "I am going offline".getBytes(), 1, true);

        return mqttConnectOptions;

    }

}
