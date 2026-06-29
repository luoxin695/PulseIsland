package com.pulseisland.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.pulseisland.R;

public class DisclaimerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);

        findViewById(R.id.btnAgree).setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
        findViewById(R.id.btnRefuse).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}
