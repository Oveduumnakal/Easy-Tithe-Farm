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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies the automatic loop, crop-count trimming, and preferred-order handling of the route planner. */
public class PlantRouteTest
{
	/** Two paths: columns x=0 and x=4 share one, columns x=10 and x=14 share the other; three levels each. */
	private static List<int[]> twoPaths()
	{
		List<int[]> tiles = new ArrayList<>();
		for (int x : new int[]{0, 4, 10, 14})
		{
			for (int y : new int[]{0, 3, 6})
				tiles.add(new int[]{x, y});
		}

		Collections.shuffle(tiles, new Random(7));
		return tiles;
	}

	private static void assertRoute(int[][] expected, List<int[]> route)
	{
		assertEquals(expected.length, route.size());
		for (int i = 0; i < expected.length; i++)
			assertArrayEquals("index " + i, expected[i], route.get(i));
	}

	@Test
	public void loopsSouthDownOnePathAndBackNorthUpTheNext()
	{
		int[][] expected =
		{
			{0, 6}, {4, 6}, {4, 3}, {0, 3}, {0, 0}, {4, 0},
			{10, 0}, {14, 0}, {14, 3}, {10, 3}, {10, 6}, {14, 6}
		};
		assertRoute(expected, PlantRoute.order(twoPaths(), 12));
	}

	@Test
	public void smallRunStaysAtTheNorthEdgeOfBothPaths()
	{
		int[][] expected = {{0, 6}, {4, 6}, {10, 6}, {14, 6}};
		assertRoute(expected, PlantRoute.order(twoPaths(), 4));
	}

	@Test
	public void touchingColumnsAreWalkedFromTheirOwnPaths()
	{
		List<int[]> tiles = Arrays.asList(
			new int[]{0, 0}, new int[]{0, 3},
			new int[]{3, 0}, new int[]{3, 3},
			new int[]{8, 0}, new int[]{8, 3});
		int[][] expected =
		{
			{0, 3}, {0, 0},
			{3, 0}, {8, 0}, {8, 3}, {3, 3}
		};
		assertRoute(expected, PlantRoute.order(tiles, 6));
	}

	@Test
	public void twentyOnTheRealFarmLoopsBackBesidePlotOne()
	{
		List<int[]> route = PlantRoute.order(TitheRoutesTest.farm(), 20);
		assertEquals(20, route.size());
		assertArrayEquals(new int[]{1811, 3513}, route.get(0));
		assertArrayEquals(new int[]{1826, 3513}, route.get(19));
		for (int[] tile : route)
			assertTrue("stays north of the second south row", tile[1] >= 3498);
	}

	@Test
	public void preferredOrderComesFirstAndAutomaticFillsTheRest()
	{
		List<int[]> preferred = Arrays.asList(new int[]{14, 6}, new int[]{99, 99}, new int[]{0, 0});
		int[][] expected = {{14, 6}, {0, 0}, {0, 6}, {4, 6}};
		assertRoute(expected, PlantRoute.order(twoPaths(), preferred, 4));
	}

	@Test
	public void zeroOrNegativeCountIsEmpty()
	{
		assertTrue(PlantRoute.order(twoPaths(), 0).isEmpty());
		assertTrue(PlantRoute.order(twoPaths(), -5).isEmpty());
	}

	@Test
	public void countAboveAvailableReturnsAll()
	{
		assertEquals(12, PlantRoute.order(twoPaths(), 50).size());
	}
}
