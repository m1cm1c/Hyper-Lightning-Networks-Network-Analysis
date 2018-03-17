package edu.kit.dsn.michelbach.network_analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

class HyperLightningNetworkTest {

	@Test
	void testGetCheapestPaymentRoute_hyperChannelConnection() {
		HyperLightningNetwork hyperLightningNetwork = new HyperLightningNetwork();
		Member[] members = new Member[10];

		for (int i = 0; i < 10; i++) {
			members[i] = new Member(hyperLightningNetwork);
			hyperLightningNetwork.addMember(members[i]);
		}

		HyperPaymentChannel h1 = new HyperPaymentChannel(hyperLightningNetwork, Arrays.asList(members[0], members[8]),
				Arrays.asList(70_000_000L, 30_000_000L));
		HyperPaymentChannel h2 = new HyperPaymentChannel(hyperLightningNetwork,
				Arrays.asList(members[9], members[1], members[0]),
				Arrays.asList(70_000_000L, 30_000_000L, 11_000_000L));
		HyperPaymentChannel h3 = new HyperPaymentChannel(hyperLightningNetwork,
				Arrays.asList(members[1], members[3], members[4]),
				Arrays.asList(90_000_000L, 30_000_000L, 60_000_000L));
		HyperPaymentChannel h4 = new HyperPaymentChannel(hyperLightningNetwork,
				Arrays.asList(members[2], members[3], members[4]),
				Arrays.asList(220_000_000L, 80_000_000L, 110_000_000L));
		HyperPaymentChannel h5 = new HyperPaymentChannel(hyperLightningNetwork,
				Arrays.asList(members[7], members[6], members[2], members[5]),
				Arrays.asList(380_000_000L, 370_000_000L, 130_000_000L, 120_000_000L));

		hyperLightningNetwork.addHyperPaymentChannel(h2);
		hyperLightningNetwork.addHyperPaymentChannel(h4);
		hyperLightningNetwork.addHyperPaymentChannel(h5);
		hyperLightningNetwork.addHyperPaymentChannel(h3);
		hyperLightningNetwork.addHyperPaymentChannel(h1);

		PaymentRoute paymentRoute = hyperLightningNetwork.getCheapestPaymentRoute(members[8], members[6], 10_000_000L);

		assertNotNull(paymentRoute);

		ArrayList<Member> hopsTaken = paymentRoute.getListOfHops();
		ArrayList<HyperPaymentChannel> channelsTaken = paymentRoute.getListOfHyperPaymentChannels();

		assertTrue(hopsTaken.get(0) == members[8]);
		assertTrue(hopsTaken.get(1) == members[0]);
		assertTrue(hopsTaken.get(2) == members[1]);
		assertTrue(hopsTaken.get(3) == members[3] || hopsTaken.get(3) == members[4]);
		assertTrue(hopsTaken.get(4) == members[2]);
		assertTrue(hopsTaken.get(5) == members[6]);

		assertTrue(channelsTaken.equals(Arrays.asList(h1, h2, h3, h4, h5)));
		assertFalse(channelsTaken.equals(Arrays.asList(h1, h2, h2, h4, h5)));

	}

	@Test
	void testGetCheapestPaymentRoute_simpleConnection() {
		HyperLightningNetwork hyperLightningNetwork = new HyperLightningNetwork();
		Member[] members = new Member[10];

		for (int i = 0; i < 10; i++) {
			members[i] = new Member(hyperLightningNetwork);
			hyperLightningNetwork.addMember(members[i]);
		}

		HyperPaymentChannel h1 = new HyperPaymentChannel(hyperLightningNetwork, Arrays.asList(members[0], members[1]),
				Arrays.asList(70_000_000L, 30_000_000L));

		hyperLightningNetwork.addHyperPaymentChannel(h1);

		PaymentRoute paymentRoute = hyperLightningNetwork.getCheapestPaymentRoute(members[0], members[1], 10_000_000L);

		assertNotNull(paymentRoute);

		LinkedList<Member> onlyPossibleHops = new LinkedList<>();
		onlyPossibleHops.add(members[0]);
		onlyPossibleHops.add(members[1]);

		assertEquals(paymentRoute.getListOfHops(), onlyPossibleHops);

		LinkedList<Member> incompleteHops = new LinkedList<>();
		incompleteHops.add(members[0]);

		assertNotEquals(paymentRoute.getListOfHops(), incompleteHops);
	}

	@Test
	void testGetCheapestPaymentRoute_unconnectedNodes() {
		HyperLightningNetwork hyperLightningNetwork = new HyperLightningNetwork();
		Member[] members = new Member[10];

		for (int i = 0; i < 10; i++) {
			members[i] = new Member(hyperLightningNetwork);
			hyperLightningNetwork.addMember(members[i]);
		}

		HyperPaymentChannel h1 = new HyperPaymentChannel(hyperLightningNetwork, Arrays.asList(members[0], members[1]),
				Arrays.asList(70_000_000L, 30_000_000L));

		hyperLightningNetwork.addHyperPaymentChannel(h1);

		PaymentRoute paymentRoute = hyperLightningNetwork.getCheapestPaymentRoute(members[0], members[4], 10_000_000L);

		assertNull(paymentRoute);
	}

}
