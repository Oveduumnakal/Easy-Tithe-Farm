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
 * <li>Water a plant close to the end of its stage, the only thing that can still kill it — but only when the
 * plants and seeds ahead of it would not leave time to reach it in turn. A plant that the pass will reach in
 * time waits its turn, so a pass is not broken off to walk back to the start of the route.</li>
 * <li>Between runs — nothing growing — deposit before the next seed goes in once 100 or more fruit is carried.
 * A smaller haul stays in the backpack, and fruit is never deposited mid-run unless a harvest would not fit.</li>
 * <li>Unless a plant is grown, take the next plot in the route that needs a seed: clear it if its plant died,
 * otherwise plant it. When the water would not cover the rest of the run, spend what is carried on plants
 * waiting for water first, then refill.</li>
 * <li>Water any other plant waiting for this stage's water.</li>
 * <li>Harvest a grown plant whose fruit fits in the backpack. When none fits, deposit to make room, or free a
 * slot when there is no fruit to deposit.</li>
 * <li>Collect seeds when a plot is waiting and none are carried.</li>
 * <li>Otherwise wait.</li>
 * </ol>
 * Because planting outranks routine watering, a plant that ages into its next stage mid-pass no longer pulls
 * the player off the pass; it is only jumped to when its stage is nearly over. Once any plant is grown, every
 * grown plant is harvested before the next seed goes in.
 *
 * <p>Among the plants waiting for the same action — urgent watering, routine watering, or harvest — the one
 * planted earliest goes first, so every pass retraces the planting pass even when the seeds went in out of
 * route order. A plant whose planting was not seen was in before tracking began, so it counts as the earliest.
 * Ties fall back to the plant that has sat longest in its current state, then to route order. Routine watering
 * first finishes the pass under way: a plant still owed an earlier stage's water comes before one that has
 * already moved on to its next stage, so the first plants reaching their next stage do not pull the player back
 * to the start of the route mid-pass.
 */
final class ActionAdvisor
{
	/** Ticks into a stage after which an unwatered plant jumps the queue (60 of 100 leaves about 24 seconds). */
	static final int URGENT_TICKS = 60;

	/** Ticks one plant or water takes in a real run, walking included; logged passes run 4 to 5 ticks a step. */
	static final int PASS_STEP_TICKS = 5;

	/** Spare ticks an urgent plant must keep after the pass reaches it, or it jumps the queue. */
	static final int URGENT_MARGIN_TICKS = 10;

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
	 * @param backpack      the fruit carried and the room left for a harvest
	 * @param plantLimit    how many more seeds the run allows now; no seed is suggested at zero
	 * @return the chosen action and the route index it targets ({@code -1} for non-plot actions)
	 */
	static Advice decide(List<PlotInfo> plots, int seeds, int water, Backpack backpack, int plantLimit)
	{
		int fresh = earliestPlanted(plots, PlotInfo::isFreshSeed);
		if (fresh >= 0)
			return water(fresh, water);

		int urgent = earliestPlanted(plots, ActionAdvisor::isUrgent);
		if (urgent >= 0 && urgentAtRisk(plots, seedsAhead(plots, seeds, plantLimit)))
			return water(urgent, water);

		if (backpack.getFruit() >= BONUS_BATCH && RunSnapshot.nothingGrowing(plots))
			return new Advice(NextAction.DEPOSIT_FRUIT, -1);

		int grown = indexOf(plots, plot -> plot.getState() == TithePlotState.GROWN);
		int open = indexOf(plots, PlotInfo::needsSeed);
		int unwatered = nextInPass(plots);
		if (grown < 0 && open >= 0 && seeds > 0 && plantLimit > 0)
		{
			if (plots.get(open).getState() == TithePlotState.DEAD)
				return new Advice(NextAction.CLEAR_DEAD, open);

			if (WaterTracker.canAffordPlant(plots, water, plantLimit))
				return new Advice(NextAction.PLANT_SEED, open);

			if (unwatered >= 0 && water > 0)
				return new Advice(NextAction.WATER_PLANT, unwatered);

			return new Advice(NextAction.REFILL_WATER, -1);
		}

		if (unwatered >= 0)
			return water(unwatered, water);

		if (grown >= 0)
		{
			int fits = earliestPlanted(plots,
				plot -> plot.getState() == TithePlotState.GROWN && backpack.fits(plot.getTier()));
			if (fits >= 0)
				return new Advice(NextAction.HARVEST, fits);

			return new Advice(backpack.hasFruit() ? NextAction.DEPOSIT_FRUIT : NextAction.FREE_SLOT, -1);
		}

		boolean emptyPlot = indexOf(plots, plot -> plot.getState() == TithePlotState.EMPTY) >= 0;
		if (emptyPlot && seeds == 0 && plantLimit > 0)
			return new Advice(NextAction.GET_SEEDS, -1);

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
	 * Whether some urgent plant would die before the pass reaches it: the seeds still to plant (each planted and
	 * watered) and the plants watered ahead of it, at {@link #PASS_STEP_TICKS} each, leave it less than
	 * {@link #URGENT_MARGIN_TICKS} to spare.
	 *
	 * @param plots      the plots of the run
	 * @param seedsAhead the seeds that will be planted before routine watering resumes
	 * @return true when an urgent plant must be watered now
	 */
	private static boolean urgentAtRisk(List<PlotInfo> plots, int seedsAhead)
	{
		for (PlotInfo plot : plots)
		{
			if (!isUrgent(plot))
				continue;

			int steps = 2 * seedsAhead + wateredAhead(plots, plot) + 1;
			if (steps * PASS_STEP_TICKS + URGENT_MARGIN_TICKS >= plot.ticksUntilDeath())
				return true;
		}

		return false;
	}

	/** How many seeds go in before routine watering resumes: none once a plant is grown, as harvest comes first. */
	private static int seedsAhead(List<PlotInfo> plots, int seeds, int plantLimit)
	{
		int open = 0;
		for (PlotInfo plot : plots)
		{
			if (plot.getState() == TithePlotState.GROWN)
				return 0;

			if (plot.needsSeed())
				open++;
		}

		return Math.min(open, Math.min(seeds, plantLimit));
	}

	/** How many other unwatered plants the pass waters before this one: lower stages, then earlier plants. */
	private static int wateredAhead(List<PlotInfo> plots, PlotInfo target)
	{
		int ahead = 0;
		for (PlotInfo plot : plots)
		{
			if (plot == target || plot.getState() != TithePlotState.UNWATERED)
				continue;

			boolean lowerStage = plot.getStage() < target.getStage();
			if (lowerStage || (plot.getStage() == target.getStage() && plantedBefore(plot, target)))
				ahead++;
		}

		return ahead;
	}

	/**
	 * The index of the unwatered plant next in the watering pass under way, or {@code -1} when none is waiting:
	 * the lowest growth stage first, since those plants are still owed the current pass's water, then the one
	 * planted earliest.
	 */
	private static int nextInPass(List<PlotInfo> plots)
	{
		int lowest = Integer.MAX_VALUE;
		for (PlotInfo plot : plots)
		{
			if (plot.getState() == TithePlotState.UNWATERED)
				lowest = Math.min(lowest, plot.getStage());
		}

		int stage = lowest;
		return earliestPlanted(plots, plot -> plot.getState() == TithePlotState.UNWATERED && plot.getStage() == stage);
	}

	/**
	 * The index of the accepted plot whose plant went in earliest, or {@code -1} when none is accepted. A plant
	 * whose planting was not seen counts as the earliest, since it was already there when tracking began. Ties
	 * go to the plot that has sat longest in its current state, an unknown age counting as the longest, then to
	 * the earliest in the list.
	 */
	private static int earliestPlanted(List<PlotInfo> plots, Predicate<PlotInfo> wanted)
	{
		int best = -1;
		for (int i = 0; i < plots.size(); i++)
		{
			PlotInfo plot = plots.get(i);
			if (wanted.test(plot) && (best < 0 || plantedBefore(plot, plots.get(best))))
				best = i;
		}

		return best;
	}

	/** Whether one plant went in strictly before another, by planting age and then by age in its current state. */
	private static boolean plantedBefore(PlotInfo plot, PlotInfo other)
	{
		int planted = oldestIfUnknown(plot.getPlantAgeTicks());
		int otherPlanted = oldestIfUnknown(other.getPlantAgeTicks());
		if (planted != otherPlanted)
			return planted > otherPlanted;

		return oldestIfUnknown(plot.getAgeTicks()) > oldestIfUnknown(other.getAgeTicks());
	}

	/** An age in ticks, with {@link PlotInfo#AGE_UNKNOWN} read as older than any known age. */
	private static int oldestIfUnknown(int ticks)
	{
		return ticks == PlotInfo.AGE_UNKNOWN ? Integer.MAX_VALUE : ticks;
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
