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

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/** Drives the plot tracker with mocked objects: the two plot clocks, planting ticks, and despawns by hash. */
public class TithePlotTrackerTest
{
	private static final int EMPTY = TitheFarmIds.PLOT_EMPTY;
	private static final int STAGE1_DRY = TitheFarmIds.PLOT_GROWTH_FIRST;
	private static final int STAGE1_WET = STAGE1_DRY + 1;
	private static final int STAGE1_DEAD = STAGE1_DRY + 2;
	private static final int STAGE2_DRY = STAGE1_DRY + 3;
	private static final int STAGE2_WET = STAGE1_DRY + 4;
	private static final WorldPoint TILE = new WorldPoint(1811, 3489, 0);

	private TithePlotTracker tracker;
	private long hash;

	@Before
	public void setUp()
	{
		tracker = new TithePlotTracker();
	}

	/** A mocked object with the given id on the given tile, with a fresh hash. */
	private GameObject object(int id, WorldPoint tile)
	{
		GameObject object = TestRuns.plot(id, ++hash);
		when(object.getWorldLocation()).thenReturn(tile);
		return object;
	}

	/** Spawns a fresh object with the given id on the test plot's tile at the given tick. */
	private int change(int id, int tick)
	{
		return tracker.onSpawnOrChanged(object(id, TILE), tick);
	}

	private PlotInfo info(int tick)
	{
		GameObject plot = tracker.getPlotsByTile().get(TILE);
		return tracker.infoOf(TILE, plot, tick);
	}

	@Test
	public void firstSightingStartsBothClocks()
	{
		assertEquals(-1, change(STAGE1_DRY, 10));
		PlotInfo info = info(15);
		assertEquals(5, info.getAgeTicks());
		assertEquals(5, info.getStageAgeTicks());
	}

	@Test
	public void wateringRestartsTheIdClockButKeepsTheStageClock()
	{
		change(EMPTY, 0);
		change(STAGE1_DRY, 10);
		assertEquals(STAGE1_DRY, change(STAGE1_WET, 25));
		PlotInfo info = info(40);
		assertEquals(15, info.getAgeTicks());
		assertEquals(30, info.getStageAgeTicks());
	}

	@Test
	public void growingIntoTheNextStageRestartsBothClocks()
	{
		change(STAGE1_DRY, 10);
		change(STAGE1_WET, 20);
		change(STAGE2_DRY, 110);
		PlotInfo info = info(115);
		assertEquals(5, info.getAgeTicks());
		assertEquals(5, info.getStageAgeTicks());
	}

	@Test
	public void wateringAtTheNextStageKeepsThatStagesClock()
	{
		change(STAGE1_DRY, 10);
		change(STAGE1_WET, 20);
		change(STAGE2_DRY, 110);
		change(STAGE2_WET, 130);
		PlotInfo info = info(140);
		assertEquals(10, info.getAgeTicks());
		assertEquals(30, info.getStageAgeTicks());
	}

	@Test
	public void dyingRestartsTheStageClock()
	{
		change(STAGE1_DRY, 10);
		change(STAGE1_DEAD, 110);
		PlotInfo info = info(112);
		assertEquals(2, info.getAgeTicks());
		assertEquals(2, info.getStageAgeTicks());
	}

	@Test
	public void theSameIdAgainKeepsBothClocks()
	{
		change(STAGE1_DRY, 10);
		assertEquals(STAGE1_DRY, change(STAGE1_DRY, 30));
		PlotInfo info = info(40);
		assertEquals(30, info.getAgeTicks());
		assertEquals(30, info.getStageAgeTicks());
	}

	@Test
	public void plantingAnEmptyPlotRecordsThePlantingTick()
	{
		change(EMPTY, 0);
		assertEquals(Integer.MAX_VALUE, tracker.plantedTick(TILE));
		assertEquals(EMPTY, change(STAGE1_DRY, 12));
		assertEquals(12, tracker.plantedTick(TILE));
		change(STAGE1_WET, 20);
		change(STAGE2_DRY, 112);
		assertEquals(12, tracker.plantedTick(TILE));
	}

	@Test
	public void aPlantFirstSeenGrowingHasNoPlantingTick()
	{
		change(STAGE1_DRY, 12);
		assertEquals(Integer.MAX_VALUE, tracker.plantedTick(TILE));
	}

	@Test
	public void anEmptiedPlotForgetsItsPlantingTick()
	{
		change(EMPTY, 0);
		change(STAGE1_DRY, 12);
		change(EMPTY, 400);
		assertEquals(Integer.MAX_VALUE, tracker.plantedTick(TILE));
	}

	@Test
	public void despawnDropsThePlotOnlyWhenItIsStillTheTrackedObject()
	{
		GameObject before = object(EMPTY, TILE);
		tracker.onSpawnOrChanged(before, 0);
		GameObject after = object(STAGE1_DRY, TILE);
		tracker.onSpawnOrChanged(after, 1);
		tracker.onDespawn(before);
		assertSame(after, tracker.getPlotsByTile().get(TILE));
		assertTrue(tracker.inTitheFarm());
		tracker.onDespawn(after);
		assertFalse(tracker.inTitheFarm());
	}

	@Test
	public void barrelsSacksAndTheSeedTableAreTrackedAndDroppedByHash()
	{
		GameObject barrel = object(TitheFarmIds.FARM_WATER_BARREL_A, new WorldPoint(1, 1, 0));
		GameObject sack = object(TitheFarmIds.SACK_OF_FRUIT, new WorldPoint(2, 2, 0));
		GameObject table = object(TitheFarmIds.SEED_TABLE, new WorldPoint(3, 3, 0));
		assertEquals(-1, tracker.onSpawnOrChanged(barrel, 0));
		tracker.onSpawnOrChanged(sack, 0);
		tracker.onSpawnOrChanged(table, 0);
		assertEquals(1, tracker.getWaterBarrels().size());
		assertEquals(1, tracker.getSacks().size());
		assertSame(table, tracker.getSeedTable());
		assertFalse(tracker.inTitheFarm());

		tracker.onDespawn(object(TitheFarmIds.SEED_TABLE, new WorldPoint(3, 3, 0)));
		assertSame(table, tracker.getSeedTable());
		tracker.onDespawn(barrel);
		tracker.onDespawn(sack);
		tracker.onDespawn(table);
		assertTrue(tracker.getWaterBarrels().isEmpty());
		assertTrue(tracker.getSacks().isEmpty());
		assertNull(tracker.getSeedTable());
	}

	@Test
	public void clearForgetsEverything()
	{
		change(EMPTY, 0);
		change(STAGE1_DRY, 12);
		tracker.onSpawnOrChanged(object(TitheFarmIds.SACK_OF_FRUIT, new WorldPoint(2, 2, 0)), 0);
		tracker.clear();
		assertFalse(tracker.inTitheFarm());
		assertTrue(tracker.getSacks().isEmpty());
		assertEquals(Integer.MAX_VALUE, tracker.plantedTick(TILE));
		assertEquals(-1, change(STAGE1_DRY, 20));
	}

	@Test
	public void ignoresNullsAndOtherObjects()
	{
		assertEquals(-1, tracker.onSpawnOrChanged(null, 0));
		assertEquals(-1, tracker.onSpawnOrChanged(object(1, TILE), 0));
		tracker.onDespawn(null);
		assertFalse(tracker.inTitheFarm());
		assertTrue(tracker.getWaterBarrels().isEmpty());
	}
}
