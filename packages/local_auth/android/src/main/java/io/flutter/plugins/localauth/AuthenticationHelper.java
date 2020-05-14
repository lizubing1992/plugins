// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package io.flutter.plugins.localauth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugins.localauth.fingerprint.ConfirmDialog;
import io.flutter.plugins.localauth.fingerprint.FingerprintCallback;
import io.flutter.plugins.localauth.fingerprint.FingerprintDialog;
import io.flutter.plugins.localauth.fingerprint.FingerprintVerifyManager;

import java.util.concurrent.Executor;

/**
 * Authenticates the user with fingerprint and sends corresponding response back to Flutter.
 *
 * <p>One instance per call is generated to ensure readable separation of executable paths across
 * method calls.
 */
@SuppressWarnings("deprecation")
class AuthenticationHelper
        implements Application.ActivityLifecycleCallbacks {

    /**
     * The callback that handles the result of this authentication process.
     */
    interface AuthCompletionHandler {

        /**
         * Called when authentication was successful.
         */
        void onSuccess();

        /**
         * Called when authentication failed due to user. For instance, when user cancels the auth or
         * quits the app.
         */
        void onFailure();

        /**
         * Called when authentication fails due to non-user related problems such as system errors,
         * phone not having a FP reader etc.
         *
         * @param code  The error code to be returned to Flutter app.
         * @param error The description of the error.
         */
        void onError(String code, String error);
    }

    private final FragmentActivity activity;
    private final AuthCompletionHandler completionHandler;
    private final MethodCall call;
    private final boolean isAuthSticky;
    private final UiThreadExecutor uiThreadExecutor;
    private boolean activityPaused = false;

   private FingerprintVerifyManager fingerprintVerifyManager;
    private FingerprintVerifyManager.Builder builder;

    public AuthenticationHelper(
            FragmentActivity activity, MethodCall call, AuthCompletionHandler completionHandler) {
        this.activity = activity;
        this.completionHandler = completionHandler;
        this.call = call;
        this.isAuthSticky = call.argument("stickyAuth");
        this.uiThreadExecutor = new UiThreadExecutor();
        builder = new FingerprintVerifyManager.Builder(activity);
        builder.cancelBtnText((String) call.argument("cancelButton"));
        builder.title((String) call.argument("signInTitle"));
        builder.subTitle((String) call.argument("fingerprintHint"));
        builder.callback(fingerprintCallback);
        builder.enableAndroidP((Boolean) call.argument("enableAndroidP"));
        activity.getApplication().registerActivityLifecycleCallbacks(this);
    }

    private FingerprintCallback fingerprintCallback = new FingerprintCallback() {
        @Override
        public void onSucceeded() {
            completionHandler.onSuccess();
            stop();
        }

        @Override
        public void onFailed() {
//            completionHandler.onFailure();
//            stop();
        }

        @Override
        public void onUsepwd() {
            completionHandler.onFailure();
            stop();
        }

        @Override
        public void onCancel() {
            if (activityPaused && isAuthSticky) {
                return;
            } else {
                completionHandler.onFailure();
                stop();
            }
        }

        @Override
        public void onHwUnavailable() {
            completionHandler.onFailure();
        }

        @Override
        public void onNoneEnrolled() {
            //弹出提示框，跳转指纹添加页面
            if (call.argument("useErrorDialogs")) {
                showGoToSettingsDialog();
                return;
            }
            completionHandler.onFailure();
            stop();
        }

    };

    /**
     * Start the fingerprint listener.
     */
    public void authenticate() {

        fingerprintVerifyManager= builder.build();
    }

    /** Cancels the fingerprint authentication. */
    void stopAuthentication() {
//        if (biometricPrompt != null) {
//            biometricPrompt.cancelAuthentication();
//            biometricPrompt = null;
//        }
    }

    /**
     * Stops the fingerprint listener.
     */
    public void stop() {
        activity.getApplication().unregisterActivityLifecycleCallbacks(this);
    }


    /**
     * If the activity is paused, we keep track because fingerprint dialog simply returns "User
     * cancelled" when the activity is paused.
     */
    @Override
    public void onActivityPaused(Activity ignored) {
        if (isAuthSticky) {
            activityPaused = true;
        }
    }

    @Override
    public void onActivityResumed(Activity ignored) {
        if (isAuthSticky) {
            activityPaused = false;
//            if(fingerprintVerifyManager!=null){
//                fingerprintVerifyManager.cancelAuthenticate(builder);
//            }
            fingerprintVerifyManager= builder.build();

            // When activity is resuming, we cannot show the prompt right away. We need to post it to the
            // UI queue.
//            uiThreadExecutor.handler.post(
//                    new Runnable() {
//                        @Override
//                        public void run() {
////              prompt.authenticate(promptInfo);
//                            if(fingerprintVerifyManager!=null){
//                                fingerprintVerifyManager.cancelAuthenticate(builder);
//                            }
//
//                            fingerprintVerifyManager= builder.build();
//                        }
//                    });
        }
    }

    // Suppress inflateParams lint because dialogs do not need to attach to a parent view.
    @SuppressLint("InflateParams")
    private void showGoToSettingsDialog() {
        String message = (String) call.argument("goToSettingDescription");
        String cancelButton = (String) call.argument("cancelButton");
        String goToSetting = (String) call.argument("goToSetting");
        ConfirmDialog dialog = new ConfirmDialog(activity, message, goToSetting, cancelButton,false)
                .setOnCancelListener(new ConfirmDialog.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        completionHandler.onFailure();
                        AuthenticationHelper.this.stop();
                    }
                }).setOnConfirmListener(new ConfirmDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        completionHandler.onFailure();
                        AuthenticationHelper.this.stop();
                        activity.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                    }
                });
        dialog.show();
    }

    // Unused methods for activity lifecycle.

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    private static class UiThreadExecutor implements Executor {
        public final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
