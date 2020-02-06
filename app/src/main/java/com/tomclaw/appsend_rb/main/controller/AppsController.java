package com.tomclaw.appsend_rb.main.controller;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.tomclaw.appsend_rb.R;
import com.tomclaw.appsend_rb.core.MainExecutor;
import com.tomclaw.appsend_rb.main.item.AppItem;
import com.tomclaw.appsend_rb.main.item.BaseItem;
import com.tomclaw.appsend_rb.main.item.DonateItem;
import com.tomclaw.appsend_rb.util.PreferenceHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.content.pm.PackageManager.GET_PERMISSIONS;
import static com.tomclaw.appsend_rb.util.PreferenceHelper.IS_DONATE_ENABLED;

/**
 * Created by ivsolkin on 08.01.17.
 */
public class AppsController extends AbstractController<AppsController.AppsCallback> {

    private static class Holder {

        static AppsController instance = new AppsController();
    }

    public static AppsController getInstance() {
        return Holder.instance;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<BaseItem> list;
    private boolean isError = false;

    private Future<?> future;

    @Override
    public void onAttached(AppsCallback callback) {
        if (isLoaded()) {
            callback.onLoaded(list);
        } else if (isError) {
            callback.onError();
        } else {
            callback.onProgress();
        }
    }

    public boolean isLoaded() {
        return list != null;
    }

    public boolean isStarted() {
        return future != null;
    }

    public void reload(final Context context) {
        list = null;
        isError = false;
        future = executor.submit(() -> {
            try {
                loadInternal(context);
            } catch (Throwable ignored) {
                onError();
            }
        });
    }

    @Override
    public void onDetached(AppsCallback callback) {
    }

    private void onProgress() {
        MainExecutor.execute(() -> operateCallbacks(AppsCallback::onProgress));
    }

    private void onLoaded(final List<BaseItem> list) {
        this.list = list;
        MainExecutor.execute(() -> operateCallbacks(callback -> callback.onLoaded(list)));
    }

    private void onError() {
        this.isError = true;
        MainExecutor.execute(() -> operateCallbacks(AppsCallback::onError));
    }

    private void loadInternal(Context context) {
        onProgress();
        PackageManager packageManager = context.getPackageManager();
        ArrayList<AppItem> appItemList = new ArrayList<>();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : packages) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(
                        info.packageName, GET_PERMISSIONS);
                File file = new File(info.publicSourceDir);
                if (file.exists()) {
                    String label = packageManager.getApplicationLabel(info).toString();
                    String version = packageInfo.versionName;
                    long firstInstallTime = packageInfo.firstInstallTime;
                    long lastUpdateTime = packageInfo.lastUpdateTime;
                    Intent launchIntent = packageManager.getLaunchIntentForPackage(info.packageName);
                    AppItem appItem = new AppItem(label, info.packageName, version, file.getPath(),
                            file.length(), firstInstallTime, lastUpdateTime, packageInfo);
                    boolean isUserApp = ((info.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM &&
                            (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != ApplicationInfo.FLAG_UPDATED_SYSTEM_APP);
                    if (isUserApp || PreferenceHelper.isShowSystemApps(context)) {
                        if (launchIntent != null || !PreferenceHelper.isRunnableOnly(context)) {
                            appItemList.add(appItem);
                        }
                    }
                }
            } catch (Throwable ignored) {
                // Bad package.
            }
        }
        String sortOrder = PreferenceHelper.getSortOrder(context);
        if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_ascending_value))) {
            Collections.sort(appItemList, (lhs, rhs) -> lhs.getLabel().toUpperCase().compareTo(rhs.getLabel().toUpperCase()));
        } else if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_descending_value))) {
            Collections.sort(appItemList, (lhs, rhs) -> rhs.getLabel().toUpperCase().compareTo(lhs.getLabel().toUpperCase()));
        } else if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_app_size_value))) {
            Collections.sort(appItemList, (lhs, rhs) -> compareLong(rhs.getSize(), lhs.getSize()));
        } else if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_install_time_value))) {
            Collections.sort(appItemList, (lhs, rhs) -> compareLong(rhs.getFirstInstallTime(), lhs.getFirstInstallTime()));
        } else if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_update_time_value))) {
            Collections.sort(appItemList, (lhs, rhs) -> compareLong(rhs.getLastUpdateTime(), lhs.getLastUpdateTime()));
        }
        List<BaseItem> baseItems = new ArrayList<>(appItemList);
        if (IS_DONATE_ENABLED) {
            int count = Math.min(baseItems.size(), 7);
            Random random = new Random(System.currentTimeMillis());
            int position = random.nextInt(count);
            DonateItem donateItem = new DonateItem();
            baseItems.add(position, donateItem);
        }
        onLoaded(baseItems);
    }

    private int compareLong(long lhs, long rhs) {
        return Long.compare(lhs, rhs);
    }

    public interface AppsCallback extends AbstractController.ControllerCallback {

        void onProgress();

        void onLoaded(List<BaseItem> list);

        void onError();

    }
}
