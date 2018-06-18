package com.arterialist.searchsploit.activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arterialist.searchsploit.R;
import com.arterialist.searchsploit.utils.HawkUtil;
import com.arterialist.searchsploit.utils.NetworkUtils;

import butterknife.BindView;
import butterknife.OnClick;
import ir.sohreco.androidfilechooser.ExternalStorageNotAvailableException;
import ir.sohreco.androidfilechooser.FileChooserDialog;

public class ConfigureDatabaseActivity extends BaseActivity implements FileChooserDialog.ChooserListener {

    public static final int PERMISSIONS_REQUEST_CODE = 0x11;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.do_download)
    TextView doDownloadTV;
    @BindView(R.id.download_choice)
    LinearLayout downloadChoiceLL;
    @BindView(R.id.choose_file_text)
    TextView chooseFileTV;
    @BindView(R.id.choose_file)
    Button chooseFileB;
    @BindView(R.id.first_divider)
    View dividerV;
    @BindView(R.id.finish)
    Button finishB;

    public static Intent createIntent(Context context) {
        return new Intent(context, ConfigureDatabaseActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_database);

        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
    }

    @OnClick(R.id.db_no)
    public void onNoLocalDatabaseClick(View view) {
        chooseFileTV.setVisibility(View.GONE);
        chooseFileB.setVisibility(View.GONE);
        dividerV.setBackgroundColor(ContextCompat.getColor(this, R.color.red500));
        animateDivider();
        showView(doDownloadTV);
        showView(downloadChoiceLL);
    }

    private void showView(final View view) {
        view.setVisibility(View.VISIBLE);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setAlpha((Float) animation.getAnimatedValue());
            }
        });
        animator.setDuration(400);
        animator.start();
    }

    @OnClick(R.id.db_yes)
    public void onLocalDatabaseClick(View view) {
        doDownloadTV.setVisibility(View.GONE);
        downloadChoiceLL.setVisibility(View.GONE);
        dividerV.setBackgroundColor(ContextCompat.getColor(this, R.color.lime500));
        animateDivider();
        showView(chooseFileTV);
        showView(chooseFileB);
    }

    @OnClick(R.id.download_no)
    public void onNotDownloadDatabaseClick(View view) {
        new AlertDialog.Builder(this)
                .setMessage("Come back later to finish setup. Bye)")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).show();
    }

    @OnClick(R.id.download_yes)
    public void onDownloadDatabaseClick(View view) {
        if (NetworkUtils.isOnline(this)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                NetworkUtils.downloadFile(this, NetworkUtils.DownloadOptions.databaseOptions(this));
                HawkUtil.set(HawkUtil.DATABASE_PATH_KEY, "/storage/emulated/0/Documents/Searchsploit/database.txt");
                showView(finishB);

                Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.text_normal_no_network)
                    .setMessage(R.string.text_normal_connect_to_internet)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    @OnClick(R.id.choose_file)
    public void onChooseFileClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            FileChooserDialog.Builder builder = new FileChooserDialog.Builder(FileChooserDialog.ChooserType.FILE_CHOOSER, this)
                    .setFileFormats(new String[]{".txt", ".csv"})
                    .setInitialDirectory(Environment.getExternalStorageDirectory());
            try {
                builder.build().show(getSupportFragmentManager(), null);
            } catch (ExternalStorageNotAvailableException e) {
                Toast.makeText(this, "Storage not available!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @OnClick(R.id.finish)
    public void onFinishClick(View view) {
        HawkUtil.set(HawkUtil.DATABASE_CONFIGURED_KEY, true);
        startActivity(MainActivity.createIntent(this));
        finish();
    }

    public void animateDivider() {
        ValueAnimator animator = ValueAnimator.ofInt(0, getResources().getDisplayMetrics().widthPixels);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = dividerV.getLayoutParams();
                layoutParams.width = animatedValue;
                dividerV.setLayoutParams(layoutParams);
            }
        });
        animator.setDuration(700);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    @Override
    public void onSelect(String path) {
        HawkUtil.set(HawkUtil.DATABASE_PATH_KEY, path);
        showView(finishB);
    }
}
