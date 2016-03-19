package net.vexelon.currencybg.srv.api;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.vexelon.currencybg.srv.db.DataSource;
import net.vexelon.currencybg.srv.db.DataSourceException;
import net.vexelon.currencybg.srv.db.DataSourceInterface;
import net.vexelon.currencybg.srv.utils.DateTimeUtils;

@Path("/rates")
public class Currencies {

	private static final Logger log = LoggerFactory.getLogger(Currencies.class);

	@GET
	@Produces("application/json" + ";charset=utf-8")
	@Path("/{dateFrom}")
	public String getAllRatesByDate(@PathParam("dateFrom") String initialDate) throws Exception {

		Date dateFrom = DateTimeUtils.parseStringToDate(initialDate, "yyyy-MM-dd");

		String currencies = null;
		DataSourceInterface source = null;
		try {
			source = new DataSource();
			source.dbConnect();
			currencies = source.getAllRatesByDate(dateFrom);
		} catch (DataSourceException e) {
			log.error("Error selecting rows!", e);
		} finally {
			IOUtils.closeQuietly(source);
		}
		System.out.println("Get all currencies");
		return currencies;

	}

	@GET
	@Produces("application/json" + ";charset=utf-8")
	@Path("/nonfixed/{dateFrom}")
	public String getNonFixedRates(@PathParam("dateFrom") String initialDate) throws Exception {

		Date dateFrom = DateTimeUtils.parseStringToDate(initialDate, "yyyy-MM-dd");

		String currencies = null;
		DataSourceInterface source = null;
		try {
			source = new DataSource();
			source.dbConnect();
			currencies = source.getNonfixedRates(dateFrom);
		} catch (DataSourceException e) {
			log.error("Error selecting rows!", e);
		} finally {
			IOUtils.closeQuietly(source);
		}

		return currencies;

	}

}
