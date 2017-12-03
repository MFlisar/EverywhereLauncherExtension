package com.michaelflisar.everywherelauncher.extension.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.michaelflisar.everywherelauncher.extension.MainApp;
import com.michaelflisar.everywherelauncher.extension.common.CommonExtensionManager;
import com.michaelflisar.everywherelauncher.extension.common.ExtensionStateBroadcastReceiver;

import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = MyAccessibilityService.class.getName();

    private static final String ACCESSIBILITY_SERVICE_INTENT = MyAccessibilityService.class.getName() + "-ACCESSIBILITY_SERVICE_INTENT";

    private BroadcastReceiver mReceiver;
    private Handler mHandler;
    private ServiceConnection mRemoteServiceConnection;
    private Messenger mAppMessenger;
    private boolean mRemoteServiceConnected;

    public static boolean isAccessibilityEnabled() {
        final String packageName = MainApp.get().getPackageName();
        final String ACCESSIBILITY_SERVICE_NAME = packageName + "/" + MyAccessibilityService.class.getName().replace(packageName, "");

        AccessibilityManager am = (AccessibilityManager) MainApp.get().getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> runningServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            Log.d(TAG, String.format("ServiceId: %s", service.getId()));
            if (ACCESSIBILITY_SERVICE_NAME.equals(service.getId())) {
                Log.d(TAG, "Accessibility Service is enabled!");
                return true;
            }
        }

        return false;
    }

    public static void sendRegisterAppIntent(int msgWhat, RemoteService service, Messenger replyTo, boolean register) {
        Intent i = new Intent(ACCESSIBILITY_SERVICE_INTENT);
        i.putExtra("action", 0);
        i.putExtra("msgWhat", msgWhat);
        i.putExtra("replyTo", register ? replyTo : null);
        service.sendBroadcast(i);
    }

    public static void sendBackIntent(int msgWhat, RemoteService service, Messenger replyTo) {
        Intent i = new Intent(ACCESSIBILITY_SERVICE_INTENT);
        i.putExtra("action", GLOBAL_ACTION_BACK);
        i.putExtra("msgWhat", msgWhat);
        i.putExtra("reportFinished", true);
        service.sendBroadcast(i);
    }

    public static void sendRecentIntent(int msgWhat, RemoteService service, Messenger replyTo) {
        Intent i = new Intent(ACCESSIBILITY_SERVICE_INTENT);
        i.putExtra("action", GLOBAL_ACTION_RECENTS);
        i.putExtra("msgWhat", msgWhat);
        i.putExtra("reportFinished", false);
        service.sendBroadcast(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyAccessibilityService onCreate");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACCESSIBILITY_SERVICE_INTENT);
        mReceiver = new MyReceiver();
        registerReceiver(mReceiver, filter);
        mHandler = new Handler();
        mRemoteServiceConnected = false;
        mRemoteServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
//                mRemoteServiceMessenger = new Messenger(service);
                mRemoteServiceConnected = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
//                mRemoteServiceMessenger = null;
                mRemoteServiceConnected = false;
            }
        };
        bindService(new Intent(this, RemoteService.class), mRemoteServiceConnection, Context.BIND_AUTO_CREATE);
        ExtensionStateBroadcastReceiver.broadcastState(this, true, ExtensionStateBroadcastReceiver.EXTENSION_FLAG_ACCESSIBILITY_RUNNING);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MyAccessibilityService onDestroy");
        mHandler = null;
        unregisterReceiver(mReceiver);
        unbindService(mRemoteServiceConnection);
        ExtensionStateBroadcastReceiver.broadcastState(this, true);
        super.onDestroy();
    }

    private void sendMessage(final Message message) {
        sendMessage(message, 0);
    }

    private void sendMessage(final Message message, int delay) {
        final RemoteService service = MainApp.getRemoteService();
        if (mAppMessenger != null && service != null) {
            if (delay == 0) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        service.send(mAppMessenger, message);
                    }
                });
            } else {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        service.send(mAppMessenger, message);
                    }
                }, delay);
            }
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;// | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;// |  AccessibilityServiceInfo.FEEDBACK_SPOKEN;

        if (Build.VERSION.SDK_INT >= 16)
        //Just in case this helps
        {
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        }

        setServiceInfo(config);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            String activityName = event.getClassName() != null ? event.getClassName().toString() : "";

            Bundle b = new Bundle();
            b.putString(CommonExtensionManager.MSG_ARG_PACKAGE_NAME, packageName);
            b.putString(CommonExtensionManager.MSG_ARG_ACTIVITY_NAME, activityName);
            sendMessage(Message.obtain(null, CommonExtensionManager.EVENT_FOREGROUND_CHANGED, 0, 0, b));

//            TopAppChangedEvent e = new TopAppChangedEvent(packageName, activityName);
//            MainApp.cache(TopAppChangedEvent.class.getName(), e);
//            BusProvider.getInstance().post(e);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d(TAG, String.format("event: %s", event.toString()));
        }
    }

    @Override
    public void onInterrupt() {
    }

    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACCESSIBILITY_SERVICE_INTENT)) {
                Integer action = null;
                boolean reportFinished = false;
                int msgWhat = 0;

                if (intent.getExtras() != null) {
                    action = intent.getExtras().getInt("action");
                    reportFinished = intent.getExtras().getBoolean("reportFinished");
                    msgWhat = intent.getExtras().getInt("msgWhat");
                }

                if (action != null) {
                    switch (action) {
                        case 0:
                            mAppMessenger = null;
                            if (intent.getExtras().containsKey("replyTo")) {
                                mAppMessenger = intent.getExtras().getParcelable("replyTo");
                            }
                            break;
                        case GLOBAL_ACTION_BACK:
                        case GLOBAL_ACTION_HOME:
                        case GLOBAL_ACTION_NOTIFICATIONS:
                        case GLOBAL_ACTION_POWER_DIALOG:
                        case GLOBAL_ACTION_QUICK_SETTINGS:
                        case GLOBAL_ACTION_RECENTS:
                        case GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN: {
                            boolean result = performGlobalAction(action);
                            if (reportFinished) {
                                sendMessage(Message.obtain(null, msgWhat, CommonExtensionManager.ARG1_ACTION_FINISHED, 0), 500);
                            }
                            Log.d(TAG, String.format("Action %d executed: %b", action, result));
                            break;
                        }
                        default: {
                            Log.e(TAG, String.format("Unknown action %d", action));
                            break;
                        }
                    }
                }
            }
        }
    }
}