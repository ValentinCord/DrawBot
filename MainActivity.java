package com.f.drawbot01;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity {

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void DrawActivity(View view) {
        Intent intent = new Intent(this, Draw.class);
        startActivity(intent);
    }
    public void BluetoothActivity(View view) {
        Intent intent = new Intent(this, Bluetooth.class);
        startActivity(intent);
    }

    public void toto(){

    }
}

