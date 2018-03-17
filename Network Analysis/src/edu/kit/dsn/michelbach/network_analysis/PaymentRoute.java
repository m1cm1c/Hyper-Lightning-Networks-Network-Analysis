package edu.kit.dsn.michelbach.network_analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Objects of this class represent payment routes through a Hyper Lightning Network. Each payment route consists of
 * {@code Member}s and {@code HyperPaymentChannels}.
 *
 * @author Christoph Michelbach
 */
public class PaymentRoute {
	private final ArrayList<HyperPaymentChannel> hyperPaymentChannels;
	private final ArrayList<Member> hops;

	/**
	 * Constructs a new {@code PaymentRoute} over hops {@code} via Hyper Payment Channels {@code HyperPaymentChannels}.
	 *
	 * @param hyperPaymentChannels
	 * @param hops
	 */
	public PaymentRoute(List<Member> hops, List<HyperPaymentChannel> hyperPaymentChannels) {
		this.hops = new ArrayList<>(hops);
		this.hyperPaymentChannels = new ArrayList<>(hyperPaymentChannels);
	}

	/**
	 * Returns the list of {@code Member}s this payment route consists of.
	 *
	 * @return
	 */
	public ArrayList<Member> getListOfHops() {
		return new ArrayList<>(this.hops);
	}

	/**
	 * Returns the list of {@code HyperPaymentChannel}s this payment route consists of.
	 *
	 * @return
	 */
	public ArrayList<HyperPaymentChannel> getListOfHyperPaymentChannels() {
		return new ArrayList<>(this.hyperPaymentChannels);
	}

	/**
	 * Returns the sum of the fees due on this {@code PaymentRoute} when transacting the amount {@code amount}.
	 *
	 * @param amount
	 *            The amount the payee receives. Note that the amount sent by the sender may be higher due to fees.
	 * @return
	 */
	public long getTotalFees(long amount) {
		long sum = 0;

		for (int i = this.hyperPaymentChannels.size() - 1; i >= 0; i--) {
			long fee = this.hyperPaymentChannels.get(i).getFeeForLightningTransaction(this.hops.get(i),
					this.hops.get(i + 1), amount, i);
			sum += fee;
			amount += fee; // Update the amount to transact because the next channel closer to the sender will have to
							// transact more due to the fees in this one.
		}

		return sum;
	}
}
