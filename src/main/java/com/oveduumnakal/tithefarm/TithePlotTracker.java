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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Singleton;

import net.runelite.api.GameObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Finds Tithe Farm plots, water barrels, and deposit sacks in the loaded scene and reads plot state on demand.
 *
 * <p>Plots are keyed by their <em>template</em> tile — the real-world tile the farm instance was copied from —
 * rather than by object hash or instance tile. A plot changes object id as it grows while staying on the same
 * tile, and the farm is a fresh instance on every entry, so the template tile is the one key that stays stable
 * across both. It is also what a recorded route stores. State is decoded on demand from the current object id
 * (see {@link TithePlotState}); the tracker additionally remembers the tick each plot last changed id, which
 * gives the advisor a plant's age in its current id, and the tick each plant entered its growth stage, which
 * watering does not reset.
 */
@Singleton
class TithePlotTracker
{
	private final Map<WorldPoint, GameObject> plots = new LinkedHashMap<>();
	private final Map<WorldPoint, Integer> lastIds = new HashMap<>();
	private final Map<WorldPoint, Integer> changedTicks = new HashMap<>();
	private final Map<WorldPoint, Integer> stageTicks = new HashMap<>();
	private final Map<WorldPoint, Integer> plantedTicks = new HashMap<>();
	private final Map<Long, GameObject> waterBarrels = new LinkedHashMap<>();
	private final Map<Long, GameObject> sacks = new LinkedHashMap<>();

	private GameObject seedTable;

	/**
	 * Records or updates an object that spawned or changed id in place.
	 *
	 * @param object the spawned object
	 * @param tick   the client tick count when it spawned
	 * @return for a plot, the id previously seen on its tile, or {@code -1} when the tile is new or the object
	 *     is not a plot
	 */
	int onSpawnOrChanged(GameObject object, int tick)
	{
		if (object == null)
			return -1;

		int id = object.getId();
		if (TitheFarmIds.isPlot(id))
		{
			WorldPoint tile = templateTile(object);
			plots.put(tile, object);
			Integer previous = lastIds.put(tile, id);
			if (previous == null || previous != id)
			{
				changedTicks.put(tile, tick);
				if (previous == null || !isWatering(previous, id))
					stageTicks.put(tile, tick);
			}

			if (id == TitheFarmIds.PLOT_EMPTY)
				plantedTicks.remove(tile);
			else if (previous != null && previous == TitheFarmIds.PLOT_EMPTY && TithePlotState.isFreshSeed(id))
				plantedTicks.put(tile, tick);

			return previous == null ? -1 : previous;
		}

		if (TitheFarmIds.isWaterBarrel(id))
			waterBarrels.put(object.getHash(), object);
		else if (TitheFarmIds.isSack(id))
			sacks.put(object.getHash(), object);
		else if (id == TitheFarmIds.SEED_TABLE)
			this.seedTable = object;

		return -1;
	}

	/** Drops an object that has left the scene, only when it is still the tracked object for its slot. */
	void onDespawn(GameObject object)
	{
		if (object == null)
			return;

		int id = object.getId();
		if (TitheFarmIds.isPlot(id))
		{
			WorldPoint tile = templateTile(object);
			GameObject current = plots.get(tile);
			if (current != null && current.getHash() == object.getHash())
				plots.remove(tile);
		}
		else if (TitheFarmIds.isWaterBarrel(id))
		{
			waterBarrels.remove(object.getHash());
		}
		else if (TitheFarmIds.isSack(id))
		{
			sacks.remove(object.getHash());
		}
		else if (id == TitheFarmIds.SEED_TABLE && seedTable != null && seedTable.getHash() == object.getHash())
		{
			seedTable = null;
		}
	}

	/** Forgets every tracked object, for a world hop or a scene reload. */
	void clear()
	{
		plots.clear();
		lastIds.clear();
		changedTicks.clear();
		stageTicks.clear();
		plantedTicks.clear();
		waterBarrels.clear();
		sacks.clear();
		seedTable = null;
	}

	/** Every plot currently loaded, keyed by template tile, in the order they first spawned. */
	Map<WorldPoint, GameObject> getPlotsByTile()
	{
		return Collections.unmodifiableMap(plots);
	}

	/** The water barrels currently loaded, if any. */
	Collection<GameObject> getWaterBarrels()
	{
		return Collections.unmodifiableCollection(waterBarrels.values());
	}

	/** The deposit sacks currently loaded, if any. */
	Collection<GameObject> getSacks()
	{
		return Collections.unmodifiableCollection(sacks.values());
	}

	/** The seed table currently loaded, or {@code null} when none is in the scene. */
	GameObject getSeedTable()
	{
		return seedTable;
	}

	/**
	 * The client-free snapshot of a plot on the given tile.
	 *
	 * @param tile the plot's template tile
	 * @param plot the plot object on that tile
	 * @param tick the current client tick count
	 * @return the plot's state, stage, remaining waters, age in its current id, age in its growth stage, and the
	 *     ticks since its plant was seeded
	 */
	PlotInfo infoOf(WorldPoint tile, GameObject plot, int tick)
	{
		return PlotInfo.of(plot.getId(), ageSince(changedTicks.get(tile), tick), ageSince(stageTicks.get(tile), tick),
			ageSince(plantedTicks.get(tile), tick));
	}

	/** Ticks since a recorded tick, or {@link PlotInfo#AGE_UNKNOWN} when none was recorded. */
	private static int ageSince(Integer since, int tick)
	{
		return since == null ? PlotInfo.AGE_UNKNOWN : Math.max(0, tick - since);
	}

	/** Whether an id change is a watering: unwatered to watered within the same growth stage. */
	private static boolean isWatering(int previous, int id)
	{
		return TithePlotState.fromObjectId(previous) == TithePlotState.UNWATERED
			&& TithePlotState.fromObjectId(id) == TithePlotState.WATERED
			&& TithePlotState.stageOf(previous) == TithePlotState.stageOf(id);
	}

	/**
	 * The tick a plot's current plant was seeded, for ordering plants by age.
	 *
	 * @param tile the plot's template tile
	 * @return the planting tick, or {@link Integer#MAX_VALUE} when the plant was not seen going in
	 */
	int plantedTick(WorldPoint tile)
	{
		return plantedTicks.getOrDefault(tile, Integer.MAX_VALUE);
	}

	/** Whether the player is at the Tithe Farm, judged by any plot being loaded in the scene. */
	boolean inTitheFarm()
	{
		return !plots.isEmpty();
	}

	/**
	 * The template tile of an object: inside an instance, the real-world tile its chunk was copied from;
	 * outside one, its plain world tile.
	 *
	 * @param object the scene object
	 * @return the object's stable tile
	 */
	static WorldPoint templateTile(GameObject object)
	{
		WorldView view = object.getWorldView();
		LocalPoint local = object.getLocalLocation();
		if (view == null || local == null)
			return object.getWorldLocation();

		return WorldPoint.fromLocalInstance(view.getScene(), local, object.getPlane());
	}
}
