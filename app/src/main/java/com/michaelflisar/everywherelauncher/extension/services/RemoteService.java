package com.michaelflisar.everywherelauncher.extension.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.michaelflisar.everywherelauncher.extension.BuildConfig;
import com.michaelflisar.everywherelauncher.extension.MainApp;
import com.michaelflisar.everywherelauncher.extension.common.CommonExtensionManager;
import com.michaelflisar.everywherelauncher.extension.utils.Utils;

/**
 * Created by flisar on 27.11.2017.
 */

public class RemoteService extends Service {

    private static final String TAG = RemoteService.class.getName();

    private Messenger mMessenger;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RemoteService created");
        MainApp.setRemoteService(this);
    }

    @Override
    public void onDestroy() {
        MainApp.setRemoteService(null);
        Log.d(TAG, "RemoteService destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "RemoteService onBind");
        if (mMessenger == null) {
            synchronized (RemoteService.class) {
                if (mMessenger == null) {
                    mMessenger = new Messenger(new IncomingHandler());
                }
            }
        }
        return mMessenger.getBinder();
    }

    public void send(Messenger replyTo, Message replyMessage) {
        if (mMessenger != null) {
            if (replyTo != null && replyMessage != null) {
                try {
                    replyTo.send(replyMessage);
                } catch (RemoteException rme) {
                    if (!BuildConfig.DEBUG) {
                        Log.e(TAG, "Can't send result message!", rme);
                    }
                }
            }
        }
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            Log.d(TAG, String.format("Remote Service invoked (what = %d)", msg.what));

            // 1) Handle message
            Message replyMessage = null;
            switch (msg.what) {
                case CommonExtensionManager.REQUEST_REQUEST_REGISTER_APP:
                    MyAccessibilityService.sendRegisterAppIntent(msg.what, RemoteService.this, msg.replyTo, msg.arg1 == CommonExtensionManager.ARG1_REGISTER);
                    break;
                case CommonExtensionManager.REQUEST_REQUEST_VERSION_INFOS:
                    Integer newestExtensionVersion = Utils.getNewestExtensionVersion();
                    int installedVersion = Utils.getAppVersion(RemoteService.this);
                    replyMessage = Message.obtain(null, msg.what, installedVersion, newestExtensionVersion == null ? -1 : newestExtensionVersion);
                    break;
                case CommonExtensionManager.ACTION_BACK:
                    MyAccessibilityService.sendBackIntent(msg.what, RemoteService.this, msg.replyTo);
                    break;
                case CommonExtensionManager.ACTION_RECENTS:
                    MyAccessibilityService.sendRecentIntent(msg.what, RemoteService.this, msg.replyTo);
                    break;
                case CommonExtensionManager.EVENT_FOREGROUND_CHANGED:
                    replyMessage = msg;
                    break;
                default:
                    Log.d(TAG, String.format("Can't handle message with what = %d", msg.what));
                    break;
            }

            // 2) Reply message
            send(msg.replyTo, replyMessage);
        }
    }
}