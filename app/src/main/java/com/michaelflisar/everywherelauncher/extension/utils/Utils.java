package com.michaelflisar.everywherelauncher.extension.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.michaelflisar.everywherelauncher.extension.common.CommonExtensionManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by flisar on 07.12.2017.
 */

public class Utils {

    public static Integer getNewestExtensionVersion() {
        try {
            URL url = new URL(CommonExtensionManager.Link.LATEST_RELEASE.getLink());
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            String link = new URL(connection.getHeaderField("Location")).toString();
            String versionString = link.replace(CommonExtensionManager.RELEASE_PAGE_MAIN_LINK, "");
            return Integer.parseInt(versionString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
