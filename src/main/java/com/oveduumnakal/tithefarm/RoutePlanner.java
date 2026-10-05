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
 * Plans the rest of a run after the player has planted somewhere off the suggested route. Pure and static so it
 * can be unit-tested with a hand-made distance matrix.
 *
 * <p>A run is a loop: once the last seed is in, the player walks back to the first plant to water it. So the
 * plan is the cheapest way to visit the requested number of empty plots on the way from the last plant back to
 * the first. It is built by cheapest insertion — repeatedly adding whichever empty plot lengthens the walk the
 * least, wherever it fits best — then tidied with 2-opt, which reverses any stretch of the plan that crosses
 * itself. Both are quick for a farm of a few dozen plots and give compact, back-and-forth-free loops.
 *
 * <p>Most plot-to-plot moves in the farm cost one or two tiles, so walking distance alone leaves dozens of loops
 * tied and the planner would pick a jagged one. {@link #shapedCosts} breaks those ties toward orderly routes by
 * adding the number of plot columns and rows between two plots, so the plan snakes row by row like a player
 * would.
 */
final class RoutePlanner
{
	/** Upper bound on 2-opt passes, so a pathological matrix cannot stall a frame. */
	private static final int MAX_PASSES = 50;

	/** Weight of a walked tile against a step across the plot grid; walking dominates, the grid breaks ties. */
	static final int WALK_WEIGHT = 3;

	/** World tiles between neighbouring plot columns. */
	static final int COLUMN_SPACING = 5;

	/** World tiles between neighbouring plot rows. */
	static final int ROW_SPACING = 3;

	private RoutePlanner()
	{
	}

	/**
	 * Plans the plots to visit between the last plant and the first.
	 *
	 * @param distance   the walking distance between every pair of plots
	 * @param last       the plot planted most recently, where the walk starts
	 * @param first      the plot planted first, where the walk must end up
	 * @param candidates the empty plots that may be planted
	 * @param count      how many plots to plan
	 * @return the planned plots in visiting order, at most {@code count} of them
	 */
	static List<Integer> plan(int[][] distance, int last, int first, List<Integer> candidates, int count)
	{
		List<Integer> planned = new ArrayList<>();
		List<Integer> left = new ArrayList<>(candidates);
		while (planned.size() < count && !left.isEmpty())
		{
			int bestCandidate = -1;
			int bestPosition = -1;
			long bestCost = Long.MAX_VALUE;
			for (int c = 0; c < left.size(); c++)
			{
				int plot = left.get(c);
				for (int position = 0; position <= planned.size(); position++)
				{
					int before = position == 0 ? last : planned.get(position - 1);
					int after = position == planned.size() ? first : planned.get(position);
					long cost = (long) distance[before][plot] + distance[plot][after] - distance[before][after];
					if (cost < bestCost)
					{
						bestCost = cost;
						bestCandidate = c;
						bestPosition = position;
					}
				}
			}

			planned.add(bestPosition, left.remove(bestCandidate));
		}

		twoOpt(distance, last, first, planned);
		return planned;
	}

	/**
	 * Combines walking distance with plot-grid distance into the costs the planner minimises.
	 *
	 * @param walking the walking distance between every pair of plots, from {@link PlotGraph}
	 * @param centers each plot's centre tile as {@code {x, y}}, index for index with {@code walking}
	 * @return {@link #WALK_WEIGHT} times the walk plus the columns and rows crossed
	 */
	static int[][] shapedCosts(int[][] walking, List<int[]> centers)
	{
		int n = centers.size();
		int[][] cost = new int[n][n];
		for (int i = 0; i < n; i++)
		{
			for (int j = 0; j < n; j++)
			{
				if (i == j)
					continue;

				int columns = Math.abs(centers.get(i)[0] - centers.get(j)[0]) / COLUMN_SPACING;
				int rows = (Math.abs(centers.get(i)[1] - centers.get(j)[1]) + ROW_SPACING - 1) / ROW_SPACING;
				cost[i][j] = WALK_WEIGHT * walking[i][j] + columns + rows;
			}
		}

		return cost;
	}

	/** The length of the walk from {@code last} through the plan to {@code first}. */
	static long length(int[][] distance, int last, int first, List<Integer> planned)
	{
		long total = 0;
		int at = last;
		for (int plot : planned)
		{
			total += distance[at][plot];
			at = plot;
		}

		return total + distance[at][first];
	}

	/** Reverses any stretch of the plan whose reversal shortens the walk, until none does. */
	private static void twoOpt(int[][] distance, int last, int first, List<Integer> planned)
	{
		boolean improved = true;
		for (int pass = 0; improved && pass < MAX_PASSES; pass++)
		{
			improved = false;
			for (int i = 0; i < planned.size() - 1; i++)
			{
				for (int j = i + 1; j < planned.size(); j++)
				{
					int before = i == 0 ? last : planned.get(i - 1);
					int after = j == planned.size() - 1 ? first : planned.get(j + 1);
					long current = (long) distance[before][planned.get(i)] + distance[planned.get(j)][after];
					long swapped = (long) distance[before][planned.get(j)] + distance[planned.get(i)][after];
					if (swapped < current)
					{
						Collections.reverse(planned.subList(i, j + 1));
						improved = true;
					}
				}
			}
		}
	}
}
