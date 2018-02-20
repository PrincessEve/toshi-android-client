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

package com.toshi.view.adapter.viewholder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.toshi.R;
import com.toshi.model.local.SendState;
import com.toshi.model.network.SofaError;
import com.toshi.model.sofa.payment.Payment;
import com.toshi.model.sofa.SofaMessage;
import com.toshi.util.EthUtil;
import com.toshi.util.ImageUtil;
import com.toshi.view.BaseApplication;
import com.toshi.view.adapter.listeners.OnItemClickListener;

public final class PaymentViewHolder extends RecyclerView.ViewHolder {

    private @NonNull TextView title;
    private @NonNull TextView ethereumAmount;
    private @NonNull TextView body;
    private @Nullable ImageView sendStatus;
    private @Nullable ImageView avatar;
    private @Nullable TextView errorMessage;

    private Payment payment;
    private @SendState.State int sendState;
    private String avatarUri;
    private SofaError sofaError;

    public PaymentViewHolder(final View v) {
        super(v);
        this.avatar = v.findViewById(R.id.avatar);
        this.title = v.findViewById(R.id.title);
        this.ethereumAmount = v.findViewById(R.id.eth_amount);
        this.body = v.findViewById(R.id.body);
        this.sendStatus = v.findViewById(R.id.sent_status);
        this.errorMessage = v.findViewById(R.id.error_message);
    }

    public PaymentViewHolder setPayment(final Payment payment) {
        this.payment = payment;
        return this;
    }

    public PaymentViewHolder setSendState(final @SendState.State int sendState) {
        this.sendState = sendState;
        return this;
    }

    public PaymentViewHolder setAvatarUri(final String avatarUri) {
        this.avatarUri = avatarUri;
        return this;
    }

    public PaymentViewHolder setSofaError(final SofaError sofaError) {
        this.sofaError = sofaError;
        return this;
    }

    public PaymentViewHolder setOnResendPaymentListener(final OnItemClickListener<SofaMessage> listener,
                                                        final SofaMessage sofaMessage) {
        if (this.sendState == SendState.STATE_FAILED) {
            this.itemView.setOnClickListener(v -> listener.onItemClick(sofaMessage));
        }
        return this;
    }

    public void draw() {
        renderAmounts();
        renderAvatar();
        setSendState();
    }

    private void renderAmounts() {
        final String requestTitle = BaseApplication.get().getString(R.string.payment_for_value, this.payment.getLocalPrice());
        this.title.setText(requestTitle);
        final String ethAmount = String.format(
                BaseApplication.get().getResources().getString(R.string.eth_amount),
                EthUtil.hexAmountToUserVisibleString(this.payment.getValue()));
        this.ethereumAmount.setText(ethAmount);
    }

    private void renderAvatar() {
        if (this.avatar == null) return;
        ImageUtil.load(this.avatarUri, this.avatar);
    }

    private void setSendState() {
        if (this.sendStatus == null || this.errorMessage == null) return;
        final int visibility = this.sendState == SendState.STATE_FAILED || this.sendState == SendState.STATE_PENDING
                ? View.VISIBLE
                : View.GONE;
        this.sendStatus.setVisibility(visibility);
        this.errorMessage.setVisibility(visibility);
        setErrorMessage();
    }

    private void setErrorMessage() {
        if (this.sofaError == null || this.errorMessage == null) return;
        final String errorMessage = this.sofaError.getUserReadableErrorMessage(this.errorMessage.getContext());
        this.errorMessage.setText(errorMessage);
        this.sofaError = null;
    }
}
