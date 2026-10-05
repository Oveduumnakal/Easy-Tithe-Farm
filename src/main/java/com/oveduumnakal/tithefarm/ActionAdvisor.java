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

import java.util.List;
import java.util.function.Predicate;

/**
 * Chooses the single next action for the run from its plots, the backpack, and the water carried. The plots
 * are the route in order followed by any planted plots off the route, so every plant is tended, while only the
 * route's empty plots are offered for planting — and only until the crop count is in the ground.
 *
 * <p>Pure and static so it can be unit-tested without a client. The order follows how efficient runs are
 * played — plant and water each seed in one go, keep planting until the pass is done, then loop back:
 * <ol>
 * <li>Water a seed planted moments ago, so every seed is watered as it goes in.</li>
 * <li>Water any plant close to the end of its stage, the only thing that can still kill it.</li>
 * <li>Between runs — nothing growing — deposit any fruit carried before the next seed goes in. Fruit is never
 * deposited mid-run unless a harvest would not fit.</li>
 * <li>Unless a plant is grown, take the next plot in the route that needs a seed: clear it if its plant died,
 * otherwise plant it — or refill first when the water would not cover the rest of the run.</li>
 * <li>Water any other plant waiting for this stage's water, the one waiting longest first.</li>
 * <li>Harvest a grown plant, or deposit first when the backpack has no room for the fruit.</li>
 * <li>Collect seeds when a plot is waiting and none are carried.</li>
 * <li>Otherwise wait — or, on a last run with nothing left in the ground, leave.</li>
 * </ol>
 * Because planting outranks routine watering, a plant that ages into its next stage mid-pass no longer pulls
 * the player off the pass; it is only jumped to when its stage is nearly over. Plants age into each stage in
 * the order they were watered, so watering the one waiting longest walks the watering pass in the same order
 * rather than snapping back to the start of the route. Once any plant is grown, every grown plant is
 * harvested before the next seed goes in.
 */
final class ActionAdvisor
{
	/** Ticks into a stage after which an unwatered plant jumps the queue (60 of 100 leaves about 24 seconds). */
	static final int URGENT_TICKS = 60;

	/** Fruit per bonus: every hundredth fruit deposited in a game earns 2 extra points. */
	static final int BONUS_BATCH = 100;

	private ActionAdvisor()
	{
	}

	/**
	 * Decides the next action.
	 *
	 * @param plots         the route plots in planting order
	 * @param seeds         the seeds carried
	 * @param water         the water charges carried
	 * @param hasFruit      whether the backpack holds any Tithe Farm fruit
	 * @param inventoryFull whether a harvest would not fit in the backpack
	 * @param plantLimit    how many more seeds the run allows now; no seed is suggested at zero
	 * @param wrapUp        whether this is the last run: no seed fetching, and "leave" once everything is in
	 * @return the chosen action and the route index it targets ({@code -1} for non-plot actions)
	 */
	static Advice decide(List<PlotInfo> plots, int seeds, int water, boolean hasFruit, boolean inventoryFull,
		int plantLimit, boolean wrapUp)
	{
		int fresh = indexOf(plots, PlotInfo::isFreshSeed);
		if (fresh >= 0)
			return water(fresh, water);

		int urgent = longestWaiting(plots, ActionAdvisor::isUrgent);
		if (urgent >= 0)
			return water(urgent, water);

		if (hasFruit && RunSnapshot.nothingGrowing(plots))
			return new Advice(NextAction.DEPOSIT_FRUIT, -1);

		int grown = indexOf(plots, plot -> plot.getState() == TithePlotState.GROWN);
		int open = indexOf(plots, PlotInfo::needsSeed);
		if (grown < 0 && open >= 0 && seeds > 0 && plantLimit > 0)
		{
			if (plots.get(open).getState() == TithePlotState.DEAD)
				return new Advice(NextAction.CLEAR_DEAD, open);

			if (WaterTracker.canAffordPlant(plots, water, plantLimit))
				return new Advice(NextAction.PLANT_SEED, open);

			return new Advice(NextAction.REFILL_WATER, -1);
		}

		int unwatered = longestWaiting(plots, plot -> plot.getState() == TithePlotState.UNWATERED);
		if (unwatered >= 0)
			return water(unwatered, water);

		if (grown >= 0)
		{
			if (inventoryFull)
				return new Advice(NextAction.DEPOSIT_FRUIT, -1);

			return new Advice(NextAction.HARVEST, grown);
		}

		boolean emptyPlot = indexOf(plots, plot -> plot.getState() == TithePlotState.EMPTY) >= 0;
		if (emptyPlot && seeds == 0 && plantLimit > 0 && !wrapUp)
			return new Advice(NextAction.GET_SEEDS, -1);

		if (wrapUp && indexOf(plots, plot -> !plot.needsSeed()) < 0)
			return new Advice(NextAction.LEAVE, -1);

		return new Advice(NextAction.WAIT, -1);
	}

	/**
	 * Holds the first seed of a run until every watering can is full: between runs, with a can not yet full,
	 * planting gives way to refilling. Any other advice is kept.
	 *
	 * @param advice      the advice from {@link #decide}
	 * @param betweenRuns whether nothing is growing
	 * @param cansFull    whether every watering can carried is full
	 * @return the advice to follow
	 */
	static Advice topUpFirst(Advice advice, boolean betweenRuns, boolean cansFull)
	{
		if (betweenRuns && !cansFull && advice.getAction() == NextAction.PLANT_SEED)
			return new Advice(NextAction.REFILL_WATER, -1);

		return advice;
	}

	/**
	 * How many more seeds the run has room for: the crop count less the plants already in the ground, wherever
	 * they are.
	 *
	 * @param plots     every plot the run knows about
	 * @param cropCount how many plants the run holds
	 * @return the seeds still to plant, never negative
	 */
	static int plantSlots(List<PlotInfo> plots, int cropCount)
	{
		int planted = 0;
		for (PlotInfo plot : plots)
		{
			if (!plot.needsSeed())
				planted++;
		}

		return Math.max(0, cropCount - planted);
	}

	/**
	 * How many seeds a last run should still plant: exactly enough to bring the fruit deposited this game up to
	 * the next hundred — worth 2 bonus points — when that fits in the run, otherwise none.
	 *
	 * @param deposited the fruit deposited this game
	 * @param carried   the fruit in the backpack
	 * @param planted   the plants in the ground that will still yield fruit
	 * @param slots     the seeds the crop count still has room for
	 * @return the seeds to plant before finishing
	 */
	static int wrapUpSeeds(int deposited, int carried, int planted, int slots)
	{
		int total = Math.max(0, deposited) + Math.max(0, carried) + Math.max(0, planted);
		int toHundred = (BONUS_BATCH - total % BONUS_BATCH) % BONUS_BATCH;
		return toHundred <= slots ? toHundred : 0;
	}

	/** Water the given plot, or refill first when no water is left. */
	private static Advice water(int index, int water)
	{
		if (water > 0)
			return new Advice(NextAction.WATER_PLANT, index);

		return new Advice(NextAction.REFILL_WATER, -1);
	}

	/** Whether a plot is unwatered and far enough into its stage that it must be watered now. */
	private static boolean isUrgent(PlotInfo plot)
	{
		return plot.getState() == TithePlotState.UNWATERED && plot.getAgeTicks() >= URGENT_TICKS;
	}

	/**
	 * The index of the accepted plot that has waited longest in its current state, the earliest in the list on a
	 * tie, or {@code -1} when none is accepted. A plot of unknown age counts as the longest waiting, since it was
	 * already there when tracking began.
	 */
	private static int longestWaiting(List<PlotInfo> plots, Predicate<PlotInfo> wanted)
	{
		int best = -1;
		int bestAge = -1;
		for (int i = 0; i < plots.size(); i++)
		{
			PlotInfo plot = plots.get(i);
			if (!wanted.test(plot))
				continue;

			int age = plot.getAgeTicks() == PlotInfo.AGE_UNKNOWN ? Integer.MAX_VALUE : plot.getAgeTicks();
			if (age > bestAge)
			{
				best = i;
				bestAge = age;
			}
		}

		return best;
	}

	/** The index of the first plot the predicate accepts, or {@code -1} when none does. */
	private static int indexOf(List<PlotInfo> plots, Predicate<PlotInfo> wanted)
	{
		for (int i = 0; i < plots.size(); i++)
		{
			if (wanted.test(plots.get(i)))
				return i;
		}

		return -1;
	}

	/** An action plus the route index it targets, or {@code -1} when the action is not tied to a plot. */
	static final class Advice
	{
		private final NextAction action;
		private final int plotIndex;

		/**
		 * Creates an advice result.
		 *
		 * @param action    the chosen action
		 * @param plotIndex the targeted route index, or {@code -1}
		 */
		Advice(NextAction action, int plotIndex)
		{
			this.action = action;
			this.plotIndex = plotIndex;
		}

		/** The chosen action. */
		NextAction getAction()
		{
			return action;
		}

		/** The targeted route index, or {@code -1} when the action targets no plot. */
		int getPlotIndex()
		{
			return plotIndex;
		}
	}
}
