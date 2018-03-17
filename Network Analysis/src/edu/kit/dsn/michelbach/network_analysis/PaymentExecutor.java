package edu.kit.dsn.michelbach.network_analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

/**
 * Objects of this class attempt to execute payments on the associated {@code HyperLightningNetwork}. Statistics
 * including how many of the payments were executed successfully can be queried.
 *
 * If on different runs the same seed is used and the members of the provided {@code HyperLightningNetwork} each have
 * the same amount of money, the payments attempted and their order are the same.
 *
 * @author Christoph Michelbach
 */
public class PaymentExecutor {
	/**
	 * This {@code PaymentExecutor} builder has to be used to construct {@code PaymentExecutor}. Various parameters can
	 * be set before the constructed object is returned on call of {@code generate()}.
	 */
	public static class Builder {
		private PaymentExecutor paymentExecutor;
		private boolean returnedPaymentExecutorObject = false;
		private long seed;
		private Random random;

		/**
		 * Constructs a new {@code PaymentExecutor} {@code Builder} using a random seed.
		 */
		public Builder(HyperLightningNetwork hyperLightningNetwork, long seed) {
			this.paymentExecutor = new PaymentExecutor(hyperLightningNetwork);
			this.seed = seed;
			this.random = new Random(this.seed);
		}

		/**
		 * Checks whether {@code generate()} has already been called and throws an {@code IllegalAccessError} if so.
		 */
		private void checkSetterAvailablity() {
			if (this.returnedPaymentExecutorObject) {
				throw new IllegalAccessError("Setters may not be used on Builder after generate() has been called.");
			}
		}

		/**
		 * Returns the built object. This makes subsequent changes of parameters impossible. This method may only be
		 * called once per instance.
		 *
		 * @return
		 */
		public PaymentExecutor generate() {
			if (this.returnedPaymentExecutorObject) {
				throw new IllegalAccessError("Method may only be called once.");
			}

			this.paymentExecutor.seed = this.seed;
			this.paymentExecutor.random = this.random;

			// Compute dependent fields.
			this.paymentExecutor.paymentSizeExponentRange = Math
					.log(((double) this.paymentExecutor.paymentSizeMinimum) / this.paymentExecutor.paymentSizeMaximum);

			this.returnedPaymentExecutorObject = true;
			return this.paymentExecutor;
		}

		/**
		 * Returns the seed this {@code PaymentExecutor} {@code Builder} gives to its {@code PaymentExecutor}.
		 *
		 * @return
		 */
		public long getSeed() {
			return this.seed;
		}

		public PaymentExecutor.Builder setCompanyWealthMinimum(long companyWealthMinumum) {
			this.checkSetterAvailablity();
			this.paymentExecutor.companyWealthMinumum = companyWealthMinumum;
			return this;
		}

		public PaymentExecutor.Builder setMinimumMonthlyPay(long minimumMonthlyPay) {
			this.checkSetterAvailablity();
			this.paymentExecutor.minimumMonthlyPay = minimumMonthlyPay;
			return this;
		}

		public PaymentExecutor.Builder setNumberOfPayments(long numberOfPayments) {
			this.checkSetterAvailablity();
			this.paymentExecutor.numberOfPayments = numberOfPayments;
			return this;
		}

		public PaymentExecutor.Builder setPaymentMontlyPayProbability(double paymentMontlyPayProbability) {
			this.checkSetterAvailablity();
			this.paymentExecutor.paymentMontlyPayProbability = paymentMontlyPayProbability;
			return this;
		}

		public PaymentExecutor.Builder setPaymentSizeMaximum(long paymentSizeMaximum) {
			this.checkSetterAvailablity();
			this.paymentExecutor.paymentSizeMaximum = paymentSizeMaximum;
			return this;
		}

		public PaymentExecutor.Builder setPaymentSizeMinimum(long paymentSizeMinimum) {
			this.checkSetterAvailablity();
			this.paymentExecutor.paymentSizeMinimum = paymentSizeMinimum;
			return this;
		}
	}

	/**
	 * Represents a payment transferring {@code amount} from {@code origin} to {@code destination}.
	 */
	private class Payment {
		Member origin;
		Member destination;
		long amount;
	}

	/**
	 * The minimum size of a payment.
	 */
	private long paymentSizeMinimum = 2_000_000L;

	/**
	 * The maximum size of a payment.
	 */
	private long paymentSizeMaximum = 10_000_000_000L;

	private double paymentSizeExponentRange = Math.log(((double) this.paymentSizeMinimum) / this.paymentSizeMaximum);

	/**
	 * The minimum size of payments to potentially be monthly pays (at probability
	 * {@code largePaymentMontlyPayProbability}).
	 */
	private long minimumMonthlyPay = 1_500_000_000;

	/**
	 * The number of payments conducted.
	 */
	private long numberOfPayments = 1_000;

	/**
	 * The minimum amount of starting wealth a member needs to have to be considered a company.
	 */
	private long companyWealthMinumum = 20_000_000_000L;

	/**
	 * The probability that a payment is a monthly pay. These payments may only originate from entities with a starting
	 * wealth of at least {@code companyWealthMinumum} and may be no less than {@code minimumMonthlyPay} in size
	 */
	private double paymentMontlyPayProbability = 0.02d;

	/**
	 * The seed this {@code PaymentExecutor} uses.
	 */
	private long seed;

	/**
	 * The source of pseudo-randomness this {@code PaymentExecutor} uses.
	 */
	private Random random;

	/**
	 * The {@code HyperLightningNetwork} this {@code PaymentExecutor} operates on.
	 */
	private HyperLightningNetwork hyperLightningNetwork;

	/**
	 * Whether this {@code PaymentExecutor} has been initialized. Whether a method can be used may depend on this
	 * variable.
	 */
	private boolean initialized = false;

	/**
	 * Addressable list of companies this {@code PaymentExecutor} knows. Any member whose initial wealth is no less than
	 * {@code companyWealthMinumum} is considered a company.
	 */
	private ArrayList<Member> companyList = new ArrayList<>();

	/**
	 * Addressable list of {@code Member}s {@code PaymentExecutor} knows.
	 */
	private ArrayList<Member> memberList;

	/**
	 * The list of payments this {@code PaymentExecutor} performs on {@code hyperLightningNetwork}.
	 */
	private LinkedList<Payment> payments = new LinkedList<>();

	/**
	 * The list of failed payments.
	 */
	private ArrayList<Payment> failedPayments = new ArrayList<>();

	/**
	 * The list of paid fees.
	 */
	private ArrayList<Long> paidFees = new ArrayList<>();

	/**
	 * Private constructor.
	 */
	private PaymentExecutor(HyperLightningNetwork hyperLightningNetwork) {
		this.hyperLightningNetwork = hyperLightningNetwork;

		this.memberList = new ArrayList<Member>(this.hyperLightningNetwork.getMembers());

		if (this.numberOfPayments < 0) {
			throw new IllegalStateException(
					"Number of payments to be executed by PaymentExecutor must not be smaller than zero.");
		}

		if (this.numberOfPayments != 0 && this.memberList.size() < 2) {
			throw new IllegalStateException(
					"PaymentExecutor cannot operato on HyperLightningNetworks with less than two members if the number of payments to be executed is non-zero.");
		}

	}

	/**
	 * Determines the set of companies. Any member whose initial wealth is no less than {@code companyWealthMinumum} is
	 * considered a company.
	 */
	private void determineCompanies() {
		for (Member member : this.hyperLightningNetwork.getMembers()) {
			if (member.getFortune(this.hyperLightningNetwork) >= this.companyWealthMinumum) {
				this.companyList.add(member);
			}
		}
	}

	/**
	 * Determines the payments to be executed. {@code numberOfPayments} payments will be generated. All payments are in
	 * principle possible when only considering the wealth of each {@code Member}.
	 *
	 * The payments are selected from potential payments whose distribution amongst pairs of members is uniformly
	 * distributed and whose size is distributed exponentially.
	 *
	 * Each payment has a probability of {@code paymentMonthlyPayProbability} of being a monthly pay. It then may be no
	 * less than {@code minimumMonthlyPay} and may only originate from a {@code Member} whose initial wealth was at
	 * least {@code companyWealthMinimum}.
	 */
	private void determinePayments() {
		HashMap<Member, Long> fortunes = new HashMap<>();
		for (Member member : this.hyperLightningNetwork.getMembers()) {
			fortunes.put(member, member.getFortune(this.hyperLightningNetwork));
		}

		for (int i = 0; i < this.numberOfPayments;) {
			Payment payment = new Payment();
			boolean isMonthlyPay = this.random.nextDouble() <= this.paymentMontlyPayProbability;

			if (isMonthlyPay) {
				while ((payment.amount = this.getRandomPaymentAmount()) < this.minimumMonthlyPay) {
				}
			} else {
				payment.amount = this.getRandomPaymentAmount();
			}

			ArrayList<Member> originCandidates;
			if (isMonthlyPay) {
				// Can only choose the company list if it's not empty.
				if (this.companyList.size() > 0) {
					originCandidates = this.companyList;
				} else {
					originCandidates = this.memberList;
				}
			} else {
				originCandidates = this.memberList;
			}

			payment.origin = originCandidates.get(this.random.nextInt(originCandidates.size()));

			// Make sure that origin and destination are different members.
			while ((payment.destination = this.memberList
					.get(this.random.nextInt(this.memberList.size()))) == payment.origin) {
			}

			if (fortunes.get(payment.origin) >= payment.amount) {
				// Update fortunes.
				fortunes.put(payment.origin, fortunes.get(payment.origin) - payment.amount);
				fortunes.put(payment.destination, fortunes.get(payment.destination) - payment.amount);

				// Store payment and increase counter.
				this.payments.add(payment);
				i++;
			}
		}
	}

	/**
	 * Returns the average size of failed payments.
	 *
	 * @return
	 */
	public double getAverageFailedPaymentSize() {
		return this.failedPayments.stream().map(payment -> payment.amount).mapToLong(Long::longValue).average()
				.getAsDouble();
	}

	/**
	 * Returns the list of paid fees.
	 *
	 * @return
	 */
	public ArrayList<Long> getListOfPaidFees() {
		return new ArrayList<>(this.paidFees);
	}

	/**
	 * Returns the number of failed payments.
	 *
	 * @return
	 */
	public long getNumberOfFailedPayments() {
		return this.failedPayments.size();
	}

	/**
	 * Returns a randomly chosen payment amount with exponential distribution between {@code paymentSizeMinimum} and
	 * {@code paymentSizeMaximum}.
	 *
	 * @return
	 */
	private long getRandomPaymentAmount() {
		return (long) (this.paymentSizeMaximum
				* Math.pow(Math.E, this.random.nextDouble() * this.paymentSizeExponentRange));
	}

	/**
	 * Returns the seed this {@code PaymentExecutor} uses.
	 *
	 * @return
	 */
	public long getSeed() {
		return this.seed;
	}

	/**
	 * Initializes this {@code PaymentExecutor}. Initialization may only happen once. Subsequent calls to this method
	 * result in an {@code IllegalStateException} being thrown.
	 *
	 * This is a potentially long-running method.
	 *
	 * @throws IllegalStateException
	 */
	public void init() {
		if (this.initialized) {
			throw new IllegalStateException("Initialization of a PaymentExecutor may only happen once.");
		}

		this.determineCompanies();
		this.determinePayments();
		this.performPayments();

		this.initialized = true;
	}

	/**
	 * Performs the payments determined by {@link determinePayments()} in the {@code HyperLightningNetwork}
	 * {@code hyperLightningNetwork}.
	 */
	private void performPayments() {
		for (Payment payment : this.payments) {
			long fee = this.hyperLightningNetwork.performPayment(payment.origin, payment.destination, payment.amount);

			// Count failed payments.
			if (fee == -1L) {
				this.failedPayments.add(payment);
			} else {
				this.paidFees.add(fee);
			}
		}
	}
}
