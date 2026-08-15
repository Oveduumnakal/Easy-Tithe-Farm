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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

/**
 * Bridges the client-free {@link PlantRoute} to live scene objects: turns the tracked plots into the ordered
 * list of plot objects the route visits. Both the route overlay (for numbering) and the highlight overlay (for
 * the next-action target) share this so they agree on order.
 */
final class TitheRoutePlots
{
	private TitheRoutePlots()
	{
	}

	/**
	 * The plot objects in planting-route order, trimmed to the crop count.
	 *
	 * @param plots the tracked plot objects, in any order
	 * @param count the crop count for the run
	 * @return the plot objects in route order
	 */
	static List<GameObject> ordered(Collection<GameObject> plots, int count)
	{
		Map<Long, GameObject> byTile = new HashMap<>();
		List<int[]> tiles = new ArrayList<>();
		for (GameObject plot : plots)
		{
			WorldPoint point = plot.getWorldLocation();
			byTile.put(key(point.getX(), point.getY()), plot);
			tiles.add(new int[]{point.getX(), point.getY()});
		}

		List<int[]> route = PlantRoute.order(tiles, count);
		List<GameObject> ordered = new ArrayList<>();
		for (int[] tile : route)
		{
			GameObject plot = byTile.get(key(tile[0], tile[1]));
			if (plot != null)
				ordered.add(plot);
		}

		return ordered;
	}

	/** Packs a tile's world x and y into one long key. */
	private static long key(int x, int y)
	{
		return ((long) x << 32) | (y & 0xffffffffL);
	}
}
