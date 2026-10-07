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
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.OverlayManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives the plugin's lifecycle with mocked collaborators and a queued client thread: config changes wait for
 * the client thread (#10), start-up re-reads a farm that is already loaded (#11), an account switch restarts
 * the session (#18), and start-up carries the old step-numbers box over to the step-marker dropdown (#58).
 */
public class TitheFarmPluginLifecycleTest
{
	private static final long ACCOUNT = 1L;
	private static final long OTHER_ACCOUNT = 2L;

	private final List<Runnable> clientThreadQueue = new ArrayList<>();

	@Mock
	private Client client;

	@Mock
	private ClientThread clientThread;

	@Mock
	private TitheFarmConfig config;

	@Mock
	private ConfigManager configManager;

	@Spy
	private TithePlotTracker tracker = new TithePlotTracker();

	@Mock
	private RouteRecorder recorder;

	@Mock
	private TitheRun run;

	@Mock
	private TitheTimerOverlay timerOverlay;

	@Mock
	private DeathWarner deathWarner;

	@Mock
	private SessionTracker session;

	@Mock
	private GoalTracker goals;

	@Mock
	private RewardGoalOverlay goalOverlay;

	@Mock
	private OverlayManager overlayManager;

	@Mock
	private TitheWaterOverlay waterOverlay;

	@Mock
	private TitheHighlightOverlay highlightOverlay;

	@Mock
	private TitheInventoryOverlay inventoryOverlay;

	@Mock
	private WaterReminder waterReminder;

	@Mock
	private TitheMenuSwapper menuSwapper;

	@InjectMocks
	private TitheFarmPlugin plugin;

	private AutoCloseable mocks;
	private long hash;

	@Before
	public void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
		doAnswer(invocation -> clientThreadQueue.add(invocation.getArgument(0)))
			.when(clientThread)
			.invokeLater(any(Runnable.class));
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getAccountHash()).thenReturn(ACCOUNT);
	}

	@After
	public void tearDown() throws Exception
	{
		mocks.close();
	}

	/** Runs what was handed to the client thread, as the client does at the end of a cycle. */
	private void runClientThread()
	{
		List<Runnable> queued = new ArrayList<>(clientThreadQueue);
		clientThreadQueue.clear();
		for (Runnable task : queued)
			task.run();
	}

	private GameObject object(int id, int x, int y)
	{
		GameObject object = TestRuns.plot(id, ++hash);
		when(object.getWorldLocation()).thenReturn(new WorldPoint(x, y, 0));
		return object;
	}

	private static Tile tile(GameObject... objects)
	{
		Tile tile = mock(Tile.class);
		when(tile.getGameObjects()).thenReturn(objects);
		return tile;
	}

	/** Loads a scene holding two plots, the second wide enough to sit on two tiles, a barrel, a sack, and a table. */
	private GameObject loadFarmScene()
	{
		GameObject empty = object(TitheFarmIds.PLOT_EMPTY, 1811, 3489);
		GameObject growing = object(TitheFarmIds.PLOT_GROWTH_FIRST, 1816, 3489);
		GameObject barrel = object(TitheFarmIds.FARM_WATER_BARREL_A, 1806, 3501);
		GameObject sack = object(TitheFarmIds.SACK_OF_FRUIT, 1810, 3480);
		GameObject table = object(TitheFarmIds.SEED_TABLE, 1806, 3484);
		Tile[][][] tiles = new Tile[2][][];
		tiles[0] = new Tile[][]{
			{tile(empty, null), null},
			{tile(growing), tile(growing, barrel)},
			null,
			{tile(sack, table), tile()}
		};
		Scene scene = mock(Scene.class);
		when(scene.getTiles()).thenReturn(tiles);
		WorldView view = mock(WorldView.class);
		when(view.getScene()).thenReturn(scene);
		when(client.getTopLevelWorldView()).thenReturn(view);
		return table;
	}

	private void gameState(GameState state, long account)
	{
		when(client.getGameState()).thenReturn(state);
		when(client.getAccountHash()).thenReturn(account);
		GameStateChanged event = new GameStateChanged();
		event.setGameState(state);
		plugin.onGameStateChanged(event);
	}

	private static ConfigChanged change(String group, String key, String value)
	{
		ConfigChanged event = new ConfigChanged();
		event.setGroup(group);
		event.setKey(key);
		event.setNewValue(value);
		return event;
	}

	/** Stores a raw config value as {@link ConfigManager} would hand it back. */
	private void stored(String key, String value)
	{
		when(configManager.getConfiguration(TitheFarmConfig.GROUP, key)).thenReturn(value);
	}

	@Test
	public void anUncheckedStepNumbersBoxBecomesOff()
	{
		stored(TitheFarmConfig.LEGACY_STEP_NUMBERS, "false");
		plugin.startUp();
		verify(configManager).setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.STEP_MARKERS, StepMarker.OFF);
		verify(configManager).unsetConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.LEGACY_STEP_NUMBERS);
	}

	@Test
	public void aCheckedStepNumbersBoxKeepsTheNumbersDefault()
	{
		stored(TitheFarmConfig.LEGACY_STEP_NUMBERS, "true");
		plugin.startUp();
		verify(configManager, never()).setConfiguration(anyString(), anyString(), any(StepMarker.class));
		verify(configManager).unsetConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.LEGACY_STEP_NUMBERS);
	}

	@Test
	public void aChosenStepMarkerIsNotOverwritten()
	{
		stored(TitheFarmConfig.LEGACY_STEP_NUMBERS, "false");
		stored(TitheFarmConfig.STEP_MARKERS, StepMarker.BLIPS.name());
		plugin.startUp();
		verify(configManager, never()).setConfiguration(anyString(), anyString(), any(StepMarker.class));
		verify(configManager).unsetConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.LEGACY_STEP_NUMBERS);
	}

	@Test
	public void withoutTheOldBoxNothingIsMigrated()
	{
		plugin.startUp();
		verify(configManager, never()).setConfiguration(anyString(), anyString(), any(StepMarker.class));
		verify(configManager, never()).unsetConfiguration(anyString(), anyString());
	}

	@Test
	public void startUpInsideTheFarmTracksTheLoadedScene()
	{
		GameObject table = loadFarmScene();
		plugin.startUp();
		assertFalse(tracker.inTitheFarm());

		runClientThread();
		assertTrue(tracker.inTitheFarm());
		assertEquals(2, tracker.getPlotsByTile().size());
		assertEquals(1, tracker.getWaterBarrels().size());
		assertEquals(1, tracker.getSacks().size());
		assertSame(table, tracker.getSeedTable());
		verify(run).reset();
		verify(deathWarner).reset();
		verify(recorder, never()).onPlanted(any());
	}

	@Test
	public void reEnablingInsideTheFarmRestoresTracking()
	{
		loadFarmScene();
		plugin.startUp();
		runClientThread();
		plugin.shutDown();
		assertFalse(tracker.inTitheFarm());

		plugin.startUp();
		runClientThread();
		assertEquals(2, tracker.getPlotsByTile().size());
	}

	@Test
	public void startUpWhileLoggedOutTracksNothing()
	{
		loadFarmScene();
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		plugin.startUp();
		runClientThread();
		assertFalse(tracker.inTitheFarm());
		verify(run).reset();
	}

	@Test
	public void aRouteSettingChangeWaitsForTheClientThread()
	{
		ConfigChanged event = change(TitheFarmConfig.GROUP, TitheFarmConfig.ROUTE_MODE, "RECORDED");
		plugin.onConfigChanged(event);
		verify(run, never()).reset();
		verify(recorder, never()).onConfigChanged(any(), any());

		event.setKey("minimalView");
		event.setNewValue("true");
		runClientThread();
		verify(recorder).onConfigChanged(TitheFarmConfig.ROUTE_MODE, "RECORDED");
		verify(run).reset();
	}

	@Test
	public void theRecordToggleReachesTheRecorderOnTheClientThread()
	{
		plugin.onConfigChanged(change(TitheFarmConfig.GROUP, TitheFarmConfig.RECORD_ROUTE, "true"));
		verify(recorder, never()).onConfigChanged(any(), any());

		runClientThread();
		verify(recorder).onConfigChanged(TitheFarmConfig.RECORD_ROUTE, "true");
		verify(run).reset();
	}

	@Test
	public void otherSettingsKeepTheAdaptedRoute()
	{
		plugin.onConfigChanged(change(TitheFarmConfig.GROUP, "minimalView", "true"));
		runClientThread();
		verify(recorder).onConfigChanged("minimalView", "true");
		verify(run, never()).reset();
	}

	@Test
	public void otherPluginsConfigIsIgnored()
	{
		plugin.onConfigChanged(change("someOtherPlugin", TitheFarmConfig.ROUTE_MODE, "x"));
		assertTrue(clientThreadQueue.isEmpty());
	}

	@Test
	public void aDifferentAccountRestartsTheSession()
	{
		plugin.startUp();
		runClientThread();
		clearInvocations(session);

		gameState(GameState.LOADING, ACCOUNT);
		gameState(GameState.LOGGED_IN, ACCOUNT);
		gameState(GameState.HOPPING, ACCOUNT);
		gameState(GameState.LOGGED_IN, ACCOUNT);
		gameState(GameState.LOGIN_SCREEN, -1L);
		gameState(GameState.LOGGED_IN, ACCOUNT);
		verify(session, never()).reset();

		gameState(GameState.LOGIN_SCREEN, -1L);
		gameState(GameState.LOGGED_IN, OTHER_ACCOUNT);
		verify(session).reset();
	}
}
