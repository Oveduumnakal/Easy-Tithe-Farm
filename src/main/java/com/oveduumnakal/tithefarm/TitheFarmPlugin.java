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

import java.util.Set;
import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/** Entry point: registers overlays and feeds the trackers with scene, inventory, and menu events. */
@PluginDescriptor(
	name = "Easy Tithe Farm",
	description = "Draws the optimal Tithe Farm planting and watering route, highlights what to click next, "
		+ "tracks water, and reminds you to refill",
	tags = {"tithe", "farm", "farming", "minigame", "hosidius", "kourend", "overlay", "route", "water"}
)
public class TitheFarmPlugin extends Plugin
{
	/** Config keys whose change invalidates an adapted route. */
	private static final Set<String> ROUTE_KEYS = ImmutableSet.of(TitheFarmConfig.ROUTE_MODE, "cropCount",
		TitheFarmConfig.RECORD_ROUTE, TitheFarmConfig.RECORDED_ROUTE);

	@Inject
	private Client client;

	@Inject
	private TitheFarmConfig config;

	@Inject
	private TithePlotTracker tracker;

	@Inject
	private RouteRecorder recorder;

	@Inject
	private TitheLayoutLogger layoutLogger;

	@Inject
	private TitheRun run;

	@Inject
	private TitheTimerOverlay timerOverlay;

	@Inject
	private DeathWarner deathWarner;

	@Inject
	private SessionTracker session;

	@Inject
	private GoalTracker goals;

	@Inject
	private RewardGoalOverlay goalOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TitheWaterOverlay waterOverlay;

	@Inject
	private TitheHighlightOverlay highlightOverlay;

	@Inject
	private TitheInventoryOverlay inventoryOverlay;

	@Inject
	private WaterReminder waterReminder;

	@Inject
	private TitheMenuSwapper menuSwapper;

	@Override
	protected void startUp()
	{
		overlayManager.add(waterOverlay);
		overlayManager.add(highlightOverlay);
		overlayManager.add(inventoryOverlay);
		overlayManager.add(timerOverlay);
		overlayManager.add(goalOverlay);
		waterReminder.reset();
		layoutLogger.requestDump();
		session.reset();
		goals.reset();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(waterOverlay);
		overlayManager.remove(highlightOverlay);
		overlayManager.remove(inventoryOverlay);
		overlayManager.remove(timerOverlay);
		overlayManager.remove(goalOverlay);
		tracker.clear();
		waterReminder.reset();
	}

	/** Runs the per-tick checks. */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		waterReminder.onTick();
		deathWarner.onTick();
		session.onTick();
		goals.onTick();
		layoutLogger.onTick();
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		menuSwapper.onPostMenuSort();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		recorder.onConfigChanged(event);
		if (TitheFarmConfig.GROUP.equals(event.getGroup()) && ROUTE_KEYS.contains(event.getKey()))
			run.reset();
	}

	/** Tracks the object and, when an empty plot has just taken a seed, tells the route recorder. */
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		int previous = tracker.onSpawnOrChanged(object, client.getTickCount());
		if (TitheFarmIds.isPlot(object.getId()))
			layoutLogger.onPlotChanged(object, previous);

		if (previous == TitheFarmIds.PLOT_EMPTY && TithePlotState.isFreshSeed(object.getId()))
		{
			WorldPoint tile = TithePlotTracker.templateTile(object);
			recorder.onPlanted(tile);
			run.onPlanted(tile);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		tracker.onDespawn(event.getGameObject());
	}

	/**
	 * Clears tracked objects when the scene is torn down, and asks for a fresh layout dump once a new scene is
	 * in. Despawn events do not always fire on a world hop, so without this the plugin would keep drawing plots
	 * that are no longer loaded.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOADING || state == GameState.HOPPING || state == GameState.LOGIN_SCREEN)
		{
			tracker.clear();
			waterReminder.reset();
			run.reset();
			deathWarner.reset();
			if (state != GameState.LOADING)
				goals.reset();
		}
		else if (state == GameState.LOGGED_IN)
		{
			layoutLogger.requestDump();
		}
	}

	@Provides
	TitheFarmConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TitheFarmConfig.class);
	}
}
