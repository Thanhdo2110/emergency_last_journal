package com.example.emergencylastjournal.util;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import java.util.List;
import java.util.concurrent.Executors;

public class SmsHelper {
    private static final String TAG = "SmsHelper";

    public static void sendEmergencyAlert(Context context, int sessionId) {
        // Đảm bảo lấy ApplicationContext để tránh rò rỉ bộ nhớ hoặc crash khi Activity bị đóng
        final Context appContext = context.getApplicationContext();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                SessionEntity session = db.sessionDao().getSessionById(sessionId);
                List<ContactEntity> contacts = db.contactDao().getEmergencyContactsSync();

                if (session == null || contacts == null || contacts.isEmpty()) {
                    Log.e(TAG, "Không tìm thấy phiên làm việc hoặc danh bạ khẩn cấp.");
                    return;
                }

                LocationHelper.getLastLocation(appContext, location -> {
                    String locationUrl = "";
                    if (location != null) {
                        locationUrl = "\n Vị trí: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                    }

                    String message = "CỨU TÔI VỚI! Tôi đang gặp nguy hiểm. Lộ trình: " + session.route + locationUrl;
                    
                    // Sử dụng cách lấy SmsManager an toàn hơn cho mọi phiên bản Android
                    SmsManager smsManager;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        smsManager = appContext.getSystemService(SmsManager.class);
                    } else {
                        smsManager = SmsManager.getDefault();
                    }

                    if (smsManager == null) return;

                    for (ContactEntity contact : contacts) {
                        try {
                            if (contact.phone != null && !contact.phone.isEmpty()) {
                                smsManager.sendTextMessage(contact.phone, null, message, null, null);
                                Log.d(TAG, "Đã gửi SMS tới: " + contact.phone);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi gửi SMS tới " + contact.phone, e);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi hệ thống khi gửi SOS", e);
            }
        });
    }
}