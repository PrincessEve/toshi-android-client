package com.bakkenbaeck.token.model.local;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

import com.bakkenbaeck.token.R;
import com.bakkenbaeck.token.view.BaseApplication;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class User extends RealmObject implements Parcelable {

    @PrimaryKey
    private String owner_address;
    private String username;
    private CustomUserInformation customUserInfo;
    private CustomAppInformation customAppInfo;

    // ctors
    public User() {}

    public User(final User user) {
        this.owner_address = user.getOwnerAddress();
        this.username = user.getUsername();
        this.customUserInfo = new CustomUserInformation(user.getCustomUserInfo());
    }

    private User(final Parcel in) {
        owner_address = in.readString();
        username = in.readString();
        customUserInfo = in.readParcelable(CustomUserInformation.class.getClassLoader());
        customAppInfo = in.readParcelable(CustomAppInformation.class.getClassLoader());
    }

    public void setCustomAppInfo(final CustomAppInformation customAppInfo) {
        this.customAppInfo = customAppInfo;
    }

    // Getters

    public String getUsername() {
        return String.format("@%s", username);
    }

    // Defaults to the username if no name is set.
    public String getDisplayName() {
        if (customUserInfo == null || customUserInfo.getName() == null) {
            return username;
        }
        return customUserInfo.getName();
    }

    public String getOwnerAddress() {
        return owner_address;
    }

    public String getAbout() {
        return customUserInfo == null ? null : this.customUserInfo.getAbout();
    }

    public String getLocation() {
        return customUserInfo == null ? null : this.customUserInfo.getLocation();
    }

    public String getPaymentAddress() {
        return customUserInfo == null ? null : this.customUserInfo.getPaymentAddress();
    }

    public Bitmap getImage() {
        return BitmapFactory.decodeResource(BaseApplication.get().getResources(), R.mipmap.launcher);
    }

    private CustomUserInformation getCustomUserInfo() {
        return this.customUserInfo;
    }

    // Setters

    public void setUsername(final String username) {
        this.username = username;
    }


    // Parcelable implementation
    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(owner_address);
        dest.writeString(username);
        dest.writeParcelable(customUserInfo, flags);
        dest.writeParcelable(customAppInfo, flags);
    }
}
