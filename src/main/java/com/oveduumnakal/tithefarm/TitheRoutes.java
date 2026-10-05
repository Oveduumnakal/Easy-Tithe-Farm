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
 * The planting routes published in the OSRS Wiki's Tithe Farm strategy guide, as plot-centre tiles.
 *
 * <p>The farm (map region 7222) holds five plot columns, centred at region x 19, 24, 29, 34 and 39, each with
 * four plots in the south half (region y 33, 36, 39, 42) and four in the north half (48, 51, 54, 57), plus a
 * single plot at (29, 27). The wiki's tile-marker files mark the tile the player stands on; each one was mapped
 * to the plot it faces across the path. Tiles here are world coordinates of plot centres — the same tiles
 * {@link TithePlotTracker#templateTile} reports — so a preset matches the tracked plots in any farm instance.
 */
final class TitheRoutes
{
	/** World x of map region 7222's south-west corner. */
	static final int REGION_BASE_X = 1792;

	/** World y of map region 7222's south-west corner. */
	static final int REGION_BASE_Y = 3456;

	/**
	 * The wiki's basic 20-plant route: down both eastern columns zig-zag through the north half, down the far
	 * east column and back up its neighbour in the south half, then up the centre column's north half, ending
	 * beside plot 1. The wiki notes it can be cut off at 16.
	 */
	static final List<int[]> BASIC_20 = route(
		34, 57, 39, 57, 34, 54, 39, 54, 39, 51, 34, 51, 34, 48, 39, 48,
		39, 42, 39, 39, 39, 36, 39, 33, 34, 33, 34, 36, 34, 39, 34, 42,
		29, 48, 29, 51, 29, 54, 29, 57);

	/**
	 * The wiki's 20-plant combo route: zig-zag down both western columns through both halves, then up the
	 * centre column's south half. Each plot is harvested, replanted and watered in one visit.
	 */
	static final List<int[]> COMBO_20 = route(
		19, 57, 24, 57, 24, 54, 19, 54, 19, 51, 24, 51, 24, 48, 19, 48,
		19, 42, 24, 42, 24, 39, 19, 39, 19, 36, 24, 36, 24, 33, 19, 33,
		29, 33, 29, 36, 29, 39, 29, 42);

	/**
	 * The wiki's 23-plant simple route: plant and water each plot in turn, down the centre column to the single
	 * plot, back up the west side, finishing at the north-west corner.
	 */
	static final List<int[]> SIMPLE_23 = route(
		24, 57, 29, 57, 24, 54, 29, 54, 24, 51, 29, 51, 29, 48, 29, 42,
		29, 39, 29, 36, 29, 33, 29, 27, 19, 33, 19, 36, 24, 39, 19, 39,
		24, 42, 19, 42, 24, 48, 19, 48, 19, 51, 19, 54, 19, 57);

	private TitheRoutes()
	{
	}

	/** Builds a route from region-relative x, y pairs, converting each to world coordinates. */
	private static List<int[]> route(int... regionXy)
	{
		List<int[]> tiles = new ArrayList<>();
		for (int i = 0; i + 1 < regionXy.length; i += 2)
			tiles.add(new int[]{REGION_BASE_X + regionXy[i], REGION_BASE_Y + regionXy[i + 1]});

		return Collections.unmodifiableList(tiles);
	}
}
