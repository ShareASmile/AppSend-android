package com.tomclaw.appteka;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.flurry.android.FlurryAgent;
import com.greysonparrelli.permiso.Permiso;
import com.greysonparrelli.permiso.PermisoActivity;
import com.tomclaw.appteka.main.controller.DownloadController;
import com.tomclaw.appteka.main.dto.StoreInfo;
import com.tomclaw.appteka.main.dto.StoreVersion;
import com.tomclaw.appteka.main.item.StoreItem;
import com.tomclaw.appteka.main.view.PlayView;
import com.tomclaw.appteka.util.FileHelper;
import com.tomclaw.appteka.util.IntentHelper;
import com.tomclaw.appteka.util.LocaleHelper;
import com.tomclaw.appteka.util.PermissionHelper;
import com.tomclaw.appteka.util.PreferenceHelper;
import com.tomclaw.appteka.util.StringUtil;
import com.tomclaw.appteka.util.ThemeHelper;
import com.tomclaw.appteka.util.TimeHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.appteka.util.FileHelper.getExternalDirectory;
import static com.tomclaw.appteka.util.IntentHelper.formatText;
import static com.tomclaw.appteka.util.IntentHelper.openGooglePlay;
import static com.tomclaw.appteka.util.IntentHelper.shareUrl;
import static com.tomclaw.appteka.util.PermissionHelper.getPermissionSmallInfo;

/**
 * Created by ivsolkin on 14.01.17.
 */
public class DownloadActivity extends PermisoActivity implements DownloadController.DownloadCallback {

    public static final String STORE_APP_ID = "app_id";
    public static final String STORE_APP_LABEL = "app_label";

    private static final int MAX_PERMISSIONS_COUNT = 3;
    private static final long DEBOUNCE_DELAY = 100;

    private TimeHelper timeHelper;

    private String appId;
    private String appLabel;

    private ViewFlipper viewFlipper;
    private ImageView iconView;
    private TextView labelView;
    private TextView packageView;
    private PlayView downloadsView;
    private PlayView sizeView;
    private PlayView minAndroidView;
    private RelativeLayout permissionsBlock;
    private ViewGroup permissionsContainer;
    private TextView versionView;
    private TextView uploadedTimeView;
    private TextView checksumView;
    private View shadowView;
    private View readMoreButton;
    private View otherVersionsTitle;
    private ViewGroup versionsContainer;
    private ViewFlipper buttonsSwitcher;
    private Button buttonOne;
    private Button buttonFirst;
    private Button buttonSecond;
    private ProgressBar progress;
    private TextView extraAccess;
    private SwipeRefreshLayout swipeRefresh;

    private StoreInfo info;

    private transient long progressUpdateTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.updateTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.download_activity);
        ThemeHelper.updateStatusBar(this);

        timeHelper = new TimeHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        boolean isCreateInstance = savedInstanceState == null;
        if (isCreateInstance) {
            Uri data = getIntent().getData();
            if (data != null) {
                appId = data.getQueryParameter("id");
                appLabel = getString(R.string.download);
            } else if (TextUtils.isEmpty(appId)) {
                appId = getIntent().getStringExtra(STORE_APP_ID);
                appLabel = getIntent().getStringExtra(STORE_APP_LABEL);
            }
        } else {
            appId = savedInstanceState.getString(STORE_APP_ID);
            appLabel = savedInstanceState.getString(STORE_APP_LABEL);
        }
        if (TextUtils.isEmpty(appId)) {
            openStore();
            finish();
        }

        setTitle(appLabel);

        viewFlipper = findViewById(R.id.view_flipper);
        iconView = findViewById(R.id.app_icon);
        labelView = findViewById(R.id.app_label);
        packageView = findViewById(R.id.app_package);
        downloadsView = findViewById(R.id.app_downloads);
        sizeView = findViewById(R.id.app_size);
        minAndroidView = findViewById(R.id.min_android);
        permissionsBlock = findViewById(R.id.permissions_block);
        permissionsContainer = findViewById(R.id.permissions_container);
        versionView = findViewById(R.id.app_version);
        uploadedTimeView = findViewById(R.id.uploaded_time);
        checksumView = findViewById(R.id.app_checksum);
        shadowView = findViewById(R.id.read_more_shadow);
        readMoreButton = findViewById(R.id.read_more_button);
        otherVersionsTitle = findViewById(R.id.other_versions_title);
        versionsContainer = findViewById(R.id.app_versions);
        buttonsSwitcher = findViewById(R.id.buttons_switcher);
        buttonOne = findViewById(R.id.button_one);
        buttonFirst = findViewById(R.id.button_first);
        buttonSecond = findViewById(R.id.button_second);
        progress = findViewById(R.id.progress);
        extraAccess = findViewById(R.id.extra_access);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadInfo();
            }
        });
        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDownload();
            }
        });
        findViewById(R.id.button_retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadInfo();
            }
        });
        findViewById(R.id.share_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("Store app: share");
                String text = formatText(getResources(), info.getUrl(),
                        LocaleHelper.getLocalizedLabel(info.getItem()), info.getItem().getSize());
                shareUrl(DownloadActivity.this, text);
            }
        });
        findViewById(R.id.play_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("Store app: Google Play");
                openGooglePlay(DownloadActivity.this, info.getItem().getPackageName());
            }
        });
        View.OnClickListener checksumClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("Store app: copy SHA1");
                StringUtil.copyStringToClipboard(
                        DownloadActivity.this,
                        checksumView.getText().toString(),
                        R.string.checksum_copied);
            }
        };
        findViewById(R.id.app_checksum_title).setOnClickListener(checksumClickListener);
        checksumView.setOnClickListener(checksumClickListener);

        if (isCreateInstance) {
            loadInfo();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.download_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finishAttempt(new Runnable() {
                    @Override
                    public void run() {
                        openStore();
                    }
                });
                break;
            }
            case R.id.abuse: {
                onAbusePressed();
                break;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        finishAttempt(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DownloadController.getInstance().onAttach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        DownloadController.getInstance().onDetach(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindButtons();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STORE_APP_ID, appId);
        outState.putString(STORE_APP_LABEL, appLabel);
    }

    private void loadInfo() {
        DownloadController.getInstance().loadInfo(appId);
    }

    private void reloadInfo() {
        if (!DownloadController.getInstance().isStarted()) {
            loadInfo();
        }
    }

    private void openStore() {
        Intent intent = new Intent(DownloadActivity.this, MainActivity.class)
                .setAction(MainActivity.ACTION_CLOUD)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @SuppressLint("DefaultLocale")
    private void bindStoreItem(StoreInfo info) {
        this.info = info;
        StoreItem item = info.getItem();
        Glide.with(this)
                .load(item.getIcon())
                .into(iconView);
        String sizeText;
        int sizeFactor;
        long bytes = item.getSize();
        if (bytes < 1024 * 1024) {
            sizeText = String.format("%d", bytes / 1024);
            sizeFactor = R.string.kilobytes;
        } else if (bytes < 10 * 1024 * 1024) {
            sizeText = String.format("%.1f", bytes / 1024.0f / 1024.0f);
            sizeFactor = R.string.megabytes;
        } else {
            sizeText = String.format("%d", bytes / 1024 / 1024);
            sizeFactor = R.string.megabytes;
        }
        labelView.setText(LocaleHelper.getLocalizedLabel(item));
        packageView.setText(item.getPackageName());
        downloadsView.setCount(String.valueOf(item.getDownloads()));
        sizeView.setCount(sizeText);
        sizeView.setDescription(getString(sizeFactor));
        minAndroidView.setCount(item.getAndroidVersion());
        versionView.setText(getString(R.string.app_version_format, item.getVersion(), item.getVersionCode()));
        uploadedTimeView.setText(timeHelper.getFormattedDate(item.getTime()));
        checksumView.setText(item.getSha1());
        bindButtons(item.getPackageName(), item.getVersionCode());
        bindPermissions(item.getPermissions());
        bindVersions(info.getVersions(), item.getAppId(), item.getVersionCode());
        appLabel = labelView.getText().toString();
        setTitle(appLabel);
    }

    private void bindButtons() {
        if (info != null) {
            StoreItem item = info.getItem();
            bindButtons(item.getPackageName(), item.getVersionCode());
        }
    }

    private void bindButtons(final String packageName, int versionCode) {
        if (DownloadController.getInstance().isDownloading()) {
            buttonsSwitcher.setDisplayedChild(2);
            return;
        }
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                final Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                boolean isRunnable = launchIntent != null;
                boolean isNewer = versionCode > packageInfo.versionCode;
                if (isRunnable) {
                    buttonsSwitcher.setDisplayedChild(1);
                    buttonFirst.setText(R.string.remove);
                    buttonFirst.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FlurryAgent.logEvent("Store app: remove");
                            removeApp(packageName);
                        }
                    });
                    if (isNewer) {
                        buttonSecond.setText(R.string.update);
                        buttonSecond.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                FlurryAgent.logEvent("Store app: update");
                                updateApp();
                            }
                        });
                    } else {
                        buttonSecond.setText(R.string.open);
                        buttonSecond.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                FlurryAgent.logEvent("Store app: open");
                                openApp(launchIntent);
                            }
                        });
                    }
                } else {
                    buttonsSwitcher.setDisplayedChild(0);
                    if (isNewer) {
                        buttonOne.setText(R.string.update);
                        buttonOne.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                FlurryAgent.logEvent("Store app: update");
                                updateApp();
                            }
                        });
                    } else {
                        buttonOne.setText(R.string.remove);
                        buttonOne.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                FlurryAgent.logEvent("Store app: remove");
                                removeApp(packageName);
                            }
                        });
                    }
                }
            }
            return;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        buttonsSwitcher.setDisplayedChild(0);
        buttonOne.setText(R.string.install);
        buttonOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("Store app: install");
                checkPermissionsForInstall();
            }
        });
    }

    private void responsibilityDenial() {
        if (PreferenceHelper.isShowResponsibilityDenial(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.responsibility_denial_title))
                    .setMessage(getString(R.string.responsibility_denial_text))
                    .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PreferenceHelper.setShowResponsibilityDenial(DownloadActivity.this, false);
                            installApp();
                        }
                    })
                    .setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showError(R.string.agree_with_responsibility_condition);
                        }
                    })
                    .show();
        } else {
            installApp();
        }
    }

    private void checkPermissionsForInstall() {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    responsibilityDenial();
                } else {
                    showError(R.string.write_permission_install);
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                String title = DownloadActivity.this.getString(R.string.app_name);
                String message = DownloadActivity.this.getString(R.string.write_permission_install);
                Permiso.getInstance().showRationaleInDialog(title, message, null, callback);
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void installApp() {
        File directory = getExternalDirectory();
        File destination = new File(directory, getApkName(info.getItem()));
        if (destination.exists()) {
            destination.delete();
        }
        String filePath = destination.getAbsolutePath();
        DownloadController.getInstance().download(info.getLink(), filePath);
    }

    private void updateApp() {
        checkPermissionsForInstall();
    }

    private void cancelDownload() {
        DownloadController.getInstance().cancelDownload();
    }

    private void removeApp(String packageName) {
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
        startActivity(uninstallIntent);
    }

    private void openApp(Intent launchIntent) {
        startActivity(launchIntent);
    }

    private void bindPermissions(final List<String> permissions) {
        final boolean hasPermissions = !permissions.isEmpty();
        int count = Math.min(MAX_PERMISSIONS_COUNT, permissions.size());
        permissionsContainer.removeAllViews();
        for (int c = 0; c < count; c++) {
            String permission = permissions.get(c);
            View permissionView = getLayoutInflater().inflate(R.layout.permission_view, permissionsContainer, false);
            TextView permissionDescription = permissionView.findViewById(R.id.permission_description);
            TextView permissionName = permissionView.findViewById(R.id.permission_name);
            String description = getPermissionSmallInfo(this, permission).getDescription();
            permissionDescription.setText(description);
            permissionName.setText(permission);
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) permissionView.getLayoutParams();
            layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.permissions_margin);
            permissionView.setLayoutParams(layoutParams);
            permissionsContainer.addView(permissionView);
        }
        permissionsBlock.setVisibility(hasPermissions ? View.VISIBLE : View.GONE);
        boolean isOverflow = permissions.size() > MAX_PERMISSIONS_COUNT;
        readMoreButton.setVisibility(hasPermissions && isOverflow ? View.VISIBLE : View.GONE);
        shadowView.setVisibility(readMoreButton.getVisibility());
        if (isOverflow) {
            permissionsBlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FlurryAgent.logEvent("Store app: permissions");
                    Intent intent = new Intent(DownloadActivity.this, PermissionsActivity.class)
                            .putStringArrayListExtra(PermissionsActivity.EXTRA_PERMISSIONS,
                                    new ArrayList<>(permissions));
                    startActivity(intent);
                }
            });
        }

        int stringRes;
        String access = "";
        boolean hasSmsAccess = false;
        boolean hasGeoAccess = false;
        boolean hasCallAccess = false;
        for (String permission : permissions) {
            String permissionUpper = permission.toUpperCase();
            boolean isDangerous = PermissionHelper.getPermissionSmallInfo(this, permission).isDangerous();
            if (isDangerous) {
                if (!hasSmsAccess && permissionUpper.contains("SMS")) {
                    stringRes = R.string.access_sms;
                    hasSmsAccess = true;
                } else if (!hasGeoAccess && permissionUpper.contains("LOCATION")) {
                    stringRes = R.string.access_geo;
                    hasGeoAccess = true;
                } else if (!hasCallAccess && permissionUpper.contains("CALL")) {
                    stringRes = R.string.access_call;
                    hasCallAccess = true;
                } else {
                    stringRes = 0;
                }
                if (stringRes != 0) {
                    if (!TextUtils.isEmpty(access)) {
                        access += ", ";
                    }
                    access += getString(stringRes);
                }
            }
        }
        extraAccess.setVisibility(TextUtils.isEmpty(access) ? View.GONE : View.VISIBLE);
        extraAccess.setText(getString(R.string.has_access, access));
    }

    private void bindVersions(List<StoreVersion> versions, String appId, int versionCode) {
        versionsContainer.removeAllViews();
        boolean isVersionsAdded = false;
        for (final StoreVersion version : versions) {
            if (TextUtils.equals(version.getAppId(), appId)) {
                continue;
            }
            View versionView = getLayoutInflater().inflate(R.layout.version_view, versionsContainer, false);
            TextView versionNameView = versionView.findViewById(R.id.app_version_name);
            TextView versionCodeView = versionView.findViewById(R.id.app_version_code);
            TextView versionDownloads = versionView.findViewById(R.id.app_version_downloads);
            TextView newerBadge = versionView.findViewById(R.id.app_newer_badge);
            versionNameView.setText(version.getVerName());
            versionCodeView.setText('(' + String.valueOf(version.getVerCode()) + ')');
            versionDownloads.setText(String.valueOf(version.getDownloads()));
            boolean isNewer = version.getVerCode() > versionCode;
            boolean isSame = version.getVerCode() == versionCode;
            if (isNewer) {
                newerBadge.setVisibility(View.VISIBLE);
                newerBadge.setText(R.string.newer);
            } else if (isSame) {
                newerBadge.setVisibility(View.VISIBLE);
                newerBadge.setText(R.string.same);
            } else {
                newerBadge.setVisibility(View.GONE);
            }
            versionView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishAttempt(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(DownloadActivity.this, DownloadActivity.class);
                            intent.putExtra(DownloadActivity.STORE_APP_ID, version.getAppId());
                            intent.putExtra(DownloadActivity.STORE_APP_LABEL, LocaleHelper.getLocalizedLabel(info.getItem()));
                            startActivity(intent);
                        }
                    });
                }
            });
            versionsContainer.addView(versionView);
            isVersionsAdded = true;
        }
        versionsContainer.setVisibility(isVersionsAdded ? View.VISIBLE : View.GONE);
        otherVersionsTitle.setVisibility(versionsContainer.getVisibility());
    }

    @Override
    public void onInfoLoaded(StoreInfo storeInfo) {
        bindStoreItem(storeInfo);
        viewFlipper.setDisplayedChild(1);
        swipeRefresh.setEnabled(true);
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onInfoError() {
        viewFlipper.setDisplayedChild(2);
        swipeRefresh.setEnabled(true);
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onInfoProgress() {
        if (!swipeRefresh.isRefreshing()) {
            viewFlipper.setDisplayedChild(0);
        }
    }

    @Override
    public void onDownloadStarted() {
        buttonsSwitcher.setDisplayedChild(2);
        swipeRefresh.setEnabled(false);
        progress.setIndeterminate(true);
    }

    @Override
    public void onDownloadProgress(long downloadedBytes) {
        progress.setIndeterminate(false);
        StoreItem item = info.getItem();
        long time = System.currentTimeMillis();
        if (item.getSize() > 0 && time - progressUpdateTime >= DEBOUNCE_DELAY) {
            progressUpdateTime = time;
            progress.setProgress((int) (100 * downloadedBytes / item.getSize()));
        }
    }

    @Override
    public void onDownloaded(String filePath) {
        viewFlipper.setDisplayedChild(0);
        swipeRefresh.setEnabled(true);
        bindButtons();

        IntentHelper.openFile(this, filePath, "application/vnd.android.package-archive");
    }

    @Override
    public void onDownloadError() {
        showError(R.string.downloading_error);
        viewFlipper.setDisplayedChild(0);
        swipeRefresh.setEnabled(true);
        bindButtons();
    }

    private void onAbusePressed() {
        Intent intent = new Intent(this, AbuseActivity.class)
                .putExtra(AbuseActivity.APP_ID, appId)
                .putExtra(AbuseActivity.APP_LABEL, appLabel);
        startActivity(intent);
    }

    private void showError(@StringRes int message) {
        Snackbar.make(viewFlipper, message, Snackbar.LENGTH_LONG).show();
    }

    private static String getApkPrefix(StoreItem item) {
        return FileHelper.escapeFileSymbols(item.getLabel() + "-" + item.getVersion());
    }

    private static String getApkSuffix() {
        return ".apk";
    }

    private static String getApkName(StoreItem item) {
        return getApkPrefix(item) + getApkSuffix();
    }

    private void finishAttempt(final Runnable runnable) {
        if (DownloadController.getInstance().isDownloading()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.cancel_download_title))
                    .setMessage(getString(R.string.cancel_download_text))
                    .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelDownload();
                            finish();
                            if (runnable != null) {
                                runnable.run();
                            }
                        }
                    })
                    .setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        } else {
            finish();
            if (runnable != null) {
                runnable.run();
            }
        }
    }
}
