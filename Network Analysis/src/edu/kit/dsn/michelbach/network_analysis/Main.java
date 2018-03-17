package edu.kit.dsn.michelbach.network_analysis;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {

	@SuppressWarnings("unused")
	private static void crossSeedComparison() {
		long[] seeds = { 8265215505318339183L, 2312065966272110664L, -6801099551804891715L, 3178607175451165769L,
				-8239179076166085499L, -6800396986249563140L, -300718535325288563L, -3693703477738302396L,
				-6733899900160963130L, -7024264911932356603L };
		int i = 0;
		for (long seed : seeds) {
			System.out.println(i + " : " + seed);
			LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
					.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000)
					.setNumberOfClassicPaymentChannels(1200).generate();
			lightningNetworkPair.init();

			HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
			HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

			System.out.println(cln.getStats());

			System.out.println(hln.getStats());

			System.out.println("\n\n");
			i++;
		}
	}

	@SuppressWarnings("unused")
	private static void feeIntakes() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
				.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000).setNumberOfClassicPaymentChannels(1200)
				.generate();
		lightningNetworkPair.init();

		HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
		HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

		PaymentExecutor clnExecutor = new PaymentExecutor.Builder(cln, seed).setNumberOfPayments(5000).generate();
		PaymentExecutor hlnExecutor = new PaymentExecutor.Builder(hln, seed).setNumberOfPayments(5000).generate();

		clnExecutor.init();
		hlnExecutor.init();

		HashMap<Member, Long> clnFeeIntakes = cln.getFeeIntakes();
		HashMap<Member, Long> hlnFeeIntakes = hln.getFeeIntakes();

		ArrayList<Member> members = cln.getMembers();

		for (Member member : members) {
			System.out.println(clnFeeIntakes.get(member) / 1_000_000D);
		}

		System.out.println("\n\n\n");

		for (Member member : members) {
			System.out.println(hlnFeeIntakes.get(member) / 1_000_000D);
		}
	}

	@SuppressWarnings("unused")
	private static void feesPaid() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
				.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000).setNumberOfClassicPaymentChannels(1200)
				.generate();
		lightningNetworkPair.init();

		HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
		HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

		PaymentExecutor clnExecutor = new PaymentExecutor.Builder(cln, seed).setNumberOfPayments(5000).generate();
		PaymentExecutor hlnExecutor = new PaymentExecutor.Builder(hln, seed).setNumberOfPayments(5000).generate();

		clnExecutor.init();
		hlnExecutor.init();

		System.out.println(clnExecutor.getListOfPaidFees().stream().mapToLong(Long::longValue).average().getAsDouble());
		System.out.println(hlnExecutor.getListOfPaidFees().stream().mapToLong(Long::longValue).average().getAsDouble());

		System.out.println(clnExecutor.getListOfPaidFees().stream().mapToLong(Long::longValue).sum());
		System.out.println(hlnExecutor.getListOfPaidFees().stream().mapToLong(Long::longValue).sum());
	}

	@SuppressWarnings("unused")
	private static void fewHpcsCompariosn(boolean staticAnalysisOnly) {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
				.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000).setNumberOfClassicPaymentChannels(1200)
				.generate();
		lightningNetworkPair.init();

		HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
		HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

		LightningNetworkPair lightningNetworkPair2 = new LightningNetworkPair.Builder(seed)
				.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000).setNumberOfClassicPaymentChannels(1200)
				.setHpcParsimony(true).generate();
		lightningNetworkPair2.init();

		HyperLightningNetwork hlnHpcParsimony = lightningNetworkPair2.getHyperLightningNetwork();

		System.out.println(cln.getStats());
		System.out.println(hln.getStats());
		System.out.println(hlnHpcParsimony.getStats());

		if (!staticAnalysisOnly) {
			PaymentExecutor clnExecutor = new PaymentExecutor.Builder(cln, seed).setNumberOfPayments(5000).generate();
			PaymentExecutor hlnExecutor = new PaymentExecutor.Builder(hln, seed).setNumberOfPayments(5000).generate();
			PaymentExecutor hlnParsimonyExecutor = new PaymentExecutor.Builder(hlnHpcParsimony, seed)
					.setNumberOfPayments(5000).generate();

			clnExecutor.init();
			hlnExecutor.init();
			hlnParsimonyExecutor.init();

			System.out.println(clnExecutor.getNumberOfFailedPayments());
			System.out.println(hlnExecutor.getNumberOfFailedPayments());
			System.out.println(hlnParsimonyExecutor.getNumberOfFailedPayments());

			System.out.println(clnExecutor.getAverageFailedPaymentSize());
			System.out.println(hlnExecutor.getAverageFailedPaymentSize());
			System.out.println(hlnParsimonyExecutor.getAverageFailedPaymentSize());
		}
	}

	@SuppressWarnings("unused")
	private static void findDiameterDropsinCln() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		for (int i = 1200; i < 14000; i += 100) {
			LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
					.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000)
					.setNumberOfClassicPaymentChannels(i).generate();
			lightningNetworkPair.init();

			HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
			System.out.println(i + " : " + cln.getDiameter());
		}
	}

	@SuppressWarnings("unused")
	private static void findDiameterDropsinCln2() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;
		int startingPoint = 1100;

		for (int i = startingPoint; i <= startingPoint + 100; i += 1) {
			LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
					.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000)
					.setNumberOfClassicPaymentChannels(i).generate();
			lightningNetworkPair.init();

			HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
			System.out.println(i + " : " + cln.getDiameter());
		}
	}

	@SuppressWarnings("unused")
	private static void graphVisualization() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
				.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000).setNumberOfClassicPaymentChannels(1200)
				.generate();
		lightningNetworkPair.init();

		HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
		HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

		System.out.println(cln.toGraphMlWithCliques());
		System.out.println(hln.toGraphMlWithCliques());
	}

	public static void main(String[] args) {
		graphVisualization();
	}

	@SuppressWarnings("unused")
	private static void minimumFees() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		for (int maximumChannelSize = 3; maximumChannelSize <= 18; maximumChannelSize++) {
			LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
					.setMaximumHyperPaymentChannelSize(maximumChannelSize).setNumberOfMembers(1_000)
					.setNumberOfClassicPaymentChannels(1200).generate();
			lightningNetworkPair.init();

			HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

			PaymentExecutor hlnExecutor = new PaymentExecutor.Builder(hln, seed).setNumberOfPayments(5000).generate();

			hlnExecutor.init();

			System.out.println(maximumChannelSize);
			System.out.println(hlnExecutor.getNumberOfFailedPayments());
			System.out.println(
					hlnExecutor.getListOfPaidFees().stream().mapToLong(Long::longValue).average().getAsDouble());
			System.out.println(hlnExecutor.getListOfPaidFees().stream().mapToLong(Long::longValue).sum());
			System.out.println("\n");
		}
	}

	@SuppressWarnings("unused")
	private static void numberOfMembersComparison() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		for (int i = 8000; i <= 10_0000; i += 1000) {
			System.out.println(i);
			LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
					.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(i)
					.setNumberOfClassicPaymentChannels((long) (i * 1.2d)).generate();
			lightningNetworkPair.init();

			HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
			HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

			System.out.println(cln.getStats());

			System.out.println(hln.getStats());

			System.out.println("\n\n");
		}
	}

	@SuppressWarnings("unused")
	private static void stats() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
				.setMaximumHyperPaymentChannelSize(17).setNumberOfMembers(1_000).setNumberOfClassicPaymentChannels(1200)
				.generate();
		lightningNetworkPair.init();

		HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
		HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

		System.out.println(cln.getStats());
		System.out.println(hln.getStats());
	}

	@SuppressWarnings("unused")
	private static void statsMaximumHpcSize() {
		// Hash of the first Bitcoin block modulo 2^64.
		long seed = 8265215505318339183L;

		boolean printedStatsOfCln = false;

		for (int maximumChannelSize = 3; maximumChannelSize <= 40; maximumChannelSize++) {
			LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(seed)
					.setMaximumHyperPaymentChannelSize(maximumChannelSize).setNumberOfMembers(1_000)
					.setNumberOfClassicPaymentChannels(1200).generate();
			lightningNetworkPair.init();

			if (!printedStatsOfCln) {
				System.out.println("CLN");
				System.out.println(lightningNetworkPair.getClassicLightningNetwork().getStats());
				System.out.println("\n");

				printedStatsOfCln = true;
			}

			System.out.println(maximumChannelSize);
			System.out.println(lightningNetworkPair.getHyperLightningNetwork().getStats());
			System.out.println("\n");
		}
	}
}
