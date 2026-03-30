package com.example.emergencylastjournal.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.entity.UserEntity;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailHelper {
    private static final String TAG = "EmailHelper";
    
    // THAY ĐỔI THÔNG TIN TÀI KHOẢN GỬI TẠI ĐÂY (Nên dùng App Password của Gmail)
    private static final String SENDER_EMAIL = "Nguyendo.51947@gmail.com";
    private static final String SENDER_PASSWORD = "vztv kizd qcbg nwdx";

    public static void sendEmergencyEmail(Context context, int sessionId) {
        final Context appContext = context.getApplicationContext();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                SessionEntity session = db.sessionDao().getSessionById(sessionId);
                List<ContactEntity> contacts = db.contactDao().getEmergencyContactsSync();
                UserEntity user = db.userDao().getUserSync();
                
                // Lấy tọa độ GPS cuối cùng từ nhật ký di chuyển
                GpsLogEntity lastGpsLog = db.gpsLogDao().getLastLogForSessionSync(sessionId);

                if (session == null || contacts == null || contacts.isEmpty()) {
                    Log.d(TAG, "Không tìm thấy phiên hoặc danh bạ để gửi Email.");
                    return;
                }

                // Kiểm tra quyền trước khi lấy vị trí (theo tham khảo code bạn gửi)
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationHelper.getLastLocation(appContext, freshLocation -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            // Logic ưu tiên vị trí tươi nhất
                            Location bestLocation = freshLocation;
                            if (bestLocation == null && lastGpsLog != null) {
                                bestLocation = new Location("stored");
                                bestLocation.setLatitude(lastGpsLog.latitude);
                                bestLocation.setLongitude(lastGpsLog.longitude);
                            }
                            if (bestLocation == null && session.latitude != null) {
                                bestLocation = new Location("start");
                                bestLocation.setLatitude(session.latitude);
                                bestLocation.setLongitude(session.longitude);
                            }

                            String emailContent = buildEmailBody(user, session, bestLocation);
                            sendToAllContacts(contacts, user, emailContent);
                        });
                    });
                } else {
                    // Nếu không có quyền lấy vị trí mới, dùng vị trí cuối trong DB hoặc vị trí bắt đầu
                    Location bestLocation = null;
                    if (lastGpsLog != null) {
                        bestLocation = new Location("stored");
                        bestLocation.setLatitude(lastGpsLog.latitude);
                        bestLocation.setLongitude(lastGpsLog.longitude);
                    } else if (session.latitude != null) {
                        bestLocation = new Location("start");
                        bestLocation.setLatitude(session.latitude);
                        bestLocation.setLongitude(session.longitude);
                    }
                    String emailContent = buildEmailBody(user, session, bestLocation);
                    sendToAllContacts(contacts, user, emailContent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi chuẩn bị gửi Email", e);
            }
        });
    }

    private static void sendToAllContacts(List<ContactEntity> contacts, UserEntity user, String content) {
        for (ContactEntity contact : contacts) {
            if (contact.email != null && !contact.email.isEmpty()) {
                String subject = "CẢNH BÁO SOS: " + (user != null ? user.name : "Người thân của bạn") + " ĐANG GẶP NGUY HIỂM";
                sendMail(contact.email, subject, content);
            }
        }
    }

    private static String buildEmailBody(UserEntity user, SessionEntity session, Location location) {
        StringBuilder sb = new StringBuilder();
        // Khung viền ngoài màu đỏ giống ảnh mẫu
        sb.append("<div style='font-family: sans-serif; border: 1px solid #ff0000; padding: 25px; border-radius: 8px; max-width: 600px;'>");
        
        // Tiêu đề căn giữa
        sb.append("<h2 style='color: #ff0000; text-align: center; margin-bottom: 25px;'>CẢNH BÁO KHẨN CẤP (SOS)</h2>");
        
        sb.append("<p style='margin-bottom: 10px;'>Chào bạn,</p>");
        sb.append("<p style='margin-bottom: 20px;'>Đây là thông báo khẩn cấp từ ứng dụng <b>Emergency Journal</b>.</p>");
        
        // Chi tiết phiên di chuyển
        sb.append("<p style='margin-bottom: 5px;'><b>Chi tiết phiên di chuyển:</b></p>");
        sb.append("<p style='margin-top: 0;'>- Lộ trình: ").append(session.route != null ? session.route : "Chưa xác định").append("<br>");
        sb.append("- Trạng thái cuối: ").append(session.status != null ? session.status : "danger").append("</p>");

        // Khối vị trí: Nền hồng nhạt, thanh đỏ bên trái y hệt ảnh bạn gửi
        if (location != null) {
            String mapUrl = "https://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "," + location.getLongitude();
            sb.append("<div style='background-color: #fff5f5; padding: 15px; border-left: 4px solid #ff0000; margin: 20px 0;'>");
            sb.append("<p style='margin: 0; font-weight: bold; font-size: 14px;'>VỊ TRÍ HIỆN TẠI:</p>");
            sb.append("<p style='margin: 5px 0;'>Tọa độ: ").append(location.getLatitude()).append(", ").append(location.getLongitude()).append("</p>");
            sb.append("<a href='").append(mapUrl).append("' style='color: #ff0000; font-weight: bold; text-decoration: underline; font-size: 13px;'>XEM TRÊN GOOGLE MAPS</a>");
            sb.append("</div>");
        } else {
            sb.append("<p style='color: red; margin: 20px 0;'><i>(Không thể xác định tọa độ hiện tại)</i></p>");
        }

        // Chú thích cuối email màu xám, in nghiêng
        sb.append("<p style='color: #777; font-size: 12px; margin-top: 30px;'><i>Email này được gửi tự động vì người dùng đã hết thời gian an toàn mà không xác nhận. Hãy liên lạc với họ ngay lập tức.</i></p>");

        sb.append("</div>");
        
        return sb.toString();
    }

    private static void sendMail(String recipient, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            Transport.send(message);
            Log.d(TAG, "Đã gửi Email thành công tới: " + recipient);
        } catch (MessagingException e) {
            Log.e(TAG, "Lỗi khi gửi Email tới " + recipient, e);
        }
    }
}
