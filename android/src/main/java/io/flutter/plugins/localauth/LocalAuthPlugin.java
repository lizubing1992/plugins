// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.localauth;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.fragment.app.FragmentActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugins.localauth.AuthenticationHelper.AuthCompletionHandler;
import io.flutter.plugins.localauth.fingerprint.FingerprintImplForAndrM;
import io.flutter.plugins.localauth.fingerprint.FingerprintImplForAndrP;
import io.flutter.plugins.localauth.fingerprint.IFingerprint;
import io.flutter.plugins.localauth.fingerprint.uitls.AndrVersionUtil;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** LocalAuthPlugin */
@SuppressWarnings("deprecation")
public class LocalAuthPlugin implements MethodCallHandler {
  private final Registrar registrar;
  private final AtomicBoolean authInProgress = new AtomicBoolean(false);

    AuthenticationHelper authenticationHelper;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel =
        new MethodChannel(registrar.messenger(), "plugins.flutter.io/local_auth");
    channel.setMethodCallHandler(new LocalAuthPlugin(registrar));
  }

  private LocalAuthPlugin(Registrar registrar) {
    this.registrar = registrar;
  }

  @Override
  public void onMethodCall(MethodCall call, final Result result) {
    if (call.method.equals("authenticateWithBiometrics")) {
//      if (!authInProgress.compareAndSet(false, true)) {
//        // Apps should not invoke another authentication request while one is in progress,
//        // so we classify this as an error condition. If we ever find a legitimate use case for
//        // this, we can try to cancel the ongoing auth and start a new one but for now, not worth
//        // the complexity.
//        result.error("auth_in_progress", "Authentication in progress", null);
//        return;
//      }

      Activity activity = registrar.activity();
      if (activity == null || activity.isFinishing()) {
        result.error("no_activity", "local_auth plugin requires a foreground activity", null);
        return;
      }

      if (!(activity instanceof FragmentActivity)) {
        result.error(
            "no_fragment_activity",
            "local_auth plugin requires activity to be a FragmentActivity.",
            null);
        return;
      }
       authenticationHelper =
          new AuthenticationHelper(
              (FragmentActivity) activity,
              call,
              new AuthCompletionHandler() {
                @Override
                public void onSuccess() {
//                  if (authInProgress.compareAndSet(true, false)) {
                    result.success(true);
//                  }
                }

                @Override
                public void onFailure() {
//                  if (authInProgress.compareAndSet(true, false)) {
                    result.success(false);
//                  }
                }

                @Override
                public void onError(String code, String error) {
//                  if (authInProgress.compareAndSet(true, false)) {
                    result.error(code, error, null);
//                  }
                }
              });
      authenticationHelper.authenticate();
    } else if (call.method.equals("getAvailableBiometrics")) {
      try {
        Activity activity = registrar.activity();
        if (activity == null || activity.isFinishing()) {
          result.error("no_activity", "local_auth plugin requires a foreground activity", null);
          return;
        }
        ArrayList<String> biometrics = new ArrayList<String>();
        PackageManager packageManager = activity.getPackageManager();
        if (Build.VERSION.SDK_INT >= 23) {
          if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            biometrics.add("fingerprint");
          }
        }
        if (Build.VERSION.SDK_INT >= 29) {
          if (packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)) {
            biometrics.add("face");
          }
          if (packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)) {
            biometrics.add("iris");
          }
        }
        result.success(biometrics);
      } catch (Exception e) {
        result.error("no_biometrics_available", e.getMessage(), null);
      }
    }else if (call.method.equals("cancelBiometrics")) {
      try {
        Activity activity = registrar.activity();
        if (activity == null || activity.isFinishing()) {
          result.error("no_activity", "local_auth plugin requires a foreground activity", null);
          return;
        }
       boolean enableAndroidP=false;
        if(call.hasArgument("enableAndroidP")){
          enableAndroidP= call.argument("enableAndroidP");
        }

        if(authenticationHelper!=null){
            authenticationHelper.stop();
        }
        IFingerprint fingerprint;
        if (AndrVersionUtil.isAboveAndrP()) {
          if (enableAndroidP)
            fingerprint = FingerprintImplForAndrP.newInstance();
          else
            fingerprint = FingerprintImplForAndrM.newInstance();
          fingerprint.cancelAuthenticate();
          result.success(true);
        } else if (AndrVersionUtil.isAboveAndrM()) {
          fingerprint = FingerprintImplForAndrM.newInstance();
          fingerprint.cancelAuthenticate();
          result.success(true);
        }else{
          result.success(true);
        }
      } catch (Exception e) {
        result.error("no_biometrics_available", e.getMessage(), null);
      }
    } else {
      result.notImplemented();
    }
  }
}