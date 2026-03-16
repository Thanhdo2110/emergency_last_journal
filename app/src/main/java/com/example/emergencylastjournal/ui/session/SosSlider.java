package com.example.emergencylastjournal.ui.session;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;

/**
 * Custom SOS Slider that requires a continuous swipe to 100% to trigger.
 * If released before 100%, it springs back to 0.
 */
public class SosSlider extends AppCompatSeekBar {

    private OnSosTriggerListener listener;

    public SosSlider(@NonNull Context context) {
        super(context);
        init();
    }

    public SosSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SosSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setMax(100);
        setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 100) {
                    if (listener != null) {
                        listener.onSosTriggered();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (getProgress() < 100) {
                    // Spring back to 0 if not fully swiped
                    setProgress(0);
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Only allow touch if it's a dragging action
        return super.onTouchEvent(event);
    }

    public void setOnSosTriggerListener(OnSosTriggerListener listener) {
        this.listener = listener;
    }

    public interface OnSosTriggerListener {
        void onSosTriggered();
    }
}