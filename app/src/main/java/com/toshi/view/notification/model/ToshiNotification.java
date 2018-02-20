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

package com.toshi.view.notification.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.toshi.R;
import com.toshi.model.sofa.payment.Payment;
import com.toshi.model.sofa.PaymentRequest;
import com.toshi.model.sofa.SofaAdapters;
import com.toshi.model.sofa.SofaMessage;
import com.toshi.model.sofa.SofaType;
import com.toshi.util.LogUtil;
import com.toshi.view.BaseApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ToshiNotification {
    public static final String DEFAULT_TAG = "unknown";

    private final String id;
    private final ArrayList<SofaMessage> messages;
    private SofaMessage lastMessage;
    private static final int MAXIMUM_NUMBER_OF_SHOWN_MESSAGES = 5;
    /* package */ Bitmap largeIcon;
    private boolean isAccepted;

    /* package */ ToshiNotification(final String id) {
        this.id = id;
        this.messages = new ArrayList<>();
        this.lastMessage = new SofaMessage().makeNew("");
    }

    public void addUnreadMessage(final SofaMessage unreadMessage) {
        synchronized (this.messages) {
            this.messages.add(unreadMessage);
            generateLatestMessages(this.messages);
        }
    }

    private void generateLatestMessages(final ArrayList<SofaMessage> messages) {
        this.lastMessage = messages.get(messages.size() - 1);
    }

    public String getId() {
        return this.id;
    }

    public String getTag() {
        return DEFAULT_TAG;
    }

    public abstract String getTitle();

    public Bitmap getLargeIcon() {
        return this.largeIcon;
    }

    public int getNumberOfUnreadMessages() {
        return this.messages.size();
    }

    /* package */ Bitmap setDefaultLargeIcon() {
        return this.largeIcon = BitmapFactory.decodeResource(BaseApplication.get().getResources(), R.mipmap.ic_launcher);
    }

    public List<String> getLastFewMessages() {
        synchronized (this.messages) {
            final int end = Math.max(messages.size(), 0);
            final int start = Math.max(end - MAXIMUM_NUMBER_OF_SHOWN_MESSAGES, 0);
            final List<SofaMessage> sofaMessages = this.messages.subList(start, end);
            final List<String> messages = new ArrayList<>(sofaMessages.size());
            for (final SofaMessage sofaMessage : sofaMessages) {
                final String message = getMessage(sofaMessage);
                messages.add(message);
            }
            return messages;
        }
    }

    public String getLastMessage() {
        return getMessage(this.lastMessage);
    }

    private String getMessage(final SofaMessage sofaMessage) {
        if (!this.isAccepted) return getUnacceptedText();

        final @SofaType.Type int sofaType = sofaMessage.hasAttachment() ? sofaMessage.getAttachmentType() : sofaMessage.getType();
        if (sofaType == SofaType.PAYMENT_REQUEST) {
            final PaymentRequest paymentRequest = getPaymentRequestFromSofaMessage(sofaMessage);
            return paymentRequest != null && paymentRequest.getLocalPrice() != null
                    ? BaseApplication.get().getString(R.string.latest_message__payment_request, paymentRequest.getLocalPrice())
                    : null;
        } else if (sofaType == SofaType.PAYMENT) {
            final Payment payment = getPaymentFromSofaMessage(sofaMessage);
            return payment != null && payment.getLocalPrice() != null
                    ? BaseApplication.get().getString(R.string.latest_message__payment_incoming, payment.getLocalPrice())
                    : null;
        } else if (sofaType == SofaType.IMAGE || sofaType == SofaType.FILE) {
            return BaseApplication.get().getString(R.string.latest_received_attachment);
        }  else {
            return getBodyFromSofaMessage(sofaMessage);
        }
    }

    private PaymentRequest getPaymentRequestFromSofaMessage(final SofaMessage sofaMessage) {
        try {
            return SofaAdapters.get().txRequestFrom(sofaMessage.getPayload());
        } catch (IOException e) {
            LogUtil.e(getTag(), "Error while parsing payment request " + e);
        }
        return null;
    }

    private Payment getPaymentFromSofaMessage(final SofaMessage sofaMessage) {
        try {
            return SofaAdapters.get().paymentFrom(sofaMessage.getPayload());
        } catch (IOException e) {
            LogUtil.e(getTag(), "Error while parsing payment " + e);
        }
        return null;
    }

    private String getBodyFromSofaMessage(final SofaMessage sofaMessage) {
        try {
            return SofaAdapters.get().messageFrom(sofaMessage.getPayload()).getBody();
        } catch (final IOException ex) {
            LogUtil.e("ChatNotificationManager", "Error while parsing message " + ex);
        }
        return null;
    }

    public @SofaType.Type int getTypeOfLastMessage() {
        return this.lastMessage.getType();
    }

    public boolean isAccepted() {
        return this.isAccepted;
    }

    public ToshiNotification setIsAccepted(final boolean isAccepted) {
        this.isAccepted = isAccepted;
        return this;
    }

    /* package */ abstract String getUnacceptedText();
}
