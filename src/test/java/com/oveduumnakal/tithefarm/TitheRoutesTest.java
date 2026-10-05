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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies the wiki presets land on real plots of the farm grid and drive the route unchanged. */
public class TitheRoutesTest
{
	/** Every plot centre in the farm: five columns of eight, plus the single plot. */
	static List<int[]> farm()
	{
		List<int[]> tiles = new ArrayList<>();
		for (int x : new int[]{1811, 1816, 1821, 1826, 1831})
		{
			for (int y : new int[]{3489, 3492, 3495, 3498, 3504, 3507, 3510, 3513})
				tiles.add(new int[]{x, y});
		}

		tiles.add(new int[]{1821, 3483});
		return tiles;
	}

	private static void assertOnFarmAndUnique(List<int[]> preset, int size)
	{
		Set<Long> farm = new HashSet<>();
		for (int[] tile : farm())
			farm.add(PlantRoute.key(tile));

		Set<Long> seen = new HashSet<>();
		for (int[] tile : preset)
		{
			assertTrue("on the farm: " + tile[0] + "," + tile[1], farm.contains(PlantRoute.key(tile)));
			assertTrue("unique: " + tile[0] + "," + tile[1], seen.add(PlantRoute.key(tile)));
		}

		assertEquals(size, preset.size());
	}

	@Test
	public void presetsAreRealUniquePlots()
	{
		assertOnFarmAndUnique(TitheRoutes.BASIC_20, 20);
		assertOnFarmAndUnique(TitheRoutes.COMBO_20, 20);
		assertOnFarmAndUnique(TitheRoutes.SIMPLE_23, 23);
	}

	@Test
	public void basicRouteStartsAndEndsSideBySide()
	{
		assertArrayEquals(new int[]{1826, 3513}, TitheRoutes.BASIC_20.get(0));
		assertArrayEquals(new int[]{1821, 3513}, TitheRoutes.BASIC_20.get(19));
	}

	@Test
	public void presetDrivesTheRouteUnchanged()
	{
		List<int[]> route = PlantRoute.order(farm(), TitheRoutes.BASIC_20, 20);
		for (int i = 0; i < 20; i++)
			assertArrayEquals(TitheRoutes.BASIC_20.get(i), route.get(i));
	}

	@Test
	public void presetCutsToCropCountAndExtendsPastItsLength()
	{
		assertEquals(16, PlantRoute.order(farm(), TitheRoutes.BASIC_20, 16).size());
		List<int[]> longer = PlantRoute.order(farm(), TitheRoutes.BASIC_20, 24);
		assertEquals(24, longer.size());
		assertArrayEquals(TitheRoutes.BASIC_20.get(19), longer.get(19));
	}
}
