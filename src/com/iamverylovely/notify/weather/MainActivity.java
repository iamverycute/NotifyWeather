package com.iamverylovely.notify.weather;

import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.os.Bundle;
import android.app.Activity;
import android.widget.Button;
import android.content.Intent;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.annotation.SuppressLint;

public class MainActivity extends Activity {

	private Button btn;
	private Intent intent;
	private PowerManager manager;
	private boolean isIgnore = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		btn = findViewById(R.id.btn_start);
		btn.setText(ForegroundService.isRunning ? R.string.btn_stop : R.string.btn_start);
		manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		isIgnore = manager.isIgnoringBatteryOptimizations(getPackageName());
		if (!isIgnore) {
			startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:" + getPackageName())));
		}
		intent = new Intent(this, ForegroundService.class);
	}

	@Override
	public void onUserLeaveHint() {
		super.onUserLeaveHint();
		if (isIgnore)
			finish();
		if (!ForegroundService.isRunning)
			System.exit(0);
	}

	@SuppressLint("NewApi")
	public void ClickEvent(View view) {
		if (ForegroundService.isRunning) {
			stopService(intent);
			btn.setText(R.string.btn_start);
		} else {
			if (Build.VERSION.SDK_INT >= 26) {
				startForegroundService(intent);
			} else {
				startService(intent);
			}
			btn.setText(R.string.btn_stop);
		}
	}
}