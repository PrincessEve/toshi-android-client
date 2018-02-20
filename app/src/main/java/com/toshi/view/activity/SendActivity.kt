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
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.toshi.BuildConfig
import com.toshi.R
import com.toshi.crypto.util.isPaymentAddressValid
import com.toshi.extensions.isVisible
import com.toshi.extensions.startActivityForResult
import com.toshi.extensions.toast
import com.toshi.model.local.Networks
import com.toshi.util.BuildTypes
import com.toshi.util.PaymentType
import com.toshi.util.QrCodeHandler
import com.toshi.util.ScannerResultType
import com.toshi.view.adapter.listeners.TextChangedListener
import com.toshi.view.fragment.PaymentConfirmationFragment
import com.toshi.viewModel.SendViewModel
import kotlinx.android.synthetic.main.activity_send.*

class SendActivity : AppCompatActivity() {

    companion object {
        const val INTENT_EXTRA__ETH_AMOUNT = "eth_amount"
        private const val PAYMENT_SCAN_REQUEST_CODE = 200
    }

    private lateinit var viewModel: SendViewModel

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        setContentView(R.layout.activity_send)
        init()
    }

    private fun init() {
        initViewModel()
        generateAmount()
        initNetworkView()
        initUiListeners()
        initObservers()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)
    }

    private fun generateAmount() {
        val encodedEthAmount = getEncodedEthAmountFromIntent()
        encodedEthAmount
                ?.let { viewModel.generateAmount(it) }
                ?: toast(R.string.invalid_eth_amount)
    }

    private fun initNetworkView() {
        val showNetwork = BuildConfig.BUILD_TYPE == BuildTypes.DEBUG
        networkView.isVisible(showNetwork)
        if (showNetwork) {
            val network = Networks.getInstance().currentNetwork
            networkView.text = network.name
        }
    }

    private fun initUiListeners() {
        scan.setOnClickListener { startScanQrActivity() }
        sendButton.setOnClickListener { handleSendPaymentClicked() }
        closeButton.setOnClickListener { finish() }
        recipientAddress.addTextChangedListener(onTextChangedListener)
    }

    private fun startScanQrActivity() = startActivityForResult<ScannerActivity>(PAYMENT_SCAN_REQUEST_CODE) {
        putExtra(ScannerActivity.SCANNER_RESULT_TYPE, ScannerResultType.PAYMENT_ADDRESS)
    }

    private fun handleSendPaymentClicked() {
        val paymentAddress = getRecipientAddress()
        val isPaymentValid = isPaymentAddressValid(paymentAddress)
        if (!isPaymentValid) {
            toast(R.string.invalid_payment_address)
            return
        }

        val encodedEthAmount = getEncodedEthAmountFromIntent()
        encodedEthAmount
                ?.let { showPaymentConfirmationDialog(it) }
                ?: toast(R.string.invalid_eth_amount)
    }

    private val onTextChangedListener = object : TextChangedListener() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            sendButton.isEnabled = s?.let { it.length > 16 } ?: false
        }
    }

    private fun showPaymentConfirmationDialog(encodedEthAmount: String) {
        val dialog = PaymentConfirmationFragment.newInstanceExternalPayment(
                getRecipientAddress(),
                encodedEthAmount,
                null,
                PaymentType.TYPE_SEND
        )
        dialog.show(supportFragmentManager, PaymentConfirmationFragment.TAG)
        dialog.setOnPaymentConfirmationFinishedListener { finish() }
    }

    private fun getRecipientAddress(): String {
        val userInput = recipientAddress.text.toString()
        return if (userInput.contains(":")) userInput.split(":")[1] else userInput
    }

    private fun getEncodedEthAmountFromIntent() = intent.getStringExtra(AmountActivity.INTENT_EXTRA__ETH_AMOUNT)

    private fun initObservers() {
        viewModel.localAmount.observe(this, Observer {
            localAmount -> localAmount?.let { renderAmount(it) }
        })
    }

    private fun renderAmount(localAmount: String) {
        val usdEth = getString(R.string.local_amount, localAmount)
        toolbarTitle.text = usdEth
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultIntent)
        if (requestCode != PAYMENT_SCAN_REQUEST_CODE || resultCode != Activity.RESULT_OK) return
        resultIntent?.let {
            val paymentAddress = it.getStringExtra(QrCodeHandler.ACTIVITY_RESULT)
            recipientAddress.setText(paymentAddress)
        }
    }
}