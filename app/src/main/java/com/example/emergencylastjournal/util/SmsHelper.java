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
    
    private static final String DEFAULT_TEMPLATE = "SOS! Toi gap nguy hiem. Vi tri cua toi : [Location]";

    public static void sendEmergencyAlert(Context context, int sessionId) {
        final Context appContext = context.getApplicationContext();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                List<ContactEntity> contacts = db.contactDao().getEmergencyContactsSync();

                if (contacts == null || contacts.isEmpty()) {
                    Log.e(TAG, "Không có người thân nào để gửi SOS.");
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(appContext, "Chưa có danh bạ người thân để gửi SOS!", Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                SessionEntity sessionTemp = null;
                if (sessionId != -1) {
                    sessionTemp = db.sessionDao().getSessionById(sessionId);
                    if (sessionTemp != null) {
                        String names = contacts.stream().map(c -> c.name).collect(Collectors.joining(", "));
                        db.sessionDao().updateNotifiedContacts(sessionId, names);
                    }
                }
                final SessionEntity session = sessionTemp;

                SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String template = prefs.getString(KEY_SOS_MESSAGE, DEFAULT_TEMPLATE);

                String photoInfo = (session != null && session.photoPath != null) ? "Đã chụp và lưu trong Nhật ký" : "Không có";
                
                final String baseTemplate = template.replace("[Photo]", photoInfo);

                // Lấy vị trí thực tế và thực hiện gửi
                LocationHelper.getLastLocation(appContext, location -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        String fullSms = baseTemplate;
                        String locationUrl = "Không xác định";
                        
                        if (location != null) {
                            locationUrl = "https://www.google.com/maps/search/?api=1&query=" 
                                       + location.getLatitude() + "," + location.getLongitude();
                        } else if (session != null && session.latitude != null) {
                            locationUrl = "https://www.google.com/maps/search/?api=1&query=" 
                                       + session.latitude + "," + session.longitude;
                        }
                        
                        fullSms = fullSms.replace("[Location]", locationUrl);

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
                                    
                                    contact.sosCount++;
                                    contact.lastSosMessage = fullSms;
                                    db.contactDao().update(contact);
                                    
                                    Log.d(TAG, "Đã gửi SMS SOS cho: " + contact.name);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi khi gửi SMS cho: " + contact.name, e);
                            }
                        }
                        
                        new Handler(Looper.getMainLooper()).post(() -> 
                            Toast.makeText(appContext, "Hệ thống đã gửi SMS SOS thành công!", Toast.LENGTH_LONG).show()
                        );
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi hệ thống khẩn cấp", e);
            }
        });
    }
}
