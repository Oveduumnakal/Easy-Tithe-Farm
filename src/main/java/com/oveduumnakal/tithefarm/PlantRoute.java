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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure route planner over the Tithe Farm plot grid, unit-testable without a {@code Client}.
 *
 * <p>The plots sit in grid-aligned rows. The efficient run plants them in a boustrophedon (snake): the first
 * row left to right, the next right to left, and so on, so the player never backtracks across a row. Watering
 * later follows this same numbering, since the plant that was seeded first is the first to need water. When a
 * run wants fewer crops than there are plots, the first {@code count} plots of the snake are chosen, keeping
 * the worked block compact and close to the start.
 *
 * <p>Everything works on {@code int[]{x, y}} tiles so the geometry stays free of the client; the overlay maps
 * the ordered tiles back to their scene objects for drawing.
 */
final class PlantRoute
{
	private PlantRoute()
	{
	}

	/**
	 * Orders plot tiles into the snake planting route, then trims to the requested crop count.
	 *
	 * @param plotTiles the plot tiles as {@code int[]{x, y}}, in any order
	 * @param count     how many crops the run wants
	 * @return the chosen tiles in planting order; empty when {@code count <= 0} or no tiles are given
	 */
	static List<int[]> order(List<int[]> plotTiles, int count)
	{
		List<int[]> route = new ArrayList<>();
		if (count <= 0 || plotTiles == null || plotTiles.isEmpty())
			return route;

		Map<Integer, List<int[]>> rows = new LinkedHashMap<>();
		for (int[] tile : plotTiles)
			rows.computeIfAbsent(tile[1], key -> new ArrayList<>()).add(tile);

		List<Integer> rowKeys = new ArrayList<>(rows.keySet());
		rowKeys.sort(Comparator.naturalOrder());
		boolean leftToRight = true;
		for (int rowKey : rowKeys)
		{
			List<int[]> row = rows.get(rowKey);
			row.sort(Comparator.comparingInt(tile -> tile[0]));
			if (!leftToRight)
				reverse(row);

			route.addAll(row);
			leftToRight = !leftToRight;
		}

		if (route.size() > count)
			return new ArrayList<>(route.subList(0, count));

		return route;
	}

	/** Reverses a list in place. */
	private static void reverse(List<int[]> list)
	{
		for (int i = 0, j = list.size() - 1; i < j; i++, j--)
		{
			int[] tmp = list.get(i);
			list.set(i, list.get(j));
			list.set(j, tmp);
		}
	}
}
