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

package com.toshi.view.activity

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.toshi.R
import com.toshi.crypto.util.TypeConverter
import com.toshi.crypto.util.isPaymentAddressValid
import com.toshi.extensions.startActivityForResult
import com.toshi.extensions.toast
import com.toshi.manager.model.PaymentTask
import com.toshi.model.network.Balance
import com.toshi.model.network.Token
import com.toshi.util.EthUtil
import com.toshi.util.PaymentType
import com.toshi.util.QrCodeHandler
import com.toshi.util.ScannerResultType
import com.toshi.view.fragment.PaymentConfirmationFragment
import com.toshi.viewModel.SendERC20TokenViewModel
import com.toshi.viewModel.ViewModelFactory.SendERC20TokenViewModelFactory
import kotlinx.android.synthetic.main.activity_send_erc20_token.*

class SendERC20TokenActivity : AppCompatActivity() {

    companion object {
        private const val PAYMENT_SCAN_REQUEST_CODE = 200
    }

    private lateinit var viewModel: SendERC20TokenViewModel
    private var token: Token? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_erc20_token)
        init()
    }

    private fun init() {
        initViewModel()
        initClickListeners()
        token = Token.getTokenFromIntent(intent)
        updateUi(token)
        initObservers()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(
                this,
                SendERC20TokenViewModelFactory(false)
        ).get(SendERC20TokenViewModel::class.java)
    }

    private fun initClickListeners() {
        closeButton.setOnClickListener { finish() }
        qrCodeBtn.setOnClickListener { startScanQrActivity() }
        paste.setOnClickListener { pasteToAddress() }
        max.setOnClickListener { setMaxAmount() }
        continueBtn.setOnClickListener { validateAddressAndShowPaymentConfirmation() }
    }

    private fun startScanQrActivity() = startActivityForResult<ScannerActivity>(PAYMENT_SCAN_REQUEST_CODE) {
        putExtra(ScannerActivity.SCANNER_RESULT_TYPE, ScannerResultType.PAYMENT_ADDRESS)
    }

    private fun pasteToAddress() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData == null || clipData.itemCount == 0) return
        val clipItem = clipData.getItemAt(0)
        if (clipItem == null || clipItem.text == null) return
        val textFromClipboard = clipItem.text.toString()
        toAddress.setText(textFromClipboard)
    }

    private fun setMaxAmount() {
        val token = token
        if (token != null) {
            val tokenValue = TypeConverter.formatHexString(token.value, token.decimals, "#.000000")
            toAmount.setText(tokenValue)
        } else toast(R.string.invalid_token)
    }

    private fun validateAddressAndShowPaymentConfirmation() {
        token?.let {
            val address = toAddress.text.toString()
            val transferValue = toAmount.text.toString()
            if (isPaymentAddressValid(address)) showPaymentConfirmation(it, transferValue, address)
            else toast(R.string.invalid_payment_address)
        } ?: toast(R.string.invalid_token)
    }

    private fun showPaymentConfirmation(token: Token, value: String, toAddress: String) {
        val dialog = PaymentConfirmationFragment.newInstanceERC20TokenPayment(
                toAddress,
                value,
                token.contractAddress,
                token.symbol,
                token.decimals,
                null,
                PaymentType.TYPE_SEND
        )
        dialog.setOnPaymentConfirmationApprovedListener { onPaymentApproved(it) }
        dialog.show(supportFragmentManager, PaymentConfirmationFragment.TAG)
    }

    private fun onPaymentApproved(paymentTask: PaymentTask) = viewModel.sendPayment(paymentTask)

    private fun updateUi(token: Token?) {
        if (token == null) {
            toast(R.string.invalid_token)
            return
        }
        renderToolbar(token)
        renderERC20TokenBalance(token)
        setAmountSuffix(token)
    }

    private fun setAmountSuffix(token: Token) = toAmount.setSuffix(token.symbol)

    private fun renderToolbar(token: Token) {
        toolbarTitle.text = getString(R.string.send_token, token.symbol)
    }

    private fun renderERC20TokenBalance(token: Token) {
        val tokenValue = TypeConverter.formatHexString(token.value, token.decimals, "#.000000")
        balance.text = getString(R.string.erc20_balance, token.symbol, tokenValue, token.symbol)
    }

    private fun initObservers() {
        viewModel.ethBalance.observe(this, Observer {
            if (it != null) renderEthBalance(it)
        })
    }

    private fun renderEthBalance(currentBalance: Balance) {
        val totalEthAmount = EthUtil.weiAmountToUserVisibleString(currentBalance.unconfirmedBalance)
        val ethAmountString = getString(R.string.eth_amount, totalEthAmount)
        val statusMessage = getString(R.string.your_balance_eth_fiat, ethAmountString, currentBalance.localBalance)
        balance.text = statusMessage
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultIntent)
        if (requestCode != PAYMENT_SCAN_REQUEST_CODE || resultCode != Activity.RESULT_OK || resultIntent == null) return
        val paymentAddress = resultIntent.getStringExtra(QrCodeHandler.ACTIVITY_RESULT)
        toAddress.setText(paymentAddress)
    }
}