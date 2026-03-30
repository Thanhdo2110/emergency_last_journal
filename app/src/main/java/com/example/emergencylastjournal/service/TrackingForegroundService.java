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
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.entity.SessionState;
import com.example.emergencylastjournal.util.LocationHelper;
import com.example.emergencylastjournal.util.EmailHelper;
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
    private boolean isEmailSent = false;
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
                extendTimer(600000); 
            } else {
                currentSessionId = intent.getIntExtra("SESSION_ID", -1);
                int durationSeconds = intent.getIntExtra("DURATION_SECONDS", 0);
                
                if (durationSeconds > 0) {
                    isEmailSent = false;
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
            isEmailSent = false;
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

                if (seconds == 60) {
                    if (currentState.getValue() != SessionState.URGENT) {
                        currentState.postValue(SessionState.URGENT);
                    }
                    showSpecialNotification(NOTIF_ID_URGENT, "CẤP BÁCH: Còn 1 phút! Sắp gửi Email cảnh báo!");
                } else if (seconds <= 300 && seconds > 60) {
                    if (currentState.getValue() != SessionState.WARNING) {
                        currentState.postValue(SessionState.WARNING);
                    }
                    if (seconds % 60 == 0) {
                        showSpecialNotification(NOTIF_ID_WARNING, "CẢNH BÁO: Còn " + (seconds/60) + " phút bảo vệ");
                    }
                }
            }

            @Override
            public void onFinish() {
                currentRemainingMillis = 0;
                timeLeftSeconds.postValue(0L);
                currentState.postValue(SessionState.EMERGENCY);
                
                if (!isEmailSent) {
                    isEmailSent = true;
                    showSpecialNotification(NOTIF_ID_URGENT, "HẾT GIỜ! Đã gửi Email SOS tới người thân!");
                    updateSessionOutcome("emergency");
                    
                    // CHUYỂN SANG GỬI EMAIL THAY VÌ SMS
                    EmailHelper.sendEmergencyEmail(TrackingForegroundService.this, currentSessionId);
                }
            }
        }.start();
    }

    private void updateSessionOutcome(String outcome) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            SessionEntity session = db.sessionDao().getSessionById(currentSessionId);
            if (session != null) {
                session.outcome = outcome;
                session.endedAt = System.currentTimeMillis();
                db.sessionDao().update(session);
            }
        });
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
            AppDatabase db = AppDatabase.getInstance(this);
            if (db != null) {
                db.sessionDao().insertGpsLog(
                        new GpsLogEntity(currentSessionId, location.getLatitude(), location.getLongitude(), System.currentTimeMillis())
                );
            }
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
