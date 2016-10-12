package com.bakkenbaeck.toshi.view.dialog;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import com.bakkenbaeck.toshi.R;
import com.bakkenbaeck.toshi.network.rest.model.VerificationSent;
import com.bakkenbaeck.toshi.network.ws.model.VerificationStart;
import com.bakkenbaeck.toshi.network.ws.model.WebSocketError;
import com.bakkenbaeck.toshi.network.ws.model.WebSocketErrors;
import com.bakkenbaeck.toshi.util.LocaleUtil;
import com.bakkenbaeck.toshi.util.OnNextSubscriber;
import com.bakkenbaeck.toshi.util.OnSingleClickListener;
import com.bakkenbaeck.toshi.view.BaseApplication;
import com.bakkenbaeck.toshi.view.activity.ChatActivity;
import com.hbb20.CountryCodePicker;

import java.util.Locale;

import rx.Observable;
import rx.subjects.PublishSubject;

public class PhoneInputDialog extends DialogFragment {

    private String inputtedPhoneNumber;
    private Listener listener;
    private View view;

    private OnNextSubscriber<WebSocketError> errorSubscriber;
    private OnNextSubscriber<VerificationSent> verificationSentSubscriber;
    private PublishSubject<String> errorSubject = PublishSubject.create();

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface Listener {
        void onPhoneInputSuccess(final PhoneInputDialog dialog);
    }

    public String getInputtedPhoneNumber() {
        return this.inputtedPhoneNumber;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            this.listener = (PhoneInputDialog.Listener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement PhoneInputDialog.Listener");
        }

        this.errorSubscriber = generateErrorSubscriber();
        this.verificationSentSubscriber = generateVerificationSentSubscriber();
        BaseApplication.get().getSocketObservables().getErrorObservable().subscribe(this.errorSubscriber);
        BaseApplication.get().getSocketObservables().getVerificationSentObservable().subscribe(this.verificationSentSubscriber);
    }

    private OnNextSubscriber<WebSocketError> generateErrorSubscriber() {
        return new OnNextSubscriber<WebSocketError>() {
            @Override
            public void onNext(final WebSocketError webSocketError) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        view.findViewById(R.id.spinner_view).setVisibility(View.INVISIBLE);
                        setErrorOnPhoneField(webSocketError);
                    }
                });
            }
        };
    }

    private OnNextSubscriber<VerificationSent> generateVerificationSentSubscriber() {
        return new OnNextSubscriber<VerificationSent>() {
            @Override
            public void onNext(final VerificationSent verificationSent) {
                listener.onPhoneInputSuccess(PhoneInputDialog.this);
                dismiss();
            }
        };
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        Dialog dialog = super.onCreateDialog(state);
        if(dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.dialog_phone_input, container, true);
        getDialog().setCanceledOnTouchOutside(false);

        initViews(view);

        return view;
    }

    private void initViews(final View view) {
        final Locale currentLocale = LocaleUtil.getLocale();
        ((CountryCodePicker)view.findViewById(R.id.country_code)).setCountryForNameCode(currentLocale.getCountry());
        view.findViewById(R.id.cancelButton).setOnClickListener(this.dismissDialog);
        view.findViewById(R.id.continueButton).setOnClickListener(new PhoneInputDialog.ValidateAndContinueDialog(view));
    }

    private final View.OnClickListener dismissDialog = new OnSingleClickListener() {
        @Override
        public void onSingleClick(final View v) {
            dismiss();
        }
    };

    private class ValidateAndContinueDialog implements View.OnClickListener {

        private final View view;

        private ValidateAndContinueDialog(final View view) {
            this.view = view;
        }

        @Override
        public void onClick(final View v) {
            final EditText phoneNumberField = (EditText) this.view.findViewById(R.id.phone_number);
            if (TextUtils.isEmpty(phoneNumberField.getText())) {
                setErrorOnPhoneField();
                return;
            }

            final String countryCode = ((CountryCodePicker)view.findViewById(R.id.country_code)).getSelectedCountryCodeWithPlus();
            inputtedPhoneNumber = countryCode + phoneNumberField.getText();

            final VerificationStart vsFrame = new VerificationStart(inputtedPhoneNumber);
            BaseApplication.get().sendWebSocketMessage(vsFrame.toString());

            this.view.findViewById(R.id.spinner_view).setVisibility(View.VISIBLE);
        }
    }

    private void setErrorOnPhoneField() {
        setErrorOnPhoneField(null);
    }

    private void setErrorOnPhoneField(final WebSocketError error) {
        final EditText phoneNumberField = (EditText) this.view.findViewById(R.id.phone_number);
        phoneNumberField.requestFocus();

        if (error != null && error.getCode().equals(WebSocketErrors.phone_number_already_in_use)) {
            String errorMessage = getContext().getResources().getString(R.string.error__phone_number_in_use);
            errorSubject.onNext(errorMessage);
        } else {
            String errorMessage = getContext().getResources().getString(R.string.error__invalid_phone_number);
            errorSubject.onNext(errorMessage);
        }
    }

    public Observable<String> getErrorObservable(){
        return errorSubject.asObservable();
    }

    @Override
    public void onDetach() {
        this.errorSubscriber.unsubscribe();
        this.verificationSentSubscriber.unsubscribe();
        this.errorSubscriber = null;
        this.verificationSentSubscriber = null;
        super.onDetach();
    }

}
