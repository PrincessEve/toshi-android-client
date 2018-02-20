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

package com.toshi.presenter.chat;


import com.toshi.model.local.PendingTransaction;
import com.toshi.model.sofa.SofaMessage;
import com.toshi.model.local.User;
import com.toshi.model.sofa.payment.Payment;
import com.toshi.model.sofa.SofaAdapters;
import com.toshi.util.LogUtil;
import com.toshi.view.BaseApplication;

import java.io.IOException;

import rx.Observable;

/**
 * An Observable that only returns PendingTransactions for a single user.
 * Example usage:
 * <pre> {@code

PendingTransactionsObservable pto = new PendingTransactionsObservable();
Observable<PendingTransaction> observable = pto.init(user); // Only PendingTransactions involving this user will be returned
} </pre>
 */
/* package */ class PendingTransactionsObservable {

    private User remoteUser;

    /**
     * Initialises PendingTransactionsObservable with the user whose PendingTransactions to observe
     * <p>
     * @param remoteUser
     *              The user to be observed.
     */
    /* package */ Observable<PendingTransaction> init(final User remoteUser) {
        this.remoteUser = remoteUser;
        return subscribeToPendingTransactionChanges();
    }

    private Observable<PendingTransaction> subscribeToPendingTransactionChanges() {
        return
                BaseApplication
                .get()
                .getTransactionManager()
                .getPendingTransactionObservable()
                .filter(this::shouldBeBroadcast)
                .doOnError(t -> LogUtil.exception(getClass(), "subscribeToPendingTransactionChanges", t))
                .onErrorReturn(t -> null);
    }

    private boolean shouldBeBroadcast(final PendingTransaction pendingTransaction) {
        try {
            final SofaMessage sofaMessage = pendingTransaction.getSofaMessage();
            final Payment payment = SofaAdapters.get().paymentFrom(sofaMessage.getPayload());
            final @Payment.PaymentDirection int paymentDirection =
                    payment.getPaymentDirection()
                            .toBlocking()
                            .value();
            return paymentDirection != Payment.NOT_RELEVANT
                    && isWatchingRemoteAddress(payment, paymentDirection);
        } catch (final IOException ex) {
            return false;
        }
    }

    private boolean isWatchingRemoteAddress(final Payment payment, final @Payment.PaymentDirection int paymentDirection) {
        final String remoteAddress = paymentDirection == Payment.FROM_LOCAL_USER
                ? payment.getToAddress()
                : payment.getFromAddress();
        return remoteAddress.equals(this.remoteUser.getPaymentAddress());
    }
}
