package edu.kit.dsn.michelbach.network_analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Objects of this class represent Hyper Payment Channels. Each Hyper Payment Channels consists of {@code Members}s and
 * their respective balances.
 *
 * @author Christoph Michelbach
 */
public class HyperPaymentChannel {
	private class NewBalancesAndFeeBalanceChanges {
		HashMap<Member, Long> newBalancesAfterPayment;
		HashMap<Member, Long> feeBalanceChagnes;
	}

	private static final long INTAKE_PER_LIGHTNING_TRANSACTION_PER_MEMBER = 40;
	private static final long INTAKE_BONUS_FOR_SENDER = 10_000;
	private static final long INTEREST_IN_AVAILABILITY_OF_FUNDS_PER_MEMBER = 10;
	private static final long INVERSE_OF_INTEREST_PER_TX_TIME_UNIT = 12_000_000;

	private static final double DEVIATION_PENALTY = 1E-5;

	/**
	 * This {@code HyperPaymentChannel}'s {@code HyperLightningNetwork}.
	 */
	private final HyperLightningNetwork hyperLightningNetwork;

	/**
	 * The list of members of this {@code HyperPaymentChannel}.
	 */
	private ArrayList<Member> members = new ArrayList<>();

	/**
	 * Stores the balances the members of this {@code HyperPaymentChannel}.
	 */
	private HashMap<Member, Long> balances = new HashMap<>();

	/**
	 * The total amount of money this {@code HyperPaymentChannel} controls.
	 */
	private final long fundingAmount;

	/**
	 * Constructor of {@code HyperPaymentChannel}. {@code members} and {@code deposits} must have equally many members
	 * and must not be {@code null}. {@code deposits} must only consist of non-negative (long) integers. Otherwise, an
	 * {@code IllegalArgumentException} is thrown.
	 *
	 * @param hyperLightningNetwork
	 * @param members
	 * @param deposits
	 * @throw IllegalArgumentException See method description.
	 */
	public HyperPaymentChannel(HyperLightningNetwork hyperLightningNetwork, List<Member> members, List<Long> deposits) {
		if (hyperLightningNetwork == null || members == null || deposits == null) {
			throw new IllegalArgumentException(
					"Arguments hyperLightningNetwork, members, and deposits of constructiors of HyperPaymentChannel must not be null.");
		}

		this.hyperLightningNetwork = hyperLightningNetwork;

		if (members.size() != deposits.size()) {
			throw new IllegalArgumentException(
					"Arguments members and deposits of constructiors of HyperPaymentChannel must be equal in size, but members has "
							+ members.size() + " elements and deposits has " + deposits.size() + " elements.");
		}

		if (deposits.stream().mapToLong(Long::longValue).min().getAsLong() < 0) {
			throw new IllegalArgumentException(
					"Elements of deposits arugment of constructor of HyperPayemntChannel must not be negative.");
		}

		for (Member member : members) {
			if (!member.getHyperLightningNetworks().contains(this.hyperLightningNetwork)) {
				throw new IllegalArgumentException(
						"HyperPaymentChannels and their members must belong to the same HyperLightningNetwork.");
			}
		}

		this.members.addAll(members);

		Iterator<Member> membersItr = members.iterator();
		Iterator<Long> depositsItr = deposits.iterator();

		while (membersItr.hasNext()) {
			Member member = membersItr.next();
			this.balances.put(member, depositsItr.next());
			member.makeMemberOfHyperPaymentChannel(hyperLightningNetwork, this);
		}
		assert (!depositsItr.hasNext());

		this.fundingAmount = deposits.stream().mapToLong(Long::longValue).sum();

		this.hyperLightningNetwork.addHyperPaymentChannel(this);
	}

	/**
	 * Returns the balance of {@code member} in this {@code HyperPaymentChannel}.
	 *
	 * @param member
	 * @return
	 * @throws IllegalArgumentException
	 *             Thrown iff {@code member} is not a member of this {@code HyperPaymentChannel}.
	 */
	public long getBalanceOfMember(Member member) throws IllegalArgumentException {
		if (!this.members.contains(member)) {
			throw new IllegalArgumentException(
					"The member given to getBalanceOfMember() is not a member of the HyperPaymentChannel it has been called on.");
		}

		return this.balances.get(member);
	}

	/**
	 * Returns the balances {@code HashMap} of this {@code HyperPaymentChannel}.
	 *
	 * @return
	 */
	public HashMap<Member, Long> getBalances() {
		return new HashMap<>(this.balances);
	}

	/**
	 * Returns the balance changes due to fees for the described payment.
	 *
	 * @param origin
	 * @param destination
	 * @param amount
	 * @param numberOfHops
	 */
	private HashMap<Member, Long> getFeeBalanceChanges(Member origin, Member destination, long amount,
			int numberOfHops) {
		long imbalanceCompensation = this.getImbalanceCompensation(origin, destination, amount);

		HashMap<Member, Long> balanceChanges = new HashMap<>();
		for (Member member : this.members) {
			balanceChanges.put(member,
					INTAKE_PER_LIGHTNING_TRANSACTION_PER_MEMBER
							+ (1 + 2 * numberOfHops) * (this.balances.get(member) / INVERSE_OF_INTEREST_PER_TX_TIME_UNIT
									+ INTEREST_IN_AVAILABILITY_OF_FUNDS_PER_MEMBER)
							+ imbalanceCompensation / this.members.size());
		}

		long sumOfChanges = balanceChanges.values().stream().mapToLong(Long::longValue).sum();
		balanceChanges.put(origin, balanceChanges.get(origin) + sumOfChanges * (-1));

		return balanceChanges;
	}

	/**
	 * Returns the fee required to transact {@code amount} from {@code origin} to {@code destination} when the number of
	 * hops to the payee is {@code numberOfHops}. Returns {@code -1} if the transaction cannot be performed with any
	 * fee.
	 *
	 * The fee increases with the number of hops to the payee because the payment path has to be locked for longer.
	 *
	 * @param origin
	 * @param destination
	 * @param amount
	 * @param numberOfHops
	 *            0 if the payee is in the Hyper Payment Channel. Otherwise, 1 larger than the value for the previous
	 *            Hyper Payment Channel on the payment path.
	 * @return
	 */
	public long getFeeForLightningTransaction(Member origin, Member destination, long amount, int numberOfHops) {
		if (this.getNewBalancesAndFeeBalanceChangesAfterPayment(origin, destination, amount, numberOfHops) == null) {
			// Payment cannot be performed.
			return -1L;
		} else {
			long naiveFee = this.getFeeBalanceChanges(origin, destination, amount, numberOfHops).get(origin) * (-1)
					+ INTAKE_BONUS_FOR_SENDER;
			if (naiveFee < 0) {
				return 0L;
			} else {
				return naiveFee;
			}
		}
	}

	/**
	 * Returns the funding amount of this {@code HyperPaymentChannel}.
	 *
	 * @return
	 */
	public long getFundingAmount() {
		return this.fundingAmount;
	}

	/**
	 * Returns the imbalance compensation for performing a payment over amount {@code} amount from {@code origin} to
	 * {@code destination}.
	 *
	 * @param origin
	 * @param destination
	 * @param amount
	 * @return
	 */
	private long getImbalanceCompensation(Member origin, Member destination, long amount) {
		Statistics statisticsBeforeTransaction = new Statistics(this.balances.values());

		HashMap<Member, Long> balancesAfterTransactionWithoutFeeImplications = new HashMap<>(this.balances);
		balancesAfterTransactionWithoutFeeImplications.put(origin,
				balancesAfterTransactionWithoutFeeImplications.get(origin) - amount);
		balancesAfterTransactionWithoutFeeImplications.put(destination,
				balancesAfterTransactionWithoutFeeImplications.get(destination) + amount);
		Statistics statisticsAfterTransactionWithoutFeeImplications = new Statistics(
				balancesAfterTransactionWithoutFeeImplications.values());

		double standardDeviationBeforeTransaction = statisticsBeforeTransaction.getStandardDeviation();
		double standardDeviationAfterTransactionWithoutFeeImplications = statisticsAfterTransactionWithoutFeeImplications
				.getStandardDeviation();

		return Math.round((standardDeviationAfterTransactionWithoutFeeImplications - standardDeviationBeforeTransaction)
				* DEVIATION_PENALTY);
	}

	/**
	 * Returns the set of members of this {@code HyperPaymentChannel}.
	 *
	 * @return
	 */
	public ArrayList<Member> getMembers() {
		return new ArrayList<>(this.members);
	}

	/**
	 * Returns the minimum amount of on-chain storage space required to fund this Hyper Payment Channel's wallet and
	 * distribute its funds to its members. The result is in Byte.
	 *
	 * @return
	 */
	public int getMinimumAmountOfFullCycleOnChainStorageSpace() {
		int memberCount = this.members.size();

		final int fixedSize = 10;
		final int inputSize = 180;
		final int signatureSize = 73;
		final int receivingAddressSize = 34;

		return fixedSize + 1 * inputSize + memberCount * (signatureSize + receivingAddressSize);
	}

	/**
	 * Returns the new balances after the described payment would be performed or {@code null} if the described payment
	 * currently is impossible to perform.
	 *
	 * @param origin
	 * @param destination
	 * @param amount
	 * @param numberOfHops
	 * @return
	 */
	private NewBalancesAndFeeBalanceChanges getNewBalancesAndFeeBalanceChangesAfterPayment(Member origin,
			Member destination, long amount, int numberOfHops) {
		HashMap<Member, Long> newBalances = new HashMap<>(this.balances);

		// Distribute fees.
		HashMap<Member, Long> feeBalanceChanges = this.getFeeBalanceChanges(origin, destination, amount, numberOfHops);
		assert (feeBalanceChanges.values().stream().mapToLong(Long::longValue).sum() == 0L);

		for (Member member : this.members) {
			newBalances.put(member, newBalances.get(member) + feeBalanceChanges.get(member));
		}

		// Perform actual payment.
		newBalances.put(origin, newBalances.get(origin) - amount);
		newBalances.put(destination, newBalances.get(destination) + amount);

		// If there is a negative balance, the payment cannot be performed.
		if (newBalances.values().stream().filter(balance -> balance < 0).collect(Collectors.toSet()).size() > 0) {
			return null;
		}

		NewBalancesAndFeeBalanceChanges newBalancesAndFeeBalanceChanges = new NewBalancesAndFeeBalanceChanges();
		newBalancesAndFeeBalanceChanges.newBalancesAfterPayment = newBalances;
		newBalancesAndFeeBalanceChanges.feeBalanceChagnes = feeBalanceChanges;

		return newBalancesAndFeeBalanceChanges;
	}

	/**
	 * Returns the number of members this {@code HyperPaymentChannel} has.
	 *
	 * @return
	 */
	public int getNumberOfMembers() {
		return this.members.size();
	}

	/**
	 * Returns the time value of the funds in this {@code HyperPaymentChannel}. This is how much the members of the
	 * channel value their channel not being locked. It is measured per time a lightning transaction takes per hop.
	 *
	 * @return
	 */
	public long getTimeValueOfFunds() {
		return this.fundingAmount / INVERSE_OF_INTEREST_PER_TX_TIME_UNIT
				+ this.members.size() * INTEREST_IN_AVAILABILITY_OF_FUNDS_PER_MEMBER;
	}

	/**
	 * Returns {@code true} iff {@code member} is a member of this {@code HyperPaymentChannel}.
	 *
	 * @param member
	 * @return
	 */
	public boolean isMember(Member member) {
		return this.members.contains(member);
	}

	/**
	 * Performs a payment from {@code origin} to {@code destination} over amount {@code amount}.
	 *
	 * If the payment cannot be performed, the state of the channel remains unchanged.
	 *
	 * @param origin
	 * @param destination
	 * @param amount
	 * @param numberOfHops
	 *            0 if the payee is in the Hyper Payment Channel. Otherwise, 1 larger than the value for the previous
	 *            Hyper Payment Channel on the payment path.
	 * @return {@code true} iff the payment was performed.
	 */
	public boolean performPayment(Member origin, Member destination, long amount, int numberOfHops) {
		NewBalancesAndFeeBalanceChanges newBalancesAndFeeBalanceChangesAfterPayment = this
				.getNewBalancesAndFeeBalanceChangesAfterPayment(origin, destination, amount, numberOfHops);

		if (newBalancesAndFeeBalanceChangesAfterPayment == null) {
			return false;
		} else {
			HashMap<Member, Long> newBalances = newBalancesAndFeeBalanceChangesAfterPayment.newBalancesAfterPayment;
			HashMap<Member, Long> feeBalanceChanges = newBalancesAndFeeBalanceChangesAfterPayment.feeBalanceChagnes;

			for (Member member : this.members) {
				this.hyperLightningNetwork.reportFeeIntake(member, feeBalanceChanges.get(member));
			}

			this.hyperLightningNetwork.reportFeeIntake(origin, INTAKE_BONUS_FOR_SENDER);

			this.balances = newBalances;
			return true;
		}
	}
}
