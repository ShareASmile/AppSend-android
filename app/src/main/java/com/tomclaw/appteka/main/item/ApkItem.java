package com.tomclaw.appteka.main.item;

import android.content.pm.PackageInfo;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ivsolkin on 09.01.17.
 */
public class ApkItem extends CommonItem implements Parcelable {

    private final String installedVersion;
    private final long createTime;

    public ApkItem(String label, String packageName, String version, String path, long size,
                   String installedVersion, long createTime, PackageInfo packageInfo) {
        super(label, packageName, version, path, size, packageInfo);
        this.installedVersion = installedVersion;
        this.createTime = createTime;
    }

    public ApkItem(Parcel in, String installedVersion, long createTime) {
        super(in);
        this.installedVersion = installedVersion;
        this.createTime = createTime;
    }

    private ApkItem(Parcel in) {
        super(in);
        installedVersion = in.readString();
        createTime = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(installedVersion);
        dest.writeLong(createTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ApkItem> CREATOR = new Creator<ApkItem>() {
        @Override
        public ApkItem createFromParcel(Parcel in) {
            return new ApkItem(in);
        }

        @Override
        public ApkItem[] newArray(int size) {
            return new ApkItem[size];
        }
    };

    @Override
    public int getType() {
        return APK_ITEM;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public long getCreateTime() {
        return createTime;
    }
}
