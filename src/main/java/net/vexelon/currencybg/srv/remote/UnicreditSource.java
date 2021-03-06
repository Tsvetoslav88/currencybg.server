package net.vexelon.currencybg.srv.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import net.vexelon.currencybg.srv.db.models.CurrencyData;
import net.vexelon.currencybg.srv.db.models.Sources;
import net.vexelon.currencybg.srv.reports.Reporter;
import net.vexelon.currencybg.srv.utils.DateTimeUtils;

public class UnicreditSource extends AbstractSource {

	private static final Logger log = LoggerFactory.getLogger(UnicreditSource.class);
	private static final String TAG_NAME = UnicreditSource.class.getSimpleName();

	private static final String URL_SOURCE = "https://www.unicreditbulbank.bg/bg/valutni-kursove/";
	private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm";

	public UnicreditSource(Reporter reporter) {
		super(reporter);
	}

	public List<CurrencyData> getUnicreditRates(InputStream input) throws IOException, ParseException {
		List<CurrencyData> result = Lists.newArrayList();

		Document doc = Jsoup.parse(input, Charsets.UTF_8.name(), URL_SOURCE);

		try {
			// Parse date and time
			Element dateArrtibute = doc
			        .select("div.container > div.gray-background.exchnage-form-container > div.row > div.col-xs-12.col-sm-12 > form#index_table_form.form-inline.searchform > div.form-group.col-xs-12.col-sm-3.col-md-3 > div.controls > input[name=date]")
			        .first();
			String date = dateArrtibute.attr("value");

			Element timeArrtibute = doc
			        .select("div.container > div.gray-background.exchnage-form-container > div.row > div.col-xs-12.col-sm-12 > form#index_table_form.form-inline.searchform > div.form-group.col-xs-12.col-sm-3.col-md-3 > div.controls.clockpicker > input[name=time]")
			        .first();
			String time = timeArrtibute.attr("value");

			Date updateDate = DateTimeUtils.parseDate(date.replace("/", ".") + " " + time, DATE_FORMAT);

			// Parse data content
			Element content = doc
			        .select("div.container > div.index-currency-table > div.row > div.col-xs-12 > table.table--exchange.table--exchange--responsive > tbody")
			        .first();

			Elements children1 = content.children();

			int row = 0;
			for (Element child : children1) {
				CurrencyData currencyData = new CurrencyData();
				try {
					if (row > 0) {
						currencyData.setDate(updateDate);
						currencyData.setCode(child.child(0).text());
						currencyData.setBuy(child.child(2).text().replace(",", "."));
						currencyData.setSell(child.child(3).text().replace(",", "."));
						currencyData.setRatio(Integer.parseInt(child.child(1).text()));
						currencyData.setSource(Sources.UNICREDIT.getID());
						result.add(currencyData);
					}

				} catch (IndexOutOfBoundsException e) {
					log.warn("Failed on row='{}', Exception={}", row, e.getMessage());
					getReporter().write(TAG_NAME, "Could not process currency on row='{}'!", row + "");
				}

				row++;

			}

			return normalizeCurrencyData(result);
		} catch (RuntimeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void getRates(Callback callback) throws SourceException {
		try {
			doGet(URL_SOURCE, new HTTPCallback() {

				@Override
				public void onRequestFailed(Exception e) {
					getReporter().write(TAG_NAME, "Connection failure= {}", ExceptionUtils.getStackTrace(e));

					UnicreditSource.this.close();
					callback.onFailed(e);
				}

				@Override
				public void onRequestCompleted(HttpResponse response, boolean isCanceled) {
					List<CurrencyData> result = Lists.newArrayList();

					if (!isCanceled) {
						try {
							result = getUnicreditRates(response.getEntity().getContent());
						} catch (IOException | ParseException e) {
							log.error("Could not parse source data!", e);
							getReporter().write(TAG_NAME, "Parse failed= {}", ExceptionUtils.getStackTrace(e));
						}
					} else {
						log.warn("Request was canceled! No currencies were downloaded.");
						getReporter().write(TAG_NAME, "Request was canceled! No currencies were downloaded.");
					}
					UnicreditSource.this.close();
					callback.onCompleted(result);
				}
			});
		} catch (URISyntaxException e) {
			throw new SourceException("Invalid source url - " + URL_SOURCE, e);
		}

	}

	@Override
	public String getName() {
		return TAG_NAME;
	}

}
