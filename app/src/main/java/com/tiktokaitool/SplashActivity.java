package com.tiktokaitool;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo     = findViewById(R.id.iv_logo);
        View appName  = findViewById(R.id.tv_app_name);
        View tagline  = findViewById(R.id.tv_tagline);
        View progress = findViewById(R.id.progress_bar);

        // Start hidden
        logo.setAlpha(0f);    logo.setScaleX(0.3f); logo.setScaleY(0.3f);
        appName.setAlpha(0f); appName.setTranslationY(40f);
        tagline.setAlpha(0f); tagline.setTranslationY(30f);
        progress.setAlpha(0f);

        // Logo pop in
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(
            ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).setDuration(500),
            ObjectAnimator.ofFloat(logo, "scaleX", 0.3f, 1f).setDuration(600),
            ObjectAnimator.ofFloat(logo, "scaleY", 0.3f, 1f).setDuration(600)
        );
        logoAnim.setInterpolator(new OvershootInterpolator(1.5f));
        logoAnim.setStartDelay(200);

        // Text slide up
        AnimatorSet textAnim = new AnimatorSet();
        textAnim.playSequentially(
            buildFadeUp(appName, 400, 0),
            buildFadeUp(tagline, 350, 0)
        );
        textAnim.setStartDelay(700);

        // Progress fade
        ObjectAnimator progressAnim = ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f);
        progressAnim.setDuration(300);
        progressAnim.setStartDelay(1200);

        AnimatorSet all = new AnimatorSet();
        all.playTogether(logoAnim, textAnim, progressAnim);
        all.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                // Hold 800ms then go to main
                logo.postDelayed(() -> {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }, 800);
            }
        });
        all.start();
    }

    private AnimatorSet buildFadeUp(View v, long dur, long delay) {
        AnimatorSet s = new AnimatorSet();
        s.playTogether(
            ObjectAnimator.ofFloat(v, "alpha", 0f, 1f).setDuration(dur),
            ObjectAnimator.ofFloat(v, "translationY", 40f, 0f).setDuration(dur)
        );
        s.setInterpolator(new DecelerateInterpolator());
        s.setStartDelay(delay);
        return s;
    }
}
