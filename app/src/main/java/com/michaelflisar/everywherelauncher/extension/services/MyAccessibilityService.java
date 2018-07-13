package com.michaelflisar.everywherelauncher.extension.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.michaelflisar.everywherelauncher.extension.BuildConfig;
import com.michaelflisar.everywherelauncher.extension.MainApp;
import com.michaelflisar.everywherelauncher.extension.common.CommonExtensionManager;
import com.michaelflisar.everywherelauncher.extension.common.ExtensionStateBroadcastReceiver;

import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = MyAccessibilityService.class.getName();

    private static final String ACCESSIBILITY_SERVICE_INTENT = MyAccessibilityService.class.getName() + "-ACCESSIBILITY_SERVICE_INTENT";

    private static final boolean DETECT_KEYBOARD_IN_EDIT_TEXTS = true;
    private static final boolean DETECT_KEYBOARD_VIA_INPUT_METHOD_EVENTS = true;

    private BroadcastReceiver mReceiver;
    private Handler mHandler;
    private ServiceConnection mRemoteServiceConnection;
    private Messenger mAppMessenger;
    private boolean mRemoteServiceConnected;

    private CharSequence mEditViewPackageName = null;
    private int mEditViewFocusFoundCounter = -1;

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

//    @Override
//    protected void onServiceConnected() {
//        super.onServiceConnected();
//
//        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
//
//        //Configure these here for compatibility with API 13 and below.
//        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED | AccessibilityEvent.TYPE_VIEW_FOCUSED
//            | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
//        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;// |  AccessibilityServiceInfo.FEEDBACK_SPOKEN;
//
//        if (Build.VERSION.SDK_INT >= 16) {
//            //Just in case this helps
//            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
//        }
//
//        setServiceInfo(config);
//    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (BuildConfig.DEBUG) {
            logEvent(event);
        }

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String className = event.getClassName() != null ? event.getClassName().toString() : "";
        AccessibilityNodeInfo ani = event.getSource();
        int windowType = (ani != null && ani.getWindow() != null) ? ani.getWindow().getType() : -1;
        Boolean hasFocus = ani != null ? ani.isFocused() : null;

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Bundle b = new Bundle();
            b.putString(CommonExtensionManager.MSG_ARG_PACKAGE_NAME, packageName);
            b.putString(CommonExtensionManager.MSG_ARG_ACTIVITY_NAME, className);
            sendMessage(Message.obtain(null, CommonExtensionManager.EVENT_FOREGROUND_CHANGED, 0, 0, b));

            // app was changed, so we assume keyboard is hidden
            if (mEditViewFocusFoundCounter >= 0 && mEditViewPackageName != null && !packageName.equals(mEditViewPackageName)) {
                mEditViewFocusFoundCounter = 0;
            }

        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (mEditViewFocusFoundCounter >= 0) {
                // looks like this is not happening with hasFocus == false, so this probalby will never be used anymore
                if (className.contains("EditText") && hasFocus != null && !hasFocus) {
                    // we assume, the second event of this kind for an EditText after it got focus means that keyboard is hidden
                    // one event of this type is coming immediately after opening anyways
                    mEditViewFocusFoundCounter--;
                }
            }
            // Input method event => we assume keyboard is shown
            // happens in non EditText Views and also whenever something is inserted into an EditText View
            else if (DETECT_KEYBOARD_VIA_INPUT_METHOD_EVENTS && windowType == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                mEditViewFocusFoundCounter = 1;
                mEditViewPackageName = packageName;
                sendMessage(Message.obtain(null, CommonExtensionManager.EVENT_KEYBOARD_SHOWN, 0, 0, null));
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, String.format("Keyboard shown : %s", packageName));
                }
            }

        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            if (DETECT_KEYBOARD_IN_EDIT_TEXTS && className.contains("EditText")) {
                // EditText got focus, we assume this opens up the keyboard
                mEditViewFocusFoundCounter = 2;
                mEditViewPackageName = packageName;
                sendMessage(Message.obtain(null, CommonExtensionManager.EVENT_KEYBOARD_SHOWN, 0, 0, null));
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, String.format("Keyboard shown : %s", packageName));
                }
            } else {
                // something else got the focus, we assume keyboard is hidden
                if (mEditViewFocusFoundCounter >= 0)
                    mEditViewFocusFoundCounter = 0;
            }
        }

        // we send the keyboard hidden event once, then we set the counter to -1
        if (mEditViewFocusFoundCounter == 0) {
            sendMessage(Message.obtain(null, CommonExtensionManager.EVENT_KEYBOARD_HIDDEN, 0, 0, null));
            if (BuildConfig.DEBUG) {
                Log.d(TAG, String.format("Keyboard hidden : %s", packageName));
            }
            mEditViewFocusFoundCounter = -1;
            mEditViewPackageName = null;
        }
    }

    private void logEvent(AccessibilityEvent event) {

        String bounds = "";
        String awiBounds = "";
        AccessibilityNodeInfo ani = event.getSource();
        String viewId = "";
        AccessibilityWindowInfo awi = null;
        if (ani != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                viewId = ani.getViewIdResourceName();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                awi = ani.getWindow();
            }
            int parent = 0;
            bounds = addAccessibilityNodeInfoLog(bounds, ani, parent);
            while (ani.getParent() != null) {
                ani = ani.getParent();
                parent++;
                bounds = addAccessibilityNodeInfoLog(bounds, ani, parent);
            }

            if (awi != null) {
                parent = 0;
                awiBounds = addAccessibilityWindowInfoLog(awiBounds, awi, parent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    while (awi.getParent() != null) {
                        awi = awi.getParent();
                        parent++;
                        awiBounds = addAccessibilityWindowInfoLog(awiBounds, awi, parent);
                    }
                }
            }
        }

        Log.d(TAG, String.format("%s | className: %s | packageName: %s | viewId: %s | awi: %s | awiBounds: %s", AccessibilityEvent.eventTypeToString(event.getEventType()), event.getClassName(),
                event.getPackageName(), viewId, awi, awiBounds));
//            Log.d(TAG, String.format("%s | className: %s | bounds: %s | awi: %s", AccessibilityEvent.eventTypeToString(event.getEventType()), event.getClassName(), bounds, awi));
//            Log.d(TAG, String.format("%s | source: %s | topSource [%d]: %s", event.toString(), event.getSource(), parent, ani));
    }

    private String addAccessibilityNodeInfoLog(String log, AccessibilityNodeInfo ani, int index) {
        Rect rect1 = new Rect();
        Rect rect2 = new Rect();
        ani.getBoundsInParent(rect1);
        ani.getBoundsInScreen(rect2);
        String resName = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            resName = ani.getViewIdResourceName();
        }
        log += String.format("%sresName[%d]: %s | bip: %s, bis: %s", index == 0 ? "" : " | ", index, resName, rect1, rect2);
        return log;
    }

    private String addAccessibilityWindowInfoLog(String log, AccessibilityWindowInfo awi, int index) {
        Rect rect1 = new Rect();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            awi.getBoundsInScreen(rect1);
        }
        log += String.format("%sbis[%d]: %s", index == 0 ? "" : " | ", index, rect1);
        return log;
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