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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Debug logging of the farm's real layout, for checking the route presets against the game.
 *
 * <p>Always on for now. The first tick inside the farm after a scene load writes to the RuneLite log
 * ({@code client.log}):
 * every plot's template tile, region tile, instance tile, object id, and footprint; the barrels, sacks, and seed
 * table; the current route in order; and an ASCII map of the area built from the scene's collision flags, so the
 * walkable tiles between plots are visible. Afterwards, every plot change is logged with the tile the player is
 * standing on, which records where each plant, water, and harvest was done from. Every line starts with
 * {@value #TAG} so it can be pulled out with grep.
 */
@Singleton
class TitheLayoutLogger
{
	/** Prefix on every log line. */
	static final String TAG = "[tithe-layout]";

	/** Tiles of walkable margin drawn around the plots' bounding box. */
	private static final int MARGIN = 6;

	private static final Logger log = LoggerFactory.getLogger(TitheLayoutLogger.class);

	private static final int WALL_FLAGS = CollisionDataFlag.BLOCK_MOVEMENT_NORTH
		| CollisionDataFlag.BLOCK_MOVEMENT_EAST
		| CollisionDataFlag.BLOCK_MOVEMENT_SOUTH
		| CollisionDataFlag.BLOCK_MOVEMENT_WEST;

	private final Client client;
	private final TitheFarmConfig config;
	private final TithePlotTracker tracker;
	private final TitheRun run;

	private boolean pending;
	private String lastAdvice;
	private int lastScore = -1;
	private int lastPoints = -1;

	@Inject
	TitheLayoutLogger(Client client, TitheFarmConfig config, TithePlotTracker tracker, TitheRun run)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.run = run;
	}

	/** Asks for a layout dump on the next tick inside the farm. */
	void requestDump()
	{
		pending = true;
		lastAdvice = null;
	}

	/** Writes a pending layout dump, and logs the advice whenever it changes. */
	void onTick()
	{
		if (!tracker.inTitheFarm())
			return;

		if (pending)
		{
			pending = false;
			dump();
		}

		int score = client.getVarbitValue(TitheFarmIds.SCORE_VARBIT);
		int points = client.getVarbitValue(TitheFarmIds.POINTS_VARBIT);
		if (score != lastScore || points != lastPoints)
		{
			lastScore = score;
			lastPoints = points;
			log.info("{} varbits score={} points={} tick={}", TAG, score, points, client.getTickCount());
		}

		RunSnapshot snapshot = run.snapshot();
		ActionAdvisor.Advice advice = snapshot.getAdvice();
		GameObject target = snapshot.getTargetPlot();
		String text = advice.getAction().getLabel() + " #" + snapshot.getTargetNumber()
			+ (target == null ? "" : " " + describe(TithePlotTracker.templateTile(target)));
		if (!text.equals(lastAdvice))
		{
			lastAdvice = text;
			log.info("{} advice {} tick={}", TAG, text, client.getTickCount());
		}
	}

	/**
	 * Logs a plot changing id, with the tile the player stands on.
	 *
	 * @param plot       the plot object after the change
	 * @param previousId the id previously seen on its tile, or {@code -1}
	 */
	void onPlotChanged(GameObject plot, int previousId)
	{
		if (previousId < 0 || previousId == plot.getId())
			return;

		WorldPoint tile = TithePlotTracker.templateTile(plot);
		log.info("{} plot {} {}->{} ({}->{}) player={} tick={}", TAG, describe(tile),
			TithePlotState.fromObjectId(previousId), TithePlotState.fromObjectId(plot.getId()), previousId,
			plot.getId(), playerTile(), client.getTickCount());
	}

	/** Writes every tracked object, the route, and the collision map. */
	private void dump()
	{
		WorldView view = client.getTopLevelWorldView();
		log.info("{} ===== dump: instance={} plane={} plots={} player={} =====", TAG, view.isInstance(),
			view.getPlane(), tracker.getPlotsByTile().size(), playerTile());

		Map<Long, Character> marks = new HashMap<>();
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (Map.Entry<WorldPoint, GameObject> entry : tracker.getPlotsByTile().entrySet())
		{
			GameObject plot = entry.getValue();
			Point min = plot.getSceneMinLocation();
			Point max = plot.getSceneMaxLocation();
			log.info("{} plot {} id={} state={} instance={} size={}x{} sceneMin={},{} sceneMax={},{}", TAG,
				describe(entry.getKey()), plot.getId(), TithePlotState.fromObjectId(plot.getId()),
				plot.getWorldLocation(), plot.sizeX(), plot.sizeY(), min.getX(), min.getY(), max.getX(),
				max.getY());
			markFootprint(marks, plot, 'P');
			minX = Math.min(minX, min.getX());
			minY = Math.min(minY, min.getY());
			maxX = Math.max(maxX, max.getX());
			maxY = Math.max(maxY, max.getY());
		}

		for (GameObject barrel : tracker.getWaterBarrels())
		{
			log.info("{} barrel {} id={}", TAG, describe(TithePlotTracker.templateTile(barrel)), barrel.getId());
			markFootprint(marks, barrel, 'W');
		}

		for (GameObject sack : tracker.getSacks())
		{
			log.info("{} sack {} id={}", TAG, describe(TithePlotTracker.templateTile(sack)), sack.getId());
			markFootprint(marks, sack, 'S');
		}

		GameObject table = tracker.getSeedTable();
		if (table != null)
		{
			log.info("{} seedTable {}", TAG, describe(TithePlotTracker.templateTile(table)));
			markFootprint(marks, table, 'T');
		}

		RunSnapshot snapshot = run.snapshot();
		log.info("{} route mode={} cropCount={} length={}", TAG, config.routeMode(), config.cropCount(),
			snapshot.getRouteLength());
		List<GameObject> route = snapshot.getRoute();
		for (int i = 0; i < snapshot.getRouteLength(); i++)
			log.info("{} route #{} {}", TAG, i + 1, describe(TithePlotTracker.templateTile(route.get(i))));

		if (minX <= maxX)
			dumpGrid(view, marks, minX - MARGIN, minY - MARGIN, maxX + MARGIN, maxY + MARGIN);

		log.info("{} ===== end dump =====", TAG);
	}

	/**
	 * Logs an ASCII map of a scene rectangle, one line per row, north at the top. Rows and columns are labelled
	 * with template coordinates. Legend: P plot, W water barrel, S sack, T seed table, @ player, # blocked,
	 * + walkable with a wall on at least one side, . walkable.
	 */
	private void dumpGrid(WorldView view, Map<Long, Character> marks, int x0, int y0, int x1, int y1)
	{
		CollisionData[] maps = view.getCollisionMaps();
		if (maps == null || maps[view.getPlane()] == null)
		{
			log.info("{} no collision map", TAG);
			return;
		}

		int[][] flags = maps[view.getPlane()].getFlags();
		Player player = client.getLocalPlayer();
		LocalPoint playerLocal = player == null ? null : player.getLocalLocation();
		int sceneX0 = Math.max(0, x0);
		int sceneY0 = Math.max(0, y0);
		int sceneX1 = Math.min(flags.length - 1, x1);
		int sceneY1 = Math.min(flags[0].length - 1, y1);
		log.info("{} grid legend: P plot, W barrel, S sack, T table, @ player, # blocked, + wall edge, . open",
			TAG);
		WorldPoint corner = template(view, sceneX0, sceneY0);
		log.info("{} grid scene x {}..{} y {}..{}; bottom-left template tile {}", TAG, sceneX0, sceneX1, sceneY0,
			sceneY1, corner);
		for (int sy = sceneY1; sy >= sceneY0; sy--)
		{
			StringBuilder row = new StringBuilder();
			for (int sx = sceneX0; sx <= sceneX1; sx++)
				row.append(cell(flags[sx][sy], marks.get(key(sx, sy)), playerLocal, sx, sy));

			WorldPoint left = template(view, sceneX0, sy);
			log.info("{} grid y={} x0={} {}", TAG, left.getY(), left.getX(), row);
		}
	}

	/** The map character for one scene tile. */
	private static char cell(int flag, Character mark, LocalPoint player, int sx, int sy)
	{
		if (player != null && player.getSceneX() == sx && player.getSceneY() == sy)
			return '@';

		if (mark != null)
			return mark;

		if ((flag & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
			return '#';

		return (flag & WALL_FLAGS) != 0 ? '+' : '.';
	}

	/** Marks every scene tile an object covers with the given character. */
	private static void markFootprint(Map<Long, Character> marks, GameObject object, char mark)
	{
		Point min = object.getSceneMinLocation();
		Point max = object.getSceneMaxLocation();
		for (int x = min.getX(); x <= max.getX(); x++)
		{
			for (int y = min.getY(); y <= max.getY(); y++)
				marks.put(key(x, y), mark);
		}
	}

	/** The template tile of a scene tile. */
	private static WorldPoint template(WorldView view, int sceneX, int sceneY)
	{
		return WorldPoint.fromLocalInstance(view.getScene(), LocalPoint.fromScene(sceneX, sceneY, view),
			view.getPlane());
	}

	/** The player's template tile, or {@code null} when there is no player. */
	private WorldPoint playerTile()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getLocalLocation() == null)
			return null;

		WorldView view = client.getTopLevelWorldView();
		return WorldPoint.fromLocalInstance(view.getScene(), player.getLocalLocation(), view.getPlane());
	}

	/** A tile as world and region coordinates, e.g. {@code (1826,3513) r7222(34,57)}. */
	private static String describe(WorldPoint tile)
	{
		return "(" + tile.getX() + "," + tile.getY() + ") r" + tile.getRegionID() + "(" + tile.getRegionX() + ","
			+ tile.getRegionY() + ")";
	}

	/** Packs a scene tile into one map key. */
	private static long key(int x, int y)
	{
		return ((long) x << 32) | (y & 0xffffffffL);
	}
}
