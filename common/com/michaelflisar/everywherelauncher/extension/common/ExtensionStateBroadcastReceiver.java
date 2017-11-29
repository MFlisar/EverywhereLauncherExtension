package com.michaelflisar.everywherelauncher.extension.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;

/**
 * Created by flisar on 29.11.2017.
 */

public abstract class ExtensionStateBroadcastReceiver extends BroadcastReceiver {

    public static final String INTENT_FILTER_ACTION = "com.michaelflisar.everywherelauncher.extension.common.ExtensionStateBroadcastReceiver";

    public static final String EXTENSION_APP_STATE = "EXTENSION_APP_STATE";
    public static final String EXTENSION_APP_FLAGS = "EXTENSION_APP_FLAGS";

    public static final Integer EXTENSION_FLAG_ACCESSIBILITY_RUNNING = 1;

    public static final IntentFilter INTENT_FILTER = new IntentFilter(INTENT_FILTER_ACTION);

    public static void broadcastState(Context context, boolean extensionInstalled, int... flags) {
        final Intent intent = new Intent();
        intent.setAction(INTENT_FILTER_ACTION);
        intent.putExtra(EXTENSION_APP_STATE, extensionInstalled);
        ArrayList<Integer> flagList = new ArrayList<>();
        for (int i = 0; i < flags.length; i++) {
            flagList.add(flags[i]);
        }
        intent.putExtra(EXTENSION_APP_FLAGS, flagList);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            boolean extensionInstalled = intent.getExtras().getBoolean(EXTENSION_APP_STATE);
            ArrayList<Integer> flags = intent.getExtras().getIntegerArrayList(EXTENSION_APP_FLAGS);
            onStateChanged(extensionInstalled, flags);
        }
    }

    protected abstract void onStateChanged(boolean extensionInstalled, ArrayList<Integer> flags);
}
