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

package com.toshi.view.adapter;


import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.toshi.R;
import com.toshi.model.local.Conversation;
import com.toshi.model.local.ConversationDividerItem;
import com.toshi.model.local.ConversationItem;
import com.toshi.model.local.ConversationRequestsItem;
import com.toshi.model.local.LocalStatusMessage;
import com.toshi.model.local.User;
import com.toshi.model.sofa.Message;
import com.toshi.model.sofa.payment.Payment;
import com.toshi.model.sofa.PaymentRequest;
import com.toshi.model.sofa.SofaAdapters;
import com.toshi.model.sofa.SofaMessage;
import com.toshi.model.sofa.SofaType;
import com.toshi.util.LogUtil;
import com.toshi.view.BaseApplication;
import com.toshi.view.adapter.listeners.OnItemClickListener;
import com.toshi.view.adapter.listeners.OnUpdateListener;
import com.toshi.view.adapter.viewholder.ConversationDividerViewHolder;
import com.toshi.view.adapter.viewholder.ConversationRequestsViewHolder;
import com.toshi.view.adapter.viewholder.ThreadViewHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.toshi.model.local.ConversationItemType.CONVERSATION;
import static com.toshi.model.local.ConversationItemType.DIVIDER;
import static com.toshi.model.local.ConversationItemType.REQUESTS;

public class RecentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int REQUESTS_POSITION = 0;
    private static final int DIVIDER_POSITION = 1;
    private static final int FIRST_CONVERSATION_POSITION = 2;

    private final ArrayList<Conversation> conversationsToDelete;
    private final List<ConversationItem> conversations;
    private final List<Conversation> unacceptedConversations;

    public OnItemClickListener<Conversation> onItemClickListener;
    public OnItemClickListener<Conversation> onItemLongClickListener;
    public OnUpdateListener onRequestsClickListener;

    public RecentAdapter() {
        this.conversations = new ArrayList<>(0);
        this.unacceptedConversations = new ArrayList<>(0);
        this.conversationsToDelete = new ArrayList<>();
    }

    @Override
    public int getItemViewType(final int position) {
        if (this.unacceptedConversations.size() == 0) {
            return CONVERSATION.getValue();
        } else {
            if (position == REQUESTS_POSITION) return REQUESTS.getValue();
            else if (position == DIVIDER_POSITION) return DIVIDER.getValue();
            else return CONVERSATION.getValue();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == REQUESTS.getValue()) {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__conversation_requests, parent, false);
            return new ConversationRequestsViewHolder(itemView);
        } else if (viewType == DIVIDER.getValue()) {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__conversation_divider, parent, false);
            return new ConversationDividerViewHolder(itemView);
        } else {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__recent, parent, false);
            return new ThreadViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final ConversationItem conversationItem = this.conversations.get(position);
        final int viewType = holder.getItemViewType();

        if (viewType == REQUESTS.getValue()) {
            final ConversationRequestsViewHolder viewHolder = (ConversationRequestsViewHolder) holder;
            viewHolder.setNumberOfConversationRequests(this.unacceptedConversations.size())
                    .setOnItemClickListener(this.onRequestsClickListener)
                    .loadAvatar(this.unacceptedConversations);
        } else if (viewType == CONVERSATION.getValue()) {
            final ThreadViewHolder viewHolder = (ThreadViewHolder) holder;
            final Conversation conversation = (Conversation) conversationItem;
            final String formattedLatestMessage = formatLastMessage(conversation.getLatestMessage());
            viewHolder.setThread(conversation);
            viewHolder.setLatestMessage(formattedLatestMessage);
            viewHolder.setOnItemClickListener(conversation, this.onItemClickListener);
            viewHolder.setOnItemLongClickListener(conversation, this.onItemLongClickListener);
        }
    }

    public void setConversations(final List<Conversation> conversations) {
        if (conversations.isEmpty()) return;
        this.conversations.clear();
        for (final Conversation conversation : conversations) {
            if (!conversation.isRecipientInvalid()) this.conversations.add(conversation);
        }
        addRequestsViewAndDivider();
        notifyDataSetChanged();
    }

    public void setUnacceptedConversations(final List<Conversation> unacceptedConversations) {
        if (unacceptedConversations.isEmpty()) {
            removeRequestsViewAndDivider();
            this.unacceptedConversations.clear();
            return;
        }
        this.unacceptedConversations.clear();
        this.unacceptedConversations.addAll(unacceptedConversations);
        addRequestsViewAndDivider();
        notifyDataSetChanged();
    }

    private void removeRequestsViewAndDivider() {
        if (isConversationRequestsViewAdded()) {
            conversations.remove(DIVIDER_POSITION);
            conversations.remove(REQUESTS_POSITION);
        }
        notifyDataSetChanged();
    }

    public void updateAcceptedConversation(final Conversation conversation) {
        final int position = this.conversations.indexOf(conversation);
        if (position == -1) {
            final int firstPosition = isConversationRequestsViewAdded()
                    ? FIRST_CONVERSATION_POSITION
                    : 0;
            this.conversations.add(firstPosition, conversation);
            notifyItemInserted(firstPosition);
            return;
        }

        this.conversations.set(position, conversation);
        notifyItemChanged(position);
    }

    public void updateUnacceptedConversation(final Conversation conversation) {
        if (this.unacceptedConversations.contains(conversation)) {
            final int index = this.unacceptedConversations.indexOf(conversation);
            this.unacceptedConversations.set(index, conversation);
            notifyItemChanged(REQUESTS_POSITION);
        } else {
            this.unacceptedConversations.add(conversation);
            addRequestsViewAndDivider();
            notifyDataSetChanged();
        }
    }

    private void addRequestsViewAndDivider() {
        if (!this.unacceptedConversations.isEmpty() && !isConversationRequestsViewAdded()) {
            this.conversations.add(REQUESTS_POSITION, new ConversationRequestsItem());
            this.conversations.add(DIVIDER_POSITION, new ConversationDividerItem());
        }
    }

    private boolean isConversationRequestsViewAdded() {
        return this.conversations.size() >= FIRST_CONVERSATION_POSITION
                && this.conversations.get(REQUESTS_POSITION) instanceof ConversationRequestsItem
                && this.conversations.get(DIVIDER_POSITION) instanceof ConversationDividerItem;
    }

    public void removeItem(final Conversation conversation, final RecyclerView parentView) {
        final int index = this.conversations.indexOf(conversation);
        removeItemAtWithUndo(index, parentView);
    }

    public void removeItemAtWithUndo(final int position, final RecyclerView parentView) {
        if (this.conversations.get(position) instanceof Conversation) {
            final Conversation removedConversation = (Conversation) this.conversations.get(position);
            final Snackbar snackbar = generateSnackbar(parentView);
            snackbar.setAction(
                    R.string.undo,
                    handleUndoRemove(position, parentView, removedConversation)
            ).show();
            this.conversations.remove(position);
            notifyItemRemoved(position);
            conversationsToDelete.add(removedConversation);
        }
    }

    @NonNull
    private Snackbar generateSnackbar(final RecyclerView parentView) {
        final Snackbar snackbar = Snackbar
                .make(parentView, R.string.conversation_deleted, Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(BaseApplication.get(), R.color.colorAccent));

        final View snackbarView = snackbar.getView();
        final TextView snackbarTextView = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setTextColor(ContextCompat.getColor(BaseApplication.get(), R.color.textColorContrast));
        return snackbar;
    }

    @NonNull
    private View.OnClickListener handleUndoRemove(final int position, final RecyclerView parentView, final Conversation removedConversation) {
        return view -> {
            this.conversations.add(position, removedConversation);
            notifyItemInserted(position);
            parentView.scrollToPosition(position);
            conversationsToDelete.remove(removedConversation);
        };
    }

    private String formatLastMessage(final SofaMessage sofaMessage) {
        if (sofaMessage == null) return "";
        final User localUser = getCurrentLocalUser();
        final boolean sentByLocal = sofaMessage.isSentBy(localUser);

        try {
            switch (sofaMessage.getType()) {
                case SofaType.PLAIN_TEXT: {
                    final Message message = SofaAdapters.get().messageFrom(sofaMessage.getPayload());
                    return message.toUserVisibleString(sentByLocal, sofaMessage.hasAttachment());
                }
                case SofaType.PAYMENT: {
                    final Payment payment = SofaAdapters.get().paymentFrom(sofaMessage.getPayload());
                    return payment.toUserVisibleString(sentByLocal, sofaMessage.getSendState());
                }
                case SofaType.PAYMENT_REQUEST: {
                    final PaymentRequest request = SofaAdapters.get().txRequestFrom(sofaMessage.getPayload());
                    return request.toUserVisibleString(sentByLocal, sofaMessage.getSendState());
                }
                case SofaType.LOCAL_STATUS_MESSAGE: {
                    final LocalStatusMessage localStatusMessage =
                            SofaAdapters.get().localStatusMessageRequestFrom(sofaMessage.getPayload());
                    final User sender = localStatusMessage.getSender();
                    final boolean isSenderLocalUser = (localUser != null && sender != null)
                            && localUser.getToshiId().equals(sender.getToshiId());
                    return localStatusMessage.loadString(isSenderLocalUser);
                }
                case SofaType.COMMAND_REQUEST:
                case SofaType.INIT_REQUEST:
                case SofaType.INIT:
                case SofaType.TIMESTAMP:
                case SofaType.UNKNOWN:
                default:
                    return "";
            }
        } catch (final IOException ex) {
            LogUtil.error(getClass(), "Error parsing SofaMessage. " + ex);
        }

        return "";
    }

    private User getCurrentLocalUser() {
        // Yes, this blocks. But realistically, a value should be always ready for returning.
        return BaseApplication
                .get()
                .getUserManager()
                .getCurrentUser()
                .toBlocking()
                .value();
    }

    public void doDelete() {
        for (final Conversation conversationToDelete : conversationsToDelete) {
            BaseApplication
                    .get()
                    .getSofaMessageManager()
                    .deleteConversation(conversationToDelete)
                    .subscribe(
                            () -> {},
                            t -> LogUtil.e(getClass(), "Unable to delete conversation")
                    );
        }
    }

    public boolean isAcceptedConversationsEmpty() {
        return isConversationRequestsViewAdded() && this.conversations.size() == FIRST_CONVERSATION_POSITION;
    }

    public boolean isUnacceptedConversationsEmpty() {
        return this.unacceptedConversations.isEmpty();
    }

    @Override
    public int getItemCount() {
        return this.conversations.size();
    }
}
