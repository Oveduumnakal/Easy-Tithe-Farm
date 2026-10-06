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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;

/**
 * Builds one {@link RunSnapshot} of the current run from the tracked plots, the backpack, and the config.
 *
 * <p>The route adapts to how the player plants. It starts as the selected route; planting any of its plots changes
 * nothing, even out of order, so a skipped plot stays next and the player doubles back to it rather than the
 * pattern being redrawn. Planting a plot off the route keeps the shape too: the new plant takes the place of the
 * route's next empty plot, and the route's last empty plot drops out so the crop count holds. The adapted order
 * then stays put through harvest and replant rounds until the player strays again, the scene reloads, or the
 * route settings change.
 *
 * <p>The snapshot's plots are the route, in order, followed by every planted plot off the route, oldest plant
 * first — so a plant the player put somewhere else is still watered, harvested, and counted in the water need.
 * The overlays, the menu guard, and the water reminder all read the same snapshot, so they always agree on
 * which plots are in the route, how much water is carried and needed, and what the next action is. Building
 * one is cheap — a few dozen plots and a 28-slot backpack — and it is reused within a client cycle so the three
 * overlays drawn in one frame share a single build.
 */
@Singleton
class TitheRun
{
	private final Client client;
	private final TitheFarmConfig config;
	private final TithePlotTracker tracker;
	private final RouteRecorder recorder;

	private static final Logger log = LoggerFactory.getLogger(TitheRun.class);

	/** {@link Client#getEnergy()} reports hundredths of a percent. */
	private static final int ENERGY_SCALE = 100;

	private Boolean fruitStacks;
	private RunSnapshot cached;
	private int cachedCycle = -1;

	private List<int[]> adapted;
	private List<WorldPoint> routeOrder = Collections.emptyList();

	@Inject
	TitheRun(Client client, TitheFarmConfig config, TithePlotTracker tracker, RouteRecorder recorder)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.recorder = recorder;
	}

	/**
	 * The run as it stands this client cycle, built once per cycle and shared by every caller in it. Call only
	 * on the client thread.
	 */
	RunSnapshot snapshot()
	{
		int cycle = client.getGameCycle();
		if (cached == null || cycle != cachedCycle)
		{
			cached = build();
			cachedCycle = cycle;
		}

		return cached;
	}

	/** Builds a fresh snapshot from the tracked plots, the backpack, and the config. */
	private RunSnapshot build()
	{
		List<GameObject> route = new ArrayList<>();
		List<PlotInfo> plots = new ArrayList<>();
		int tick = client.getTickCount();
		Map<Long, WorldPoint> tiles = new HashMap<>();
		List<int[]> points = new ArrayList<>();
		for (WorldPoint tile : tracker.getPlotsByTile().keySet())
		{
			int[] point = {tile.getX(), tile.getY()};
			tiles.put(PlantRoute.key(point), tile);
			points.add(point);
		}

		boolean recording = recorder.isRecording();
		List<int[]> preferred;
		int count;
		if (recording)
		{
			preferred = recorder.getRecording();
			count = preferred.size();
		}
		else
		{
			RouteMode mode = config.routeMode();
			preferred = mode == RouteMode.RECORDED ? recorder.getSaved() : mode.getPreset();
			count = config.cropCount();
			if (adapted != null)
			{
				preferred = adapted;
				count = Math.max(count, adapted.size());
			}
		}

		List<WorldPoint> routeTiles = new ArrayList<>();
		for (int[] point : PlantRoute.order(points, preferred, count))
		{
			WorldPoint tile = tiles.get(PlantRoute.key(point));
			GameObject plot = tracker.getPlotsByTile().get(tile);
			routeTiles.add(tile);
			route.add(plot);
			plots.add(tracker.infoOf(tile, plot, tick));
		}

		routeOrder = routeTiles;
		Set<WorldPoint> onRoute = new HashSet<>(routeTiles);

		int routeLength = route.size();
		List<WorldPoint> offRoute = new ArrayList<>();
		for (Map.Entry<WorldPoint, GameObject> entry : tracker.getPlotsByTile().entrySet())
		{
			if (!onRoute.contains(entry.getKey()) && entry.getValue().getId() != TitheFarmIds.PLOT_EMPTY)
				offRoute.add(entry.getKey());
		}

		offRoute.sort(Comparator.comparingInt(tracker::plantedTick)
			.thenComparingInt(WorldPoint::getX)
			.thenComparingInt(WorldPoint::getY));
		for (WorldPoint tile : offRoute)
		{
			GameObject plot = tracker.getPlotsByTile().get(tile);
			route.add(plot);
			plots.add(tracker.infoOf(tile, plot, tick));
		}

		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		Item[] items = inventory == null ? new Item[0] : inventory.getItems();
		int[] ids = new int[items.length];
		int[] counts = new int[items.length];
		int seeds = 0;
		int fruit = 0;
		for (int i = 0; i < items.length; i++)
		{
			Item item = items[i];
			ids[i] = item == null ? -1 : item.getId();
			if (ids[i] < 0)
				continue;

			counts[i] = item.getQuantity();
			if (TitheFarmIds.isSeed(ids[i]))
				seeds += item.getQuantity();
			else if (TitheFarmIds.isFruit(ids[i]))
				fruit += item.getQuantity();
		}

		int gricollerCharges = client.getVarbitValue(TitheFarmIds.GRICOLLER_CHARGES_VARBIT);
		int water = WaterTracker.totalCharges(ids, gricollerCharges);
		Backpack backpack = Backpack.of(ids, counts, fruitStacks());
		int slots = ActionAdvisor.plantSlots(plots, config.cropCount());
		int deposited = client.getVarbitValue(TitheFarmIds.SCORE_VARBIT);
		List<String> missingTools = config.inventoryCheck()
			? InventoryCheck.missing(ids, config.barehandedPlanting())
			: Collections.emptyList();
		boolean fertiliser = config.inventoryCheck() && InventoryCheck.hasFertiliser(ids);
		int energy = client.getEnergy() / ENERGY_SCALE;
		boolean cansFull = WaterTracker.cansFull(ids, gricollerCharges);
		RunStatus status = new RunStatus(deposited, missingTools, fertiliser, energy, fruit,
			DepositRewards.xp(deposited, ids, counts, DepositRewards.outfitBoost(wornIds())), cansFull);
		ActionAdvisor.Advice decided = ActionAdvisor.decide(plots, seeds, water, backpack, slots);
		boolean betweenRuns = RunSnapshot.nothingGrowing(plots);
		ActionAdvisor.Advice advice = config.waterRefillWarning()
			? ActionAdvisor.topUpFirst(decided, betweenRuns, cansFull)
			: decided;
		List<ActionAdvisor.Advice> trail = Collections.emptyList();
		if (advice.getPlotIndex() >= 0)
		{
			int length = config.minimalView() ? 1 : ActionForecast.TRAIL_LENGTH;
			trail = ActionForecast.forecast(plots, seeds, water, backpack, slots, length);
		}

		int runNeed = WaterTracker.runNeed(plots, slots);
		return new RunSnapshot(route, routeLength, plots, water, runNeed,
			WaterTracker.canAffordPlant(plots, water, slots), advice, recording, recorder.getRecording().size(),
			status, trail);
	}

	/**
	 * Adapts the route when a seed went on a plot off the route, keeping its shape: the new plant takes the place
	 * of the route's next empty plot in the order, and the route's last empty plot drops out so the crop count
	 * holds. A seed on a route plot, even out of order, keeps the route, so the next plant is the earliest route
	 * plot still empty. Call after the tracker has recorded the planting, on the client thread.
	 *
	 * @param tile the template tile of the plot just planted
	 */
	void onPlanted(WorldPoint tile)
	{
		if (recorder.isRecording() || routeOrder.isEmpty() || routeOrder.contains(tile))
			return;

		List<WorldPoint> order = new ArrayList<>(routeOrder);
		int next = -1;
		int last = -1;
		for (int i = 0; i < order.size(); i++)
		{
			if (needsSeed(order.get(i)))
			{
				if (next < 0)
					next = i;

				last = i;
			}
		}

		if (last >= 0)
			order.remove(last);

		order.add(next >= 0 ? next : order.size(), tile);
		List<int[]> points = new ArrayList<>();
		for (WorldPoint plot : order)
			points.add(new int[]{plot.getX(), plot.getY()});

		adapted = points;
		cached = null;
		log.debug("{} off-route plant at {} took route place {} -> {}", TitheLayoutLogger.TAG, tile,
			next >= 0 ? next + 1 : order.size(), RouteRecorder.encode(points));
	}

	/** Forgets the adapted route, for a scene reload or a route-settings change. Call only on the client thread. */
	void reset()
	{
		adapted = null;
		routeOrder = Collections.emptyList();
		cached = null;
	}

	/** Whether a tracked plot still needs a seed: empty, or dead and waiting to be cleared. */
	private boolean needsSeed(WorldPoint tile)
	{
		GameObject plot = tracker.getPlotsByTile().get(tile);
		if (plot == null)
			return false;

		TithePlotState state = TithePlotState.fromObjectId(plot.getId());
		return state == TithePlotState.EMPTY || state == TithePlotState.DEAD;
	}

	/** The item ids of the equipment worn, empty when the equipment is not loaded. */
	private int[] wornIds()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
			return new int[0];

		Item[] items = worn.getItems();
		int[] ids = new int[items.length];
		for (int i = 0; i < items.length; i++)
			ids[i] = items[i] == null ? -1 : items[i].getId();

		return ids;
	}

	/** Whether Tithe Farm fruit stacks in the backpack, read once from the item definition. */
	private boolean fruitStacks()
	{
		if (fruitStacks == null)
			fruitStacks = client.getItemDefinition(TitheFarmIds.FRUIT_GOLOVANOVA).isStackable();

		return fruitStacks;
	}
}
