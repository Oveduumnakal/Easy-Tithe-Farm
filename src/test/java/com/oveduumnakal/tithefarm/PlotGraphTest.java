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

import org.junit.Test;

import net.runelite.api.CollisionDataFlag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies walking distances between plots over a small synthetic farm. */
public class PlotGraphTest
{
	private static final int FULL = CollisionDataFlag.BLOCK_MOVEMENT_FULL;

	/**
	 * A 15 x 9 scene with two columns of two plots each and a two-tile path between them, like the real farm:
	 * west plots at x 1-3, east plots at x 6-8, path x 4-5, rows y 1-3 and 4-6. A third column at x 11-13 sits
	 * across the next path (x 9-10).
	 */
	private static final int[][] FOOTPRINTS =
	{
		{1, 1, 3, 3}, {6, 1, 8, 3}, {1, 4, 3, 6}, {6, 4, 8, 6}, {11, 1, 13, 3}
	};

	private static int[][] scene()
	{
		int[][] flags = new int[15][9];
		for (int[] footprint : FOOTPRINTS)
		{
			for (int x = footprint[0]; x <= footprint[2]; x++)
			{
				for (int y = footprint[1]; y <= footprint[3]; y++)
					flags[x][y] = FULL;
			}
		}

		return flags;
	}

	@Test
	public void plotsAcrossAPathAreOneStepApart()
	{
		int[][] distance = PlotGraph.distances(scene(), FOOTPRINTS);
		assertEquals(1, distance[0][1]);
		assertEquals(1, distance[2][3]);
	}

	@Test
	public void distancesAreSymmetricAndZeroOnTheDiagonal()
	{
		int[][] distance = PlotGraph.distances(scene(), FOOTPRINTS);
		for (int i = 0; i < FOOTPRINTS.length; i++)
		{
			assertEquals(0, distance[i][i]);
			for (int j = 0; j < FOOTPRINTS.length; j++)
				assertEquals(distance[i][j], distance[j][i]);
		}
	}

	@Test
	public void theNextPathCostsTheWalkAroundTheColumn()
	{
		int[][] distance = PlotGraph.distances(scene(), FOOTPRINTS);
		assertEquals("across the next path", 1, distance[1][4]);
		assertEquals("west plot walks round the east column", 6, distance[0][4]);
	}

	@Test
	public void wallsBlockOnlyTheirOwnSide()
	{
		int[][] flags = new int[3][3];
		flags[1][1] = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
		assertFalse(PlotGraph.canStep(flags, 1, 1, 1, 0));
		assertTrue(PlotGraph.canStep(flags, 1, 1, -1, 0));
		assertFalse("diagonal through the wall", PlotGraph.canStep(flags, 1, 1, 1, 1));
	}

	@Test
	public void unreachablePlotsAreMarked()
	{
		int[][] flags = scene();
		for (int y = 0; y < flags[0].length; y++)
		{
			flags[9][y] = FULL;
			flags[10][y] = FULL;
		}

		assertEquals(PlotGraph.UNREACHABLE, PlotGraph.distances(flags, FOOTPRINTS)[0][4]);
	}
}
