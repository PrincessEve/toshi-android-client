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
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.toshi.R
import com.toshi.extensions.addHorizontalLineDivider
import com.toshi.extensions.startActivity
import com.toshi.model.network.ERC20Token
import com.toshi.view.activity.DepositActivity
import com.toshi.view.adapter.TokenAdapter
import com.toshi.view.adapter.viewholder.TokenType
import com.toshi.viewModel.TokenViewModel
import kotlinx.android.synthetic.main.fragment_token.*

class ERC20Fragment : Fragment() {

    private lateinit var viewModel: TokenViewModel
    private lateinit var tokenAdapter: TokenAdapter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_token, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) = init()

    private fun init() {
        initViewModel()
        initAdapter()
        initObservers()
        initClickListeners()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(activity).get(TokenViewModel::class.java)
    }

    private fun initAdapter() {
        tokenAdapter = TokenAdapter(TokenType.ERC20Token())
        tokens.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = tokenAdapter
            addHorizontalLineDivider()
        }
    }

    private fun initObservers() {
        viewModel.erc20Tokens.observe(this, Observer {
            if (it != null) showTokensOrEmptyState(it)
        })
    }

    private fun showTokensOrEmptyState(tokens: List<ERC20Token>) {
        if (tokens.isNotEmpty()) showAndAddTokens(tokens)
        else showEmptyStateView()
    }

    private fun showAndAddTokens(tokenList: List<ERC20Token>) {
        tokens.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        tokenAdapter.addTokens(tokenList)
    }

    private fun showEmptyStateView() {
        emptyState.visibility = View.VISIBLE
        tokens.visibility = View.GONE
        emptyStateTitle.text = getString(R.string.empty_state_tokens)
    }

    private fun initClickListeners() {
        shareWalletAddress.setOnClickListener { startActivity<DepositActivity>() }
    }

    override fun onStart() {
        super.onStart()
        viewModel.fetchERC20Tokens()
    }
}