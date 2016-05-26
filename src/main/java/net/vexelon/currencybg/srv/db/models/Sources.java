package net.vexelon.currencybg.srv.db.models;

import net.vexelon.currencybg.srv.remote.BNBSource;
import net.vexelon.currencybg.srv.remote.Source;
import net.vexelon.currencybg.srv.remote.TavexSource;
import net.vexelon.currencybg.srv.reports.Reporter;

public enum Sources {

	BNB(1), FIB(100), TAVEX(200);

	private int id;

	Sources(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	/**
	 * Creates a new {@link Source} mapped via the {@link Sources} constant.
	 * 
	 * @param reporter
	 * @return
	 */
	public Source newInstance(Reporter reporter) {
		switch (id) {
		// BNB
		case 1:
			return new BNBSource(reporter);

		// TAVEX
		case 200:
			return new TavexSource(reporter);

		// <unknown>
		default:
			throw new RuntimeException("Invalid source id (" + id + ")!");
		}
	}
}
