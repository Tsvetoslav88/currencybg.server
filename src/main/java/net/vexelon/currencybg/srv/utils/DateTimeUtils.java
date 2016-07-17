package net.vexelon.currencybg.srv.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {

	public static Date parseStringToDate(String date, String format) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.parse(date);
	}

	public static String parseDateToString(Date date, String dateFormat) {
		DateFormat formatter = new SimpleDateFormat(dateFormat);
		return formatter.format(date);
	}

	public static java.sql.Date convertJavaDateToSqlDate(java.util.Date date) {
		return new java.sql.Date(date.getTime());
	}

	public static int getYearByDate(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.YEAR);
	}

	public static Date getStartOfYear() {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		String dateInString = "01.01." + year;
		Date currentYear = null;
		try {
			currentYear = sdf.parse(dateInString);
		} catch (ParseException e1) {
			e1.printStackTrace();
			// use default (today)
		}
		return currentYear;
	}

	public static Calendar getCal(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	public static Calendar getCalToday() {
		return getCal(new Date());
	}

	public static boolean isSaturday(Date date) {
		return getCal(date).get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
	}

	public static boolean isSunday(Date date) {
		return getCal(date).get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
	}

	public static boolean isWeekend(Date date) {
		return isSaturday(date) && isSunday(date);
	}

	public static boolean isWeekday(Date date) {
		return !isWeekend(date);
	}
}
