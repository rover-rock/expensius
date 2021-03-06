/*
 * Copyright (C) 2016 Mantas Varnagiris.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.mvcoding.expensius.feature.premium

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import com.mvcoding.expensius.R
import com.mvcoding.expensius.extension.inflate
import com.mvcoding.expensius.extension.snackbar
import com.mvcoding.expensius.feature.ActivityStarter
import com.mvcoding.expensius.feature.BaseActivity
import com.mvcoding.expensius.feature.BaseClickableAdapter
import com.mvcoding.expensius.feature.ClickableViewHolder
import com.mvcoding.expensius.feature.Error
import kotlinx.android.synthetic.main.activity_premium.*
import rx.Observable
import rx.subjects.PublishSubject

class PremiumActivity : BaseActivity(), PremiumPresenter.View {
    companion object {
        fun start(context: Context) = ActivityStarter(context, PremiumActivity::class).start()
    }

    private val REQUEST_BILLING = 1

    private val presenter by lazy { providePremiumPresenter() }
    private val billingFlow by lazy { provideBillingFlow() }
    private val adapter by lazy { Adapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        billingFlow.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        presenter.attach(this)
    }

    override fun onStop() {
        super.onStop()
        presenter.detach(this)
    }

    override fun showFreeUser() = subscriptionTextView.setText(R.string.long_user_is_using_free_version)
    override fun showPremiumUser() = subscriptionTextView.setText(R.string.long_user_is_using_premium_version)
    override fun refreshes() = swipeRefreshLayout.refreshes()
    override fun billingProductSelects(): Observable<BillingProduct> = adapter.itemPositionClicks().map { adapter.getItem(it) }
    override fun showBillingProducts(billingProducts: List<BillingProduct>) = adapter.set(billingProducts)
    override fun displayBuyProcess(productId: String) = billingFlow.startPurchase(this, REQUEST_BILLING, productId)
    override fun showLoading(): Unit = with(swipeRefreshLayout) { isRefreshing = true }
    override fun hideLoading(): Unit = with(swipeRefreshLayout) { isRefreshing = false }
    override fun showEmptyView(): Unit = with(emptyTextView) { visibility = VISIBLE }
    override fun hideEmptyView(): Unit = with(emptyTextView) { visibility = GONE }

    override fun showError(error: Error) {
        snackbar(R.string.error_refreshing_purchases, Snackbar.LENGTH_LONG).show()
    }

    private class Adapter : BaseClickableAdapter<BillingProduct, ClickableViewHolder<View>>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int, positionClickedSubject: PublishSubject<Int>) =
                ClickableViewHolder<View>(parent.inflate(R.layout.item_view_billing_product), positionClickedSubject)

        override fun onBindViewHolder(holder: ClickableViewHolder<View>, position: Int) {
            (holder.view.findViewById(R.id.titleTextView) as TextView).text = getItem(position).title
            (holder.view.findViewById(R.id.priceTextView) as TextView).text = getItem(position).price
        }
    }
}