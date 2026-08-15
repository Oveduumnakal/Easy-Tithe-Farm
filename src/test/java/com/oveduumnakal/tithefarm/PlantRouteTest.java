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
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies the snake ordering and crop-count trimming of the route planner. */
public class PlantRouteTest
{
	private static List<int[]> grid3x3Shuffled()
	{
		return new ArrayList<>(Arrays.asList(
			new int[]{2, 2}, new int[]{0, 0}, new int[]{1, 1}, new int[]{2, 0},
			new int[]{0, 2}, new int[]{1, 0}, new int[]{2, 1}, new int[]{0, 1},
			new int[]{1, 2}));
	}

	@Test
	public void snakesRowsAlternatingDirection()
	{
		List<int[]> route = PlantRoute.order(grid3x3Shuffled(), 9);
		int[][] expected =
		{
			{0, 0}, {1, 0}, {2, 0},
			{2, 1}, {1, 1}, {0, 1},
			{0, 2}, {1, 2}, {2, 2}
		};
		assertEquals(expected.length, route.size());
		for (int i = 0; i < expected.length; i++)
			assertArrayEquals("index " + i, expected[i], route.get(i));
	}

	@Test
	public void trimsToCropCount()
	{
		List<int[]> route = PlantRoute.order(grid3x3Shuffled(), 4);
		int[][] expected = {{0, 0}, {1, 0}, {2, 0}, {2, 1}};
		assertEquals(4, route.size());
		for (int i = 0; i < expected.length; i++)
			assertArrayEquals(expected[i], route.get(i));
	}

	@Test
	public void zeroOrNegativeCountIsEmpty()
	{
		assertTrue(PlantRoute.order(grid3x3Shuffled(), 0).isEmpty());
		assertTrue(PlantRoute.order(grid3x3Shuffled(), -5).isEmpty());
	}

	@Test
	public void countAboveAvailableReturnsAll()
	{
		assertEquals(9, PlantRoute.order(grid3x3Shuffled(), 50).size());
	}
}
