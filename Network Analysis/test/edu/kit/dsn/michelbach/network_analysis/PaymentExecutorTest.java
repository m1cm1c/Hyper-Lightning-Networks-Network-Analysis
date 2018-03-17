package edu.kit.dsn.michelbach.network_analysis;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class PaymentExecutorTest {

	private void testConservationOfFunds(HyperLightningNetwork hln) {
		this.validateFurtuneOfPaymentChannelsInHyperLightningNetwork(hln);

		PaymentExecutor paymentExecutor = new PaymentExecutor.Builder(hln, 0L).setNumberOfPayments(100).generate();
		paymentExecutor.init();

		this.validateFurtuneOfPaymentChannelsInHyperLightningNetwork(hln);
	}

	@Test
	void testConservationOfFundsHelper() {
		LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(0L)
				.setMaximumHyperPaymentChannelSize(30).setNumberOfMembers(100).setNumberOfClassicPaymentChannels(120)
				.setHyperPaymentChannelAvoidanceMinumumConnectivity(20).generate();
		lightningNetworkPair.init();

		HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
		HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

		this.testConservationOfFunds(cln);
		this.testConservationOfFunds(hln);

	}

	/**
	 * Returns {@code true} iff the funding amount of each {@code HyperPaymentChannel} in the given
	 * {@code HyperLightningNetwork} matches the sum of its {@code Member}s' balances in it.
	 *
	 * @param hln
	 * @return
	 */
	private boolean validateFurtuneOfPaymentChannelsInHyperLightningNetwork(HyperLightningNetwork hln) {
		return hln
				.getHyperPaymentChannels().stream().filter(channel -> channel.getFundingAmount() != channel
						.getBalances().values().stream().mapToLong(Long::longValue).sum())
				.collect(Collectors.toSet()).size() > 0;
	}

}
