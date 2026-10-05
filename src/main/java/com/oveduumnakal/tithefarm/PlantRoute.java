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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pure route planner over the Tithe Farm plot grid, unit-testable without a {@code Client}.
 *
 * <p>Efficient runs walk a path and plant the plots on both sides of it, and they end next to where they began,
 * because the moment the last seed is in, the first plant needs its next water. The automatic route models
 * both: plots are grouped into columns by x, and two neighbouring columns with walkable ground between them
 * (centres more than a plot's width apart) form one path's worth of plots. The route is a loop — south down the
 * first group from the north edge as far as the crop count needs, zig-zagging across the path level by level,
 * then back north up the next group over the same levels — so it finishes beside plot 1. When two groups
 * cannot hold the crop count, further groups join the loop. A column with no partner across a path is walked
 * on its own.
 *
 * <p>A preferred order — a wiki preset or the player's recorded route — can be passed in; its plots come
 * first, in that order, and any shortfall is filled from the automatic route. Everything works on
 * {@code int[]{x, y}} tiles so the geometry stays free of the client.
 */
final class PlantRoute
{
	/** Width of a plot in tiles; columns whose centres are further apart than this have a path between. */
	static final int PLOT_WIDTH = 3;

	private PlantRoute()
	{
	}

	/**
	 * Orders plot tiles into the automatic planting route, then trims to the requested crop count.
	 *
	 * @param plotTiles the plot tiles as {@code int[]{x, y}}, in any order
	 * @param count     how many crops the run wants
	 * @return the chosen tiles in planting order; empty when {@code count <= 0} or no tiles are given
	 */
	static List<int[]> order(List<int[]> plotTiles, int count)
	{
		return order(plotTiles, null, count);
	}

	/**
	 * Orders plot tiles with a preferred order first, filling any shortfall from the automatic route.
	 *
	 * @param plotTiles the plot tiles as {@code int[]{x, y}}, in any order
	 * @param preferred the preferred order, or {@code null}; tiles not among {@code plotTiles} are skipped
	 * @param count     how many crops the run wants
	 * @return the chosen tiles in planting order; empty when {@code count <= 0} or no tiles are given
	 */
	static List<int[]> order(List<int[]> plotTiles, List<int[]> preferred, int count)
	{
		List<int[]> route = new ArrayList<>();
		if (count <= 0 || plotTiles == null || plotTiles.isEmpty())
			return route;

		Map<Long, int[]> byKey = new TreeMap<>();
		for (int[] tile : plotTiles)
			byKey.put(key(tile), tile);

		Set<Long> used = new HashSet<>();
		if (preferred != null)
		{
			for (int[] tile : preferred)
			{
				long key = key(tile);
				if (route.size() < count && byKey.containsKey(key) && used.add(key))
					route.add(byKey.get(key));
			}
		}

		List<int[]> all = new ArrayList<>(byKey.values());
		List<int[]> candidates = automatic(all, count);
		candidates.addAll(automatic(all, all.size()));
		for (int[] tile : candidates)
		{
			if (route.size() >= count)
				break;

			if (used.add(key(tile)))
				route.add(tile);
		}

		return route;
	}

	/**
	 * The automatic loop for a crop count: the fewest column groups, each walked over the fewest levels from the
	 * north edge, that hold the count. Groups alternate up and down so the loop closes near its start.
	 */
	private static List<int[]> automatic(List<int[]> tiles, int count)
	{
		List<TreeMap<Integer, List<int[]>>> groups = columnGroups(tiles);
		int maxLevels = 0;
		for (TreeMap<Integer, List<int[]>> group : groups)
			maxLevels = Math.max(maxLevels, group.size());

		for (int used = Math.min(2, groups.size()); used <= groups.size(); used++)
		{
			for (int levels = 1; levels <= maxLevels; levels++)
			{
				if (capacity(groups, used, levels) >= count)
					return loop(groups, used, levels);
			}
		}

		return loop(groups, groups.size(), maxLevels);
	}

	/** Groups plots into columns by x, pairing neighbouring columns that have a path between them. */
	private static List<TreeMap<Integer, List<int[]>>> columnGroups(List<int[]> tiles)
	{
		TreeMap<Integer, List<int[]>> columns = new TreeMap<>();
		for (int[] tile : tiles)
			columns.computeIfAbsent(tile[0], x -> new ArrayList<>()).add(tile);

		List<Integer> xs = new ArrayList<>(columns.keySet());
		List<TreeMap<Integer, List<int[]>>> groups = new ArrayList<>();
		int i = 0;
		while (i < xs.size())
		{
			List<int[]> members = new ArrayList<>(columns.get(xs.get(i)));
			if (i + 1 < xs.size() && xs.get(i + 1) - xs.get(i) > PLOT_WIDTH)
			{
				members.addAll(columns.get(xs.get(i + 1)));
				i += 2;
			}
			else
			{
				i++;
			}

			TreeMap<Integer, List<int[]>> levels = new TreeMap<>();
			for (int[] tile : members)
				levels.computeIfAbsent(tile[1], y -> new ArrayList<>()).add(tile);

			groups.add(levels);
		}

		return groups;
	}

	/** How many plots the first {@code used} groups hold within their {@code levels} northernmost levels. */
	private static int capacity(List<TreeMap<Integer, List<int[]>>> groups, int used, int levels)
	{
		int total = 0;
		for (int g = 0; g < used; g++)
		{
			int taken = 0;
			for (List<int[]> level : groups.get(g).descendingMap().values())
			{
				if (taken++ >= levels)
					break;

				total += level.size();
			}
		}

		return total;
	}

	/**
	 * Walks the first {@code used} groups over their {@code levels} northernmost levels: the first group heads
	 * south from the north edge (where the entrance and barrel are), the next comes back north, and so on.
	 */
	private static List<int[]> loop(List<TreeMap<Integer, List<int[]>>> groups, int used, int levels)
	{
		List<int[]> route = new ArrayList<>();
		for (int g = 0; g < used; g++)
		{
			TreeMap<Integer, List<int[]>> group = groups.get(g);
			TreeMap<Integer, List<int[]>> first = new TreeMap<>();
			for (Integer y : group.descendingKeySet())
			{
				if (first.size() >= levels)
					break;

				first.put(y, new ArrayList<>(group.get(y)));
			}

			route.addAll(walkGroup(first, g % 2 == 0));
		}

		return route;
	}

	/** Walks one group's levels south or north, zig-zagging across the path level by level. */
	private static List<int[]> walkGroup(TreeMap<Integer, List<int[]>> levels, boolean southward)
	{
		List<int[]> walk = new ArrayList<>();
		boolean leftToRight = true;
		for (List<int[]> level : (southward ? levels.descendingMap() : levels).values())
		{
			level.sort(Comparator.comparingInt(tile -> tile[0]));
			if (!leftToRight)
				reverse(level);

			walk.addAll(level);
			leftToRight = !leftToRight;
		}

		return walk;
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

	/** Packs a tile's x and y into one long key. */
	static long key(int[] tile)
	{
		return ((long) tile[0] << 32) | (tile[1] & 0xffffffffL);
	}
}
