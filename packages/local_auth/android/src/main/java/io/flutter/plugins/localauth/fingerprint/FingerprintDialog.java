package io.flutter.plugins.localauth.fingerprint;

import android.app.DialogFragment;
import android.app.Service;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import io.flutter.plugins.localauth.R;
import io.flutter.plugins.localauth.fingerprint.bean.VerificationDialogStyleBean;


/**
 * Created by ZuoHailong on 2019/3/12.
 */
public class FingerprintDialog extends DialogFragment {

    private static FingerprintDialog mDialog;
    private OnDialogActionListener actionListener;
    private TextView tvTip, tvCancel, tvUsepwd;
    private ImageView ivFingerprint;

    private VerificationDialogStyleBean verificationDialogStyleBean;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);//
        setCancelable(false);
        View view = inflater.inflate(R.layout.biometricprompt_layout_fingerprint_dialog, container);
        ivFingerprint = view.findViewById(R.id.ivFingerprint);
        tvTip = view.findViewById(R.id.tvTip);
        tvUsepwd = view.findViewById(R.id.tvUsepwd);
        tvUsepwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null)
                    actionListener.onUsepwd();
                FingerprintDialog.this.dismiss();
            }
        });
        tvCancel = view.findViewById(R.id.tvCancel);
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null)
                    actionListener.onCancle();
                FingerprintDialog.this.dismiss();
            }
        });

        //调用者定义验证框样式
        if (verificationDialogStyleBean != null) {
            if (verificationDialogStyleBean.getCancelTextColor() != 0)
                tvCancel.setTextColor(verificationDialogStyleBean.getCancelTextColor());
            if (verificationDialogStyleBean.getUsepwdTextColor() != 0)
                tvUsepwd.setTextColor(verificationDialogStyleBean.getUsepwdTextColor());

            if (verificationDialogStyleBean.getFingerprintColor() != 0) {
                Drawable drawable = ivFingerprint.getDrawable();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//Android 5.0
                    drawable.setTint(verificationDialogStyleBean.getFingerprintColor());
                }
            }
            if (verificationDialogStyleBean.isUsepwdVisible()) {
                tvUsepwd.setVisibility(View.VISIBLE);
                view.findViewById(R.id.view).setVisibility(View.VISIBLE);
            } else {
                tvUsepwd.setVisibility(View.GONE);
                view.findViewById(R.id.view).setVisibility(View.GONE);
            }
        }

        initShake();
        return view;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (actionListener != null)
            actionListener.onDismiss();
    }

    public static FingerprintDialog newInstance() {
        if (mDialog == null) {
            synchronized (FingerprintDialog.class) {
                if (mDialog == null) {
                    mDialog = new FingerprintDialog();
                }
            }
        }
        return mDialog;
    }

    // 震动动画
    private Animation shakeAnimation;
    // 插值器
    private CycleInterpolator cycleInterpolator;
    // 振动器
    private Vibrator shakeVibrator;

    void initShake(){
        // 初始化振动器
        shakeVibrator = (Vibrator) getActivity().getSystemService(Service.VIBRATOR_SERVICE);
        // 初始化震动动画
        shakeAnimation = new TranslateAnimation(0, 10, 0, 0);
        shakeAnimation.setDuration(300);
        cycleInterpolator = new CycleInterpolator(8);
        shakeAnimation.setInterpolator(cycleInterpolator);
    }

    public FingerprintDialog setActionListener(OnDialogActionListener actionListener) {
        this.actionListener = actionListener;
        return mDialog;
    }

    /**
     * 设定dialog样式
     *
     * @param bean
     */
    public FingerprintDialog setDialogStyle(VerificationDialogStyleBean bean) {
        this.verificationDialogStyleBean = bean;
        return mDialog;
    }

    /**
     * 根据指纹验证的结果更新tip的文字内容和文字颜色
     * @param tip
     * @param colorId
     */
    public void setTip(String tip, @ColorRes int colorId) {
        tvTip.setText(tip);
        tvTip.setTextColor(getResources().getColor(colorId));
        tvTip.startAnimation(shakeAnimation);
        shakeVibrator.vibrate(new long[] { 0, 500 }, -1);
    }


    /**
     * 根据指纹验证的结果更新tip的文字内容和文字颜色
     * @param tip
     * @param colorId
     */
    public void setNormalTip(String tip, @ColorRes int colorId) {
        tvTip.setText(tip);
        tvTip.setTextColor(getResources().getColor(colorId));
    }

    public interface OnDialogActionListener {
        void onUsepwd();

        void onCancle();

        void onDismiss();
    }
}
