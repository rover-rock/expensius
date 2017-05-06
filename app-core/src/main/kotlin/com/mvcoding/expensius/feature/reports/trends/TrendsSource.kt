/*
 * Copyright (C) 2017 Mantas Varnagiris.
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

package com.mvcoding.expensius.feature.reports.trends

import com.mvcoding.expensius.data.DataSource
import com.mvcoding.expensius.data.ParameterDataSource
import com.mvcoding.expensius.model.ExchangeRateCurrencies
import com.mvcoding.expensius.model.ReportSettings
import com.mvcoding.expensius.model.Transaction
import com.mvcoding.expensius.model.Trends
import rx.Observable
import java.math.BigDecimal

class TrendsSource(
        private val transactionsSource: DataSource<List<Transaction>>,
        private val reportSettingsSource: DataSource<ReportSettings>,
        private val exchangeRatesSource: ParameterDataSource<ExchangeRateCurrencies, BigDecimal>) : DataSource<Trends> {

    override fun data(): Observable<Trends> = Observable.never()//transactionsSource.data().map { it.allItems }
}