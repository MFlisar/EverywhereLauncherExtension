package com.michaelflisar.everywherelauncher.extension.common;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

/**
 * Created by flisar on 27.11.2017.
 */

public class CommonExtensionManager {

    public static final String TAG = CommonExtensionManager.class.getName();

    public static final String MSG_ARG_PACKAGE_NAME = "MSG_ARG_PACKAGE_NAME";
    public static final String MSG_ARG_ACTIVITY_NAME = "MSG_ARG_ACTIVITY_NAME";

    private static final ComponentName REMOTE_SERVICE_COMPONENT = new ComponentName(
            "com.michaelflisar.everywherelauncher.extension",
            "com.michaelflisar.everywherelauncher.extension.services.RemoteService");

    private static final ComponentName MY_ACCESSIBILITY_SERVICE_COMPONENT = new ComponentName(
            "com.michaelflisar.everywherelauncher.extension",
            "com.michaelflisar.everywherelauncher.extension.services.MyAccessibilityService");

    private static final String MY_ACCESSIBILITY_SERVICE_ID = MY_ACCESSIBILITY_SERVICE_COMPONENT.getPackageName() + "/" + MY_ACCESSIBILITY_SERVICE_COMPONENT.getClassName().replace(
            MY_ACCESSIBILITY_SERVICE_COMPONENT.getPackageName(), "");

    // Action/Requests/Event IDs - placed in the message's what field
    public static final int ACTION_BACK = 2;
    public static final int ACTION_RECENTS = 3;
    public static final int REQUEST_REQUEST_REGISTER_APP = 1001;
    public static final int EVENT_FOREGROUND_CHANGED = 2001;
    public static final int EVENT_UPDATE_OVERLAY_SERVICE_FOCUSABILITY = 2002;

    public static final int ARG1_RUNNING = 1;
    public static final int ARG1_NOT_RUNNING = 2;
    public static final int ARG1_ENABLE = 3;
    public static final int ARG1_DISABLE = 4;
    public static final int ARG1_REGISTER = 5;
    public static final int ARG1_UNREGISTER = 6;

    public static boolean isAccessibilityEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> runningServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            Log.d(TAG, String.format("ServiceId: %s", service.getId()));
            if (MY_ACCESSIBILITY_SERVICE_ID.equals(service.getId())) {
                Log.d(TAG, "Accessibility Service is enabled!");
                return true;
            }
        }
        return false;
    }

    public static int getInstalledVersion(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = REMOTE_SERVICE_COMPONENT.getPackageName();
        int version = -1;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            version = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Extension App not installed!");
        }
        return version;
    }

    public static void install(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://github.com/MFlisar/EverywhereLauncherExtension/blob/master/README.md"));
//        intent.setData(Uri.parse("https://github.com/MFlisar/EverywhereLauncherExtensions/raw/master/extension.apk"));
        context.startActivity(intent);
    }

    public static void openApp(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(REMOTE_SERVICE_COMPONENT.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    protected boolean bind(Context context, ServiceConnection serviceConnection) {
        Log.d(TAG, "Binding RemoteService");
        try {
            Intent intent = new Intent();
            intent.setComponent(REMOTE_SERVICE_COMPONENT);
            if (context.bindService(intent, serviceConnection, 0)) {
                Log.d(TAG, "Binding to RemoteService returned true");
                return true;
            } else {
                Log.d(TAG, "Binding to RemoteService returned false");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "ERROR", e);
        }
        return false;
    }

    protected void unbind(Context context, ServiceConnection serviceConnection) {
        context.unbindService(serviceConnection);
        Log.d(TAG, "Unbinding RemoteService");
    }


    protected void sendMessage(Messenger sender, Messenger receiver, Message message) {
        if (message != null && receiver != null) {
            try {
                message.replyTo = sender;
                receiver.send(message);
            } catch (RemoteException e) {
                Log.e(TAG, "ERROR", e);
            }
        }
    }
}