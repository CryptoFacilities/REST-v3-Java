/*
Crypto Facilities Ltd REST API v3

Copyright (c) 2018 Crypto Facilities

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
package com.cryptofacilities.REST.v3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CfApiMethods {
	private String apiPath;
	private String apiPublicKey;
	private String apiPrivateKey;
	private int timeout;
	private boolean checkCertificate;
	private int nonce;
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	public CfApiMethods(String apiPath, String apiPublicKey, String apiPrivateKey, int timeout,
			boolean checkCertificate) {
		this.apiPath = apiPath;
		this.apiPublicKey = apiPublicKey;
		this.apiPrivateKey = apiPrivateKey;
		this.timeout = timeout;
		this.checkCertificate = checkCertificate;
		nonce = 0;
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public CfApiMethods(String apiPath, boolean checkCertificate) {
		this(apiPath, null, null, 10, checkCertificate);
	}

	// Signs a message
	private String signMessage(String endpoint, String nonce, String postData)
			throws NoSuchAlgorithmException, InvalidKeyException {
		// Step 1: concatenate postData, nonce + endpoint
		String message = postData + nonce + endpoint;

		// Step 2: hash the result of step 1 with SHA256
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(message.getBytes(StandardCharsets.UTF_8));

		// step 3: base64 decode apiPrivateKey
		byte[] secretDecoded = Base64.getDecoder().decode(apiPrivateKey);

		// step 4: use result of step 3 to hash the resultof step 2 with
		// HMAC-SHA512
		Mac hmacsha512 = Mac.getInstance("HmacSHA512");
		hmacsha512.init(new SecretKeySpec(secretDecoded, "HmacSHA512"));
		byte[] hash2 = hmacsha512.doFinal(hash);

		// step 5: base64 encode the result of step 4 and return
		return Base64.getEncoder().encodeToString(hash2);

	}

	// Returns a unique nonce
	private String createNonce() {
		nonce += 1;
		long timestamp = (new Date()).getTime();
		return timestamp + String.format("%04d", nonce);
	}

	// Sends an HTTP request
	private String makeRequest(String requestMethod, String endpoint, String postUrl, String postBody)
			throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException,
			InvalidKeyException {
		if (!checkCertificate) {
			TrustManager[] trustManager = new TrustManager[] { new X509TrustManager() {

				@Override
				public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

			} };
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, trustManager, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}

			});
		}

		// create request
		URL url = new URL(apiPath + endpoint + "?" + postUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(timeout * 1000);
		connection.setRequestMethod(requestMethod);
		connection.setDoOutput(true);
		connection.setUseCaches(false);

		// create authentication headers
		if (apiPublicKey != null && apiPrivateKey != null) {
			String nonce = createNonce();
			String postData = postUrl + postBody;
			String signature = signMessage(endpoint, nonce, postData);
			connection.setRequestProperty("APIKey", apiPublicKey);
			connection.setRequestProperty("Nonce", nonce);
			connection.setRequestProperty("Authent", signature);
		}

		if (requestMethod.equals("POST")) {
			OutputStream wr = connection.getOutputStream();
			wr.write(postBody.getBytes(StandardCharsets.UTF_8));
			wr.close();
		}

		InputStream responseStream = connection.getInputStream();
		BufferedReader bfReader = new BufferedReader(new InputStreamReader(responseStream));
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = bfReader.readLine()) != null) {
			builder.append(line);
			builder.append('\n');
		}
		bfReader.close();
		return builder.toString();

	}

	private String makeRequest(String requestMethod, String endpoint) throws MalformedURLException, IOException,
			KeyManagementException, NoSuchAlgorithmException, InvalidKeyException {
		return makeRequest(requestMethod, endpoint, "", "");
	}

	/*---------------------------Public Endpoints-----------------------------------------------*/
	// Returns instruments
	public String getInstruments() throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/accounts";
		return makeRequest("GET", endpoint);
	}
	
	// Returns key account information
	public String getAccounts() throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/accounts";
		return makeRequest("GET", endpoint);
	}

	// Returns market data for all instruments
	public String getTickers() throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/tickers";
		return makeRequest("GET", endpoint);
	}

	// Returns the entire order book for a futures
	public String getOrderBook(String symbol) throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/orderbook";
		String postUrl = "symbol=" + symbol;
		return makeRequest("GET", endpoint, postUrl, "");
	}

	// Returns historical data for futures and indices
	public String getHistory(String symbol, Date lastTime) throws KeyManagementException, InvalidKeyException,
			MalformedURLException, NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/history";
		String postUrl = "symbol=" + symbol + "&lastTime=" + df.format(lastTime);
		return makeRequest("GET", endpoint, postUrl, "");
	}

	// Returns historical data for futures and indices
	public String getHistory(String symbol) throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/history";
		String postUrl = "symbol=" + symbol;
		return makeRequest("GET", endpoint, postUrl, "");
	}

	/*---------------------------Private Endpoints-----------------------------------------------*/

	// Returns key account information
	public String getAccount() throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/account";
		return makeRequest("GET", endpoint);
	}

	// Places an order
	public String sendOrder(String orderType, String symbol, String side, BigDecimal size, BigDecimal limitPrice,
			BigDecimal stopPrice) throws KeyManagementException, InvalidKeyException, MalformedURLException,
					NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/sendorder";
		String postBody;
		if (orderType.equals("lmt")) {
			postBody = String.format("orderType=lmt&symbol=%1s&side=%2s&size=%3$f&limitPrice=%4$f", symbol, side, size,
					limitPrice);
		} else if (orderType.equals("stp")) {
			postBody = String.format("orderType=stp&symbol=%1$s&side=%2$s&size=%3$f&limitPrice=%4$f&stopPrice=%4$f", symbol,
					side, size, limitPrice, stopPrice);
		} else {
			postBody = "";
		}

		return makeRequest("POST", endpoint, "", postBody);
	}

    // Places an order
    public String editOrder(Map<String, String> params) throws KeyManagementException, InvalidKeyException,
            NoSuchAlgorithmException, IOException {
        String endpoint = "/api/v3/editorder";
        String postBody =
                params.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));

        return makeRequest("POST", endpoint, "", postBody);
    }

	// Cancels an order
	public String cancelOrder(String orderId) throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/cancelorder";
		String postBody = "order_id=" + orderId;
		return makeRequest("POST", endpoint, "", postBody);
	}

	// Places or cancels orders in batch
	public String sendBatchOrder(String jsonElement) throws KeyManagementException, InvalidKeyException,
			MalformedURLException, NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/batchorder";
		String postBody = "json=" + jsonElement;
		return makeRequest("POST", endpoint, "", postBody);
	}

	// Returns all open orders
	public String getOpenOrders() throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/openorders";
		return makeRequest("GET", endpoint);
	}

	// Returns filled orders
	public String getFills(Date lastFillTime) throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/fills";
		String postUrl = "lastFillTime=" + df.format(lastFillTime);
		return makeRequest("GET", endpoint, postUrl, "");
	}

	// Returns filled orders
	public String getFills() throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/fills";
		return makeRequest("GET", endpoint, "", "");
	}

	// Returns all open positions
	public String getOpenPositions() throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/openpositions";
		return makeRequest("GET", endpoint);
	}

	// Sends an xbt witdrawal request
	public String sendWithdrawal(String targetAddress, String currency, BigDecimal amount) throws KeyManagementException,
			InvalidKeyException, MalformedURLException, NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/withdrawal";
		String postBody = String.format("targetAddress=%1s&currency=%2s&amount=%3f", targetAddress, currency, amount);
		return makeRequest("POST", endpoint, "", postBody);
	}

	// Returns xbt transfers
	public String getTransfers(Date lastTransferTime) throws KeyManagementException, InvalidKeyException,
			MalformedURLException, NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/transfers";
		String postUrl = "lastTransferTime=" + df.format(lastTransferTime);
		return makeRequest("GET", endpoint, postUrl, "");
	}

	// Returns xbt transfers
	public String getTransfers() throws KeyManagementException, InvalidKeyException, MalformedURLException,
			NoSuchAlgorithmException, IOException {
		String endpoint = "/api/v3/transfers";
		return makeRequest("GET", endpoint);
	}

}
