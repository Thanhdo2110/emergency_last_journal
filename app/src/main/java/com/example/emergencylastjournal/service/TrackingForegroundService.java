package com.example.emergencylastjournal.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.example.emergencylastjournal.MainActivity;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionState;
import com.example.emergencylastjournal.util.LocationHelper;
import com.example.emergencylastjournal.util.SmsHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.Executors;

public class TrackingForegroundService extends Service {
    private static final String TAG = "TrackingService";
    private static final String CHANNEL_ID = "SafetyTrackingChannel";
    private static final int NOTIF_ID_MAIN = 1001;
    private static final int NOTIF_ID_WARNING = 1002;
    private static final int NOTIF_ID_URGENT = 1003;

    public static final String ACTION_EXTEND_TIMER = "com.example.emergencylastjournal.ACTION_EXTEND_TIMER";

    public static final MutableLiveData<Long> timeLeftSeconds = new MutableLiveData<>(0L);
    public static final MutableLiveData<SessionState> currentState = new MutableLiveData<>(SessionState.IDLE);

    private CountDownTimer countDownTimer;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private int currentSessionId;
    private boolean isUrgentSmsSent = false;
    private long currentRemainingMillis = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification("Đang trong chế độ bảo vệ...", false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID_MAIN, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIF_ID_MAIN, notification);
        }

        if (intent != null) {
            if (ACTION_EXTEND_TIMER.equals(intent.getAction())) {
                extendTimer(600000); // Cộng thêm 10 phút (600,000 ms)
            } else {
                currentSessionId = intent.getIntExtra("SESSION_ID", -1);
                int durationSeconds = intent.getIntExtra("DURATION_SECONDS", 0);
                
                if (durationSeconds > 0) {
                    isUrgentSmsSent = false;
                    currentState.postValue(SessionState.ACTIVE);
                    startTimer(durationSeconds * 1000L);
                    startLocationTracking();
                }
            }
        }
        return START_STICKY;
    }

    private void extendTimer(long extraMillis) {
        long newDuration = currentRemainingMillis + extraMillis;
        if (currentState.getValue() != SessionState.ACTIVE) {
            currentState.postValue(SessionState.ACTIVE);
            isUrgentSmsSent = false;
        }
        startTimer(newDuration);
        updateMainNotification("Đã gia hạn thêm 10 phút bảo vệ", false);
    }

    private void startTimer(long duration) {
        if (countDownTimer != null) countDownTimer.cancel();
        currentRemainingMillis = duration;
        timeLeftSeconds.postValue(duration / 1000);

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentRemainingMillis = millisUntilFinished;
                long seconds = millisUntilFinished / 1000;
                timeLeftSeconds.postValue(seconds);

                // Chỉ thông báo tại các mốc quan trọng: 15, 10, 5, 4, 3, 2, 1 phút
                if (seconds == 900 || seconds == 600 || seconds == 300 || 
                    seconds == 240 || seconds == 180 || seconds == 120 || seconds == 60) {
                    
                    int minutes = (int) (seconds / 60);
                    String message = "Thời gian bảo vệ còn lại: " + minutes + " phút";

                    if (seconds == 60) { // 1 phút
                        if (currentState.getValue() != SessionState.URGENT) {
                            currentState.postValue(SessionState.URGENT);
                        }
                        if (!isUrgentSmsSent) {
                            isUrgentSmsSent = true;
                            SmsHelper.sendEmergencyAlert(TrackingForegroundService.this, currentSessionId);
                        }
                        showSpecialNotification(NOTIF_ID_URGENT, "CẤP BÁCH: " + message + ". Đang gửi SOS!");
                    } else if (seconds <= 300) { // 5, 4, 3, 2 phút
                        if (currentState.getValue() != SessionState.WARNING) {
                            currentState.postValue(SessionState.WARNING);
                        }
                        showSpecialNotification(NOTIF_ID_WARNING, "CẢNH BÁO: " + message);
                    } else { // 15, 10 phút
                        updateMainNotification(message, false);
                    }
                }
            }

            @Override
            public void onFinish() {
                currentRemainingMillis = 0;
                timeLeftSeconds.postValue(0L);
                currentState.postValue(SessionState.EMERGENCY);
                showSpecialNotification(NOTIF_ID_URGENT, "HẾT GIỜ! Đã gửi tín hiệu SOS khẩn cấp!");
                SmsHelper.sendEmergencyAlert(TrackingForegroundService.this, currentSessionId);
            }
        }.start();
    }

    private void showSpecialNotification(int id, String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(id, buildNotification(text, true));
        }
    }

    private void updateMainNotification(String text, boolean highPriority) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIF_ID_MAIN, buildNotification(text, highPriority));
        }
    }

    private Notification buildNotification(String contentText, boolean isCritical) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nhật Ký Khẩn Cấp")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(!isCritical)
                .setAutoCancel(isCritical);

        if (isCritical) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                   .setDefaults(Notification.DEFAULT_ALL)
                   .setVibrate(new long[]{0, 1000, 500, 1000});
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, 
                    "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationTracking() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (android.location.Location location : locationResult.getLocations()) {
                        saveGpsLog(location);
                    }
                }
            }
        };
        try {
            locationClient.requestLocationUpdates(LocationHelper.createLocationRequest(),
                    locationCallback, Looper.getMainLooper());
        } catch (Exception ignored) {}
    }

    private void saveGpsLog(android.location.Location location) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).sessionDao().insertGpsLog(
                    new GpsLogEntity(currentSessionId, location.getLatitude(), location.getLongitude(), System.currentTimeMillis())
            );
        });
    }

    @Override
    public void onDestroy() {
        currentState.postValue(SessionState.IDLE);
        if (countDownTimer != null) countDownTimer.cancel();
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}