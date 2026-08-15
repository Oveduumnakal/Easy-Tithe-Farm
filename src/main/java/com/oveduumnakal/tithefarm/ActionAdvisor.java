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

/**
 * Chooses the single next action for the run from the route's plot states, seed stock, and remaining water.
 *
 * <p>Pure and static so it can be unit-tested without a client. Priority follows what keeps a run alive and
 * matches the wiki's "plant and water on the first round": watering an unwatered plant comes first, because an
 * unwatered plant is the only thing that dies; if a plant needs water and none is left, refilling comes next;
 * then harvesting anything grown; then planting the next empty plot (collecting seeds first if the backpack is
 * empty). With watering ahead of planting, a freshly planted plot — which starts unwatered — is watered before
 * the next seed goes in, giving the plant-then-water rhythm. When nothing is actionable, the run waits.
 */
final class ActionAdvisor
{
	private ActionAdvisor()
	{
	}

	/**
	 * Decides the next action.
	 *
	 * @param routeStates    the plot states in planting-route order
	 * @param hasSeeds       whether the backpack holds any Tithe Farm seed
	 * @param waterAvailable the water charges the player can still pour
	 * @return the chosen action and the route index it targets ({@code -1} for non-plot actions)
	 */
	static Advice decide(List<TithePlotState> routeStates, boolean hasSeeds, int waterAvailable)
	{
		int firstUnwatered = indexOf(routeStates, TithePlotState.UNWATERED);
		if (firstUnwatered >= 0)
		{
			if (waterAvailable > 0)
				return new Advice(NextAction.WATER_PLANT, firstUnwatered);

			return new Advice(NextAction.REFILL_WATER, -1);
		}

		int firstGrown = indexOf(routeStates, TithePlotState.GROWN);
		if (firstGrown >= 0)
			return new Advice(NextAction.HARVEST, firstGrown);

		int firstEmpty = indexOf(routeStates, TithePlotState.EMPTY);
		if (firstEmpty >= 0)
		{
			if (hasSeeds)
				return new Advice(NextAction.PLANT_SEED, firstEmpty);

			return new Advice(NextAction.GET_SEEDS, -1);
		}

		return new Advice(NextAction.WAIT, -1);
	}

	/** The index of the first plot in the given state, or {@code -1} when none is. */
	private static int indexOf(List<TithePlotState> states, TithePlotState wanted)
	{
		for (int i = 0; i < states.size(); i++)
		{
			if (states.get(i) == wanted)
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
