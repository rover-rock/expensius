/*
 * Copyright (C) 2015 Mantas Varnagiris.
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

package com.mvcoding.expensius.feature.transaction

import com.mvcoding.expensius.feature.currency.CurrenciesProvider
import com.mvcoding.expensius.model.CreateTransaction
import com.mvcoding.expensius.model.Currency
import com.mvcoding.expensius.model.ModelState
import com.mvcoding.expensius.model.ModelState.ARCHIVED
import com.mvcoding.expensius.model.ModelState.NONE
import com.mvcoding.expensius.model.Note
import com.mvcoding.expensius.model.NullModels.noTransactionId
import com.mvcoding.expensius.model.Tag
import com.mvcoding.expensius.model.Transaction
import com.mvcoding.expensius.model.TransactionState
import com.mvcoding.expensius.model.TransactionType
import com.mvcoding.expensius.service.AppUserService
import com.mvcoding.expensius.service.TransactionsWriteService
import com.mvcoding.mvp.Presenter
import rx.Observable
import rx.Observable.combineLatest
import java.math.BigDecimal

class TransactionPresenter(
        private var transaction: Transaction,
        private val transactionsWriteService: TransactionsWriteService,
        private val appUserService: AppUserService,
        private val currenciesProvider: CurrenciesProvider) : Presenter<TransactionPresenter.View>() {

    override fun onViewAttached(view: View) {
        super.onViewAttached(view)

        view.showArchiveEnabled(transaction.isExisting())
        view.showModelState(transaction.modelState)

        val transactionStates = view.transactionStateChanges().startWith(transaction.transactionState).doOnNext { view.showTransactionState(it) }
        val transactionTypes = view.transactionTypeChanges().startWith(transaction.transactionType).doOnNext { view.showTransactionType(it) }
        val timestamps = view.timestampChanges().startWith(transaction.timestamp).doOnNext { view.showTimestamp(it) }
        val currencies = view.currencyChangeRequests()
                .flatMap { currenciesProvider.currencies() }
                .flatMap { view.currencyChanges(it) }
                .startWith(transaction.currency)
                .doOnNext { view.showCurrency(it) }
                .withLatestFrom(appUserService.appUser(), { currency, appUser ->
                    view.showExchangeRateVisible(currency != appUser.settings.currency)
                    currency
                })
        val exchangeRates = view.exchangeRateChanges().startWith(transaction.exchangeRate).doOnNext { view.showExchangeRate(it) }
        val amounts = view.amountChanges().startWith(transaction.amount).doOnNext { view.showAmount(it) }
        val tags = view.tagsChanges().startWith(transaction.tags).doOnNext { view.showTags(it) }
        val notes = view.noteChanges().map { Note(it) }.startWith(transaction.note).doOnNext { view.showNote(it) }

        val transaction = combineLatest(transactionStates, transactionTypes, timestamps, currencies, exchangeRates, amounts, tags, notes, {
            transactionState, transactionType, timestamp, currency, exchangeRate, amount, tags, note ->
            transaction.copy(
                    transactionState = transactionState,
                    transactionType = transactionType,
                    timestamp = timestamp,
                    currency = currency,
                    exchangeRate = exchangeRate,
                    amount = amount,
                    tags = tags,
                    note = note)
        }).doOnNext { transaction = it }

        view.saveRequests()
                .withLatestFrom(transaction, { unit, transaction -> transaction })
                .switchMap { saveTransaction(it) }
                .subscribeUntilDetached { view.displayResult() }

        view.archiveToggles()
                .map { transactionWithToggledArchiveState() }
                .switchMap { transactionsWriteService.saveTransactions(setOf(it)) }
                .subscribeUntilDetached { view.displayResult() }
    }

    private fun saveTransaction(transaction: Transaction) =
            if (transaction.isExisting()) transactionsWriteService.saveTransactions(setOf(transaction))
            else transactionsWriteService.createTransactions(setOf(transaction.toCreateTransaction()))

    private fun Transaction.isExisting() = this.transactionId != noTransactionId
    private fun Transaction.toCreateTransaction() = CreateTransaction(transactionType, transactionState, timestamp, currency, exchangeRate, amount, tags, note)
    private fun transactionWithToggledArchiveState() = transaction.withModelState(if (transaction.modelState == NONE) ARCHIVED else NONE)

    interface View : Presenter.View {
        fun transactionStateChanges(): Observable<TransactionState>
        fun transactionTypeChanges(): Observable<TransactionType>
        fun timestampChanges(): Observable<Long>
        fun exchangeRateChanges(): Observable<BigDecimal>
        fun amountChanges(): Observable<BigDecimal>
        fun tagsChanges(): Observable<Set<Tag>>
        fun noteChanges(): Observable<String>
        fun archiveToggles(): Observable<Unit>
        fun saveRequests(): Observable<Unit>
        fun currencyChangeRequests(): Observable<Unit>
        fun currencyChanges(currencies: List<Currency>): Observable<Currency>

        fun showArchiveEnabled(archiveEnabled: Boolean)
        fun showModelState(modelState: ModelState)
        fun showTransactionState(transactionState: TransactionState)
        fun showTransactionType(transactionType: TransactionType)
        fun showTimestamp(timestamp: Long)
        fun showCurrency(currency: Currency)
        fun showExchangeRate(exchangeRate: BigDecimal)
        fun showExchangeRateVisible(visible: Boolean)
        fun showAmount(amount: BigDecimal)
        fun showTags(tags: Set<Tag>)
        fun showNote(note: Note)

        fun displayResult()
    }
}