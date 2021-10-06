package com.iamverylovely.notify.weather;

import java.util.List;
import android.os.Build;
import android.view.View;
import android.os.Handler;
import android.os.IBinder;
import java.util.Calendar;
import android.os.Message;
import android.app.Service;
import java.io.IOException;
import android.widget.Toast;
import android.content.Intent;
import android.content.Context;
import com.squareup.okhttp.Call;
import android.app.Notification;
import android.app.PendingIntent;
import com.alibaba.fastjson.JSON;
import android.widget.RemoteViews;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Response;
import android.content.IntentFilter;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.annotation.SuppressLint;
import com.squareup.okhttp.OkHttpClient;
import com.iamverylovely.model.Forecast;
import android.content.BroadcastReceiver;
import com.iamverylovely.model.JsonRootBean;
import java.lang.reflect.InvocationTargetException;

public class ForegroundService extends Service implements Callback {

	private Call call;
	private List<Forecast> list;
	private Notification notify;
	private RemoteViews notifyView;
	public static boolean isRunning = false;
	private final OkHttpClient client = new OkHttpClient();
	private final ScreenReceiver receiver = new ScreenReceiver();
	private final IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
	private final Request request = new Request.Builder().url("http://wthrcdn.etouch.cn/weather_mini?citykey=101280101").build();

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		super.onCreate();
		isRunning = true;
		Notification.Builder builder;
		if (Build.VERSION.SDK_INT >= 26) {
			final NotificationChannel channel = new NotificationChannel("1", "Notify Weather Service", NotificationManager.IMPORTANCE_MIN);
			channel.setDescription("Notify Weather Service");
			channel.setSound(null, null);
			channel.enableLights(false);
			channel.setShowBadge(false);
			final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.createNotificationChannel(channel);
			builder = new Notification.Builder(this, channel.getId());
		} else {
			builder = new Notification.Builder(this);
		}
		notifyView = new RemoteViews(getPackageName(), R.layout.notify);
		notifyView.setOnClickPendingIntent(R.id.weather_refresh, PendingIntent.getService(getApplicationContext(), 1, new Intent(getApplicationContext(), ForegroundService.class).putExtra("events", "updtWeather"), PendingIntent.FLAG_UPDATE_CURRENT));
		notify = builder.setCustomContentView(notifyView).setWhen(System.currentTimeMillis()).setSmallIcon(R.drawable.icon_line).setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(true).build();
		startForeground(1, notify);
		registerReceiver(receiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String data = intent.getStringExtra("events");
			if (data != null) {
				switch (data) {
				case "showActivity":
					// startActivity(new Intent(this, MainActivity.class));
					HideStatusBar();
					break;
				case "updtWeather":
					notifyView.setViewVisibility(R.id.first_tips, View.GONE);
					notifyView.setViewVisibility(R.id.weather_info, View.GONE);
					notifyView.setViewVisibility(R.id.loading_tips, View.VISIBLE);
					startForeground(1, notify);
					updtWeather();
					break;
				}
			}
		}
		return START_STICKY;
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1000:
				int count = 0;
				for (Forecast item : list) {
					String low = item.getLow().substring(2).trim();
					String w1 = item.getDate().substring(item.getDate().indexOf(getString(R.string.day)) + 1);
					String w2 = low.substring(0, low.length() - 1) + "~" + item.getHigh().substring(2).trim();
					String w3 = item.getType().trim();
					switch (count) {
					case 0:
						notifyView.setTextViewText(R.id.day1, w1 + "\n" + w2 + "\n" + w3);
						break;
					case 1:
						notifyView.setTextViewText(R.id.day2, w1 + "\n" + w2 + "\n" + w3);
						break;
					case 2:
						notifyView.setTextViewText(R.id.day3, w1 + "\n" + w2 + "\n" + w3);
						break;
					case 3:
						notifyView.setTextViewText(R.id.day4, w1 + "\n" + w2 + "\n" + w3);
						break;
					case 4:
						notifyView.setTextViewText(R.id.day5, w1 + "\n" + w2 + "\n" + w3);
						break;
					default:
						break;
					}
					count++;
				}
				break;
			case 2000:
				HideStatusBar();
				if (list == null) {
					notifyView.setViewVisibility(R.id.first_tips, View.VISIBLE);
				}
				Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_SHORT).show();
				break;
			}
			notifyView.setViewVisibility(R.id.weather_info, View.VISIBLE);
			notifyView.setViewVisibility(R.id.loading_tips, View.GONE);
			startForeground(1, notify);
		}
	};

	@SuppressLint("NewApi")
	private void HideStatusBar() {
		Object service = getSystemService("statusbar");
		if (service != null) {
			Class<?> classObj = null;
			if (Build.VERSION.SDK_INT >= 29) {
				classObj = android.app.StatusBarManager.class;
			} else {
				try {
					classObj = Class.forName("android.app.StatusBarManager");
				} catch (ClassNotFoundException e) {
				}
			}
			if (classObj != null) {
				try {
					classObj.getMethod("collapsePanels").invoke(service);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isRunning = false;
		unregisterReceiver(receiver);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onFailure(Request arg0, IOException arg1) {
		Message msg = Message.obtain();
		msg.what = 2000;
		handler.sendMessage(msg);
	}

	@Override
	public void onResponse(Response resp) {
		try {
			String res = resp.body().string();
			if (res != null) {
				JsonRootBean weatherInfo = JSON.parseObject(res, JsonRootBean.class);
				if (weatherInfo != null && weatherInfo.getStatus() == 1000) {
					weatherInfo.getData().getForecast().get(0).setDate(getString(R.string.today));
					list = weatherInfo.getData().getForecast();
					Message msg = Message.obtain();
					msg.what = 1000;
					handler.sendMessage(msg);
				}
			}
		} catch (IOException e) {
		}
	}

	private void updtWeather() {
		if (call != null && !call.isExecuted()) {
			return;
		}
		call = client.newCall(request);
		call.enqueue(this);
	}

	class ScreenReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == filter.getAction(0) && Calendar.getInstance().get(Calendar.MINUTE) == 0) {
				updtWeather();
			}
		}
	}
}