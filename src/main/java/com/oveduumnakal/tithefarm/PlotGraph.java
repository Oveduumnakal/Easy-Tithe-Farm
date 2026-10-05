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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import net.runelite.api.CollisionDataFlag;

/**
 * Walking distances between plots, from the scene's collision flags. Pure and static so it can be unit-tested
 * with a synthetic grid.
 *
 * <p>A plot is worked from any walkable tile directly beside its footprint (not a corner). The distance between
 * two plots is the fewest steps from any tile that works one to any tile that works the other, moving one tile
 * at a time in eight directions the way the game does: walls block the side they sit on, and a diagonal step
 * needs both of its straight steps to be open. Plots on either side of a path come out one step apart, while
 * plots in neighbouring paths cost the walk around the column between them.
 */
final class PlotGraph
{
	/** Distance reported between plots with no walkable route between them. */
	static final int UNREACHABLE = Integer.MAX_VALUE / 4;

	private static final int NORTH = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
	private static final int EAST = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
	private static final int SOUTH = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
	private static final int WEST = CollisionDataFlag.BLOCK_MOVEMENT_WEST;

	private static final int[][] STEPS =
	{
		{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
	};

	private PlotGraph()
	{
	}

	/**
	 * The walking distance between every pair of plots.
	 *
	 * @param flags      the scene's collision flags, indexed {@code [x][y]}
	 * @param footprints each plot's scene footprint as {@code {minX, minY, maxX, maxY}}
	 * @return the symmetric distance matrix, {@link #UNREACHABLE} where no route exists
	 */
	static int[][] distances(int[][] flags, int[][] footprints)
	{
		int n = footprints.length;
		List<List<int[]>> stands = new ArrayList<>();
		for (int[] footprint : footprints)
			stands.add(standingTiles(flags, footprint));

		int[][] distance = new int[n][n];
		for (int i = 0; i < n; i++)
		{
			int[][] grid = flood(flags, stands.get(i));
			for (int j = 0; j < n; j++)
				distance[i][j] = i == j ? 0 : nearest(grid, stands.get(j));
		}

		return distance;
	}

	/** The walkable tiles directly beside a footprint's edges, with no wall between them and the plot. */
	static List<int[]> standingTiles(int[][] flags, int[] footprint)
	{
		List<int[]> tiles = new ArrayList<>();
		for (int y = footprint[1]; y <= footprint[3]; y++)
		{
			addStand(flags, tiles, footprint[0] - 1, y, EAST);
			addStand(flags, tiles, footprint[2] + 1, y, WEST);
		}

		for (int x = footprint[0]; x <= footprint[2]; x++)
		{
			addStand(flags, tiles, x, footprint[1] - 1, NORTH);
			addStand(flags, tiles, x, footprint[3] + 1, SOUTH);
		}

		return tiles;
	}

	/** Adds a standing tile when it is in the scene, walkable, and has no wall facing the plot. */
	private static void addStand(int[][] flags, List<int[]> tiles, int x, int y, int wallTowardPlot)
	{
		if (inBounds(flags, x, y) && !blocked(flags[x][y]) && (flags[x][y] & wallTowardPlot) == 0)
			tiles.add(new int[]{x, y});
	}

	/** Breadth-first step counts from a set of start tiles to every reachable tile. */
	private static int[][] flood(int[][] flags, List<int[]> starts)
	{
		int[][] grid = new int[flags.length][flags[0].length];
		for (int[] column : grid)
			Arrays.fill(column, UNREACHABLE);

		Deque<int[]> queue = new ArrayDeque<>();
		for (int[] start : starts)
		{
			grid[start[0]][start[1]] = 0;
			queue.add(start);
		}

		while (!queue.isEmpty())
		{
			int[] tile = queue.poll();
			for (int[] step : STEPS)
			{
				int x = tile[0] + step[0];
				int y = tile[1] + step[1];
				if (canStep(flags, tile[0], tile[1], step[0], step[1]) && grid[x][y] == UNREACHABLE)
				{
					grid[x][y] = grid[tile[0]][tile[1]] + 1;
					queue.add(new int[]{x, y});
				}
			}
		}

		return grid;
	}

	/** The smallest step count among the given tiles. */
	private static int nearest(int[][] grid, List<int[]> tiles)
	{
		int best = UNREACHABLE;
		for (int[] tile : tiles)
			best = Math.min(best, grid[tile[0]][tile[1]]);

		return best;
	}

	/** Whether one step from a tile in the given direction is allowed; a diagonal needs both straight steps. */
	static boolean canStep(int[][] flags, int x, int y, int dx, int dy)
	{
		if (dx != 0 && dy != 0)
		{
			return canStep(flags, x, y, dx, 0) && canStep(flags, x + dx, y, 0, dy)
				&& canStep(flags, x, y, 0, dy) && canStep(flags, x, y + dy, dx, 0);
		}

		int tx = x + dx;
		int ty = y + dy;
		if (!inBounds(flags, tx, ty) || blocked(flags[tx][ty]))
			return false;

		int from = flags[x][y];
		int to = flags[tx][ty];
		if (dx == 1)
			return (from & EAST) == 0 && (to & WEST) == 0;

		if (dx == -1)
			return (from & WEST) == 0 && (to & EAST) == 0;

		if (dy == 1)
			return (from & NORTH) == 0 && (to & SOUTH) == 0;

		return (from & SOUTH) == 0 && (to & NORTH) == 0;
	}

	/** Whether a tile is fully blocked to movement. */
	private static boolean blocked(int flag)
	{
		return (flag & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0;
	}

	/** Whether a tile lies inside the flag grid. */
	private static boolean inBounds(int[][] flags, int x, int y)
	{
		return x >= 0 && y >= 0 && x < flags.length && y < flags[x].length;
	}
}
