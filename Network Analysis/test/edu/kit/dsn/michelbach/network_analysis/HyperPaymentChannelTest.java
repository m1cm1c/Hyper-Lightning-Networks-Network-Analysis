package edu.kit.dsn.michelbach.network_analysis;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

class HyperPaymentChannelTest {

	@Test
	void testConservationOfFunds() {
		LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(0L).generate();
		lightningNetworkPair.init();

		HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

		ArrayList<HyperPaymentChannel> channels = hln.getHyperPaymentChannels();
		for (HyperPaymentChannel channel : channels) {
			Random random = new Random(0L);
			ArrayList<Member> memberList = new ArrayList<>(channel.getMembers());
			assertTrue(memberList.size() >= 2);

			Member sender = memberList.get(random.nextInt(memberList.size()));

			Member receiver = sender;
			while (receiver == sender) {
				receiver = memberList.get(random.nextInt(memberList.size()));
			}

			long sumOfFundsBeforeSending = channel.getBalances().values().stream().mapToLong(Long::longValue).sum();

			channel.performPayment(sender, receiver, 1000, 1);
			channel.performPayment(sender, receiver, 1000, 3);
			channel.performPayment(sender, receiver, 1000, 5);
			channel.performPayment(sender, receiver, 1000, 17);

			long sumOfFundsAfterSending = channel.getBalances().values().stream().mapToLong(Long::longValue).sum();

			assertEquals(sumOfFundsBeforeSending, sumOfFundsAfterSending);
		}
	}

}
