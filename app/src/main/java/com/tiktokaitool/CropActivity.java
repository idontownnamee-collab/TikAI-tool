package com.tiktokaitool;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class CropActivity extends AppCompatActivity {
    private CropView cropView;
    private String imagePath;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_crop);
        imagePath = getIntent().getStringExtra("path");
        cropView  = findViewById(R.id.crop_view);
        if (imagePath == null) { finish(); return; }
        Bitmap bmp = BitmapFactory.decodeFile(imagePath);
        if (bmp == null) { Toast.makeText(this,"Failed to load screenshot",Toast.LENGTH_SHORT).show(); finish(); return; }
        cropView.setBitmap(bmp);
        findViewById(R.id.btn_analyze).setOnClickListener(v -> analyze());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_full).setOnClickListener(v -> openResult(imagePath));
    }

    private void analyze() {
        Bitmap cropped = cropView.getCropped();
        if (cropped == null) { Toast.makeText(this,"No image",Toast.LENGTH_SHORT).show(); return; }
        try {
            File f = new File(getCacheDir(), "crop_"+System.currentTimeMillis()+".jpg");
            try (FileOutputStream fo = new FileOutputStream(f)) {
                cropped.compress(Bitmap.CompressFormat.JPEG, 90, fo);
            }
            openResult(f.getAbsolutePath());
        } catch (Exception e) { Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show(); }
    }

    private void openResult(String path) {
        startActivity(new Intent(this, ResultActivity.class).putExtra("path", path));
        finish();
    }
}
