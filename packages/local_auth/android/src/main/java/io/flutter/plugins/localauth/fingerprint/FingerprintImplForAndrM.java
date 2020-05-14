package io.flutter.plugins.localauth.fingerprint;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;

import io.flutter.plugins.localauth.R;
import io.flutter.plugins.localauth.fingerprint.bean.VerificationDialogStyleBean;
import io.flutter.plugins.localauth.fingerprint.uitls.CipherHelper;

/**
 * Android M == 6.0
 * Created by ZuoHailong on 2019/7/9.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerprintImplForAndrM implements IFingerprint {

    private final String TAG = FingerprintImplForAndrM.class.getName();
    private Activity context;

    private static FingerprintImplForAndrM fingerprintImplForAndrM;
    //指纹验证框
    private static FingerprintDialog fingerprintDialog;
    //指向调用者的指纹回调
    private FingerprintCallback fingerprintCallback;

    //用于取消扫描器的扫描动作
    private CancellationSignal cancellationSignal;
    //指纹加密
    private static FingerprintManagerCompat.CryptoObject cryptoObject;
    //Android 6.0 指纹管理
    private FingerprintManagerCompat fingerprintManagerCompat;

    @Override
    public void authenticate(Activity context, VerificationDialogStyleBean bean, FingerprintCallback callback) {

        this.context = context;
        this.fingerprintCallback = callback;
        //Android 6.0 指纹管理 实例化
        fingerprintManagerCompat = FingerprintManagerCompat.from(context);

        //取消扫描，每次取消后需要重新创建新示例
        if (cancellationSignal != null && !cancellationSignal.isCanceled())
            cancellationSignal.cancel();
        cancellationSignal = new CancellationSignal();
//        cancellationSignal.setOnCancelListener(() -> fingerprintDialog.dismiss());

        //调起指纹验证
        fingerprintManagerCompat.authenticate(cryptoObject, 0, cancellationSignal, authenticationCallback, null);
        //指纹验证框
        if (fingerprintDialog == null) {
            fingerprintDialog = FingerprintDialog.newInstance().setActionListener(dialogActionListener).setDialogStyle(bean);
        }
        if (fingerprintDialog != null && fingerprintDialog.getDialog() != null
                && fingerprintDialog.getDialog().isShowing()) {
            //dialog is showing so do something
        } else {
            //dialog is not showing
            fingerprintDialog.show(context.getFragmentManager(), TAG);
        }


    }

    @Override
    public boolean cancelAuthenticate() {
        if (cancellationSignal != null && !cancellationSignal.isCanceled())
            cancellationSignal.cancel();
        if (fingerprintDialog != null) {
            fingerprintDialog.dismiss();
        }
        return true;
    }


    public static FingerprintImplForAndrM newInstance() {
        if (fingerprintImplForAndrM == null) {
            synchronized (FingerprintImplForAndrM.class) {
                if (fingerprintImplForAndrM == null) {
                    fingerprintImplForAndrM = new FingerprintImplForAndrM();
                }
            }
        }
        //指纹加密，提前进行Cipher初始化，防止指纹认证时还没有初始化完成
        try {
            cryptoObject = new FingerprintManagerCompat.CryptoObject(new CipherHelper().createCipher());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fingerprintImplForAndrM;
    }

    /**
     * 指纹验证框按键监听
     */
    private FingerprintDialog.OnDialogActionListener dialogActionListener = new FingerprintDialog.OnDialogActionListener() {
        @Override
        public void onUsepwd() {
            if (fingerprintCallback != null)
                fingerprintCallback.onUsepwd();
        }

        @Override
        public void onCancle() {//取消指纹验证，通知调用者
            if (fingerprintCallback != null)
                fingerprintCallback.onCancel();
        }

        @Override
        public void onDismiss() {//验证框消失，取消指纹验证
            if (cancellationSignal != null && !cancellationSignal.isCanceled())
                cancellationSignal.cancel();
        }
    };

    /**
     * 指纹验证结果回调
     */
    private FingerprintManagerCompat.AuthenticationCallback authenticationCallback = new FingerprintManagerCompat.AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            super.onAuthenticationError(errMsgId, errString);
            if (errMsgId != 5)//用户取消指纹验证
                fingerprintDialog.setTip(errString.toString(), R.color.biometricprompt_color_FF5555);
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            super.onAuthenticationHelp(helpMsgId, helpString);
            if (TextUtils.isEmpty(helpString.toString())) {
                fingerprintDialog.setNormalTip("请验证您在系统录入的指纹", R.color.black);
            } else {
                fingerprintDialog.setTip(helpString.toString(), R.color.biometricprompt_color_FF5555);
            }
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
//            fingerprintDialog.setTip(context.getString(R.string.biometricprompt_verify_success), R.color.biometricprompt_color_82C785);
            fingerprintDialog.dismiss();
            fingerprintCallback.onSucceeded();

        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            fingerprintDialog.setTip(context.getString(R.string.biometricprompt_verify_failed), R.color.biometricprompt_color_FF5555);
//            fingerprintCallback.onFailed();
        }
    };

    /*
     * 在 Android Q，Google 提供了 Api BiometricManager.canAuthenticate() 用来检测指纹识别硬件是否可用及是否添加指纹
     * 不过尚未开放，标记为"Stub"(存根)
     * 所以暂时还是需要使用 Andorid 6.0 的 Api 进行判断
     * */
    @Override
    public boolean canAuthenticate(Context context, FingerprintCallback fingerprintCallback) {
        /*
         * 硬件是否支持指纹识别
         * */
        if (!FingerprintManagerCompat.from(context).isHardwareDetected()) {
            fingerprintCallback.onHwUnavailable();
            return false;
        }
        //是否已添加指纹
        if (!FingerprintManagerCompat.from(context).hasEnrolledFingerprints()) {
            fingerprintCallback.onNoneEnrolled();
            return false;
        }
        return true;
    }

}
