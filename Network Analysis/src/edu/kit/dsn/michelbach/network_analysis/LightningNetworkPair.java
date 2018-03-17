package edu.kit.dsn.michelbach.network_analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/**
 * Each instance of this class can be used to generate a pair of {@code HyperLightningNetwork}s. One of them represents
 * a Classic Lightning Network. It is generated to be a scale-free network. The other {@code HyperLightningNetwork} is a
 * Hyper Lightning Network generated to have the same wealth distribution between its members as the Classic Lightning
 * Network but its payment channels can encompass more than two members.
 *
 * Each {@code LightningNetworkPair} is generated pseudo-randomly using a seed. Reusing the same seed results in the
 * same {@code LightningNetworkPair}.
 *
 * @author Christoph Michelbach
 */
public class LightningNetworkPair {
	/**
	 * This {@code LightningNetworkPair} builder has to be used to construct {@code LightningNetworkPair}. Various
	 * parameters can be set before the constructed object is returned on call of {@code generate()}.
	 */
	public static class Builder {
		private LightningNetworkPair lightningNetworkPair;
		private boolean returnedLightningNetworkPairObject = false;
		private long seed;
		private Random random;

		/**
		 * Constructs a new {@code LightningNetworkPair} {@code Builder} using the seed {@code seed}.
		 *
		 * @param seed
		 */
		public Builder(long seed) {
			this.lightningNetworkPair = new LightningNetworkPair();
			this.seed = seed;
			this.random = new Random(this.seed);
		}

		/**
		 * Checks whether {@code generate()} has already been called and throws an {@code IllegalAccessError} if so.
		 */
		private void checkSetterAvailablity() {
			if (this.returnedLightningNetworkPairObject) {
				throw new IllegalAccessError("Setters may not be used on Builder after generate() has been called.");
			}
		}

		/**
		 * Returns the built object. This makes subsequent changes of parameters impossible. This method may only be
		 * called once per instance.
		 *
		 * @return
		 */
		public LightningNetworkPair generate() {
			if (this.returnedLightningNetworkPairObject) {
				throw new IllegalAccessError("Method may only be called once.");
			}

			this.lightningNetworkPair.seed = this.seed;
			this.lightningNetworkPair.random = this.random;

			// Compute dependent fields.
			this.lightningNetworkPair.fundingContributionExponentRange = Math
					.log(((double) this.lightningNetworkPair.fundingContributionMinimum)
							/ this.lightningNetworkPair.fundingContributionMaximum);

			this.returnedLightningNetworkPairObject = true;
			return this.lightningNetworkPair;
		}

		public LightningNetworkPair.Builder setFundingContributionMaximum(long fundingContributionMaximum) {
			this.checkSetterAvailablity();
			this.lightningNetworkPair.fundingContributionMaximum = fundingContributionMaximum;
			return this;
		}

		public LightningNetworkPair.Builder setFundingContributionMinimum(long fundingContributionMinimum) {
			this.checkSetterAvailablity();
			this.lightningNetworkPair.fundingContributionMinimum = fundingContributionMinimum;
			return this;
		}

		public LightningNetworkPair.Builder setHpcParsimony(boolean hpcParsimony) {
			this.checkSetterAvailablity();
			this.lightningNetworkPair.hpcParsimony = hpcParsimony;
			return this;
		}

		public LightningNetworkPair.Builder setHyperPaymentChannelAvoidanceMinumumConnectivity(
				int hyperedgeAvoidanceMinimumConnectivity) {
			this.checkSetterAvailablity();
			this.lightningNetworkPair.hyperPaymentChannelAvoidanceMinumumConnectivity = hyperedgeAvoidanceMinimumConnectivity;
			return this;
		}

		public LightningNetworkPair.Builder setMaximumHyperPaymentChannelSize(int maximumHyperPaymentChannelSize) {
			this.checkSetterAvailablity();
			this.lightningNetworkPair.maximumHyperPaymentChannelSize = maximumHyperPaymentChannelSize;
			return this;
		}

		public LightningNetworkPair.Builder setNumberOfClassicPaymentChannels(long numberOfClassicPaymentChannels) {
			this.checkSetterAvailablity();
			this.lightningNetworkPair.numberOfClassicPaymentChannel = numberOfClassicPaymentChannels;
			return this;
		}

		public LightningNetworkPair.Builder setNumberOfMembers(long numberOfMembers) {
			this.checkSetterAvailablity();
			this.lightningNetworkPair.numberOfMembers = numberOfMembers;
			return this;
		}

	}

	/**
	 * Objects of this inner class represent Hyper Payment Channel prototypes.
	 */
	private class ProtoChannel {
		ArrayList<Member> members = new ArrayList<>();
		HashMap<Member, Long> assets = new HashMap<>();
	}

	/**
	 * The minimum amount a member may contribute to a payment channel.
	 */
	private long fundingContributionMinimum = 10_000_000L;

	/**
	 * The maximum amount a member may contribute to a payment channel.
	 */
	private long fundingContributionMaximum = 10_000_000_000L;

	private double fundingContributionExponentRange = Math
			.log(((double) this.fundingContributionMinimum) / this.fundingContributionMaximum);

	/**
	 * The maximum number of {@code Member}s of a single {@code HyperPaymentChannel}.
	 */
	private int maximumHyperPaymentChannelSize = 30;

	/**
	 * The number of members per Lightning Network.
	 */
	private long numberOfMembers = 1_000;

	/**
	 * The number of payment channels in the Classic Lightning Network.
	 */
	private long numberOfClassicPaymentChannel = (long) (this.numberOfMembers * 1.2d);

	/**
	 * If two members each are connected to at least this many members in the Classic Lightning Network, payment
	 * channels between them are not converted to Hyper Payment Channels when transforming the Classic Lightning Network
	 * to a Hyper Lightning Network. The connections between the members count towards their neighbor count which is
	 * relevant for this.
	 */
	private int hyperPaymentChannelAvoidanceMinumumConnectivity = 5;

	/**
	 * Whether the number of {@code HyperPaymentChannel}s is attempted to be kept low.
	 */
	private boolean hpcParsimony = false;

	/**
	 * The seed this {@code LightningNetworkPair} uses.
	 */
	private long seed;

	/**
	 * The source of pseudo-randomness this {@code LightningNetworkPair} uses.
	 */
	private Random random;

	/**
	 * Whether this {@code LightningNetworkPair} has been initialized. Whether a method can be used may depend on this
	 * variable.
	 */
	private boolean initialized = false;

	/**
	 * The generated Classic Lightning Network.
	 */
	private HyperLightningNetwork classicLightningNetwork = null;

	/**
	 * The neighbor counts of the Classic Lightning Network.
	 */
	private HashMap<Member, Long> clnNeighborCounts = null;

	/**
	 * How much money each member has. This is the same in the Classic Lightning Network and the Hyper Lightning
	 * Network.
	 */
	private HashMap<Member, Long> memberWealth = null;

	/**
	 * The generated Hyper Lightning Network.
	 */
	private HyperLightningNetwork hyperLightningNetwork = null;

	/**
	 * The members of the Classic Lightning Network and the Hyper Lightning Network. The members are the same in both
	 * networks.
	 */
	private LinkedList<Member> members = null;

	/**
	 * Private constructor.
	 */
	private LightningNetworkPair() {
		if (this.numberOfClassicPaymentChannel < this.numberOfMembers - 1) {
			throw new IllegalStateException(
					"Number of Classic Payment Channels may not be smaller than number of members minus 1.");
		}
	}

	/**
	 * Deterministically constructs a Classic Lightning Network based on the pseudorandom number generator
	 * {@code random}.
	 */
	private void constructClassicLightningNetwork() {
		assert (this.numberOfMembers >= 2);
		assert (this.numberOfClassicPaymentChannel >= 1);

		this.classicLightningNetwork = new HyperLightningNetwork();

		this.members = new LinkedList<>();
		ArrayList<HyperPaymentChannel> channels = new ArrayList<>();
		this.clnNeighborCounts = new HashMap<>();
		this.memberWealth = new HashMap<>();
		ArrayList<Member> attchments = new ArrayList<>();

		for (long i = 0; i < this.numberOfMembers; i++) {
			Member member = new Member(this.classicLightningNetwork);

			this.members.add(member);
			this.clnNeighborCounts.put(member, 0L);
			this.memberWealth.put(member, 0L);

			this.classicLightningNetwork.addMember(member);
		}

		LinkedList<Member> memberQueue = new LinkedList<>(this.members);

		Member member1 = memberQueue.removeFirst();
		Member member2 = memberQueue.removeFirst();
		assert (member1 != member2);
		long fundingContribution1 = this.getRandomFundingContribution();
		long fundingContribution2 = this.getRandomFundingContribution();

		HyperPaymentChannel hyperPaymentChannel1 = new HyperPaymentChannel(this.classicLightningNetwork,
				Arrays.asList(member1, member2), Arrays.asList(fundingContribution1, fundingContribution2));
		this.classicLightningNetwork.addHyperPaymentChannel(hyperPaymentChannel1);

		channels.add(hyperPaymentChannel1);
		this.clnNeighborCounts.put(member1, 1L);
		this.clnNeighborCounts.put(member2, 1L);
		this.memberWealth.put(member1, fundingContribution1);
		this.memberWealth.put(member2, fundingContribution2);
		attchments.add(member1);
		attchments.add(member2);

		for (long currentNumberOfChannels = 1; currentNumberOfChannels < this.numberOfClassicPaymentChannel; currentNumberOfChannels++) {
			if (memberQueue.size() == 0) {
				memberQueue = new LinkedList<>(this.members);
			}

			Member member = memberQueue.removeFirst();
			Member partner = null;

			while (partner == null || partner == member) {
				// Pick random already-attached member with distribution proportional to number of existing attachments.
				partner = attchments.get(this.random.nextInt(attchments.size()));
			}

			// Open a new channel for the two chosen members.

			long memberFundingContribution = this.getRandomFundingContribution();
			long partnerFundingContribution = this.getRandomFundingContribution();

			HyperPaymentChannel hyperPaymentChannel = new HyperPaymentChannel(this.classicLightningNetwork,
					Arrays.asList(member, partner),
					Arrays.asList(memberFundingContribution, partnerFundingContribution));
			this.classicLightningNetwork.addHyperPaymentChannel(hyperPaymentChannel);

			channels.add(hyperPaymentChannel);
			this.clnNeighborCounts.put(member, this.clnNeighborCounts.get(member) + 1);
			this.clnNeighborCounts.put(partner, this.clnNeighborCounts.get(partner) + 1);
			this.memberWealth.put(member, this.memberWealth.get(member) + memberFundingContribution);
			this.memberWealth.put(partner, this.memberWealth.get(partner) + partnerFundingContribution);
			attchments.add(member);
			attchments.add(partner);
		}
	}

	private void constructHyperLightningNetwork() {
		ArrayList<HyperPaymentChannel> clnChannels = this.classicLightningNetwork.getHyperPaymentChannels();

		// Unify dead ends.

		ArrayList<Member> deadEndNodes = new ArrayList<>();
		HashMap<Member, ArrayList<Member>> deadEndAttachments = new HashMap<>();
		ArrayList<Member> connectors = new ArrayList<>();

		for (Member deadEndNode : this.members) {
			if (this.clnNeighborCounts.get(deadEndNode) == 1) {

				deadEndNodes.add(deadEndNode);

				ArrayList<HyperPaymentChannel> channels = deadEndNode
						.getListOfHyperPaymentChannels(this.classicLightningNetwork);
				assert (channels.size() == 1);
				HyperPaymentChannel channel = channels.get(0);

				assert (clnChannels.contains(channel));
				clnChannels.remove(channel);

				ArrayList<Member> membersOfSoleConnectingChannel = channel.getMembers();
				assert (membersOfSoleConnectingChannel.size() == 2);

				Member connector = null;
				for (Member member : membersOfSoleConnectingChannel) {
					if (member != deadEndNode) {
						connector = member;
					}
				}
				assert (connector != null);

				if (!connectors.contains(connector)) {
					connectors.add(connector);
					deadEndAttachments.put(connector, new ArrayList<>());
				}

				deadEndAttachments.get(connector).add(deadEndNode);
			}
		}

		LinkedList<ProtoChannel> protoChannels = new LinkedList<>();

		for (Member connector : connectors) {
			ArrayList<Member> connectedDeadEnds = deadEndAttachments.get(connector);

			// Don't exceed maximum payment channel size in proto channels.
			int numberOfFractionsNecessary = (int) Math
					.ceil(((double) connectedDeadEnds.size()) / (this.maximumHyperPaymentChannelSize - 1));
			int idealMaximumSize = 1
					+ (int) Math.ceil(((double) connectedDeadEnds.size()) / numberOfFractionsNecessary);

			Iterator<Member> connectedDeadEndsItr = connectedDeadEnds.iterator();
			ProtoChannel protoChannel = null;
			while (connectedDeadEndsItr.hasNext()) {
				for (int i = 0; i < idealMaximumSize && connectedDeadEndsItr.hasNext(); i++) {
					if (i == 0) {
						protoChannel = new ProtoChannel();
						protoChannels.add(protoChannel);
						protoChannel.members.add(connector);
						protoChannel.assets.put(connector, 0L);
					} else {
						Member deadEndNode = connectedDeadEndsItr.next();
						protoChannel.members.add(deadEndNode);

						ArrayList<HyperPaymentChannel> deadEndNodeChannels = deadEndNode
								.getListOfHyperPaymentChannels(this.classicLightningNetwork);
						assert (deadEndNodeChannels.size() == 1);
						HyperPaymentChannel channel = deadEndNodeChannels.get(0);

						protoChannel.assets.put(deadEndNode, channel.getBalanceOfMember(deadEndNode));
						protoChannel.assets.put(connector,
								protoChannel.assets.get(connector) + channel.getBalanceOfMember(connector));
					}
				}
			}
		}

		this.unitfyProtoChannels(protoChannels);

		if (!this.hpcParsimony) {
			// Contract paths.

			Iterator<HyperPaymentChannel> clnChannelsItr = clnChannels.iterator();
			while (clnChannelsItr.hasNext()) {
				HyperPaymentChannel channel = clnChannelsItr.next();

				Iterator<Member> membersItr = channel.getMembers().iterator();
				Member member1 = membersItr.next();
				Member member2 = membersItr.next();
				assert (!membersItr.hasNext());

				if (member1.getListOfHyperPaymentChannels(this.classicLightningNetwork)
						.size() < this.hyperPaymentChannelAvoidanceMinumumConnectivity
						|| member2.getListOfHyperPaymentChannels(this.classicLightningNetwork)
								.size() < this.hyperPaymentChannelAvoidanceMinumumConnectivity) {
					clnChannelsItr.remove();
					ProtoChannel protoChannel = new ProtoChannel();
					protoChannels.add(protoChannel);
					protoChannel.members.add(member1);
					protoChannel.members.add(member2);
					protoChannel.assets.put(member1, channel.getBalanceOfMember(member1));
					protoChannel.assets.put(member2, channel.getBalanceOfMember(member2));
				}
			}

			this.unitfyProtoChannels(protoChannels);
		}

		// Add remaining channels. These are the channels of big players if hpcParsimony is false, and all but the
		// outermost ones otherwise.
		for (HyperPaymentChannel channel : clnChannels) {
			Iterator<Member> membersItr = channel.getMembers().iterator();
			Member member1 = membersItr.next();
			Member member2 = membersItr.next();
			assert (!membersItr.hasNext());

			ProtoChannel protoChannel = new ProtoChannel();
			protoChannels.add(protoChannel);
			protoChannel.members.add(member1);
			protoChannel.members.add(member2);
			protoChannel.assets.put(member1, channel.getBalanceOfMember(member1));
			protoChannel.assets.put(member2, channel.getBalanceOfMember(member2));
		}

		this.hyperLightningNetwork = new HyperLightningNetwork();
		this.hyperLightningNetwork.addMembers(this.members);

		for (ProtoChannel protoChannel : protoChannels) {
			LinkedList<Member> members = new LinkedList<>(protoChannel.members);
			LinkedList<Long> assets = new LinkedList<>();

			for (Member member : members) {
				assets.add(protoChannel.assets.get(member));
			}

			HyperPaymentChannel hyperPaymentChannel = new HyperPaymentChannel(this.hyperLightningNetwork, members,
					assets);
			this.hyperLightningNetwork.addHyperPaymentChannel(hyperPaymentChannel);
		}
	}

	/**
	 * Returns the generated Classic Lightning Network iff this {@code LightningNetworkPair} has been initialized.
	 * Otherwise, an {@code IllegalStateException} is thrown.
	 */
	public HyperLightningNetwork getClassicLightningNetwork() {
		if (!this.initialized) {
			throw new IllegalStateException(
					"getClassicLightningNetwork() may only be called on initialized LightningNetworkPairs.");
		}

		assert (this.classicLightningNetwork != null);

		return this.classicLightningNetwork;
	}

	/**
	 * Returns the generated Hyper Lightning Network iff this {@code LightningNetworkPair} has been initialized.
	 * Otherwise, an {@code IllegalStateException} is thrown.
	 */
	public HyperLightningNetwork getHyperLightningNetwork() {
		if (!this.initialized) {
			throw new IllegalStateException(
					"getHyperLightningNetwork() may only be called on initialized LightningNetworkPairs.");
		}

		assert (this.hyperLightningNetwork != null);

		return this.hyperLightningNetwork;
	}

	/**
	 * Returns a randomly chosen funding contribution with exponential distribution between
	 * {@code fundingContributionMinimum} and {@code fundingContributionMaximum}.
	 *
	 * @return
	 */
	private long getRandomFundingContribution() {
		return (long) (this.fundingContributionMaximum
				* Math.pow(Math.E, this.random.nextDouble() * this.fundingContributionExponentRange));
	}

	/**
	 * Returns the seed this {@code LightningNetworkPair} uses.
	 *
	 * @return
	 */
	public long getSeed() {
		return this.seed;
	}

	/**
	 * Initializes this {@code LightningNetworkPair}. Initialization may only happen once. Subsequent calls to this
	 * method result in an {@code IllegalStateException} being thrown.
	 *
	 * This is a potentially long-running method.
	 *
	 * @throws IllegalStateException
	 */
	public void init() {
		if (this.initialized) {
			throw new IllegalStateException("Initialization of a LightningNetworkPair may only happen once.");
		}

		this.constructClassicLightningNetwork();
		this.constructHyperLightningNetwork();

		this.initialized = true;
	}

	/**
	 * Unifies the proto-channels {@code protoChannels}. This method does not return a new list but instead has a
	 * side-effect on the given one.
	 *
	 * @param protoChannels
	 */
	private void unitfyProtoChannels(LinkedList<ProtoChannel> protoChannels) {
		// Sort proto-channels sorted in ascending order by number of members.
		protoChannels.sort((o1, o2) -> (o1.members.size() - o2.members.size()));

		if (protoChannels.size() == 0) {
			// Nothing to unify.
			return;
		}

		// Unify proto-channels.
		outerLoop: while (true) {
			ProtoChannel smallestProtoChannel = protoChannels.getFirst();

			Iterator<ProtoChannel> protoChannelsDescItr = protoChannels.descendingIterator();
			while (protoChannelsDescItr.hasNext()) {
				ProtoChannel largerProtoChannel = protoChannelsDescItr.next();
				if (smallestProtoChannel == largerProtoChannel) {
					break outerLoop;
				}

				if (smallestProtoChannel.members.size()
						+ largerProtoChannel.members.size() <= this.maximumHyperPaymentChannelSize) {
					protoChannels.removeFirst();

					// Doesn't screw up iterator because iterator is thrown away right after this.
					for (Member member : smallestProtoChannel.members) {
						if (!largerProtoChannel.members.contains(member)) {
							largerProtoChannel.members.add(member);
						}

						largerProtoChannel.assets.put(member,
								smallestProtoChannel.assets.get(member) + (largerProtoChannel.assets.containsKey(member)
										? largerProtoChannel.assets.get(member)
										: 0));
					}

					// Sorting {@code protoChannels} again is not necessary.
					break;
				}
			}
		}
	}
}
