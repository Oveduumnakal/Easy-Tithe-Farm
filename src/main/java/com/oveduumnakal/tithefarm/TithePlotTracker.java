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
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Singleton;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

/**
 * Finds Tithe Farm plots, water barrels, and deposit sacks in the loaded scene and reads plot state on demand.
 *
 * <p>Plots are keyed by their world tile rather than by object hash, because a plot changes object id as it
 * grows and is watered while staying on the same tile. Keying by tile keeps one stable entry per plot across
 * those id changes, which the route and highlight rely on. State is decoded on demand from the current
 * object id (see {@link TithePlotState}); nothing is cached, so there is no stale-state window.
 */
@Singleton
class TithePlotTracker
{
	private final Map<WorldPoint, GameObject> plots = new LinkedHashMap<>();
	private final Map<Long, GameObject> waterBarrels = new LinkedHashMap<>();
	private final Map<Long, GameObject> sacks = new LinkedHashMap<>();

	private GameObject seedTable;

	/** Records or updates an object that spawned or changed id in place. */
	void onSpawnOrChanged(GameObject object)
	{
		if (object == null)
			return;

		int id = object.getId();
		if (TitheFarmIds.isPlot(id))
			plots.put(object.getWorldLocation(), object);
		else if (TitheFarmIds.isWaterBarrel(id))
			waterBarrels.put(object.getHash(), object);
		else if (TitheFarmIds.isSack(id))
			sacks.put(object.getHash(), object);
		else if (id == TitheFarmIds.SEED_TABLE)
			this.seedTable = object;
	}

	/** Drops an object that has left the scene, only when it is still the tracked object for its slot. */
	void onDespawn(GameObject object)
	{
		if (object == null)
			return;

		int id = object.getId();
		if (TitheFarmIds.isPlot(id))
		{
			WorldPoint point = object.getWorldLocation();
			GameObject current = plots.get(point);
			if (current != null && current.getHash() == object.getHash())
				plots.remove(point);
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
		waterBarrels.clear();
		sacks.clear();
		seedTable = null;
	}

	/** Every plot currently loaded in the scene, in the order they first spawned. */
	Collection<GameObject> getPlots()
	{
		return Collections.unmodifiableCollection(plots.values());
	}

	/** Both halves of the water barrel currently loaded, if any. */
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

	/** The decoded state of a plot object. */
	TithePlotState stateOf(GameObject plot)
	{
		return TithePlotState.fromObjectId(plot.getId());
	}

	/** Whether the player is at the Tithe Farm, judged by any plot being loaded in the scene. */
	boolean inTitheFarm()
	{
		return !plots.isEmpty();
	}
}
