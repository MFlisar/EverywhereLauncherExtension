package com.michaelflisar.everywherelauncher.extension;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.michaelflisar.everywherelauncher.extension.common.CommonExtensionManager;
import com.michaelflisar.everywherelauncher.extension.services.MyAccessibilityService;
import com.michaelflisar.everywherelauncher.extension.utils.Utils;

public class MainActivity extends Activity implements View.OnClickListener {

    private GithubTask mGithubTask = null;
    private TextView tvAccessibilityState;
    private TextView tvVersion;
    private TextView tvNewestVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvAccessibilityState = findViewById(R.id.tvAccessibilityState);
        tvVersion = findViewById(R.id.tvVersion);
        tvNewestVersion = findViewById(R.id.tvNewestVersion);
        updateView();

        tvNewestVersion.setText("...");
        mGithubTask = new GithubTask(this);
        mGithubTask.execute();

        try {
            PackageInfo info = getPackageManager().getPackageInfo(this.getPackageName(), 0);
            tvVersion.setText(String.format("v%d (%s)", info.versionCode, info.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        mGithubTask.destroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateView();
    }

    private void updateView() {
        boolean accessibilityServiceRunning = CommonExtensionManager.isAccessibilityEnabled(this);
        tvAccessibilityState.setText(accessibilityServiceRunning ? R.string.yes : R.string.no);
        tvAccessibilityState.setTextColor(accessibilityServiceRunning ? Color.GREEN : Color.RED);
    }

    public void onGithubInfoReady(Integer versionCode) {
        if (versionCode == null) {
            tvNewestVersion.setText(R.string.unknown);
        } else {
            tvNewestVersion.setText(String.format("v%d", versionCode));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btEnableAccessibilityService:
                if (!MyAccessibilityService.isAccessibilityEnabled()) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.already_running, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btReadme:
                CommonExtensionManager.openLink(CommonExtensionManager.Link.README, this);
                break;
            case R.id.btReleases:
                CommonExtensionManager.openLink(CommonExtensionManager.Link.RELEASES, this);
                break;
        }
    }

    private static class GithubTask extends AsyncTask<Void, Void, Integer> {

        private MainActivity mActivity;

        public GithubTask(MainActivity activity) {
            mActivity = activity;
        }

        public void destroy() {
            mActivity = null;
            cancel(true);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return Utils.getNewestExtensionVersion();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (mActivity != null) {
                mActivity.onGithubInfoReady(result);
            }
        }
    }
}
