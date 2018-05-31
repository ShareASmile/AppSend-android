package com.tomclaw.appteka;

import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.tomclaw.appteka.core.GlideApp;
import com.tomclaw.appteka.main.controller.CountController;
import com.tomclaw.appteka.main.controller.UploadController;
import com.tomclaw.appteka.main.item.CommonItem;
import com.tomclaw.appteka.util.FileHelper;
import com.tomclaw.appteka.util.IntentHelper;
import com.tomclaw.appteka.util.StringUtil;
import com.tomclaw.appteka.util.ThemeHelper;

import static com.tomclaw.appteka.util.IntentHelper.shareUrl;

/**
 * Created by ivsolkin on 02.01.17.
 */
public class UploadActivity extends AppCompatActivity implements UploadController.UploadCallback {

    private static final long DEBOUNCE_DELAY = 100;
    public static final String UPLOAD_ITEM = "app_info";

    private CommonItem item;
    private ImageView appIcon;
    private TextView appLabel;
    private TextView appPackage;
    private TextView appVersion;
    private TextView appSize;
    private ProgressBar progress;
    private TextView percent;

    private ViewSwitcher viewSwitcher;

    private String url;

    private transient long progressUpdateTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.updateTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.upload_activity);
        ThemeHelper.updateStatusBar(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        boolean isCreateInstance = savedInstanceState == null;
        if (isCreateInstance) {
            item = getIntent().getParcelableExtra(UPLOAD_ITEM);
        } else {
            item = savedInstanceState.getParcelable(UPLOAD_ITEM);
        }

        appIcon = findViewById(R.id.app_icon);
        appLabel = findViewById(R.id.app_label);
        appPackage = findViewById(R.id.app_package);
        appVersion = findViewById(R.id.app_version);
        appSize = findViewById(R.id.app_size);
        progress = findViewById(R.id.progress);
        percent = findViewById(R.id.percent);
        viewSwitcher = findViewById(R.id.view_switcher);
        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        findViewById(R.id.button_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareUrl(UploadActivity.this, formatText());
            }
        });
        findViewById(R.id.button_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringUtil.copyStringToClipboard(UploadActivity.this, formatText());
                Toast.makeText(UploadActivity.this, R.string.url_copied, Toast.LENGTH_SHORT).show();
            }
        });

        PackageInfo packageInfo = item.getPackageInfo();

        if (packageInfo != null) {
            GlideApp.with(this)
                    .load(packageInfo)
                    .into(appIcon);
        }

        appLabel.setText(item.getLabel());
        appPackage.setText(item.getPackageName());
        String size = FileHelper.formatBytes(getResources(), item.getSize());
        appSize.setText(getString(R.string.upload_size, size));
        appVersion.setText(item.getVersion());

        if (isCreateInstance) {
            UploadController.getInstance().upload(item);
        }
    }

    private String formatText() {
        return IntentHelper.formatText(getResources(), url, item.getLabel(), item.getSize());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        UploadController.getInstance().onAttach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        UploadController.getInstance().onDetach(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(UPLOAD_ITEM, item);
    }

    @Override
    public void onProgress(int percent) {
        if (System.currentTimeMillis() - progressUpdateTime >= DEBOUNCE_DELAY) {
            progressUpdateTime = System.currentTimeMillis();
            this.percent.setText(getString(R.string.percent, percent));
            progress.setProgress(percent);
        }
    }

    @Override
    public void onUploaded() {
        percent.setText(R.string.obtaining_link_message);
        progress.setIndeterminate(true);
    }

    @Override
    public void onCompleted(String url) {
        this.url = url;
        viewSwitcher.setDisplayedChild(1);
        CountController.getInstance().load(this);
    }

    @Override
    public void onError() {
        Toast.makeText(this, R.string.uploading_error, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (UploadController.getInstance().isCompleted()) {
            finish();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.cancel_upload_title))
                    .setMessage(getString(R.string.cancel_upload_text))
                    .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UploadController.getInstance().cancel();
                            finish();
                        }
                    })
                    .setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }
}
