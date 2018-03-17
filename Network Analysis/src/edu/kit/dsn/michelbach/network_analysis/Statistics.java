package edu.kit.dsn.michelbach.network_analysis;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class provides some statistical functions which should be built into the standard libraries of any decent
 * programming language but aren't built into Java's.
 *
 * @author Christoph Michelbach
 */
public class Statistics {
	private final ArrayList<Long> data;

	/**
	 * Constructs a new {@code Statistics} object operating on the given data.
	 *
	 * @param data
	 */
	public Statistics(Collection<Long> data) {
		this.data = new ArrayList<Long>(data);
	}

	/**
	 * Returns the arithmetic mean of the data in this object.
	 *
	 * @return
	 */
	public double getMean() {
		return this.data.stream().mapToLong(Long::longValue).average().getAsDouble();
	}

	/**
	 * Returns the standard deviation of the data in this object.
	 *
	 * @return
	 */
	public double getStandardDeviation() {
		return Math.sqrt(this.getVariance());
	}

	/**
	 * Returns the variance of the data in this object.
	 *
	 * @return
	 */
	public double getVariance() {
		double mean = this.getMean();
		double variance = 0;

		for (Long dataPoint : this.data) {
			variance += Math.pow(dataPoint - mean, 2);
		}

		return variance;
	}
}
