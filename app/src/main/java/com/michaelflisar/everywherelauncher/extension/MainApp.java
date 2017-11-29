package com.michaelflisar.everywherelauncher.extension;

import android.app.Application;

import com.michaelflisar.everywherelauncher.extension.services.RemoteService;

/**
 * Created by flisar on 27.11.2017.
 */

public class MainApp extends Application {

    private static MainApp mInstance = null;
    private static RemoteService mRemoteService = null;

    public MainApp() {
        super();
        mInstance = this;
    }

    public static MainApp get() {
        return mInstance;
    }

    public static void setRemoteService(RemoteService service) {
        mRemoteService = service;
    }

    public static RemoteService getRemoteService() {
        return mRemoteService;
    }
}