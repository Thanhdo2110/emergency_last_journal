package com.example.emergencylastjournal.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SmsHelper {
    private static final String TAG = "SmsHelper";
    private static final String PREF_NAME = "sos_settings";
    private static final String KEY_SOS_MESSAGE = "sos_message";
    
    private static final String DEFAULT_TEMPLATE = "SOS! Toi gap nguy hiem .Hinh anh cua toi : [Photo] . Vi tri cua toi : [Location]";

    public static void sendEmergencyAlert(Context context, int sessionId) {
        final Context appContext = context.getApplicationContext();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                SessionEntity session = db.sessionDao().getSessionById(sessionId);
                List<ContactEntity> contacts = db.contactDao().getEmergencyContactsSync();

                if (session == null || contacts == null || contacts.isEmpty()) {
                    Log.e(TAG, "Không có người thân nào để gửi SOS.");
                    return;
                }

                // Cập nhật người đã được thông báo vào phiên nhật ký
                String names = contacts.stream().map(c -> c.name).collect(Collectors.joining(", "));
                db.sessionDao().updateNotifiedContacts(sessionId, names);

                SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String template = prefs.getString(KEY_SOS_MESSAGE, DEFAULT_TEMPLATE);

                String savedLocationUrl = "Chưa có dữ liệu";
                if (session.latitude != null && session.longitude != null) {
                    savedLocationUrl = "https://www.google.com/maps/search/?api=1&query=" 
                                      + session.latitude + "," + session.longitude;
                }
                String photoInfo = (session.photoPath != null) ? "Đã chụp và lưu trong Nhật ký" : "Không có";

                String finalBaseMessage = template
                        .replace("[Photo]", photoInfo)
                        .replace("[Location]", savedLocationUrl);

                // Lấy vị trí thực tế và thực hiện gửi
                LocationHelper.getLastLocation(appContext, location -> {
                    // PHẢI CHẠY TRONG LUỒNG NỀN ĐỂ CẬP NHẬT DB
                    Executors.newSingleThreadExecutor().execute(() -> {
                        String fullSms = finalBaseMessage;
                        if (location != null) {
                            fullSms += " . Vị trí hiện tại: https://www.google.com/maps/search/?api=1&query=" 
                                       + location.getLatitude() + "," + location.getLongitude();
                        }

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
                                    smsManager.sendTextMessage(contact.phone, null, fullSms, null, null);
                                    
                                    // CẬP NHẬT DATABASE TẠI ĐÂY (ĐÃ AN TOÀN TRONG LUỒNG NỀN)
                                    contact.sosCount++;
                                    contact.lastSosMessage = fullSms;
                                    db.contactDao().update(contact);
                                    
                                    Log.d(TAG, "Đã lưu trạng thái SOS cho: " + contact.name);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi khi xử lý người thân: " + contact.name, e);
                            }
                        }
                        
                        // Thông báo cho người dùng trên UI
                        new Handler(Looper.getMainLooper()).post(() -> 
                            Toast.makeText(appContext, "Hệ thống đã gửi SOS thành công!", Toast.LENGTH_LONG).show()
                        );
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi hệ thống khẩn cấp", e);
            }
        });
    }
}
