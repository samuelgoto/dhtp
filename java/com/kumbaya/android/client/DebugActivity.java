package com.kumbaya.android.client;

import com.kumbaya.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class DebugActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        TextView mDisplay = (TextView) findViewById(R.id.display);
        mDisplay.setText("Hello World!");
    }
}
