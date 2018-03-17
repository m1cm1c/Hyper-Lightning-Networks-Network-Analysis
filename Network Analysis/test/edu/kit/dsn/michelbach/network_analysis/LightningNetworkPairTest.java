package edu.kit.dsn.michelbach.network_analysis;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LightningNetworkPairTest {

	/**
	 * Tests whether each member's fortune is the same in the Classic Lightning Network and in the corresponding Hyper
	 * Lightning Network.
	 */
	@Test
	void testEqualFortunes() {
		LightningNetworkPair lightningNetworkPair = new LightningNetworkPair.Builder(0L).generate();
		lightningNetworkPair.init();

		HyperLightningNetwork cln = lightningNetworkPair.getClassicLightningNetwork();
		HyperLightningNetwork hln = lightningNetworkPair.getHyperLightningNetwork();

		ArrayList<Member> clnMembers = cln.getMembers();
		ArrayList<Member> hlnMembers = hln.getMembers();

		assertTrue(clnMembers.equals(hlnMembers));

		ArrayList<Member> members = clnMembers;

		Set<Member> membersWithMismatchingFortunes = members.stream()
				.filter(member -> member.getFortune(cln) != member.getFortune(hln)).collect(Collectors.toSet());
		assertTrue(membersWithMismatchingFortunes.size() == 0);

	}

}
