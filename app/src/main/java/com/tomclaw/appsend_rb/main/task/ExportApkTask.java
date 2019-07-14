package com.tomclaw.appsend_rb.main.task;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.widget.Toast;

import com.tomclaw.appsend_rb.R;
import com.tomclaw.appsend_rb.core.PleaseWaitTask;
import com.tomclaw.appsend_rb.main.item.AppItem;
import com.tomclaw.appsend_rb.main.item.CommonItem;
import com.tomclaw.appsend_rb.util.FileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.appcompat.app.AlertDialog;

import static com.tomclaw.appsend_rb.util.FileHelper.getExternalDirectory;
import static com.tomclaw.appsend_rb.util.IntentHelper.shareApk;

/**
 * Created by Solkin on 11.12.2014.
 */
public class ExportApkTask extends PleaseWaitTask {

    public static final int ACTION_EXTRACT = 0x00;
    public static final int ACTION_SHARE = 0x01;

    private final AppItem appItem;
    private final int actionType;

    private File destination;

    public ExportApkTask(Context context, AppItem appItem, int actionType) {
        super(context);
        this.appItem = appItem;
        this.actionType = actionType;
    }

    @Override
    public void executeBackground() throws Throwable {
        Context context = getWeakObject();
        if (context != null) {
            File file = new File(appItem.getPath());
            File directory = getExternalDirectory();
            destination = new File(directory, getApkName(appItem));
            if (destination.exists()) {
                destination.delete();
            }
            byte[] buffer = new byte[200 * 1024];
            InputStream inputStream = new FileInputStream(file);
            OutputStream outputStream = new FileOutputStream(destination);
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        }
    }

    public static String getApkPrefix(CommonItem item) {
        return FileHelper.escapeFileSymbols(item.getLabel() + "-" + item.getVersion());
    }

    public static String getApkSuffix() {
        return ".apk";
    }

    public static String getApkName(CommonItem item) {
        return getApkPrefix(item) + getApkSuffix();
    }

    @Override
    public void onSuccessMain() {
        final Context context = getWeakObject();
        if (context != null) {
            switch (actionType) {
                case ACTION_EXTRACT: {
                    AlertDialog alertDialog = new AlertDialog.Builder(context)
                            .setTitle(R.string.success)
                            .setMessage(Html.fromHtml(context.getString(R.string.app_extract_success, destination.getPath())))
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    shareApk(context, destination);
                                }
                            }).setNegativeButton(R.string.no, null)
                            .create();
                    alertDialog.show();
                    break;
                }
                case ACTION_SHARE: {
                    shareApk(context, destination);
                    break;
                }
            }
        }
    }

    @Override
    public void onFailMain(Throwable ex) {
        Context context = getWeakObject();
        if (context != null) {
            Toast.makeText(context, R.string.app_extract_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
