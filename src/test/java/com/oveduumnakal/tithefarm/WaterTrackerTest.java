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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies the water-charge arithmetic across regular cans and Gricoller's can, and the run's water need. */
public class WaterTrackerTest
{
	private static final int EMPTY = 27383;
	private static final int STAGE1_WET = 27385;
	private static final int DEAD = 27386;
	private static final int STAGE3_DRY = 27390;
	private static final int GROWN = 27393;

	private static List<PlotInfo> run(int wateredFirstStage, int empty)
	{
		List<PlotInfo> plots = new ArrayList<>();
		for (int i = 0; i < wateredFirstStage; i++)
			plots.add(PlotInfo.of(STAGE1_WET, 0));

		for (int i = 0; i < empty; i++)
			plots.add(PlotInfo.of(EMPTY, 0));

		return plots;
	}

	@Test
	public void sumsRegularCans()
	{
		int[] inventory = {5340, 5340, 5333, -1, 995};
		assertEquals(17, WaterTracker.totalCharges(inventory, 0));
	}

	@Test
	public void addsGricollerWhenCanPresent()
	{
		int[] inventory = {13353, 5331};
		assertEquals(250, WaterTracker.totalCharges(inventory, 250));
	}

	@Test
	public void ignoresGricollerVarbitWhenCanAbsent()
	{
		int[] inventory = {5340};
		assertEquals(8, WaterTracker.totalCharges(inventory, 999));
	}

	@Test
	public void mixesGricollerAndRegularCans()
	{
		int[] inventory = {13353, 5340, 5336};
		assertEquals(100 + 8 + 4, WaterTracker.totalCharges(inventory, 100));
	}

	@Test
	public void clampsNegativeGricollerCharge()
	{
		int[] inventory = {13353};
		assertEquals(0, WaterTracker.totalCharges(inventory, -1));
	}

	@Test
	public void freshRunNeedsThreePerSeededPlot()
	{
		assertEquals(60, WaterTracker.runNeed(run(0, 20), 20));
	}

	@Test
	public void runNeedIgnoresSeedsCarried()
	{
		assertEquals(60, WaterTracker.runNeed(run(0, 20), 20));
		assertEquals(0, WaterTracker.runNeed(run(0, 20), 0));
	}

	@Test
	public void runNeedDropsByOneWithEveryWatering()
	{
		assertEquals(5 * 2 + 15 * 3, WaterTracker.runNeed(run(5, 15), 15));
	}

	@Test
	public void runNeedCountsDeadPlotsAsReplantable()
	{
		List<PlotInfo> plots = new ArrayList<>();
		plots.add(PlotInfo.of(DEAD, 0));
		plots.add(PlotInfo.of(STAGE3_DRY, 0));
		plots.add(PlotInfo.of(GROWN, 0));
		assertEquals(3 + 1, WaterTracker.runNeed(plots, 20));
	}

	@Test
	public void eightCansCoverAFullTwentyRunMidway()
	{
		int water = 64 - 5;
		assertTrue(WaterTracker.canAffordPlant(run(5, 15), water, 15));
		assertFalse(water < WaterTracker.runNeed(run(5, 15), 15));
	}

	@Test
	public void blocksTheFirstSeedUnlessTheWholeRunIsCovered()
	{
		assertTrue(WaterTracker.canAffordPlant(run(0, 20), 60, 20));
		assertFalse(WaterTracker.canAffordPlant(run(0, 20), 59, 20));
	}

	@Test
	public void blocksPlantingMidRunWhenTheRestOfTheRunIsShort()
	{
		assertTrue(WaterTracker.canAffordPlant(run(5, 15), 5 * 2 + 15 * 3, 15));
		assertFalse(WaterTracker.canAffordPlant(run(5, 15), 5 * 2 + 15 * 3 - 1, 15));
	}

	@Test
	public void anExtraSeedStillNeedsItsOwnWater()
	{
		assertTrue(WaterTracker.canAffordPlant(run(5, 15), 13, 0));
		assertFalse(WaterTracker.canAffordPlant(run(5, 15), 12, 0));
	}

	@Test
	public void runNeedIsCappedByFreeCropSlots()
	{
		assertEquals(5 * 2 + 3 * 3, WaterTracker.runNeed(run(5, 15), 3));
		assertEquals(5 * 2, WaterTracker.runNeed(run(5, 15), 0));
	}

	@Test
	public void aCanUnderCapacityNeedsFilling()
	{
		assertTrue(WaterTracker.needsFill(TitheFarmIds.WATERING_CAN_EMPTY, 0));
		assertTrue(WaterTracker.needsFill(TitheFarmIds.WATERING_CAN_FULL - 1, 0));
		assertFalse(WaterTracker.needsFill(TitheFarmIds.WATERING_CAN_FULL, 0));
		assertTrue(WaterTracker.needsFill(TitheFarmIds.GRICOLLER_CAN, 999));
		assertFalse(WaterTracker.needsFill(TitheFarmIds.GRICOLLER_CAN, 1000));
		assertFalse(WaterTracker.needsFill(TitheFarmIds.SEED_GOLOVANOVA, 0));
	}

	@Test
	public void cansAreFullOnlyWhenEveryCanIsFull()
	{
		int full = TitheFarmIds.WATERING_CAN_FULL;
		assertTrue(WaterTracker.cansFull(new int[]{full, full, -1}, 0));
		assertFalse(WaterTracker.cansFull(new int[]{full, TitheFarmIds.WATERING_CAN_EMPTY}, 0));
		assertTrue(WaterTracker.cansFull(new int[]{-1, -1}, 0));
	}
}
