package com.tomclaw.appteka.main.dto;

import android.os.Parcel;
import android.os.Parcelable;

import com.tomclaw.appteka.main.item.StoreItem;

import java.util.List;

/**
 * Created by ivsolkin on 17.01.17.
 */
public class StoreInfo implements Parcelable {

    public long expiresIn;
    public StoreItem info;
    public String link;
    public String url;
    public int status;
    public List<StoreVersion> versions;

    public StoreInfo(long expiresIn, StoreItem info, String link, String url, int status, List<StoreVersion> versions) {
        this.expiresIn = expiresIn;
        this.info = info;
        this.link = link;
        this.url = url;
        this.status = status;
        this.versions = versions;
    }

    protected StoreInfo(Parcel in) {
        expiresIn = in.readLong();
        info = in.readParcelable(StoreItem.class.getClassLoader());
        link = in.readString();
        status = in.readInt();
        versions = in.createTypedArrayList(StoreVersion.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(expiresIn);
        dest.writeParcelable(info, flags);
        dest.writeString(link);
        dest.writeInt(status);
        dest.writeTypedList(versions);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public StoreItem getItem() {
        return info;
    }

    public String getUrl() {
        return url;
    }

    public String getLink() {
        return link;
    }

    public int getStatus() {
        return status;
    }

    public List<StoreVersion> getVersions() {
        return versions;
    }

    public static final Creator<StoreInfo> CREATOR = new Creator<StoreInfo>() {
        @Override
        public StoreInfo createFromParcel(Parcel in) {
            return new StoreInfo(in);
        }

        @Override
        public StoreInfo[] newArray(int size) {
            return new StoreInfo[size];
        }
    };
}
