/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.toshi.R;
import com.toshi.exception.InvalidQrCode;
import com.toshi.exception.InvalidQrCodePayment;
import com.toshi.model.local.CurrencyMode;
import com.toshi.model.local.QrCodePayment;
import com.toshi.model.local.User;
import com.toshi.view.BaseApplication;
import com.toshi.view.activity.ViewUserActivity;
import com.toshi.view.fragment.PaymentConfirmationFragment;

import kotlin.Unit;
import rx.Completable;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class QrCodeHandler {

    public interface QrCodeHandlerListener {
        void onInvalidQrCode();
    }

    private static final String WEB_SIGNIN = "web-signin:";
    public static final String ACTIVITY_RESULT = "activity_result";

    private CompositeSubscription subscriptions;
    private AppCompatActivity activity;
    private QrCodeHandlerListener listener;
    private @ScannerResultType.TYPE int scannerResultType;

    public QrCodeHandler(final AppCompatActivity activity, final @ScannerResultType.TYPE int scannerResultType) {
        this.activity = activity;
        this.subscriptions = new CompositeSubscription();
        this.scannerResultType = scannerResultType;
    }

    public void setOnQrCodeHandlerListener(final QrCodeHandlerListener listener) {
        this.listener = listener;
    }

    public void handleResult(final String result) {
        final QrCode qrCode = new QrCode(result);
        final @QrCodeType.Type int qrCodeType = qrCode.getQrCodeType();
        final boolean isScannerResultTypePaymentAddress = this.scannerResultType == ScannerResultType.PAYMENT_ADDRESS;

        if (isScannerResultTypePaymentAddress && qrCodeType != QrCodeType.PAYMENT_ADDRESS) {
            showToast(BaseApplication.get().getString(R.string.scan_error_payment));
            this.activity.finish();
        } else if (qrCodeType == QrCodeType.EXTERNAL_PAY) {
            handleExternalPayment(qrCode);
        } else if (qrCodeType == QrCodeType.ADD) {
            handleAddQrCode(qrCode);
        } else if (qrCodeType == QrCodeType.PAY) {
            handlePaymentQrCode(qrCode);
        } else if (qrCodeType == QrCodeType.PAYMENT_ADDRESS) {
            handlePaymentAddressQrCode(qrCode);
        } else if (result.startsWith(WEB_SIGNIN)) {
            handleWebLogin(result);
        } else {
            handleInvalidQrCode();
        }
    }

    private void handleExternalPayment(final QrCode qrCode)  {
        try {
            final QrCodePayment payment = qrCode.getExternalPayment();
            final Subscription sub =
                    getUserFromPaymentAddress(payment.getAddress())
                    .subscribe(
                            user -> showToshiPaymentConfirmationDialog(user.getToshiId(), payment),
                            __ -> showExternalPaymentConfirmationDialog(payment)
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private Single<User> getUserFromPaymentAddress(final String paymentAddress) {
        return BaseApplication
                .get()
                .getRecipientManager()
                .getUserFromPaymentAddress(paymentAddress)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void showToshiPaymentConfirmationDialog(final String toshiId, final QrCodePayment payment) {
        if (this.activity == null) return;
        try {
            final PaymentConfirmationFragment dialog =
                    PaymentConfirmationFragment.Companion
                            .newInstanceToshiPayment(
                                    toshiId,
                                    payment.getValue(),
                                    payment.getMemo(),
                                    PaymentType.TYPE_REQUEST
                            );
            dialog.show(this.activity.getSupportFragmentManager(), PaymentConfirmationFragment.TAG);
            dialog.setOnPaymentConfirmationCanceledListener(__ -> finishActivity())
                    .setOnPaymentConfirmationFinishedListener(this::finishActivity);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private void showExternalPaymentConfirmationDialog(final QrCodePayment payment) {
        if (this.activity == null) return;
        try {
            final PaymentConfirmationFragment dialog =
                    PaymentConfirmationFragment.Companion
                            .newInstanceExternalPayment(
                                    payment.getAddress(),
                                    payment.getValue(),
                                    payment.getMemo(),
                                    PaymentType.TYPE_REQUEST,
                                    false,
                                    CurrencyMode.FIAT
                            );
            dialog.show(this.activity.getSupportFragmentManager(), PaymentConfirmationFragment.TAG);
            dialog.setOnPaymentConfirmationCanceledListener(__ -> finishActivity())
                    .setOnPaymentConfirmationFinishedListener(this::finishActivity);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private Unit finishActivity() {
        if (this.activity != null) this.activity.finish();
        return null;
    }

    private void handleAddQrCode(final QrCode qrCode) {
        try {
            final Subscription sub =
                    getUserByUsername(qrCode.getUsername())
                    .doOnSuccess(__ -> playScanSound())
                    .subscribe(
                            this::handleUserToAdd,
                            __ -> handleInvalidQrCode()
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCode e) {
            handleInvalidQrCode();
        }
    }

    private void handleUserToAdd(final User user) {
        if (user == null) {
            handleInvalidQrCode();
            return;
        }

        goToProfileView(user.getToshiId());
    }

    private void goToProfileView(final String toshiId) {
        if (this.activity == null) return;
        final Intent intent = new Intent(this.activity, ViewUserActivity.class)
                .putExtra(ViewUserActivity.EXTRA__USER_ADDRESS, toshiId)
                .putExtra(ViewUserActivity.EXTRA__PLAY_SCAN_SOUNDS, true);
        this.activity.startActivity(intent);
        this.activity.finish();
    }

    private void handlePaymentQrCode(final QrCode qrCode) {
        try {
            final QrCodePayment payment = qrCode.getToshiPayment();
            final Subscription sub =
                    getUserByUsername(payment.getUsername())
                    .doOnSuccess(__ -> playScanSound())
                    .subscribe(
                            user -> showToshiPaymentConfirmationDialog(user.getToshiId(), payment),
                            __ -> handleInvalidQrCode()
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private Single<User> getUserByUsername(final String username) {
        return BaseApplication
                .get()
                .getRecipientManager()
                .getUserFromUsername(username)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void handlePaymentAddressQrCode(final QrCode qrCode) {
        if (this.scannerResultType == ScannerResultType.PAYMENT_ADDRESS) {
            finishActivityWithResult(qrCode);
        } else {
            copyPaymentAddressToClipBoard(qrCode);
            showToast(BaseApplication.get().getString(R.string.copied_payment_address_to_clipboard, qrCode.getPayload()));
        }
        this.activity.finish();
    }

    private void finishActivityWithResult(final QrCode qrCode) {
        if (this.activity == null) return;
        final Intent intent = new Intent()
                .putExtra(ACTIVITY_RESULT, qrCode.getPayloadAsAddress().getHexAddress());
        this.activity.setResult(Activity.RESULT_OK, intent);
        this.activity.finish();
    }

    private void copyPaymentAddressToClipBoard(final QrCode qrCode) {
        if (this.activity == null) return;
        final ClipboardManager clipboard = (ClipboardManager) this.activity.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(this.activity.getString(R.string.payment_address), qrCode.getPayloadAsAddress().getHexAddress());
        clipboard.setPrimaryClip(clip);
    }

    private void handleWebLogin(final String result) {
        final String token = result.substring(WEB_SIGNIN.length());
        final Subscription sub =
                loginWithToken(token)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(this::playScanSound)
                .subscribe(
                        this::handleLoginSuccess,
                        this::handleLoginFailure
                );

        this.subscriptions.add(sub);
    }

    private Completable loginWithToken(final String token) {
        return BaseApplication
                .get()
                .getUserManager()
                .webLogin(token)
                .subscribeOn(Schedulers.io());
    }

    private void playScanSound() {
        SoundManager.getInstance().playSound(SoundManager.SCAN);
    }

    private void handleLoginSuccess() {
        showToast(BaseApplication.get().getString(R.string.web_signin));
        if (this.activity == null) return;
        this.activity.finish();
    }

    private void handleLoginFailure(final Throwable throwable) {
        LogUtil.exception(getClass(), "Login failure", throwable);
        showToast(BaseApplication.get().getString(R.string.error__web_signin));
    }

    private void handleInvalidQrCode() {
        SoundManager.getInstance().playSound(SoundManager.SCAN_ERROR);
        this.listener.onInvalidQrCode();
    }

    private void showToast(final String string) {
        Toast.makeText(
                BaseApplication.get(),
                string,
                Toast.LENGTH_LONG
        ).show();
    }

    public void clear() {
        this.subscriptions.clear();
        this.activity = null;
    }
}
