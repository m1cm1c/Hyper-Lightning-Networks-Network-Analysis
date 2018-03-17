package edu.kit.dsn.michelbach.network_analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;
import edu.uci.ics.jung.graph.SetHypergraph;

/**
 * Objects of this class represent Hyper Lightning Networks. Each {@code HyperLightningNetwork} consists of
 * {@code Member}s and {@code HyperPaymentChannel}s.
 *
 * @author Christoph Michelbach
 */
public class HyperLightningNetwork {
	/**
	 * The hypergraph representing this {@code HyperLightningNetwork}.
	 */
	private SetHypergraph<Member, HyperPaymentChannel> hypergraph = new SetHypergraph<>();

	/**
	 * List of {@code Member}s.
	 */
	private ArrayList<Member> members = new ArrayList<>();

	/**
	 * List of {@code HyperPaymentChannel}s.
	 */
	private ArrayList<HyperPaymentChannel> hyperPaymentChannels = new ArrayList<>();

	/**
	 * List of fee intakes.
	 */
	private HashMap<Member, Long> feeIntakes = new HashMap<>();

	/**
	 * Adds {@code hyperPaymentChannel} as a Hyper Payment Channel to this {@code HyperLightningNetwork}.
	 */
	public void addHyperPaymentChannel(HyperPaymentChannel hyperPaymentChannel) {
		if (!this.hypergraph.containsEdge(hyperPaymentChannel)) {
			this.hyperPaymentChannels.add(hyperPaymentChannel);
			this.hypergraph.addEdge(hyperPaymentChannel, hyperPaymentChannel.getMembers());
		}
	}

	/**
	 * Adds {@code member} as a member to this {@code HyperLightningNetwork}.
	 */
	public void addMember(Member member) {
		if (!member.getHyperLightningNetworks().contains(this)) {
			member.addToHyperLightningNetwork(this);
		}

		if (!this.hypergraph.containsVertex(member)) {
			this.members.add(member);
			this.hypergraph.addVertex(member);
			this.feeIntakes.put(member, 0L);
		}
	}

	/**
	 * Adds all elements of {@code members} as members to this {@code HyperLightningNetwork}.
	 */
	public void addMembers(LinkedList<Member> members) {
		for (Member member : members) {
			this.addMember(member);
		}
	}

	/**
	 * Returns the cheapest payment route for a payment of amount {@code amount} from {@code origin} to
	 * {@code destination}. If there are several cheapest payment routes, any one of them may be returned. If there are
	 * none, {@code null} is returned.
	 *
	 * @param origin
	 * @param destination
	 * @param amount
	 * @return
	 */
	public PaymentRoute getCheapestPaymentRoute(Member origin, Member destination, long amount) {
		HashMap<Member, Long> distances = new HashMap<>();
		HashMap<Member, Member> previous = new HashMap<>();
		HashMap<Member, LinkedList<HyperPaymentChannel>> channelStacks = new HashMap<>();

		PriorityQueue<Member> queue = new PriorityQueue<>(new Comparator<Member>() {

			@Override
			public int compare(Member o1, Member o2) {
				long diff = distances.get(o1) - distances.get(o2);

				if (diff < Integer.MIN_VALUE) {
					return Integer.MIN_VALUE;
				}

				if (diff > Integer.MAX_VALUE) {
					return Integer.MAX_VALUE;
				}

				return (int) diff;
			}
		});

		distances.put(destination, 0L);
		previous.put(destination, destination);
		channelStacks.put(destination, new LinkedList<>());

		for (Member v : this.members) {
			if (v != destination) {
				distances.put(v, Long.MAX_VALUE); // Long.MAX_VALUE represents infinity.
				previous.put(v, null);
			}

			queue.add(v);
		}

		while (queue.size() > 0) {
			Member u = queue.poll();

			if (distances.get(u) == Long.MAX_VALUE) {
				break; // It doesn't make sense to shorten paths on nodes with infinite distance, so abort the search in
						// this case.
			}

			for (HyperPaymentChannel incidentHyperPaymentChannel : u.getListOfHyperPaymentChannels(this)) {
				if (channelStacks.get(u).contains(incidentHyperPaymentChannel)) {
					continue; // It doesn't make sense to go through the same channel twice.
				}

				for (Member v : incidentHyperPaymentChannel.getMembers()) {
					if (v == u) {
						continue; // Don't treat a member as their own neighbor.
					}

					assert (distances.get(u) != Long.MAX_VALUE);

					long additionalFee = incidentHyperPaymentChannel.getFeeForLightningTransaction(v, u,
							amount + distances.get(u), channelStacks.get(u).size());

					if (additionalFee == -1) {
						continue; // Channel cannot be used for transaction.
					}

					long alternativeDistance = distances.get(u) + additionalFee;

					assert (alternativeDistance >= 0);

					if (alternativeDistance < distances.get(v)) {
						queue.remove(v);
						distances.put(v, alternativeDistance);
						queue.add(v);

						LinkedList<HyperPaymentChannel> channelStack = new LinkedList<>(channelStacks.get(u));
						channelStack.addFirst(incidentHyperPaymentChannel);
						channelStacks.put(v, channelStack);

						previous.put(v, u);
					}

					assert (distances.get(u) >= 0 || distances.get(v) >= 0);
				}
			}
		}

		if (distances.get(origin) == Long.MAX_VALUE) { // No route could be found.
			return null;
		}

		// Construct route.
		ArrayList<Member> hops = new ArrayList<>();

		hops.add(origin);
		Member currentNode = origin;

		while (previous.get(currentNode) != currentNode) { // Remember: previous.put(destination, destination);
			currentNode = previous.get(currentNode);
			hops.add(currentNode);
		}

		return new PaymentRoute(hops, channelStacks.get(origin));
	}

	/**
	 * Returns the diameter of this {@code HyperLightningNetwork}.
	 *
	 * @return
	 */
	public double getDiameter() {
		return DistanceStatistics.diameter(this.hypergraph);
	}

	/**
	 * Returns how much each member made / lost due to fees.
	 *
	 * @return
	 */
	public HashMap<Member, Long> getFeeIntakes() {
		return new HashMap<>(this.feeIntakes);
	}

	/**
	 * Returns this {@code HyperLightningNetwork}'s set of {@code HyperPaymentChannel}s.
	 *
	 * @return
	 */
	public ArrayList<HyperPaymentChannel> getHyperPaymentChannels() {
		return new ArrayList<HyperPaymentChannel>(this.hyperPaymentChannels);
	}

	/**
	 * Returns the set of {@code Member}s of this {@code HyperLightningNetwork}.
	 *
	 * @return
	 */
	public ArrayList<Member> getMembers() {
		return new ArrayList<>(this.members);
	}

	/**
	 * Returns the number of channel memberships in this {@code HyperLightningNetwork}. This is the sum of the
	 * membership counts of the {@code HyperPaymentChannels} in this {@code HyperLightningNetwork}.
	 *
	 * @return
	 */
	public int getNumberOfChannelMemberships() {
		return this.hyperPaymentChannels.stream()
				.mapToInt(hyperPaymentChannel -> hyperPaymentChannel.getMembers().size()).sum();
	}

	/**
	 * Returns the number of payment channels with only two members this {@code HyperLightningNetwork} contains.
	 *
	 * @return
	 */
	public long getNumberOfClassicPaymentChannels() {
		return this.hyperPaymentChannels.stream()
				.filter(hyperPaymentChannel -> hyperPaymentChannel.getNumberOfMembers() == 2).count();
	}

	/**
	 * Returns the number of {@code HyperPaymentChannel}s in this {@code HyperLightningNetwork}.
	 *
	 * @return
	 */
	public int getNumberOfHyperPaymentChannels() {
		return this.hypergraph.getEdgeCount();
	}

	/**
	 * Returns the number of payment channels with more than two members this {@code HyperLightningNetwork} contains.
	 *
	 * @return
	 */
	public long getNumberOfProperHyperPaymentChannels() {
		return this.hyperPaymentChannels.stream()
				.filter(hyperPaymentChannel -> hyperPaymentChannel.getNumberOfMembers() > 2).count();
	}

	/**
	 * Returns a string listing various measurements of this {@code HyperLightningNetwork}.
	 *
	 * @return
	 */
	public String getStats() {
		String returnValue = "";

		returnValue += "Number of channels:\t\t\t\t" + this.getNumberOfHyperPaymentChannels() + System.lineSeparator();
		returnValue += "Number of channel memberships:\t\t\t" + this.getNumberOfChannelMemberships()
				+ System.lineSeparator();
		returnValue += "Diameter:\t\t\t\t\t" + this.getDiameter() + System.lineSeparator();
		returnValue += "Number of channels per member:\t\t\t"
				+ (((double) this.getNumberOfHyperPaymentChannels()) / this.hypergraph.getVertexCount())
				+ System.lineSeparator();
		returnValue += "Avg. number of channel memberships per member:\t"
				+ (((double) this.getNumberOfChannelMemberships()) / this.hypergraph.getVertexCount())
				+ System.lineSeparator();
		returnValue += "Total amount of on-chain storage space req.:\t"
				+ (this.hyperPaymentChannels.stream()
						.mapToInt(hyperPaymentChannel -> hyperPaymentChannel
								.getMinimumAmountOfFullCycleOnChainStorageSpace())
						.sum()) / 1000d
				+ " MB" + System.lineSeparator();
		returnValue += "Average fortune:\t\t\t\t"
				+ this.members.stream().mapToLong(member -> member.getFortune(this)).average().getAsDouble()
						/ 1_000_000d
				+ " €" + System.lineSeparator();
		returnValue += "Minimum fortune:\t\t\t\t"
				+ this.members.stream().mapToLong(member -> member.getFortune(this)).min().getAsLong() / 1_000_000d
				+ " €" + System.lineSeparator();
		returnValue += "Average max. receipt:\t\t\t\t"
				+ this.members.stream().mapToLong(member -> member.getMaximumReceipt(this)).average().getAsDouble()
						/ 1_000_000d
				+ " €" + System.lineSeparator();
		returnValue += "Minimum max. receipt:\t\t\t\t"
				+ this.members.stream().mapToLong(member -> member.getMaximumReceipt(this)).min().getAsLong()
						/ 1_000_000d
				+ " €" + System.lineSeparator();
		returnValue += "Proper HPC proportion:\t\t\t\t"
				+ (((double) this.getNumberOfProperHyperPaymentChannels()) / this.getNumberOfHyperPaymentChannels())
				+ System.lineSeparator();

		return returnValue;
	}

	/**
	 * Attempts to perform a payment over amount {@code amount} from {@code origin} to {@code destination}. Returns a
	 * non-negative number indicating the lightning fee iff the payment was performed successfully. Otherwise,
	 * {@code -1} is returned.
	 *
	 * If the payment could not be performed, the {@code HyperLightningNetwork} is left unchanged.
	 *
	 * @param origin
	 * @param destination
	 * @param amount
	 * @return
	 */
	public long performPayment(Member origin, Member destination, long amount) {
		PaymentRoute paymentRoute = this.getCheapestPaymentRoute(origin, destination, amount);

		if (paymentRoute == null) {
			// Payment is unroutable.
			return -1L;
		}

		long amountOfFees = paymentRoute.getTotalFees(amount);
		amount += amountOfFees;

		ArrayList<HyperPaymentChannel> channels = paymentRoute.getListOfHyperPaymentChannels();
		ArrayList<Member> members = paymentRoute.getListOfHops();

		for (int i = channels.size() - 1; i >= 0; i--) {
			HyperPaymentChannel channel = channels.get(i);
			Member in = members.get(i);
			Member out = members.get(i + 1);
			long fee = channel.getFeeForLightningTransaction(in, out, amount, i);
			amount -= fee;

			channel.performPayment(in, out, amount, i);
		}

		return amountOfFees;
	}

	/**
	 * Takes notice of the fact that {@code member} earned {@code amount} as fees. {@code amount} may be negative.
	 *
	 * @param member
	 * @param amount
	 */
	public void reportFeeIntake(Member member, long amount) {
		this.feeIntakes.put(member, this.feeIntakes.get(member) + amount);
	}

	/**
	 * Returns the Hyper Lightning Network as a Graph ML hypergraph.
	 *
	 * @return
	 */
	public String toGraphMl() {
		String returnValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator();
		returnValue += "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" >" + System.lineSeparator();
		returnValue += "<graph id=\"G\" edgedefault=\"undirected\">" + System.lineSeparator();

		HashMap<Member, Integer> memberHashMap = new HashMap<>();

		int memberCounter = 0;
		for (Member member : this.members) {
			memberCounter++;
			memberHashMap.put(member, memberCounter);
			returnValue += "<node id=\"n" + memberCounter + "\"/>" + System.lineSeparator();
		}
		for (HyperPaymentChannel channel : this.hyperPaymentChannels) {
			returnValue += "<hyperedge>" + System.lineSeparator();

			ArrayList<Member> members = channel.getMembers();
			for (Member member : members) {
				returnValue += "<endpoint node=\"n" + memberHashMap.get(member) + "\"/>" + System.lineSeparator();
			}

			returnValue += "</hyperedge>" + System.lineSeparator();
		}

		returnValue += "</graph>" + System.lineSeparator();
		returnValue += "</graphml>" + System.lineSeparator();

		return returnValue;
	}

	/**
	 * Returns the Hyper Lightning Network as a Graph ML graph where each hyperedge has been replaced by a clique.
	 *
	 * @return
	 */
	public String toGraphMlWithCliques() {
		String returnValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator();
		returnValue += "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" >" + System.lineSeparator();
		returnValue += "<graph id=\"G\" edgedefault=\"undirected\">" + System.lineSeparator();

		HashMap<Member, Integer> memberHashMap = new HashMap<>();

		int memberCounter = 0;
		for (Member member : this.members) {
			memberCounter++;
			memberHashMap.put(member, memberCounter);
			returnValue += "<node id=\"n" + memberCounter + "\"/>" + System.lineSeparator();
		}

		int edgeCounter = 0;
		for (HyperPaymentChannel channel : this.hyperPaymentChannels) {
			ArrayList<Member> members = channel.getMembers();
			if (members.size() < 2) {
				continue;
			}

			for (Member member1 : members) {
				Iterator<Member> membersItr = members.iterator();

				Member member2 = null;
				while (member2 != member1) {
					member2 = membersItr.next();
				}

				while (membersItr.hasNext()) {
					member2 = membersItr.next();

					edgeCounter++;
					returnValue += "<edge id=\"e" + edgeCounter + "\" source=\"n" + memberHashMap.get(member1)
							+ "\" target=\"n" + memberHashMap.get(member2) + "\"/>" + System.lineSeparator();
				}
			}

		}

		returnValue += "</graph>" + System.lineSeparator();
		returnValue += "</graphml>" + System.lineSeparator();

		return returnValue;
	}

	/**
	 * Returns a string representing this {@code HyperLightningNetwork}.
	 */
	@Override
	public String toString() {
		String returnValue = "";

		HashMap<Member, Integer> memberHashMap = new HashMap<>();

		int memberCounter = 0;
		for (Member member : this.members) {
			memberCounter++;
			memberHashMap.put(member, memberCounter);
		}

		int channelCounter = 0;
		for (HyperPaymentChannel channel : this.hyperPaymentChannels) {
			channelCounter++;

			returnValue += "C" + channelCounter + ":\t";

			boolean firstIteration = true;
			ArrayList<Member> members = channel.getMembers();
			for (Member member : members) {
				if (firstIteration) {
					firstIteration = false;
				} else {
					returnValue += ",\t";
				}

				returnValue += "M" + memberHashMap.get(member);
			}

			returnValue += System.lineSeparator();
		}

		return returnValue;
	}
}
