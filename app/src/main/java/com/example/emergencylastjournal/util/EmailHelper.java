package com.example.emergencylastjournal.util;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.ContactEntity;
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

                if (session == null || contacts == null || contacts.isEmpty()) {
                    Log.d(TAG, "Không tìm thấy phiên hoặc danh bạ khẩn cấp để gửi Email.");
                    return;
                }

                LocationHelper.getLastLocation(appContext, location -> {
                    // Chạy việc gửi email trong thread riêng vì LocationHelper callback thường ở Main UI
                    Executors.newSingleThreadExecutor().execute(() -> {
                        String emailContent = buildEmailBody(user, session, location);
                        
                        for (ContactEntity contact : contacts) {
                            if (contact.email != null && !contact.email.isEmpty()) {
                                String subject = "CẢNH BÁO SOS: " + (user != null ? user.name : "Người thân của bạn") + " ĐANG GẶP NGUY HIỂM";
                                sendMail(contact.email, subject, emailContent);
                            }
                        }
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi chuẩn bị gửi Email", e);
            }
        });
    }

    private static String buildEmailBody(UserEntity user, SessionEntity session, Location location) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: Arial, sans-serif; border: 2px solid #ff0000; padding: 20px; border-radius: 10px;'>");
        sb.append("<h2 style='color: #ff0000; text-align: center;'>CẢNH BÁO KHẨN CẤP (SOS)</h2>");
        sb.append("<p>Chào bạn,</p>");
        sb.append("<p>Đây là thông báo khẩn cấp từ ứng dụng <b>Emergency Journal</b>.</p>");
        
        if (user != null) {
            sb.append("<p><b>Thông tin người dùng:</b><br>");
            sb.append("- Họ tên: ").append(user.name).append("<br>");
            sb.append("- Nhóm máu: ").append(user.bloodType != null ? user.bloodType : "Không rõ").append("<br>");
            sb.append("- Ghi chú y tế: ").append(user.emergencyNotes != null ? user.emergencyNotes : "Không có").append("</p>");
        }

        sb.append("<p><b>Chi tiết phiên di chuyển:</b><br>");
        sb.append("- Lộ trình: ").append(session.route != null ? session.route : "Không xác định").append("<br>");
        sb.append("- Trạng thái cuối: ").append(session.status).append("</p>");

        if (location != null) {
            String mapUrl = "https://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "," + location.getLongitude();
            sb.append("<p style='background-color: #fff3f3; padding: 10px; border-left: 5px solid #ff0000;'>");
            sb.append("<b>VỊ TRÍ HIỆN TẠI:</b><br>");
            sb.append("Tọa độ: ").append(location.getLatitude()).append(", ").append(location.getLongitude()).append("<br>");
            sb.append("<a href='").append(mapUrl).append("' style='color: #d32f2f; font-weight: bold;'>XEM TRÊN GOOGLE MAPS</a>");
            sb.append("</p>");
        } else if (session.latitude != null && session.longitude != null) {
            String mapUrl = "https://www.google.com/maps/search/?api=1&query=" + session.latitude + "," + session.longitude;
            sb.append("<p><b>Vị trí lúc bắt đầu phiên:</b><br>");
            sb.append("<a href='").append(mapUrl).append("'>Xem vị trí khởi hành</a></p>");
        }

        sb.append("<p style='color: #555; font-size: 12px; margin-top: 20px;'><i>Email này được gửi tự động vì người dùng đã hết thời gian an toàn mà không xác nhận. Hãy liên lạc với họ ngay lập tức.</i></p>");
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
