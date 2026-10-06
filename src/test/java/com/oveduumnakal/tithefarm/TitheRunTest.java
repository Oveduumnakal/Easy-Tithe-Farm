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

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Drives the run builder over a mocked 41-plot farm: route, adaptation, water need, wrap-up, and status. */
public class TitheRunTest
{
	private static final int EMPTY = 27383;
	private static final int FRESH_C = 27406;
	private static final int STAGE1_WET_C = 27407;
	private static final int GROWN_C = 27415;

	private Client client;
	private TitheFarmConfig config;
	private TithePlotTracker tracker;
	private RouteRecorder recorder;
	private TitheRun run;
	private int cycle;
	private int tick;
	private long hash;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		config = TestRuns.defaultConfig();
		tracker = new TithePlotTracker();
		recorder = mock(RouteRecorder.class);
		when(recorder.getRecording()).thenReturn(Collections.emptyList());
		when(recorder.getSaved()).thenReturn(Collections.emptyList());
		when(client.getGameCycle()).thenAnswer(invocation -> cycle++);
		when(client.getTickCount()).thenAnswer(invocation -> tick);
		when(client.getEnergy()).thenReturn(10_000);
		ItemComposition fruit = mock(ItemComposition.class);
		when(fruit.isStackable()).thenReturn(true);
		when(client.getItemDefinition(anyInt())).thenReturn(fruit);
		inventory(new Item(952, 1), new Item(5343, 1), new Item(13353, 1), new Item(13425, 20));
		when(client.getVarbitValue(TitheFarmIds.GRICOLLER_CHARGES_VARBIT)).thenReturn(1000);
		for (int[] tile : TitheRoutesTest.farm())
			spawn(tile, EMPTY);

		run = new TitheRun(client, config, tracker, recorder);
	}

	private void inventory(Item... items)
	{
		ItemContainer container = mock(ItemContainer.class);
		when(container.getItems()).thenReturn(items);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(container);
	}

	private GameObject spawn(int[] tile, int id)
	{
		GameObject plot = TestRuns.plot(id, ++hash);
		when(plot.getWorldLocation()).thenReturn(new WorldPoint(tile[0], tile[1], 0));
		tracker.onSpawnOrChanged(plot, tick);
		return plot;
	}

	/** Plants a seed on a plot the way the game does — a new object id — and tells the run. */
	private void plant(int[] tile)
	{
		tick++;
		spawn(tile, FRESH_C);
		run.onPlanted(new WorldPoint(tile[0], tile[1], 0));
	}

	private static int[] tileOf(GameObject plot)
	{
		return new int[]{plot.getWorldLocation().getX(), plot.getWorldLocation().getY()};
	}

	@Test
	public void freshFarmFollowsTheWikiBasicRoute()
	{
		RunSnapshot snapshot = run.snapshot();
		assertEquals(20, snapshot.getRouteLength());
		assertEquals(NextAction.PLANT_SEED, snapshot.getAdvice().getAction());
		int[] first = tileOf(snapshot.getTargetPlot());
		assertEquals(TitheRoutes.BASIC_20.get(0)[0], first[0]);
		assertEquals(TitheRoutes.BASIC_20.get(0)[1], first[1]);
		assertEquals(60, snapshot.getRunNeed());
	}

	@Test
	public void plantingTheSuggestedPlotKeepsTheRoute()
	{
		run.snapshot();
		plant(TitheRoutes.BASIC_20.get(0));
		RunSnapshot snapshot = run.snapshot();
		assertEquals(NextAction.WATER_PLANT, snapshot.getAdvice().getAction());
		assertEquals(1, snapshot.getTargetNumber());
		int[] second = tileOf(snapshot.getRoute().get(1));
		assertEquals(TitheRoutes.BASIC_20.get(1)[0], second[0]);
	}

	@Test
	public void plantingOffRouteReplansAroundThePlayer()
	{
		run.snapshot();
		int[] southWest = {1811, 3489};
		plant(southWest);
		RunSnapshot snapshot = run.snapshot();
		int[] first = tileOf(snapshot.getRoute().get(0));
		assertEquals(1811, first[0]);
		assertEquals(3489, first[1]);
		assertEquals(20, snapshot.getRouteLength());
		assertEquals(NextAction.WATER_PLANT, snapshot.getAdvice().getAction());
		assertSame(snapshot.getRoute().get(0), snapshot.getTargetPlot());
	}

	@Test
	public void waterNeedCountsPlantsWhereverTheyAre()
	{
		run.snapshot();
		tick++;
		spawn(new int[]{1811, 3489}, STAGE1_WET_C);
		RunSnapshot snapshot = run.snapshot();
		assertEquals(2 + 19 * 3, snapshot.getRunNeed());
	}

	@Test
	public void theFruitDepositedNeverShortensTheRun()
	{
		when(client.getVarbitValue(TitheFarmIds.SCORE_VARBIT)).thenReturn(88);
		RunSnapshot snapshot = run.snapshot();
		assertEquals(88, snapshot.getStatus().getDeposited());
		assertEquals(20 * 3, snapshot.getRunNeed());
		assertEquals(NextAction.PLANT_SEED, snapshot.getAdvice().getAction());
	}

	@Test
	public void statusReportsMissingToolsFertiliserAndEnergy()
	{
		inventory(new Item(13353, 1), new Item(13420, 1), new Item(13425, 20));
		when(client.getEnergy()).thenReturn(1_250);
		RunStatus status = run.snapshot().getStatus();
		List<String> missing = status.getMissingTools();
		assertTrue(missing.contains(InventoryCheck.SPADE));
		assertFalse(missing.contains(InventoryCheck.DIBBER));
		assertFalse(missing.contains(InventoryCheck.CAN));
		assertTrue(status.hasFertiliser());
		assertEquals(12, status.getEnergyPercent());
	}

	@Test
	public void resetForgetsTheAdaptedRoute()
	{
		run.snapshot();
		plant(new int[]{1811, 3489});
		run.reset();
		tracker.clear();
		for (int[] tile : TitheRoutesTest.farm())
			spawn(tile, EMPTY);

		RunSnapshot snapshot = run.snapshot();
		int[] first = tileOf(snapshot.getRoute().get(0));
		assertEquals(TitheRoutes.BASIC_20.get(0)[0], first[0]);
	}

	/** Fills the backpack: the usual tools and seeds, the given item, then spades up to 28 slots. */
	private void fullBackpackWith(Item item)
	{
		Item[] items = new Item[TitheFarmIds.INVENTORY_SIZE];
		items[0] = new Item(13353, 1);
		items[1] = new Item(13425, 20);
		items[2] = item;
		for (int i = 3; i < items.length; i++)
			items[i] = new Item(TitheFarmIds.SPADE, 1);

		inventory(items);
	}

	@Test
	public void fullBackpackWithNoFruitAsksToFreeASlot()
	{
		fullBackpackWith(new Item(TitheFarmIds.SPADE, 1));
		spawn(new int[]{1811, 3489}, GROWN_C);
		RunSnapshot snapshot = run.snapshot();
		assertEquals(NextAction.FREE_SLOT, snapshot.getAdvice().getAction());
	}

	@Test
	public void fullBackpackHarvestsIntoTheMatchingStack()
	{
		fullBackpackWith(new Item(TitheFarmIds.FRUIT_LOGAVANO, 12));
		spawn(new int[]{1811, 3489}, GROWN_C);
		RunSnapshot snapshot = run.snapshot();
		assertEquals(NextAction.HARVEST, snapshot.getAdvice().getAction());
	}

	@Test
	public void fullBackpackWithAnotherTiersStackDepositsFirst()
	{
		fullBackpackWith(new Item(TitheFarmIds.FRUIT_GOLOVANOVA, 12));
		spawn(new int[]{1811, 3489}, GROWN_C);
		RunSnapshot snapshot = run.snapshot();
		assertEquals(NextAction.DEPOSIT_FRUIT, snapshot.getAdvice().getAction());
	}
}
