package com.tiktokaitool;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

public class ScreenCaptureActivity extends Activity {
    private static final int REQ = 200;
    private MediaProjectionManager mpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ) {
            if (resultCode == RESULT_OK && data != null) {
                // Store projection and trigger capture
                MediaProjection projection = mpm.getMediaProjection(resultCode, data);
                OverlayService.sProjection = projection;
                // Tell service to capture now
                if (OverlayService.isRunning) {
                    Intent i = new Intent(this, OverlayService.class);
                    i.setAction("CAPTURE");
                    startService(i);
                }
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                // Show overlay again
                Intent i = new Intent(this, OverlayService.class);
                i.setAction("SHOW");
                startService(i);
            }
            finish();
        }
    }
}
