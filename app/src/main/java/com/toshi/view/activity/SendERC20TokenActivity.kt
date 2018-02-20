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
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.toshi.R
import com.toshi.crypto.util.TypeConverter
import com.toshi.crypto.util.isPaymentAddressValid
import com.toshi.extensions.isVisible
import com.toshi.extensions.startActivityForResult
import com.toshi.extensions.toast
import com.toshi.model.network.token.ERCToken
import com.toshi.util.EthUtil
import com.toshi.util.PaymentType
import com.toshi.util.QrCodeHandler
import com.toshi.util.ScannerResultType
import com.toshi.view.adapter.listeners.TextChangedListener
import com.toshi.view.fragment.PaymentConfirmationFragment
import com.toshi.viewModel.SendERC20TokenViewModel
import com.toshi.viewModel.ViewModelFactory.SendERC20TokenViewModelFactory
import kotlinx.android.synthetic.main.activity_send_erc20_token.*

class SendERC20TokenActivity : AppCompatActivity() {

    companion object {
        private const val PAYMENT_SCAN_REQUEST_CODE = 200
    }

    private lateinit var viewModel: SendERC20TokenViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_erc20_token)
        init()
    }

    private fun init() {
        initViewModel()
        initClickListeners()
        updateUi()
        initTextListeners()
    }

    private fun initViewModel() {
        val token = ERCToken.getTokenFromIntent(intent)
        if (token == null) {
            toast(R.string.invalid_token)
            finish()
            return
        }
        viewModel = ViewModelProviders.of(
                this,
                SendERC20TokenViewModelFactory(token)
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
        val tokenValue = viewModel.getMaxAmount()
        toAmount.setText(tokenValue)
    }

    private fun validateAddressAndShowPaymentConfirmation() {
        val token = viewModel.token
        addressError.isVisible(false)
        val address = toAddress.text.toString()
        val transferValue = toAmount.text.toString()
        showPaymentConfirmation(token, transferValue, address)
    }

    private fun showPaymentConfirmation(ERCToken: ERCToken, value: String, toAddress: String) {
        val dialog = PaymentConfirmationFragment.newInstanceERC20TokenPayment(
                toAddress,
                value,
                ERCToken.contractAddress,
                ERCToken.symbol,
                ERCToken.decimals,
                null,
                PaymentType.TYPE_SEND
        )
        dialog.show(supportFragmentManager, PaymentConfirmationFragment.TAG)
        dialog.setOnPaymentConfirmationFinishedListener { finish() }
    }

    private fun updateUi() {
        val token = viewModel.token
        renderToolbar(token)
        renderERC20TokenBalance(token)
        setAmountSuffix(token)
    }

    private fun setAmountSuffix(token: ERCToken) = toAmount.setSuffix(token.symbol)

    private fun renderToolbar(ERCToken: ERCToken) {
        toolbarTitle.text = getString(R.string.send_token, ERCToken.symbol)
    }

    private fun renderERC20TokenBalance(ERCToken: ERCToken) {
        val tokenValue = TypeConverter.formatHexString(ERCToken.value, ERCToken.decimals, EthUtil.DECIMAL_FORMAT)
        balance.text = getString(R.string.erc20_balance, ERCToken.symbol, tokenValue, ERCToken.symbol)
    }

    private fun initTextListeners() {
        toAmount.addTextChangedListener(object : TextChangedListener() {
            override fun onTextChanged(amountInput: CharSequence?, start: Int, before: Int, count: Int) {
                validateAmount(amountInput.toString())
                enableOrDisableContinueButton()
            }
        })
        toAddress.addTextChangedListener(object : TextChangedListener() {
            override fun onTextChanged(addressInput: CharSequence?, start: Int, before: Int, count: Int) {
                validateAddress(addressInput.toString())
                enableOrDisableContinueButton()
            }
        })
    }

    private fun validateAmount(amountInput: String) {
        amountError.isVisible(false)
        if (amountInput.isEmpty()) return
        val hasEnoughBalance = viewModel.hasEnoughBalance(amountInput)
        val isAmountValid = viewModel.isAmountValid(amountInput)
        if (!hasEnoughBalance) showAmountError()
        if (!isAmountValid) showInvalidAmountError()
    }

    private fun validateAddress(addressInput: String) {
        addressError.isVisible(false)
        if (addressInput.isEmpty()) return
        val isAddressValid = isPaymentAddressValid(addressInput)
        if (!isAddressValid) showAddressError()
    }

    private fun enableOrDisableContinueButton() {
        val amount = toAmount.text.toString()
        val address = toAddress.text.toString()
        val isAmountValid = viewModel.isAmountValid(amount) && viewModel.hasEnoughBalance(amount)
        val isAddressValid = isPaymentAddressValid(address)
        if (isAmountValid && isAddressValid) enableContinueButton()
        else disableContinueButton()
    }

    private fun showInvalidAmountError() {
        amountError.isVisible(true)
        amountError.text = getString(R.string.invalid_format)
    }

    private fun showAmountError() {
        val token = viewModel.token
        val balanceAmount = TypeConverter.formatHexString(token.value, token.decimals, EthUtil.DECIMAL_FORMAT)
        amountError.isVisible(true)
        amountError.text = getString(R.string.insufficient_balance, balanceAmount)
    }

    private fun showAddressError() {
        addressError.isVisible(true)
        addressError.text = getString(R.string.invalid_payment_address)
    }

    private fun enableContinueButton() {
        continueBtn.isEnabled = true
        continueBtn.setBackgroundResource(R.drawable.background_with_radius_primary_color)
    }

    private fun disableContinueButton() {
        continueBtn.isEnabled = true
        continueBtn.setBackgroundResource(R.drawable.background_with_radius_disabled)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultIntent)
        if (requestCode != PAYMENT_SCAN_REQUEST_CODE || resultCode != Activity.RESULT_OK || resultIntent == null) return
        val paymentAddress = resultIntent.getStringExtra(QrCodeHandler.ACTIVITY_RESULT)
        toAddress.setText(paymentAddress)
    }
}