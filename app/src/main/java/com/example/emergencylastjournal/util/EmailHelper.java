package com.example.emergencylastjournal.util;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.example.emergencylastjournal.R;
import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.example.emergencylastjournal.data.entity.GpsLogEntity;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.entity.UserEntity;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Executors;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailHelper {
    private static final String TAG = "EmailHelper";
    
    private static final String SENDER_EMAIL = "emergencylastjournal@gmail.com";
    private static final String SENDER_PASSWORD = "amcu peah vpsi opis";

    public static void sendEmergencyEmail(Context context, int sessionId) {
        // Lấy localized context để đảm bảo ngôn ngữ hiển thị đúng trong Email
        final Context localizedContext = getLocalizedContext(context.getApplicationContext());
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(localizedContext);
                SessionEntity session = (sessionId != -1) ? db.sessionDao().getSessionById(sessionId) : null;
                List<ContactEntity> contacts = db.contactDao().getEmergencyContactsSync();
                UserEntity user = db.userDao().getUserSync();
                GpsLogEntity lastGpsLog = (sessionId != -1) ? db.gpsLogDao().getLastLogForSessionSync(sessionId) : null;

                if (contacts == null || contacts.isEmpty()) {
                    Log.d(TAG, "Không tìm thấy danh bạ để gửi Email.");
                    return;
                }

                if (ContextCompat.checkSelfPermission(localizedContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationHelper.getLastLocation(localizedContext, freshLocation -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
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

                            String emailContent = buildEmailBody(localizedContext, user, session, bestLocation);
                            sendToAllContacts(localizedContext, contacts, user, emailContent, (sessionId != -1 && session != null) ? session.photoPath : null);
                        });
                    });
                } else {
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
                    String emailContent = buildEmailBody(localizedContext, user, session, bestLocation);
                    sendToAllContacts(localizedContext, contacts, user, emailContent, (sessionId != -1 && session != null) ? session.photoPath : null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi chuẩn bị gửi Email", e);
            }
        });
    }

    private static Context getLocalizedContext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String language = prefs.getString("language", "Vietnam");
        String langCode = language.equalsIgnoreCase("English") ? "en" : "vi";
        
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    private static void sendToAllContacts(Context context, List<ContactEntity> contacts, UserEntity user, String content, String photoPath) {
        String userName = (user != null && user.name != null && !user.name.isEmpty()) ? user.name : context.getString(R.string.email_unknown_relative);
        String subject = context.getString(R.string.email_subject_sos, userName);

        for (ContactEntity contact : contacts) {
            if (contact.email != null && !contact.email.isEmpty()) {
                sendMail(contact.email, subject, content, photoPath);
            }
        }
    }

    private static String buildEmailBody(Context context, UserEntity user, SessionEntity session, Location location) {
        StringBuilder sb = new StringBuilder();
        String na = context.getString(R.string.email_not_available);

        sb.append("<div style='font-family: sans-serif; border: 3px solid #ff0000; padding: 25px; border-radius: 12px; max-width: 600px; margin: auto;'>");
        sb.append("<h1 style='color: #ff0000; text-align: center; text-transform: uppercase; margin-bottom: 20px; font-size: 24px;'>")
          .append(context.getString(R.string.email_header_sos)).append("</h1>");
        
        sb.append("<div style='background-color: #f8f9fa; padding: 15px; border-radius: 8px; border: 1px solid #dee2e6; margin-bottom: 20px;'>");
        sb.append("<p style='margin: 0; font-weight: bold; color: #d32f2f; font-size: 16px;'>")
          .append(context.getString(R.string.email_info_title)).append("</p>");
        
        sb.append("<p style='margin: 5px 0;'>- ").append(context.getString(R.string.email_full_name)).append(" <b>")
          .append(user != null ? user.name : na).append("</b></p>");
        
        sb.append("<p style='margin: 5px 0;'>- ").append(context.getString(R.string.email_dob)).append(" ")
          .append(user != null && user.dateOfBirth != null ? user.dateOfBirth : na).append("</p>");
        
        if (user != null && user.bloodType != null) {
            sb.append("<p style='margin: 5px 0;'>- ").append(context.getString(R.string.email_blood_type)).append(" ")
              .append(user.bloodType).append("</p>");
        }
        sb.append("</div>");

        sb.append("<div style='background-color: #fff5f5; padding: 20px; border: 2px solid #ff4d4d; border-radius: 10px; margin-bottom: 20px; text-align: center;'>");
        sb.append("<h3 style='margin-top: 0; color: #ff0000; text-transform: uppercase; font-size: 18px;'>")
          .append(context.getString(R.string.email_location_title)).append("</h3>");
        
        if (location != null) {
            String mapUrl = "https://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "," + location.getLongitude();
            sb.append("<p style='font-size: 18px; margin: 10px 0; color: #333;'>")
              .append(context.getString(R.string.email_coordinates)).append(" <b>")
              .append(location.getLatitude()).append(", ").append(location.getLongitude()).append("</b></p>");
            sb.append("<a href='").append(mapUrl).append("' style='background-color: #ff0000; color: #ffffff; padding: 15px 25px; text-decoration: none; font-weight: bold; font-size: 16px; border-radius: 8px; display: inline-block; margin-top: 10px;'>")
              .append(context.getString(R.string.email_open_maps)).append("</a>");
        } else {
            sb.append("<p style='color: #d32f2f; font-weight: bold; font-size: 16px; margin: 10px 0;'>")
              .append(context.getString(R.string.email_location_unknown)).append("</p>");
        }
        sb.append("</div>");

        if (session != null && session.photoPath != null) {
            sb.append("<div style='text-align: center; margin-bottom: 20px;'>");
            sb.append("<p style='color: #333; font-weight: bold; text-transform: uppercase; font-size: 14px;'>")
              .append(context.getString(R.string.email_evidence_title)).append("</p>");
            sb.append("<img src='cid:image_evidence' style='max-width: 100%; height: auto; border-radius: 8px; border: 1px solid #ddd;' alt='Evidence Image' />");
            sb.append("</div>");
        }

        if (session != null) {
            sb.append("<div style='padding: 12px; border-top: 1px solid #eee; background-color: #fffaf0; font-size: 14px;'>");
            sb.append("<b>").append(context.getString(R.string.email_route)).append("</b> ")
              .append(session.route != null ? session.route : context.getString(R.string.email_route_not_available)).append("</p>");
            sb.append("</div>");
        }

        sb.append("<p style='color: #777; font-size: 12px; margin-top: 25px; border-top: 1px solid #eee; padding-top: 15px; line-height: 1.4; text-align: center;'>");
        sb.append("<i>").append(context.getString(R.string.email_footer)).append("</i>");
        sb.append("</p>");

        sb.append("</div>");
        
        return sb.toString();
    }

    private static void sendMail(String recipient, String subject, String content, String photoPath) {
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

            Multipart multipart = new MimeMultipart();
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(content, "text/html; charset=utf-8");
            multipart.addBodyPart(messageBodyPart);

            if (photoPath != null && !photoPath.isEmpty()) {
                File photoFile = new File(photoPath);
                if (photoFile.exists()) {
                    MimeBodyPart imagePart = new MimeBodyPart();
                    DataSource fds = new FileDataSource(photoPath);
                    imagePart.setDataHandler(new DataHandler(fds));
                    imagePart.setHeader("Content-ID", "<image_evidence>");
                    imagePart.setFileName(photoFile.getName());
                    multipart.addBodyPart(imagePart);
                }
            }

            message.setContent(multipart);
            Transport.send(message);
            Log.d(TAG, "Đã gửi Email thành công tới: " + recipient);
        } catch (MessagingException e) {
            Log.e(TAG, "Lỗi khi gửi Email tới " + recipient, e);
        }
    }
}
