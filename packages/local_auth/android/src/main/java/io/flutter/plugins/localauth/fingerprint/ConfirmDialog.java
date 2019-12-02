package io.flutter.plugins.localauth.fingerprint;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import io.flutter.plugins.localauth.R;

/**
 * 确认对话框
 * Created by cy on 2015/7/9.
 */
public class ConfirmDialog extends Dialog implements DialogInterface.OnCancelListener {

    TextView message_tv;
    Button mCancelBtn;
    Button mConfirmBtn;
    View mCancelLine;
    private CharSequence message;
    private String positiveText;
    private String negativeText;
    private OnCancelListener onCancelListener;
    private OnConfirmListener onConfirmListener;
    private boolean hasNegativeText = true;//是否显示取消
    private boolean cancelable = true;//是否可返回取消

    public ConfirmDialog(Context context, CharSequence message) {
        this(context, message, null);
    }

    public ConfirmDialog(Context context, CharSequence message, boolean hasNegativeText) {
        this(context, message, null, hasNegativeText);
    }

    public ConfirmDialog(Context context, CharSequence message, boolean hasNegativeText, boolean cancelable) {
        this(context, message, null, null, hasNegativeText, cancelable);
    }

    public ConfirmDialog(Context context, CharSequence message, String positiveText) {
        this(context, message, positiveText, true);
    }

    public ConfirmDialog(Context context, CharSequence message, String positiveText, boolean hasNegativeText) {
        this(context, message, positiveText, hasNegativeText, true);
    }

    public ConfirmDialog(Context context, CharSequence message, String positiveText, boolean hasNegativeText, boolean cancelable) {
        this(context, message, positiveText, null, hasNegativeText, cancelable);
    }

    public ConfirmDialog(Context context, CharSequence message, String positiveText, String negativeText) {
        this(context, message, positiveText, negativeText, true);
    }

    public ConfirmDialog(Context context, CharSequence message, String positiveText, String negativeText, boolean cancelable) {
        this(context, message, positiveText, negativeText, true, cancelable);
    }

    public ConfirmDialog(Context context, CharSequence message, String positiveText, String negativeText, boolean hasNegativeText, boolean cancelable) {
        super(context, R.style.CustomDialog);
        this.message = message;
        this.positiveText = positiveText;
        this.negativeText = negativeText;
        this.hasNegativeText = hasNegativeText;
        this.cancelable = cancelable;
        initView();
    }


    private void initView() {
        setContentView(R.layout.dialog_confirm);
        setCanceledOnTouchOutside(cancelable);
        setCancelable(cancelable);
        message_tv=findViewById(R.id.message_tv);
        mCancelLine=findViewById(R.id.cancel_line);
        mCancelBtn=findViewById(R.id.cancel_btn);
        mConfirmBtn=findViewById(R.id.confirm_btn);
        if (cancelable && !hasNegativeText) {
            setOnCancelListener(this);
        }
        if (!TextUtils.isEmpty(message)) {
            message_tv.setText(message);
        }
        if (!TextUtils.isEmpty(negativeText)) {
            mCancelBtn.setText(negativeText);
        }
        if (!TextUtils.isEmpty(positiveText)) {
            mConfirmBtn.setText(positiveText);
        }
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmDialog.this.dismiss();
                if (onCancelListener != null) {
                    onCancelListener.onCancel();
                }
            }
        });
        mConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmDialog.this.dismiss();
                if (onConfirmListener != null) {
                    onConfirmListener.onConfirm();
                }
            }
        });
        if (hasNegativeText) {
            mCancelBtn.setVisibility(View.VISIBLE);
            mCancelLine.setVisibility(View.VISIBLE);
            mConfirmBtn.setBackgroundResource(R.drawable.btn_dialog_confirm);
        } else {
            mCancelBtn.setVisibility(View.GONE);
            mCancelLine.setVisibility(View.GONE);
            mConfirmBtn.setBackgroundResource(R.drawable.btn_dialog_btn_bottom);
        }
    }

    public ConfirmDialog setMessageGravity(int gravity) {
        message_tv.setGravity(gravity);
        return this;
    }

    public ConfirmDialog setDismissedOnTouchOutside(boolean b){
        this.setCanceledOnTouchOutside(b);
        return this;
    }


    public ConfirmDialog setOnCancelListener(OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
        return this;
    }

    public ConfirmDialog setOnConfirmListener(OnConfirmListener onConfirmListener) {
        this.onConfirmListener = onConfirmListener;
        return this;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (onCancelListener != null) {
            onCancelListener.onCancel();
        }
    }

    public interface OnCancelListener {
        void onCancel();
    }

    public interface OnConfirmListener {
        void onConfirm();
    }


}
