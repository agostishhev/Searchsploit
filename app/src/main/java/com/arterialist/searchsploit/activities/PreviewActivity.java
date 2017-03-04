package com.arterialist.searchsploit.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.arterialist.searchsploit.R;

import butterknife.Bind;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class PreviewActivity extends BaseActivity {

    public static final String EXTRA_FILE_TYPE = "EXTRA_FILE_TYPE";
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.title)
    TextView titleTV;
    @Bind(R.id.previewText)
    TextView previewTV;
    @Bind(R.id.settingsBar)
    LinearLayout settingsBar;

    @Bind(R.id.themeText)
    TextView themeTextTV;

    public static final String EXTRA_PREVIEW_TEXT = "EXTRA_PREVIEW_TEXT";
    public static final String EXTRA_EXPLOIT_NAME = "EXTRA_EXPLOIT_NAME";

    public static Intent createIntent(Context context, String previewText, String exploitName, String fileType) {
        Intent intent = new Intent(context, PreviewActivity.class);
        intent.putExtra(EXTRA_PREVIEW_TEXT, previewText);
        intent.putExtra(EXTRA_EXPLOIT_NAME, exploitName);
        intent.putExtra(EXTRA_FILE_TYPE, fileType);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String content = intent.getStringExtra(EXTRA_PREVIEW_TEXT);
        String title = intent.getStringExtra(EXTRA_EXPLOIT_NAME);
        titleTV.setText(String.format("%s...", title.substring(0, title.length() > 25 ? 25 : title.length())));
        previewTV.setText(content);
        previewTV.setMovementMethod(new ScrollingMovementMethod());
    }

    @OnClick(R.id.textSizeDown)
    public void onTextSizeDown() {
        previewTV.setTextSize((previewTV.getTextSize() - 1 == 0 ? previewTV.getTextSize() : previewTV.getTextSize() - 1) / getResources().getDisplayMetrics().scaledDensity);
    }

    @OnClick(R.id.textSizeUp)
    public void onTextSizeUp() {
        previewTV.setTextSize((previewTV.getTextSize() + 1) / getResources().getDisplayMetrics().scaledDensity);
    }

    @OnCheckedChanged(R.id.themeSwitch)
    public void onThemeSwitchCheckedChanged(boolean checked) {
        int grey300 = ContextCompat.getColor(this, R.color.grey300);
        if (checked) {
            themeTextTV.setText(R.string.text_simple_dark);
            previewTV.setBackgroundColor(ContextCompat.getColor(this, R.color.blueGrey900));
            previewTV.setTextColor(grey300);
            settingsBar.setBackgroundColor(ContextCompat.getColor(this, R.color.grey900));
            changeSettingsBatTextColors(grey300);
        } else {
            int grey700 = ContextCompat.getColor(this, R.color.grey700);
            themeTextTV.setText(R.string.text_simple_light);
            previewTV.setBackgroundColor(Color.WHITE);
            previewTV.setTextColor(grey700);
            settingsBar.setBackgroundColor(grey300);
            changeSettingsBatTextColors(grey700);
        }
    }

    private void changeSettingsBatTextColors(int color) {
        for (int index = 0; index < settingsBar.getChildCount(); index++) {
            View childAt = settingsBar.getChildAt(index);
            if (childAt instanceof TextView) {
                ((TextView) childAt).setTextColor(color);
            }
        }

    }
}
