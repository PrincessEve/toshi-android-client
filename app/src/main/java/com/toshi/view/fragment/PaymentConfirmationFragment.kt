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

package com.toshi.view.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.toshi.R
import com.toshi.extensions.getColor
import com.toshi.extensions.isVisible
import com.toshi.manager.model.ERC20TokenPaymentTask
import com.toshi.manager.model.ExternalPaymentTask
import com.toshi.manager.model.PaymentTask
import com.toshi.manager.model.ToshiPaymentTask
import com.toshi.manager.model.W3PaymentTask
import com.toshi.model.local.User
import com.toshi.model.network.Balance
import com.toshi.util.EthUtil
import com.toshi.util.ImageUtil
import com.toshi.util.LogUtil
import com.toshi.util.PaymentType
import com.toshi.view.fragment.DialogFragment.PaymentConfirmationType
import com.toshi.viewModel.PaymentConfirmationViewModel
import kotlinx.android.synthetic.main.fragment_payment_confirmation.*

class PaymentConfirmationFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: PaymentConfirmationViewModel
    private lateinit var onPaymentApprovedListener: (PaymentTask) -> Unit
    private var onPaymentCanceledListener: ((String?) -> Unit)? = null
    private var approvedPayment = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, inState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_payment_confirmation, container, false)
    }

    override fun onStart() {
        super.onStart()
        setHeight()
    }

    private fun setHeight() {
        val confirmationType = arguments.getInt(CONFIRMATION_TYPE)
        val isW3Payment = confirmationType == PaymentConfirmationType.WEB
        if (isW3Payment) return // Only set max height if the payment isn't a w3 payment

        dialog?.let {
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        view?.post {
            val parent = view?.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val bottomSheetBehavior = params.behavior as BottomSheetBehavior?
            bottomSheetBehavior?.peekHeight = view?.measuredHeight ?: 0
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) = init()

    private fun init() {
        initViewModel()
        initClickListeners()
        setTitle()
        initObservers()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(PaymentConfirmationViewModel::class.java)
        viewModel.init(arguments)
    }

    private fun initClickListeners() {
        closeButton.setOnClickListener { handlePaymentCanceled() }
        pay.setOnButtonClickListener { handlePaymentApproved() }
    }

    private fun handlePaymentCanceled() {
        approvedPayment = false
        dismiss()
    }

    private fun handlePaymentApproved() {
        val paymentTask = viewModel.paymentTask.value
        paymentTask?.let {
            approvedPayment = true
            onPaymentApprovedListener(paymentTask)
            dismiss()
        }
    }

    private fun setTitle() {
        val paymentType = viewModel.getPaymentType()
        val title = if (paymentType == PaymentType.TYPE_SEND) getString(R.string.payment_confirmation_title)
        else getString(R.string.payment_request_confirmation_title)
        toolbarTitle.text = title
    }

    private fun initObservers() {
        viewModel.isGasPriceLoading.observe(this, Observer {
            isLoading -> isLoading?.let { handleLoadingVisibility(it) }
        })
        viewModel.balance.observe(this, Observer {
            formattedBalance -> formattedBalance?.let { handleBalance(it) }
        })
        viewModel.paymentTask.observe(this, Observer {
            paymentTask -> paymentTask?.let { handlePaymentTask(it) }
        })
        viewModel.paymentTaskError.observe(this, Observer {
            paymentTaskError -> paymentTaskError?.let { handlePaymentTaskError() }
        })
    }

    private fun handleLoadingVisibility(isLoading: Boolean) {
        loadingText.isVisible(isLoading)
        if (isLoading) pay.startLoading()
        else pay.stopLoading()
    }

    private fun handleBalance(currentBalance: Balance) {
        renderBalance(currentBalance)
        compareTotalAmountAndBalance()
    }

    private fun renderBalance(balance: Balance) {
        val formattedBalance = balance.localBalance
        val statusMessage = getString(R.string.your_balance, formattedBalance)
        setStatusMessage(statusMessage)
    }

    private fun handlePaymentTask(paymentTask: PaymentTask) {
        renderRecipientInfo(paymentTask)
        renderPaymentInfo(paymentTask)
        compareTotalAmountAndBalance()
    }

    private fun renderRecipientInfo(paymentTask: PaymentTask) {
        when (paymentTask) {
            is W3PaymentTask -> renderDappInfo()
            is ToshiPaymentTask -> renderToshiUserInfo(paymentTask.user)
            is ExternalPaymentTask -> renderExternalInfo(paymentTask)
            is ERC20TokenPaymentTask -> renderERC20TokenInfo(paymentTask)
            else -> LogUtil.exception(javaClass, "Unhandled payment $paymentTask")
        }
    }

    private fun renderDappInfo() {
        dappWrapper.isVisible(true)
        dappHeader.text = viewModel.getDappTitle()
        dappUrl.text = viewModel.getDappUrl()
        dappFavicon.setImageBitmap(viewModel.getDappFavicon())
    }

    private fun renderToshiUserInfo(user: User) {
        recipientWrapper.isVisible(true)
        ImageUtil.load(user.avatar, avatar)
        displayName.text = user.displayName
    }

    private fun renderExternalInfo(paymentTask: PaymentTask) {
        externalWrapper.isVisible(true)
        externalAddress.text = paymentTask.payment.toAddress
    }

    private fun renderERC20TokenInfo(paymentTask: PaymentTask) {
        externalWrapper.isVisible(true)
        externalAddress.text = paymentTask.payment.toAddress
    }

    private fun renderPaymentInfo(paymentTask: PaymentTask) {
        when (paymentTask) {
            is ERC20TokenPaymentTask -> renderERC20TokenPaymentInfo(paymentTask)
            else -> renderETHPaymentInfo(paymentTask)
        }
    }

    private fun renderERC20TokenPaymentInfo(paymentTask: ERC20TokenPaymentTask) {
        ERC20PaymentInfoWrapper.isVisible(true)
        ERC20Amount.text = "${paymentTask.tokenValue} ${paymentTask.tokenSymbol}"
        val gasEthAmount = EthUtil.ethAmountToUserVisibleString(paymentTask.gasPrice.ethAmount)
        ERC20GasPrice.text = getString(R.string.eth_amount, gasEthAmount)
        ERC20GasPriceFiat.text = paymentTask.gasPrice.localAmount
    }

    private fun renderETHPaymentInfo(paymentTask: PaymentTask) {
        ethPaymentInfoWrapper.isVisible(true)
        amount.text = paymentTask.paymentAmount.localAmount
        gasPrice.text = paymentTask.gasPrice.localAmount
        total.text = paymentTask.totalAmount.localAmount
        val totalEthAmount = EthUtil.ethAmountToUserVisibleString(paymentTask.totalAmount.ethAmount)
        totalEth.text = getString(R.string.eth_amount, totalEthAmount)
    }

    private fun handlePaymentTaskError() {
        ethPaymentInfoWrapper.isVisible(false)
        pay.disablePayButton()
        setStatusMessage(getString(R.string.gas_price_error), R.color.error_color)
    }

    private fun compareTotalAmountAndBalance() {
        val currentBalance = viewModel.balance.value
        val totalAmount = viewModel.paymentTask.value
        if (currentBalance != null && totalAmount != null) {
            val currentBalanceEth = EthUtil.weiToEth(currentBalance.unconfirmedBalance)
            val totalAmountEth = totalAmount.totalAmount.ethAmount
            if (currentBalanceEth >= totalAmountEth) {
                pay.enablePayButton()
                return
            } else {
                renderInsufficientBalance()
            }
        }

        pay.disablePayButton()
    }

    private fun renderInsufficientBalance() {
        val insufficientBalance = viewModel.balance.value?.localBalance
        insufficientBalance?.let {
            val statusMessage = getString(R.string.insufficient_balance, it)
            setStatusMessage(statusMessage, R.color.error_color)
        }
    }

    private fun setStatusMessage(text: String, color: Int = R.color.textColorPrimary) {
        statusMessage.text = text
        statusMessage.setTextColor(getColor(color))
    }

    fun setOnPaymentConfirmationApprovedListener(listener: (PaymentTask) -> Unit): PaymentConfirmationFragment {
        this.onPaymentApprovedListener = listener
        return this
    }

    fun setOnPaymentConfirmationCanceledListener(listener: (String?) -> Unit): PaymentConfirmationFragment {
        this.onPaymentCanceledListener = listener
        return this
    }

    override fun onPause() {
        super.onPause()
        dismissAllowingStateLoss()
    }

    companion object {
        const val TAG = "PaymentConfirmationFragment"
        const val CALLBACK_ID = "callback_id"
        const val CONFIRMATION_TYPE = "confirmation_type"
        const val ETH_AMOUNT = "eth_amount"
        const val MEMO = "memo"
        const val PAYMENT_ADDRESS = "payment_address"
        const val PAYMENT_TYPE = "payment_type"
        const val TOSHI_ID = "toshi_id"
        const val UNSIGNED_TRANSACTION = "unsigned_transaction"
        const val DAPP_TITLE = "dapp_title"
        const val DAPP_URL = "dapp_url"
        const val DAPP_FAVICON = "dapp_favicon"
        const val TOKEN_ADDRESS = "token_address"
        const val TOKEN_SYMBOL = "token_symbol"
        const val TOKEN_DECIMALS = "token_decimals"

        fun newInstanceToshiPayment(toshiId: String,
                                    value: String,
                                    memo: String?,
                                    @PaymentType.Type paymentType: Int): PaymentConfirmationFragment {
            val bundle = Bundle().apply {
                putInt(CONFIRMATION_TYPE, PaymentConfirmationType.TOSHI)
                putString(TOSHI_ID, toshiId)
            }
            return newInstance(bundle, value, memo, paymentType)
        }

        fun newInstanceExternalPayment(paymentAddress: String,
                                       value: String,
                                       memo: String?,
                                       @PaymentType.Type paymentType: Int): PaymentConfirmationFragment {
            val bundle = Bundle().apply {
                putInt(CONFIRMATION_TYPE, PaymentConfirmationType.EXTERNAL)
                putString(PAYMENT_ADDRESS, paymentAddress)
            }
            return newInstance(bundle, value, memo, paymentType)
        }

        fun newInstanceERC20TokenPayment(toAddress: String,
                                         value: String,
                                         tokenAddress: String,
                                         tokenSymbol: String,
                                         tokenDecimals: Int,
                                         memo: String?,
                                         @PaymentType.Type paymentType: Int): PaymentConfirmationFragment {
            val bundle = Bundle().apply {
                putInt(CONFIRMATION_TYPE, PaymentConfirmationType.EXTERNAL)
                putString(PAYMENT_ADDRESS, toAddress)
                putString(TOKEN_ADDRESS, tokenAddress)
                putString(TOKEN_SYMBOL, tokenSymbol)
                putInt(TOKEN_DECIMALS, tokenDecimals)
            }
            return newInstance(bundle, value, memo, paymentType)
        }

        fun newInstanceWebPayment(unsignedTransaction: String,
                                  paymentAddress: String,
                                  value: String?,
                                  callbackId: String,
                                  memo: String?,
                                  url: String?,
                                  title: String?,
                                  favIcon: Bitmap?): PaymentConfirmationFragment {
            val bundle = Bundle().apply {
                putInt(CONFIRMATION_TYPE, PaymentConfirmationType.WEB)
                putString(UNSIGNED_TRANSACTION, unsignedTransaction)
                putString(PAYMENT_ADDRESS, paymentAddress)
                putString(CALLBACK_ID, callbackId)
                putString(DAPP_URL, url)
                putString(DAPP_TITLE, title)
                putParcelable(DAPP_FAVICON, favIcon)
            }
            return newInstance(bundle, value, memo, PaymentType.TYPE_SEND)
        }

        private fun newInstance(bundle: Bundle,
                                value: String?,
                                memo: String?,
                                @PaymentType.Type paymentType: Int): PaymentConfirmationFragment {
            bundle.apply {
                putString(ETH_AMOUNT, value)
                putString(MEMO, memo)
                putInt(PAYMENT_TYPE, paymentType)
            }
            val fragment = PaymentConfirmationFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        val callbackId = viewModel.getCallbackId()
        if (!approvedPayment) onPaymentCanceledListener?.invoke(callbackId)
        super.onDismiss(dialog)
    }
}