package com.alexitc.coinalerts.data.anorm.dao

import java.sql.Connection

import anorm._
import com.alexitc.coinalerts.data.anorm.parsers.ExchangeCurrencyParsers
import com.alexitc.coinalerts.models._

class ExchangeCurrencyPostgresDAO {

  import ExchangeCurrencyParsers._

  def create(exchange: Exchange,
      market: Market,
      currency: Currency)(
      implicit conn: Connection): Option[ExchangeCurrency] = {

    SQL(
      """
        |INSERT INTO currencies
        |  (exchange, market, currency)
        |VALUES
        |  ({exchange}, {market}, {currency})
        |ON CONFLICT DO NOTHING
        |RETURNING currency_id, exchange, market, currency
      """.stripMargin
    ).on(
      "exchange" -> exchange.string,
      "market" -> market.string,
      "currency" -> currency.string
    ).as(parseExchangeCurrency.singleOpt)
  }

  def getBy(
      exchangeCurrencyId: ExchangeCurrencyId)(
      implicit conn: Connection): Option[ExchangeCurrency] = {

    SQL(
      """
        |SELECT currency_id, exchange, market, currency
        |FROM currencies
        |WHERE currency_id = {currency_id}
      """.stripMargin
    ).on(
      "currency_id" -> exchangeCurrencyId.int
    ).as(parseExchangeCurrency.singleOpt)
  }

  def getBy(
      exchange: Exchange,
      market: Market,
      currency: Currency)(
      implicit conn: Connection): Option[ExchangeCurrency] = {

    SQL(
      """
        |SELECT currency_id, exchange, market, currency
        |FROM currencies
        |WHERE exchange = {exchange} AND
        |      market = {market} AND
        |      currency = {currency} AND
        |      deleted_on IS NULL
      """.stripMargin
    ).on(
      "exchange" -> exchange.string,
      "market" -> market.string,
      "currency" -> currency.string
    ).as(parseExchangeCurrency.singleOpt)
  }

  def getBy(
      exchange: Exchange,
      market: Market)(
      implicit conn: Connection): List[ExchangeCurrency] = {

    SQL(
      """
        |SELECT currency_id, exchange, market, currency
        |FROM currencies
        |WHERE exchange = {exchange} AND
        |      market = {market}
      """.stripMargin
    ).on(
      "exchange" -> exchange.string,
      "market" -> market.string,
    ).as(parseExchangeCurrency.*)
  }

  def getMarkets(exchange: Exchange)(implicit conn: Connection): List[Market] = {
    SQL(
      """
        |SELECT DISTINCT market
        |FROM currencies
        |WHERE exchange = {exchange}
      """.stripMargin
    ).on(
      "exchange" -> exchange.string
    ).as(parseMarket.*)
  }

  /**
   * The total amount of currencies should not be too big to retrieve
   * them all at once, in case the number gets too big, we can get
   * all currencies for a exchange.
   */
  def getAll(implicit conn: Connection): List[ExchangeCurrency] = {
    SQL(
      """
        |SELECT currency_id, exchange, market, currency
        |FROM currencies
      """.stripMargin
    ).as(parseExchangeCurrency.*)
  }
}
