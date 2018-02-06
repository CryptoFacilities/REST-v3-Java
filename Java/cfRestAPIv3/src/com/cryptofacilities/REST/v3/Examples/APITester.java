/*
Crypto Facilities Ltd REST API V3

Copyright (c) 2016 Crypto Facilities

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.cryptofacilities.REST.v3.Examples;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.cryptofacilities.REST.v3.CfApiMethods;

public class APITester {
	private static final String apiPath = "https://www.cryptofacilities.com/derivatives";
	private static final String apiPublicKey = "..."; //accessible on your Account page under Settings -> API Keys
	private static final String apiPrivateKey = "..."; //accessible on your Account page under Settings -> API Keys

	private static final int timeout = 10;
	private static final boolean checkCertificate = true; //when using the test environment, this must be set to "False"       

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws MalformedURLException, IOException, KeyManagementException, NoSuchAlgorithmException, InvalidKeyException{
		CfApiMethods methods;
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		String result, symbol, side, orderType; ;
		BigDecimal size, limitPrice, stopPrice;

		/*---------------------------Public Endpoints-----------------------------------------------*/
		methods = new CfApiMethods(apiPath, checkCertificate);

		//get instruments
		result = methods.getInstruments();
		System.out.println("getInstruments:\n" + result);

		//get tickers
		result = methods.getTickers();
		System.out.println("getTickers:\n" + result);

		//get orderbook
		symbol = "FI_XBTUSD_180316";
		result = methods.getOrderBook(symbol);
		System.out.println("getOrderBook:\n" + result);

		//get history
		symbol = "FI_XBTUSD_180316";
		calendar.set(2016, 1, 20,0,0,0);
		result = methods.getHistory(symbol, calendar.getTime());
		System.out.println("getHistory:\n" + result);

		/*----------------------------Private Endpoints----------------------------------------------*/
		methods = new CfApiMethods(apiPath,apiPublicKey,apiPrivateKey,timeout,checkCertificate);

		//get accounts
		result = methods.getAccounts();
		System.out.println("getAccounts:\n" + result);

		//send limit order
		orderType = "lmt";
		symbol = "FI_XBTUSD_180316";
		side = "buy";
		size = BigDecimal.ONE;
		limitPrice = BigDecimal.ONE;
		result = methods.sendOrder(orderType, symbol, side, size, limitPrice,null);
		System.out.println("sendOrder (limit):\n" + result);

		//send stop order
		orderType = "stp";
		symbol = "FI_XBTUSD_180316";
		side = "buy";
		size = BigDecimal.ONE;
		limitPrice = new BigDecimal(1.1);
		stopPrice = new BigDecimal(2.0);
		result = methods.sendOrder(orderType, symbol, side, size, limitPrice, stopPrice);
		System.out.println("sendOrder (stop):\n" + result);

		//cancel order
		String orderId = "ccdf6310-9a0d-4173-9efe-2ee5696830e1";
		result = methods.cancelOrder(orderId);
		System.out.println("cancelOrder:\n" + result);

		//batch order
		String jsonElement = "{"
				+ "\"batchOrder\":"
				+ "[ {"
				+ "\"order\": \"send\","
				+ "\"order_tag\": \"1\","
				+ "\"orderType\": \"lmt\","
				+ "\"symbol\": \"FI_XBTUSD_180316\","
				+ "\"side\": \"buy\","
				+ "\"size\": 1,"
				+ "\"limitPrice\": 1.00,"
				+ "},{"
				+ "\"order\": \"send\","
				+ "\"order_tag\": \"2\","
				+ "\"orderType\": \"stp\","
				+ "\"symbol\": \"FI_XBTUSD_180316\","
				+ "\"side\": \"buy\","
				+ "\"size\": 1,"
				+ "\"limitPrice\": 2.00,"
				+ "\"stopPrice\": 3.00, "
				+ "},{"
				+ "\"order\": \"cancel\","
				+ "\"order_id\": \"7c915a4a-7113-4845-a21f-42363829fce4\","
				+ "},{"
				+ "\"order\": \"cancel\","
				+ "\"order_id\": \"de0a7dd1-4571-4f94-9b33-9a571554ba5e\","
				+ "},],"
				+ "}";
		result = methods.sendBatchOrder(jsonElement);
		System.out.println("sendBatchOrder:\n" + result);


		//get open orders
		result = methods.getOpenOrders();
		System.out.println("getOpenOrders:\n" + result);

		//get fills
		Date lastFillTime = new Date(2016, 2, 1);
		result = methods.getFills(lastFillTime);
		System.out.println("getFills:\n" + result);

		//get open positions
		result = methods.getOpenPositions();
		System.out.println("getOpenPositions:\n" + result);

		//send xbt withdrawal request
		String targetAddress = "xxxxxxxxxxxxx";
		String currency = "xbt";
		BigDecimal amount = new BigDecimal(0.123);
		result = methods.sendWithdrawal(targetAddress, currency, amount);
		System.out.println("sendWithdrawal:\n" + result);

		//get xbt transfers
		Date lastTransferTime = new Date(2016, 2, 1);
		result = methods.getTransfers(lastTransferTime);
		System.out.println("getTransfers:\n" + result);
	}

}
