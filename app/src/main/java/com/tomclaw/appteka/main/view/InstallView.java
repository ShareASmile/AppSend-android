package com.tomclaw.appteka.main.view;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.flurry.android.FlurryAgent;
import com.greysonparrelli.permiso.Permiso;
import com.tomclaw.appteka.PermissionsActivity;
import com.tomclaw.appteka.R;
import com.tomclaw.appteka.UploadActivity;
import com.tomclaw.appteka.main.adapter.BaseItemAdapter;
import com.tomclaw.appteka.main.adapter.FilterableItemAdapter;
import com.tomclaw.appteka.main.adapter.MenuAdapter;
import com.tomclaw.appteka.main.controller.ApksController;
import com.tomclaw.appteka.main.item.ApkItem;
import com.tomclaw.appteka.main.item.BaseItem;
import com.tomclaw.appteka.main.item.CouchItem;
import com.tomclaw.appteka.util.ColorHelper;
import com.tomclaw.appteka.util.EdgeChanger;
import com.tomclaw.appteka.util.IntentHelper;
import com.tomclaw.appteka.util.PreferenceHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tomclaw.appteka.util.IntentHelper.bluetoothApk;
import static com.tomclaw.appteka.util.IntentHelper.openGooglePlay;
import static com.tomclaw.appteka.util.IntentHelper.shareApk;

/**
 * Created by ivsolkin on 08.01.17.
 */
public class InstallView extends MainView implements ApksController.ApksCallback {

    private static final String HIDE_COUCH_ACTION = "hide_couch_action";

    private ViewFlipper viewFlipper;
    private SwipeRefreshLayout swipeRefresh;
    private TextView errorText;
    private RecyclerView recyclerView;
    private FilterableItemAdapter adapter;

    public InstallView(final Context context) {
        super(context);

        viewFlipper = findViewById(R.id.apps_view_switcher);

        errorText = findViewById(R.id.error_text);

        View retryButton = findViewById(R.id.button_retry);
        retryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsForInstall();
            }
        });

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView = findViewById(R.id.apps_list_view);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        recyclerView.setItemAnimator(itemAnimator);
        final int toolbarColor = ColorHelper.getAttributedColor(context, R.attr.toolbar_background);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                EdgeChanger.setEdgeGlowColor(recyclerView, toolbarColor);
            }
        });

        BaseItemAdapter.BaseItemClickListener listener = new BaseItemAdapter.BaseItemClickListener() {
            @Override
            public void onItemClicked(final BaseItem item) {
                boolean apkItem = item.getType() == BaseItem.APK_ITEM;
                if (apkItem) {
                    final ApkItem info = (ApkItem) item;
                    showActionDialog(info);
                }
            }

            @Override
            public void onActionClicked(BaseItem item, String action) {
                if (TextUtils.equals(action, HIDE_COUCH_ACTION)) {
                    FlurryAgent.logEvent("Hide install couch");
                    PreferenceHelper.setShowInstallCouch(context, false);
                    start();
                }
            }
        };

        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        adapter = new FilterableItemAdapter(context);
        adapter.setListener(listener);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.install_view;
    }

    @Override
    public void activate() {
        checkPermissionsForInstall();
    }

    @Override
    public void start() {
        ApksController.getInstance().onAttach(this);
    }

    @Override
    public void stop() {
        ApksController.getInstance().onDetach(this);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void refresh() {
        ApksController.getInstance().reload(getContext());
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

    @Override
    public void filter(String query) {
        adapter.getFilter().filter(query);
    }

    private void checkPermissionsForInstall() {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    // Permission granted!
                    if (!ApksController.getInstance().isStarted()) {
                        refresh();
                    }
                } else {
                    // Permission denied.
                    errorText.setText(R.string.write_permission_install);
                    viewFlipper.setDisplayedChild(2);
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                String title = getContext().getString(R.string.app_name);
                String message = getContext().getString(R.string.write_permission_install);
                Permiso.getInstance().showRationaleInDialog(title, message, null, callback);
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void showActionDialog(final ApkItem apkItem) {
        ListAdapter menuAdapter = new MenuAdapter(getContext(), R.array.apk_actions_titles, R.array.apk_actions_icons);
        new AlertDialog.Builder(getContext())
                .setAdapter(menuAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                FlurryAgent.logEvent("Install menu: install");
                                installApp(apkItem);
                                break;
                            }
                            case 1: {
                                FlurryAgent.logEvent("Install menu: share");
                                shareApk(getContext(), new File(apkItem.getPath()));
                                break;
                            }
                            case 2: {
                                FlurryAgent.logEvent("Install menu: upload");
                                Intent intent = new Intent(getContext(), UploadActivity.class);
                                intent.putExtra(UploadActivity.UPLOAD_ITEM, apkItem);
                                startActivity(intent);
                                break;
                            }
                            case 3: {
                                FlurryAgent.logEvent("Install menu: bluetooth");
                                bluetoothApk(getContext(), apkItem);
                                break;
                            }
                            case 4: {
                                FlurryAgent.logEvent("Install menu: Google Play");
                                final String packageName = apkItem.getPackageName();
                                openGooglePlay(getContext(), packageName);
                                break;
                            }
                            case 5: {
                                FlurryAgent.logEvent("Install menu: permissions");
                                try {
                                    PackageInfo packageInfo = apkItem.getPackageInfo();
                                    List<String> permissions = Arrays.asList(packageInfo.requestedPermissions);
                                    Intent intent = new Intent(getContext(), PermissionsActivity.class)
                                            .putStringArrayListExtra(PermissionsActivity.EXTRA_PERMISSIONS,
                                                    new ArrayList<>(permissions));
                                    startActivity(intent);
                                } catch (Throwable ex) {
                                    Snackbar.make(recyclerView, R.string.unable_to_get_permissions, Snackbar.LENGTH_LONG).show();
                                }
                                break;
                            }
                            case 6: {
                                FlurryAgent.logEvent("Install menu: remove");
                                File file = new File(apkItem.getPath());
                                file.delete();
                                refresh();
                                break;
                            }
                        }
                    }
                }).show();
    }

    private void installApp(ApkItem item) {
        IntentHelper.openFile(getContext(), item.getPath(), "application/vnd.android.package-archive");
    }

    private void setAppInfoList(List<BaseItem> appItemList) {
        adapter.setItemsList(appItemList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onProgress() {
        if (!swipeRefresh.isRefreshing()) {
            viewFlipper.setDisplayedChild(0);
        }
    }

    @Override
    public void onLoaded(List<BaseItem> list) {
        List<BaseItem> appItemList = new ArrayList<>();
        boolean isShowCouch = PreferenceHelper.isShowInstallCouch(getContext());
        if (isShowCouch) {
            int couchTextRes;
            if (list.isEmpty()) {
                couchTextRes = R.string.install_screen_no_apk_found;
            } else {
                couchTextRes = R.string.install_screen_apk_found;
            }
            String couchText = getContext().getString(couchTextRes);
            String buttonText = getContext().getString(R.string.got_it);
            BaseItem couchItem = new CouchItem(couchText, new CouchItem.CouchButton(HIDE_COUCH_ACTION, buttonText));
            appItemList.add(couchItem);
        }
        appItemList.addAll(list);
        setAppInfoList(appItemList);
        viewFlipper.setDisplayedChild(1);
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onError() {
        errorText.setText(R.string.apps_loading_error);
        viewFlipper.setDisplayedChild(2);
        swipeRefresh.setRefreshing(false);
    }
}
