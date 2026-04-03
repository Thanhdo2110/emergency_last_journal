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
                SessionEntity session = (sessionId != -1) ? db.sessionDao().getSessionById(sessionId) : null;
                List<ContactEntity> contacts = db.contactDao().getEmergencyContactsSync();
                UserEntity user = db.userDao().getUserSync();
                
                // Lấy tọa độ GPS cuối cùng từ nhật ký di chuyển (nếu có session)
                GpsLogEntity lastGpsLog = (sessionId != -1) ? db.gpsLogDao().getLastLogForSessionSync(sessionId) : null;

                if (contacts == null || contacts.isEmpty()) {
                    Log.d(TAG, "Không tìm thấy danh bạ để gửi Email.");
                    return;
                }

                // Kiểm tra quyền trước khi lấy vị trí
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
                            if (bestLocation == null && session != null && session.latitude != null) {
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
                    } else if (session != null && session.latitude != null) {
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
        // Khung viền ngoài màu đỏ đậm
        sb.append("<div style='font-family: Arial, sans-serif; border: 3px solid #ff0000; padding: 30px; border-radius: 12px; max-width: 600px; margin: auto;'>");
        
        // Tiêu đề lớn, nổi bật
        sb.append("<h1 style='color: #ff0000; text-align: center; text-transform: uppercase; margin-bottom: 30px; font-size: 26px;'>CẢNH BÁO SOS KHẨN CẤP</h1>");
        
        sb.append("<p style='font-size: 16px;'>Chào bạn,</p>");
        sb.append("<p style='font-size: 16px; line-height: 1.5;'>Người thân của bạn đang gửi tín hiệu cầu cứu khẩn cấp từ ứng dụng <b>Emergency Journal</b>.</p>");
        
        // Khối thông tin người dùng - Chuyên nghiệp hơn
        sb.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px; border: 1px solid #dee2e6; margin: 25px 0;'>");
        sb.append("<h3 style='margin-top: 0; color: #d32f2f; border-bottom: 2px solid #ff0000; padding-bottom: 8px; display: inline-block;'>THÔNG TIN NGƯỜI CẦN TRỢ GIÚP</h3>");
        sb.append("<table style='width: 100%; font-size: 16px; margin-top: 10px;'>");
        sb.append("<tr><td style='width: 40%; font-weight: bold; padding: 5px 0;'>Họ và tên:</td><td>").append(user != null ? user.name : "N/A").append("</td></tr>");
        sb.append("<tr><td style='font-weight: bold; padding: 5px 0;'>Ngày sinh:</td><td>").append(user != null && user.dateOfBirth != null ? user.dateOfBirth : "N/A").append("</td></tr>");
        sb.append("<tr><td style='font-weight: bold; padding: 5px 0;'>Nhóm máu:</td><td>").append(user != null && user.bloodType != null ? user.bloodType : "N/A").append("</td></tr>");
        sb.append("</table>");
        sb.append("</div>");

        // KHỐI VỊ TRÍ - LÀM CỰC KỲ NỔI BẬT VỚI NÚT ĐỎ LỚN
        sb.append("<div style='background-color: #fff5f5; padding: 25px; border: 2px solid #ff4d4d; border-radius: 10px; margin: 25px 0; text-align: center;'>");
        sb.append("<h3 style='margin-top: 0; color: #ff0000; text-transform: uppercase;'>VỊ TRÍ KHẨN CẤP HIỆN TẠI</h3>");
        
        if (location != null) {
            String mapUrl = "https://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "," + location.getLongitude();
            sb.append("<p style='font-size: 20px; margin: 15px 0; color: #333;'><b>Tọa độ:</b> ").append(location.getLatitude()).append(", ").append(location.getLongitude()).append("</p>");
            sb.append("<div style='margin-top: 25px;'>");
            sb.append("<a href='").append(mapUrl).append("' style='background-color: #ff0000; color: #ffffff; padding: 18px 30px; text-decoration: none; font-weight: bold; font-size: 18px; border-radius: 8px; display: inline-block; box-shadow: 0 4px 6px rgba(0,0,0,0.2);'>XEM VỊ TRÍ TRÊN BẢN ĐỒ</a>");
            sb.append("</div>");
            sb.append("<p style='font-size: 13px; color: #666; margin-top: 20px;'><i>(Nhấn vào nút đỏ phía trên để mở Google Maps và tìm đường đến cứu giúp)</i></p>");
        } else {
            sb.append("<p style='color: #d32f2f; font-weight: bold; font-size: 18px; margin: 15px 0;'>KHÔNG XÁC ĐỊNH ĐƯỢC TỌA ĐỘ CHÍNH XÁC</p>");
            sb.append("<p style='font-size: 14px; color: #333;'>Hệ thống hiện không thể lấy được vị trí GPS. Hãy cố gắng liên lạc với người thân ngay lập tức.</p>");
        }
        sb.append("</div>");

        if (session != null) {
            sb.append("<div style='padding: 15px; border-top: 1px solid #eee; background-color: #fffaf0;'>");
            sb.append("<p style='margin: 5px 0;'><b>Lộ trình dự kiến:</b> ").append(session.route != null ? session.route : "Chưa xác định").append("</p>");
            sb.append("</div>");
        }

        // Chú thích cuối email
        sb.append("<p style='color: #777; font-size: 13px; margin-top: 35px; border-top: 2px solid #eee; padding-top: 20px; line-height: 1.4;'>");
        sb.append("<i>Email này được gửi tự động từ hệ thống cứu hộ của Emergency Journal. Hãy hành động ngay để giúp đỡ người thân của bạn.</i>");
        sb.append("</p>");

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
