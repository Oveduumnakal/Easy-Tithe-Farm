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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies the off-route re-planner picks compact plots and closes the loop. */
public class RoutePlannerTest
{
	/** Plots on a line, a step apart: distance is the gap in index. */
	private static int[][] line(int n)
	{
		int[][] distance = new int[n][n];
		for (int i = 0; i < n; i++)
		{
			for (int j = 0; j < n; j++)
				distance[i][j] = Math.abs(i - j);
		}

		return distance;
	}

	private static List<Integer> range(int from, int to)
	{
		List<Integer> values = new ArrayList<>();
		for (int i = from; i < to; i++)
			values.add(i);

		return values;
	}

	@Test
	public void picksTheNearestPlotsForALoopFromASinglePlant()
	{
		List<Integer> planned = RoutePlanner.plan(line(10), 0, 0, range(1, 10), 3);
		assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(planned));
		assertEquals(6, RoutePlanner.length(line(10), 0, 0, planned));
	}

	@Test
	public void fillsTheGapBetweenLastAndFirstPlant()
	{
		List<Integer> candidates = Arrays.asList(1, 2, 3, 4, 6, 7, 8, 9);
		List<Integer> planned = RoutePlanner.plan(line(10), 5, 0, candidates, 4);
		assertEquals(Arrays.asList(4, 3, 2, 1), planned);
	}

	@Test
	public void plansNothingWhenTheRunIsFull()
	{
		assertTrue(RoutePlanner.plan(line(5), 0, 0, range(1, 5), 0).isEmpty());
		assertTrue(RoutePlanner.plan(line(5), 0, 0, range(1, 5), -2).isEmpty());
	}

	@Test
	public void plansEveryCandidateWhenThereAreTooFew()
	{
		assertEquals(2, RoutePlanner.plan(line(5), 0, 0, Arrays.asList(3, 4), 10).size());
	}

	@Test
	public void twoOptUntanglesACrossedPlan()
	{
		int[][] distance = line(6);
		List<Integer> planned = RoutePlanner.plan(distance, 0, 5, Arrays.asList(4, 2, 3, 1), 4);
		assertEquals(Arrays.asList(1, 2, 3, 4), planned);
		assertEquals(5, RoutePlanner.length(distance, 0, 5, planned));
	}

	@Test
	public void shapedCostsPreferANeighbourPlotOverADiagonalHopAtEqualWalk()
	{
		int[][] walking = {{0, 1, 1}, {1, 0, 1}, {1, 1, 0}};
		List<int[]> centers = Arrays.asList(new int[]{1811, 3489}, new int[]{1816, 3489}, new int[]{1816, 3492});
		int[][] cost = RoutePlanner.shapedCosts(walking, centers);
		assertTrue(cost[0][1] < cost[0][2]);
		assertEquals(RoutePlanner.WALK_WEIGHT + 1, cost[0][1]);
		assertEquals(0, cost[1][1]);
	}

	@Test
	public void singlePlantOnTheRealFarmPlansAnOrderlyLoopHome()
	{
		List<int[]> farm = TitheRoutesTest.farm();
		int n = farm.size();
		int[][] walking = new int[n][n];
		for (int i = 0; i < n; i++)
		{
			for (int j = 0; j < n; j++)
			{
				int dx = Math.abs(farm.get(i)[0] - farm.get(j)[0]);
				int dy = Math.abs(farm.get(i)[1] - farm.get(j)[1]);
				walking[i][j] = Math.max(dx, dy) / 3;
			}
		}

		int start = 0;
		List<Integer> planned = RoutePlanner.plan(RoutePlanner.shapedCosts(walking, farm), start, start,
			range(1, n), 19);
		assertEquals(19, planned.size());
		int last = planned.get(planned.size() - 1);
		int dx = Math.abs(farm.get(last)[0] - farm.get(start)[0]);
		int dy = Math.abs(farm.get(last)[1] - farm.get(start)[1]);
		assertTrue("ends beside the first plant", dx <= RoutePlanner.COLUMN_SPACING && dy <= RoutePlanner.ROW_SPACING);
	}
}
