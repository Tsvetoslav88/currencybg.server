package net.vexelon.currencybg.srv.remote;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import net.vexelon.currencybg.srv.db.models.CurrencyData;

public abstract class AbstractSource implements Source, Closeable {

	protected static final int DEFAULT_SOCKET_TIMEOUT = 10 * 1000;
	protected static final int DEFAULT_CONNECT_TIMEOUT = 10 * 1000;

	protected Callback callback;
	protected CloseableHttpAsyncClient client;

	public AbstractSource(Callback callback) {
		this.callback = callback;
	}

	@Override
	public void close() throws IOException {
		IOUtils.closeQuietly(client);
	}

	/**
	 * Creates an asynchronous HTTP client configuration with default timeouts.
	 * 
	 * @see #newHttpClient()
	 */
	protected static CloseableHttpAsyncClient newHttpAsyncClient() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
				.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT).build();
		return HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
	}

	protected CloseableHttpAsyncClient getClient() {
		if (client == null) {
			client = newHttpAsyncClient();
			client.start();
		}
		return client;
	}

	protected void doGet(URI uri, final ResponseCallback responseCallback) {
		HttpGet httpGet = new HttpGet(uri);
		getClient().execute(httpGet, new FutureCallback<HttpResponse>() {

			@Override
			public void failed(Exception e) {
				responseCallback.onReqFailed(e);
			}

			@Override
			public void completed(HttpResponse response) {
				responseCallback.onReqCompleted(response, false);
			}

			@Override
			public void cancelled() {
				responseCallback.onReqCompleted(null, false);
			}
		});
	}

	protected void doGet(String url, final ResponseCallback responseCallback) throws URISyntaxException {
		doGet(new URI(url), responseCallback);
	}

	public interface ResponseCallback {

		void onReqCompleted(final HttpResponse response, boolean isCanceled);

		void onReqFailed(Exception e);
	}

	public interface Callback {

		void onCompleted(List<CurrencyData> currencyDataList);

	}

}
