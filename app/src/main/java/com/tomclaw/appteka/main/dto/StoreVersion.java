package com.tomclaw.appteka.main.dto;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ivsolkin on 17.01.17.
 */

public class StoreVersion implements Parcelable {

    private String appId;
    private int downloads;
    private int verCode;
    private String verName;

    public StoreVersion(String appId, int downloads, int verCode, String verName) {
        this.appId = appId;
        this.downloads = downloads;
        this.verCode = verCode;
        this.verName = verName;
    }

    protected StoreVersion(Parcel in) {
        appId = in.readString();
        downloads = in.readInt();
        verCode = in.readInt();
        verName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(appId);
        dest.writeInt(downloads);
        dest.writeInt(verCode);
        dest.writeString(verName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getAppId() {
        return appId;
    }

    public int getDownloads() {
        return downloads;
    }

    public int getVerCode() {
        return verCode;
    }

    public String getVerName() {
        return verName;
    }

    public static final Creator<StoreVersion> CREATOR = new Creator<StoreVersion>() {
        @Override
        public StoreVersion createFromParcel(Parcel in) {
            return new StoreVersion(in);
        }

        @Override
        public StoreVersion[] newArray(int size) {
            return new StoreVersion[size];
        }
    };
}
