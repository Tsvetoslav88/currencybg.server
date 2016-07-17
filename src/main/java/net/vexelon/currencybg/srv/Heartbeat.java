package net.vexelon.currencybg.srv;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.vexelon.currencybg.srv.db.DataSource;
import net.vexelon.currencybg.srv.db.DataSourceException;
import net.vexelon.currencybg.srv.db.DataSourceInterface;
import net.vexelon.currencybg.srv.db.models.CurrencyData;
import net.vexelon.currencybg.srv.db.models.CurrencySource;
import net.vexelon.currencybg.srv.db.models.SourceUpdateRestrictions;
import net.vexelon.currencybg.srv.db.models.Sources;
import net.vexelon.currencybg.srv.remote.Source;
import net.vexelon.currencybg.srv.remote.SourceException;
import net.vexelon.currencybg.srv.reports.TelegramReporter;
import net.vexelon.currencybg.srv.utils.DateTimeUtils;

/**
 * Fetches currencies from remote server and imports them into the database.
 *
 */
public class Heartbeat implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Heartbeat.class);

	private boolean isUpdateGo(SourceUpdateRestrictions updateRestrictions) {
		if (!updateRestrictions.isEmpty()) {
			if (log.isTraceEnabled()) {
				log.trace("Source Update Restrictions: {}", updateRestrictions.toString());
			}

			try {
				DateTimeUtils dateTimeUtils = new DateTimeUtils(
						TimeZone.getTimeZone(GlobalConfig.INSTANCE.getServerTimeZone()));
				Date today = new Date();
				Calendar calToday = dateTimeUtils.getCal(today);

				if (dateTimeUtils.isWeekend(today)) {
					/*
					 * Weekends
					 */
					if (!updateRestrictions.isEnabledOnWeekends()) {
						return false;
					} else if (dateTimeUtils.isSunday(today) && !updateRestrictions.isEnabledOnSunday()) {
						return false;
					}

					if (calToday.before(updateRestrictions.getWeekendsNotBeforeCalendar())
							|| calToday.after(updateRestrictions.getWeekendsNotAfterCalendar())) {
						return false;
					}

				} else if (dateTimeUtils.isWeekday(today)) {
					/*
					 * Week days
					 */
					if (calToday.before(updateRestrictions.getWeekdaysNotBeforeCalendar())
							|| calToday.after(updateRestrictions.getWeekdaysNotAfterCalendar())) {
						return false;
					}
				}
			} catch (ParseException e) {
				log.warn("Incorrect update restrictions format!", e);
				// do not update, if time restrictions could not be parsed!
				return false;
			}
		}

		// all OK!
		return true;
	}

	@Override
	public void run() {
		log.info("Downloading rates from sources ...");
		try {

			try (final DataSourceInterface dataSource = new DataSource()) {
				/*
				 * Fetch all (active) sources from database
				 */
				dataSource.connect();
				List<CurrencySource> allSources = dataSource.getAllSources(true);

				/*
				 * Fetch currencies for every active source
				 */
				Calendar nowCalendar = Calendar.getInstance();
				for (CurrencySource currencySource : allSources) {

					// checks if it is time to update this source entry
					Calendar sourceCalendar = Calendar.getInstance();
					sourceCalendar.setTimeInMillis(currencySource.getLastUpdate().getTime()
							+ TimeUnit.SECONDS.toMillis(currencySource.getUpdatePeriod()));
					if (sourceCalendar.after(nowCalendar)) {
						log.trace("Source ({}) update skipped.", currencySource.getSourceId());
						continue;
					}

					// check if update is allowed on this date
					SourceUpdateRestrictions updateRestrictions = currencySource.getUpdateRestrictions();
					if (!isUpdateGo(updateRestrictions)) {
						log.trace("Source ({}) updates are disabled for the current time/date!",
								currencySource.getSourceId());
						continue;
					}

					final Sources sourceType = Sources.valueOf(currencySource.getSourceId());
					if (sourceType != null) {
						try {
							// TODO: add proper reporter
							// final ConsoleReporter reporter = new
							// ConsoleReporter();
							final TelegramReporter reporter = new TelegramReporter();
							final Source source = sourceType.newInstance(reporter);

							// set update datetime flag
							currencySource.setLastUpdate(new Date());
							dataSource.updateSource(currencySource.getSourceId(), currencySource);

							source.getRates(new Source.Callback() {

								@Override
								public void onFailed(Exception e) {
									log.error("{} - source download failed!", source.getName(), e);
									if (!reporter.isEmpty()) {
										try {
											reporter.send();
										} catch (IOException ioe) {
											log.error("{} - Failed sending report!", source.getName(), ioe);
										}
									}
								}

								@Override
								public void onCompleted(List<CurrencyData> currencyDataList) {
									log.debug("{} - source download succcesful.", source.getName());

									if (log.isTraceEnabled()) {
										// TODO remove this trace log
										for (CurrencyData currency : currencyDataList) {
											log.trace(currency.toString());
										}
									}

									log.debug("{} - importing downloaded rates in database ...", source.getName());
									try (final DataSourceInterface dataSource = new DataSource()) {
										dataSource.connect();
										dataSource.addRates(currencyDataList);
									} catch (IOException | DataSourceException e) {
										log.error("Could not connect to database!", e);
									}

									if (!reporter.isEmpty()) {
										try {
											reporter.send();
										} catch (IOException ioe) {
											log.error("{} - Failed sending report!", source.getName(), ioe);
										}
									}
								}
							});
						} catch (SourceException e) {
							log.error("Failed fetching rates for source id='{}'!", currencySource.getSourceId(), e);
						}
					}
				}

			} catch (IOException | DataSourceException e) {
				log.error("Could not connect to database!", e);
			}

		} catch (Throwable t) {
			/*
			 * The executor swallows exceptions, so catch Throwable instead.
			 * 
			 * @see http://stackoverflow.com/a/24902026
			 */
			log.error("Fatal hearbeat error!", t);
		}
	}
}
