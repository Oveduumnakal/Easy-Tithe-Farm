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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Predicts the next few plot actions by playing the run forward: ask {@link ActionAdvisor} for the next action,
 * apply it to a copy of the plots, let a little time pass, and ask again. The result drives the fading trail of
 * highlights, so the player's eyes already know where the next few clicks go.
 *
 * <p>Applying an action: a planted plot becomes a fresh seed, a watered plot keeps its stage clock, a harvested
 * or cleared plot becomes empty, and a deposit empties the backpack of fruit. Each action costs
 * {@link #ACTION_TICKS}; as the clock runs, watered plants grow into their next stage (or are grown after the
 * third) once their stage reaches {@link TitheFarmIds#STAGE_TICKS}, and unwatered plants that run out of time
 * die. When the advisor would wait, the clock jumps to the next plant to grow. The forecast stops at an action
 * that is not tied to a plot and that it cannot model: refilling, fetching seeds, or leaving.
 *
 * <p>Each plot appears once, at its nearest action — "plant 7, then water 7" is one entry for plot 7. The first
 * entry is always exactly the advisor's current advice; later entries are predictions. Pure and static so it can
 * be unit-tested without a client.
 */
final class ActionForecast
{
	/** How many plots the trail shows: the current target and the next four. */
	static final int TRAIL_LENGTH = 5;

	/** Ticks one predicted action is assumed to take, walking included. */
	static final int ACTION_TICKS = 3;

	/** Upper bound on simulated actions, so a forecast always ends. */
	private static final int MAX_STEPS = 60;

	private ActionForecast()
	{
	}

	/**
	 * Predicts the next distinct plot actions, starting with the current advice.
	 *
	 * @param plots         the route plots in planting order, then planted plots off the route
	 * @param seeds         the seeds carried
	 * @param water         the water charges carried
	 * @param hasFruit      whether the backpack holds any Tithe Farm fruit
	 * @param inventoryFull whether a harvest would not fit in the backpack
	 * @param plantLimit    how many more seeds the run allows now
	 * @param wrapUp        whether this is the last run
	 * @param length        the most entries to return
	 * @return up to {@code length} plot actions, each on a different plot; empty when the current advice is not a
	 *     plot action
	 */
	static List<ActionAdvisor.Advice> forecast(List<PlotInfo> plots, int seeds, int water, boolean hasFruit,
		boolean inventoryFull, int plantLimit, boolean wrapUp, int length)
	{
		List<PlotInfo> sim = new ArrayList<>(plots);
		List<ActionAdvisor.Advice> trail = new ArrayList<>();
		Set<Integer> seen = new HashSet<>();
		int seedsLeft = seeds;
		int waterLeft = water;
		int limit = plantLimit;
		boolean fruit = hasFruit;
		for (int step = 0; step < MAX_STEPS && trail.size() < length; step++)
		{
			ActionAdvisor.Advice advice = ActionAdvisor.decide(sim, seedsLeft, waterLeft, fruit, inventoryFull,
				limit, wrapUp);
			int index = advice.getPlotIndex();
			NextAction action = advice.getAction();
			if (index < 0)
			{
				if (trail.isEmpty())
					break;

				if (action == NextAction.DEPOSIT_FRUIT && !inventoryFull)
				{
					fruit = false;
					age(sim, ACTION_TICKS);
					continue;
				}

				if (action == NextAction.WAIT && skipToGrowth(sim))
					continue;

				break;
			}

			if (seen.add(index))
				trail.add(advice);

			PlotInfo plot = sim.get(index);
			switch (action)
			{
				case PLANT_SEED:
					sim.set(index, PlotInfo.predicted(TithePlotState.UNWATERED, 1, 0, 0));
					seedsLeft--;
					limit--;
					break;
				case WATER_PLANT:
					sim.set(index, PlotInfo.predicted(TithePlotState.WATERED, plot.getStage(), 0,
						plot.getStageAgeTicks()));
					waterLeft--;
					break;
				case HARVEST:
					sim.set(index, PlotInfo.predicted(TithePlotState.EMPTY, 0, 0, 0));
					fruit = true;
					if (!wrapUp)
						limit++;

					break;
				default:
					sim.set(index, PlotInfo.predicted(TithePlotState.EMPTY, 0, 0, 0));
					break;
			}

			age(sim, ACTION_TICKS);
		}

		return trail;
	}

	/**
	 * Lets time pass for every plot: ages grow, watered plants whose stage is over move to the next stage (or
	 * are grown), and unwatered plants whose stage is over die. Plots of unknown age are left as they are.
	 */
	static void age(List<PlotInfo> plots, int ticks)
	{
		for (int i = 0; i < plots.size(); i++)
		{
			PlotInfo plot = plots.get(i);
			int stageAge = plot.getStageAgeTicks();
			if (stageAge == PlotInfo.AGE_UNKNOWN)
				continue;

			int age = plot.getAgeTicks() == PlotInfo.AGE_UNKNOWN ? stageAge : plot.getAgeTicks();
			int newAge = age + ticks;
			int newStageAge = stageAge + ticks;
			if (newStageAge < TitheFarmIds.STAGE_TICKS)
			{
				plots.set(i, PlotInfo.predicted(plot.getState(), plot.getStage(), newAge, newStageAge));
				continue;
			}

			int over = newStageAge - TitheFarmIds.STAGE_TICKS;
			if (plot.getState() == TithePlotState.WATERED)
				plots.set(i, grow(plot, over));
			else if (plot.getState() == TithePlotState.UNWATERED)
				plots.set(i, PlotInfo.predicted(TithePlotState.DEAD, plot.getStage(), over, over));
			else
				plots.set(i, PlotInfo.predicted(plot.getState(), plot.getStage(), newAge, newStageAge));
		}
	}

	/**
	 * Jumps the clock to the next watered plant to grow. When no watered plant has a known stage age, the first
	 * watered plant grows on its own instead, with no time passing for the rest.
	 *
	 * @return false when nothing is watered, so waiting would never change anything
	 */
	private static boolean skipToGrowth(List<PlotInfo> plots)
	{
		int soonest = Integer.MAX_VALUE;
		int unknown = -1;
		for (int i = 0; i < plots.size(); i++)
		{
			PlotInfo plot = plots.get(i);
			if (plot.getState() != TithePlotState.WATERED)
				continue;

			if (plot.getStageAgeTicks() == PlotInfo.AGE_UNKNOWN)
			{
				if (unknown < 0)
					unknown = i;
			}
			else
			{
				soonest = Math.min(soonest, TitheFarmIds.STAGE_TICKS - plot.getStageAgeTicks());
			}
		}

		if (soonest != Integer.MAX_VALUE)
		{
			age(plots, Math.max(1, soonest));
			return true;
		}

		if (unknown >= 0)
		{
			plots.set(unknown, grow(plots.get(unknown), 0));
			return true;
		}

		return false;
	}

	/** A watered plant moved on to its next stage, or grown after the third, the given ticks into it. */
	private static PlotInfo grow(PlotInfo plot, int ticks)
	{
		if (plot.getStage() >= TitheFarmIds.WATERS_PER_CROP)
			return PlotInfo.predicted(TithePlotState.GROWN, 0, ticks, ticks);

		return PlotInfo.predicted(TithePlotState.UNWATERED, plot.getStage() + 1, ticks, ticks);
	}
}
