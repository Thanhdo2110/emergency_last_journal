package com.example.emergencylastjournal.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.emergencylastjournal.data.entity.SessionState;
import com.example.emergencylastjournal.data.repository.SessionRepository;
import com.example.emergencylastjournal.service.TrackingForegroundService;

/**
 * Listens for device boot to restore tracking if a session was active.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Device rebooted. Checking session status...");
            SessionRepository repository = new SessionRepository(context.getApplicationContext());
            
            if (repository.getSessionState() == SessionState.ACTIVE || 
                repository.getSessionState() == SessionState.WARNING ||
                repository.getSessionState() == SessionState.URGENT) {
                
                int sessionId = repository.getActiveSessionId();
                if (sessionId != -1) {
                    Intent serviceIntent = new Intent(context, TrackingForegroundService.class);
                    serviceIntent.putExtra("SESSION_ID", sessionId);
                    context.startForegroundService(serviceIntent);
                }
            }
        }
    }
}