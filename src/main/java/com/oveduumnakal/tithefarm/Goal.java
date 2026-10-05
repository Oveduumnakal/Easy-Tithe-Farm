/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oveduumnakal.tithefarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Progress toward the combined cost of every ticked reward, built from the config and the spendable points. A pure
 * value: nothing here reads the client.
 */
final class Goal
{
	/** Most points a player can hold; points earned past it are lost. */
	static final int POINTS_CAP = 16_000;

	/** The goal when no reward is ticked. */
	static final Goal NONE = new Goal(Collections.emptyList(), 0, 1);

	/** One ticked reward and how many of it. */
	static final class Selection
	{
		private final RewardItem item;
		private final int quantity;

		Selection(RewardItem item, int quantity)
		{
			this.item = item;
			this.quantity = quantity;
		}

		/** The reward. */
		RewardItem getItem()
		{
			return item;
		}

		/** How many of it, always 1 for a one-time reward. */
		int getQuantity()
		{
			return quantity;
		}
	}

	private final List<Selection> selections;
	private final int points;
	private final int pending;
	private final int total;
	private final int cropCount;

	Goal(List<Selection> selections, int points, int cropCount)
	{
		this(selections, points, 0, cropCount);
	}

	Goal(List<Selection> selections, int points, int pending, int cropCount)
	{
		this.selections = selections;
		this.points = points;
		this.pending = Math.max(0, Math.min(pending, POINTS_CAP - points));
		this.cropCount = Math.max(1, cropCount);
		int sum = 0;
		for (Selection selection : selections)
			sum += selection.item.getCost() * selection.quantity;

		this.total = sum;
	}

	/**
	 * The goal for the ticked rewards.
	 *
	 * @param config the plugin config, read for the ticks, quantities, and crop count
	 * @param points the spendable points now
	 * @return the goal, empty when nothing is ticked
	 */
	static Goal of(TitheFarmConfig config, int points)
	{
		return of(config, points, 0);
	}

	/**
	 * The goal for the ticked rewards, with the points the fruit in the backpack would add once deposited.
	 *
	 * @param config  the plugin config, read for the ticks, quantities, and crop count
	 * @param points  the spendable points now
	 * @param pending the points the carried fruit would earn, from {@link DepositRewards#points}
	 * @return the goal, empty when nothing is ticked
	 */
	static Goal of(TitheFarmConfig config, int points, int pending)
	{
		return new Goal(selections(config), points, pending, config.cropCount());
	}

	/**
	 * The ticked rewards in shop order, each with its quantity.
	 *
	 * @param config the plugin config
	 * @return the selections, empty when nothing is ticked
	 */
	static List<Selection> selections(TitheFarmConfig config)
	{
		List<Selection> list = new ArrayList<>();
		add(list, config.trackStrawhat(), RewardItem.FARMERS_STRAWHAT, 1);
		add(list, config.trackJacket(), RewardItem.FARMERS_JACKET, 1);
		add(list, config.trackTrousers(), RewardItem.FARMERS_TROUSERS, 1);
		add(list, config.trackBoots(), RewardItem.FARMERS_BOOTS, 1);
		add(list, config.trackSeedBox(), RewardItem.SEED_BOX, 1);
		add(list, config.trackHerbSack(), RewardItem.HERB_SACK, 1);
		add(list, config.trackGricollersCan(), RewardItem.GRICOLLERS_CAN, 1);
		add(list, config.trackAutoWeed(), RewardItem.AUTO_WEED, 1);
		add(list, config.trackCompost(), RewardItem.COMPOST, config.compostQuantity());
		add(list, config.trackSupercompost(), RewardItem.SUPERCOMPOST, config.supercompostQuantity());
		add(list, config.trackGrapeSeed(), RewardItem.GRAPE_SEED, config.grapeSeedQuantity());
		add(list, config.trackBologasBlessing(), RewardItem.BOLOGAS_BLESSING, config.bologasBlessingQuantity());
		add(list, config.trackHerbBox(), RewardItem.HERB_BOX, config.herbBoxQuantity());
		add(list, config.trackSeedPack(), RewardItem.SEED_PACK, config.seedPackQuantity());
		return list;
	}

	/** Adds a reward to the list when it is ticked. */
	private static void add(List<Selection> list, boolean ticked, RewardItem item, int quantity)
	{
		if (ticked)
			list.add(new Selection(item, Math.max(1, quantity)));
	}

	/** Whether no reward is ticked. */
	boolean isEmpty()
	{
		return selections.isEmpty();
	}

	/** The ticked rewards. */
	List<Selection> getSelections()
	{
		return selections;
	}

	/** The spendable points now. */
	int getPoints()
	{
		return points;
	}

	/** The combined cost of every ticked reward. */
	int getTotal()
	{
		return total;
	}

	/** The points the carried fruit would earn once deposited. */
	int getPending()
	{
		return pending;
	}

	/** Progress counting the carried fruit as deposited, from 0 to 1. */
	double getPendingProgress()
	{
		return total == 0 ? 1 : Math.min(1, (double) (points + pending) / total);
	}

	/** Points still to earn, 0 once affordable. */
	int getRemaining()
	{
		return Math.max(0, total - points);
	}

	/** Progress from 0 to 1. */
	double getProgress()
	{
		return total == 0 ? 1 : Math.min(1, (double) points / total);
	}

	/** Whether a reward is ticked and the points cover the total. */
	boolean isAffordable()
	{
		return !isEmpty() && points >= total;
	}

	/** Whether the total is more than a player can ever hold. */
	boolean isOverCap()
	{
		return total > POINTS_CAP;
	}

	/** Runs of the configured crop count still needed, 0 once affordable. */
	int getRunsLeft()
	{
		return runsToGoal(points, total, cropCount);
	}

	/**
	 * The goal's name: the reward's own name when one is ticked (with its quantity when more than one), or
	 * {@code Selected rewards (n)} for several.
	 *
	 * @return the name, empty when nothing is ticked
	 */
	String getDisplayName()
	{
		if (selections.isEmpty())
			return "";

		if (selections.size() > 1)
			return "Selected rewards (" + selections.size() + ")";

		Selection only = selections.get(0);
		return only.quantity > 1 ? only.item.getName() + " x" + only.quantity : only.item.getName();
	}

	/**
	 * How many of a single repeatable reward the points buy, e.g. {@code 3/5}.
	 *
	 * @return the count, or empty for several rewards or a single one-off
	 */
	String getCountText()
	{
		if (selections.size() != 1)
			return "";

		Selection only = selections.get(0);
		if (!only.item.isRepeatable() || only.quantity <= 1)
			return "";

		return Math.min(points / only.item.getCost(), only.quantity) + "/" + only.quantity;
	}

	/** The item whose icon stands for the goal: the first ticked reward. */
	int getIconItemId()
	{
		return selections.isEmpty() ? -1 : selections.get(0).item.getItemId();
	}

	/**
	 * Points one run of the given size earns on average, with the full-sack bonus spread across runs.
	 *
	 * @param cropCount the plants per run
	 * @return the average points per run
	 */
	static double pointsPerRun(int cropCount)
	{
		return cropCount * DepositRewards.pointsPerFruit();
	}

	/**
	 * Runs still needed to afford a cost.
	 *
	 * @param points    the spendable points now
	 * @param cost      the cost
	 * @param cropCount the plants per run
	 * @return the runs needed, 0 when already affordable
	 */
	static int runsToGoal(int points, int cost, int cropCount)
	{
		int needed = cost - points;
		if (needed <= 0)
			return 0;

		return (int) Math.ceil(needed / pointsPerRun(Math.max(1, cropCount)));
	}
}
