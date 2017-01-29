package net.vexelon.currencybg.srv.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtilsNew {

	/**
	 * Converts String ZoneDateTime from user specific to Europe/Sofia using
	 * using Java 8 DateTime API
	 * 
	 * @param date
	 * @param dateFormatter
	 * @return
	 */
	public static String removeTimeZone(String timeFrom, String dateTimeFormatter) {
		ZonedDateTime fromIsoDate = ZonedDateTime.parse(timeFrom);
		ZoneOffset offset = ZoneOffset.of("+02:00");
		ZonedDateTime acst = fromIsoDate.withZoneSameInstant(offset);

		// System.out.println("Input: " + fromIsoDate);
		// System.out.println("Output: " +
		// acst.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		// System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd
		// HH:mm:ss").format(acst));
		return DateTimeFormatter.ofPattern(dateTimeFormatter).format(acst);
	}
}