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
import net.runelite.api.CollisionData;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;

/**
 * Builds one {@link RunSnapshot} of the current run from the tracked plots, the backpack, and the config.
 *
 * <p>The route adapts to how the player plants. It starts as the selected route; planting its next plot changes
 * nothing, but planting anywhere else re-plans it: the plants already in keep the order they went in, and the
 * rest of the run is planned with {@link RoutePlanner} as the shortest walk from the newest plant back to the
 * oldest, over real walking distances from {@link PlotGraph}. The adapted order then stays put through harvest
 * and replant rounds until the player strays again, the scene reloads, or the route settings change.
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
	private WorldPoint expectedNext;
	private List<WorldPoint> graphTiles;
	private int[][] graph;

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

		Set<WorldPoint> onRoute = new HashSet<>();
		expectedNext = null;
		for (int[] point : PlantRoute.order(points, preferred, count))
		{
			WorldPoint tile = tiles.get(PlantRoute.key(point));
			GameObject plot = tracker.getPlotsByTile().get(tile);
			PlotInfo info = tracker.infoOf(tile, plot, tick);
			if (expectedNext == null && info.needsSeed())
				expectedNext = tile;

			onRoute.add(tile);
			route.add(plot);
			plots.add(info);
		}

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
		return new RunSnapshot(route, routeLength, plots, water, seeds, runNeed,
			WaterTracker.canAffordPlant(plots, water, slots), advice, recording, recorder.getRecording().size(),
			status, trail);
	}

	/**
	 * Re-plans the route when a seed went somewhere other than the route's next plot. Call after the tracker has
	 * recorded the planting.
	 *
	 * @param tile the template tile of the plot just planted
	 */
	void onPlanted(WorldPoint tile)
	{
		if (recorder.isRecording() || tile.equals(expectedNext))
			return;

		List<WorldPoint> committed = new ArrayList<>();
		List<WorldPoint> empty = new ArrayList<>();
		for (Map.Entry<WorldPoint, GameObject> entry : tracker.getPlotsByTile().entrySet())
		{
			TithePlotState state = TithePlotState.fromObjectId(entry.getValue().getId());
			if (state == TithePlotState.EMPTY)
				empty.add(entry.getKey());
			else if (state != TithePlotState.DEAD)
				committed.add(entry.getKey());
		}

		committed.sort(Comparator.comparingInt(tracker::plantedTick)
			.thenComparingInt(WorldPoint::getX)
			.thenComparingInt(WorldPoint::getY));
		if (committed.isEmpty())
			return;

		ensureGraph();
		Map<WorldPoint, Integer> index = new HashMap<>();
		for (int i = 0; i < graphTiles.size(); i++)
			index.put(graphTiles.get(i), i);

		List<Integer> candidates = new ArrayList<>();
		for (WorldPoint plot : empty)
		{
			if (index.containsKey(plot))
				candidates.add(index.get(plot));
		}

		Integer last = index.get(committed.get(committed.size() - 1));
		Integer first = index.get(committed.get(0));
		if (last == null || first == null)
			return;

		int remaining = config.cropCount() - committed.size();
		List<int[]> order = new ArrayList<>();
		for (WorldPoint plot : committed)
			order.add(new int[]{plot.getX(), plot.getY()});

		for (int planned : RoutePlanner.plan(graph, last, first, candidates, remaining))
			order.add(new int[]{graphTiles.get(planned).getX(), graphTiles.get(planned).getY()});

		adapted = order;
		cached = null;
		log.info("{} replan after off-route plant at {}: kept {}, planned {} -> {}", TitheLayoutLogger.TAG, tile,
			committed.size(), order.size() - committed.size(), RouteRecorder.encode(order));
	}

	/** Forgets the adapted route and the distance graph, for a scene reload or a route-settings change. */
	void reset()
	{
		adapted = null;
		expectedNext = null;
		graph = null;
		graphTiles = null;
		cached = null;
	}

	/** Builds the plot cost graph once per scene, from collision flags when they are available. */
	private void ensureGraph()
	{
		Map<WorldPoint, GameObject> plots = tracker.getPlotsByTile();
		if (graph != null && graphTiles.size() == plots.size())
			return;

		graphTiles = new ArrayList<>(plots.keySet());
		WorldView view = client.getTopLevelWorldView();
		CollisionData[] maps = view == null ? null : view.getCollisionMaps();
		if (maps == null || maps[view.getPlane()] == null)
		{
			graph = RoutePlanner.shapedCosts(chebyshev(graphTiles), centers(graphTiles));
			return;
		}

		int[][] footprints = new int[graphTiles.size()][];
		for (int i = 0; i < graphTiles.size(); i++)
		{
			GameObject plot = plots.get(graphTiles.get(i));
			Point min = plot.getSceneMinLocation();
			Point max = plot.getSceneMaxLocation();
			footprints[i] = new int[]{min.getX(), min.getY(), max.getX(), max.getY()};
		}

		graph = RoutePlanner.shapedCosts(PlotGraph.distances(maps[view.getPlane()].getFlags(), footprints),
			centers(graphTiles));
	}

	/** Plot centre tiles as {@code {x, y}}. */
	private static List<int[]> centers(List<WorldPoint> tiles)
	{
		List<int[]> centers = new ArrayList<>();
		for (WorldPoint tile : tiles)
			centers.add(new int[]{tile.getX(), tile.getY()});

		return centers;
	}

	/** Straight-line tile distances between plots, the fallback when no collision map is loaded. */
	private static int[][] chebyshev(List<WorldPoint> tiles)
	{
		int[][] distance = new int[tiles.size()][tiles.size()];
		for (int i = 0; i < tiles.size(); i++)
		{
			for (int j = 0; j < tiles.size(); j++)
				distance[i][j] = tiles.get(i).distanceTo2D(tiles.get(j));
		}

		return distance;
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
