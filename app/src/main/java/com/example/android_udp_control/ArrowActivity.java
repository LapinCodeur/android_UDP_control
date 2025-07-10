package com.example.android_udp_control;

import android.view.HapticFeedbackConstants;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import android.view.MotionEvent;

import android.widget.ImageButton;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ArrowActivity extends AppCompatActivity
{
    private static final String MSG_NONE    = "none";
    String message                          = MSG_NONE;

    private ImageButton up, down, left, right, center, previous, upleftarrow, uprightarrow, downleftarrow, downrightarrow;
    static TextView textX, textY, textTheta;
    UDPClient myUdpClient;
    Bundle bundle;
    Intent intent;
    String udpAddress;
    int updPort;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        try {
            getSupportActionBar().hide();
        }
        catch (Exception ex) {
            System.out.println("Exception while hiding the action bar");
        }

        int width = Resources.getSystem().getDisplayMetrics().widthPixels;

        if (width >= 1080)
            setContentView(R.layout.arrow_view_fullhd);
        else
            setContentView(R.layout.arrow_view);


        intent      = getIntent();
        bundle      = this.getIntent().getExtras();
        udpAddress  = bundle.getString("udpAddress");
        updPort     = bundle.getInt("udpPort");

        myUdpClient = new UDPClient(udpAddress, updPort);
        myUdpClient.setSocket();

        up              = findViewById(R.id.uparrow);
        down            = findViewById(R.id.downarrow);
        left            = findViewById(R.id.leftarrow);
        right           = findViewById(R.id.rightarrow);
        center          = findViewById(R.id.center);
        previous        = findViewById(R.id.previous);
        upleftarrow     = findViewById(R.id.upleftarrow);
        uprightarrow    = findViewById(R.id.uprightarrow);
        downleftarrow   = findViewById(R.id.downleftarrow);
        downrightarrow  = findViewById(R.id.downrightarrow);

        textX     = findViewById(R.id.x_pos);
        textY     = findViewById(R.id.y_pos);
        textTheta = findViewById(R.id.theta_pos);

        Thread udpThread = new Thread(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    myUdpClient.sendCommand(message);
                    System.out.println(message);
                    Thread.sleep(50);
                }
                catch(Exception e) {
                    System.out.println("Caught an exception while sleeping");
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        });

        Thread rxThread = new Thread(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    String message = myUdpClient.receiveData();
                    String[] positionArray = message.split(",");
                    runOnUiThread(new UpdatePositionRunnable(positionArray[0], positionArray[1], positionArray[2]));
                }
                catch(Exception e) {
                    System.out.println("Caught an exception while receiving message");
                }
            }
        });

        setupTouchListeners();

        previous.setOnClickListener(v -> {
            closeThread(udpThread);
            sendCommandAndCloseTheSocket(MSG_NONE);
            closeThread(rxThread);

            Intent changeToMain = new Intent(ArrowActivity.this, MainActivity.class);
            startActivity(changeToMain);
        });

        center.setOnClickListener(v -> {
            message = "0,0,180";
        });

        udpThread.start();
        rxThread.start();
    }

    private void setupTouchListeners() {
        setTouchListener(up, "255,255,0");
        setTouchListener(down, "-255,-255,0");
        setTouchListener(left, "-255,255,0");
        setTouchListener(right, "255,-255,0");
        setTouchListener(upleftarrow, "127,255,0");
        setTouchListener(uprightarrow, "255,127,0");
        setTouchListener(downleftarrow, "-127,-255,0");
        setTouchListener(downrightarrow, "-255,-127,0");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListener(ImageButton button, final String command) {
        button.setOnTouchListener((v, motionEvent) -> {
            switch (motionEvent.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    message = command;
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    return true;

                case MotionEvent.ACTION_UP:
                    v.performClick();
                    message = MSG_NONE;
                    return true;
            }
            return false;
        });
    }

    public static void updatePosition(String x, String y, String theta)
    {
        textX.setText(x);
        textY.setText(y);
        textTheta.setText(theta);
        textX.invalidate();
        textX.requestLayout();
    }

    public static void closeThread(Thread chosenThread)
    {
        if (chosenThread.isAlive())
        {
            chosenThread.interrupt();
            try
            {
                chosenThread.join();
            }
            catch (Exception ex)
            {
                System.out.println("Caught an exception while killing a thread");
            }
        }
    }

    public void sendCommandAndCloseTheSocket(String messageToSend)
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            myUdpClient.sendCommand(messageToSend);
            myUdpClient.closeSocket();
        });
    }
}
