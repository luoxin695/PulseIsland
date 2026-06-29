package com.pulseisland.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.pulseisland.R;
import com.pulseisland.core.IslandService;

public class MainHomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);

        findViewById(R.id.btnStartIsland).setOnClickListener(v -> {
            Intent intent = new Intent(this, IslandService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(this, "脉搏岛已启动", Toast.LENGTH_SHORT).show();
        });
    }
}
