package edu.kit.dsn.michelbach.network_analysis;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Objects of this class represent participants in a Hyper Lightning Network.
 *
 * @author Christoph Michelbach
 */
public class Member {
	/**
	 * The list of {@code HyperPaymentChannel}s this {@code Member} is a member of.
	 */
	private HashMap<HyperLightningNetwork, ArrayList<HyperPaymentChannel>> hyperPaymentChannels = new HashMap<>();

	/**
	 * The {@code HyperLightningNetwork} this {@code Member} belongs to.
	 */
	private final ArrayList<HyperLightningNetwork> hyperLightningNetworks = new ArrayList<>();

	/**
	 * Constructor of {@code Member}.
	 *
	 * @param hyperLightningNetwork
	 *            The {@code HyperLightningNetwork} this member belongs to.
	 */
	public Member(HyperLightningNetwork hyperLightningNetwork) {
		this.addToHyperLightningNetwork(hyperLightningNetwork);
	}

	/**
	 * Adds this {@code Member} to the Hyper Lightning Network {@code hyperLightningNetwork}.
	 *
	 * @param hyperLightningNetwork
	 */
	public void addToHyperLightningNetwork(HyperLightningNetwork hyperLightningNetwork) {
		if (!this.hyperLightningNetworks.contains(hyperLightningNetwork)) {
			this.hyperLightningNetworks.add(hyperLightningNetwork);
		}

		this.hyperPaymentChannels.put(hyperLightningNetwork, new ArrayList<>());
		hyperLightningNetwork.addMember(this);
	}

	/**
	 * Returns the total fortune of this {@code Member} in the the Hyper Lightning Network
	 * {@code hyperLightningNetwork}.
	 *
	 * @return
	 */
	public long getFortune(HyperLightningNetwork hyperLightningNetwork) {
		return this.hyperPaymentChannels.get(hyperLightningNetwork).stream()
				.mapToLong(hyperPaymentChannel -> hyperPaymentChannel.getBalanceOfMember(this)).sum();
	}

	/**
	 * Returns the {@code HyperLightningNetwork}s this {@code Member} belongs to.
	 */
	public ArrayList<HyperLightningNetwork> getHyperLightningNetworks() {
		return new ArrayList<HyperLightningNetwork>(this.hyperLightningNetworks);
	}

	/**
	 * Returns the list of {@code HyperPaymentChannel}s this {@code Member} is a member of.
	 *
	 * @return
	 */
	public ArrayList<HyperPaymentChannel> getListOfHyperPaymentChannels(HyperLightningNetwork hyperLightningNetwork) {
		return new ArrayList<>(this.hyperPaymentChannels.get(hyperLightningNetwork));
	}

	/**
	 * Returns the maximum amount of money this {@code Member} can receive in the the Hyper Lightning Network
	 * {@code hyperLightningNetwork}.
	 *
	 * @param hyperLightningNetwork
	 * @return
	 */
	public long getMaximumReceipt(HyperLightningNetwork hyperLightningNetwork) {
		return this.hyperPaymentChannels.get(hyperLightningNetwork).stream()
				.mapToLong(hyperPaymentChannel -> hyperPaymentChannel.getFundingAmount()
						- hyperPaymentChannel.getBalanceOfMember(this))
				.sum();
	}

	/**
	 * Makes this {@code Member} a member of {@code hyperPaymentChannel}.
	 *
	 * @param hyperPaymentChannel
	 * @throws IllegalArgumentException
	 *             Thrown iff this {@code member} isn't already a member of {@code hyperPaymentChannel} as seen by
	 *             {@code hyperPaymentChannel}.
	 */
	public void makeMemberOfHyperPaymentChannel(HyperLightningNetwork hyperLightningNetwork,
			HyperPaymentChannel hyperPaymentChannel) {
		if (!hyperPaymentChannel.isMember(this)) {
			throw new IllegalArgumentException(
					"makeMemberOfHyperPaymentChannel() may only be called with Hyper Payment Channels this member is a member of as seen by that Hyper Payment Channel.");
		}

		assert (this.hyperPaymentChannels.containsKey(hyperLightningNetwork));

		this.hyperPaymentChannels.get(hyperLightningNetwork).add(hyperPaymentChannel);
	}
}
