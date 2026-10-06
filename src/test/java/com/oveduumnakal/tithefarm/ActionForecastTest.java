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
import static org.junit.Assert.assertTrue;

/** Plays runs forward and checks the predicted trail of plot actions. */
public class ActionForecastTest
{
	private static final int EMPTY = 27383;
	private static final int FRESH = 27384;
	private static final int STAGE1_WET = 27385;
	private static final int DEAD = 27386;
	private static final int STAGE3_DRY = 27390;
	private static final int STAGE3_WET = 27391;
	private static final int GROWN = 27393;

	/** Twenty plots: the given id on route numbers {@code from..to} (1-based), the rest empty. */
	private static List<PlotInfo> run(int from, int to, int id)
	{
		List<PlotInfo> plots = new ArrayList<>();
		for (int i = 1; i <= 20; i++)
			plots.add(PlotInfo.of(i >= from && i <= to ? id : EMPTY, 10));

		return plots;
	}

	private static void assertTrail(List<ActionAdvisor.Advice> trail, Object... expected)
	{
		assertEquals("trail length", expected.length / 2, trail.size());
		for (int i = 0; i < trail.size(); i++)
		{
			assertEquals("action " + i, expected[i * 2], trail.get(i).getAction());
			assertEquals("plot " + i, (int) expected[i * 2 + 1] - 1, trail.get(i).getPlotIndex());
		}
	}

	@Test
	public void plantingShowsTheNextFivePlots()
	{
		List<PlotInfo> plots = run(1, 6, STAGE1_WET);
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 14, 60, Backpack.ROOMY, 14, 5);
		assertTrail(trail,
			NextAction.PLANT_SEED, 7, NextAction.PLANT_SEED, 8, NextAction.PLANT_SEED, 9,
			NextAction.PLANT_SEED, 10, NextAction.PLANT_SEED, 11);
	}

	@Test
	public void lastSeedLeadsIntoTheSecondWateringPass()
	{
		List<PlotInfo> plots = run(1, 19, STAGE1_WET);
		for (int i = 0; i < 19; i++)
			plots.set(i, PlotInfo.of(STAGE1_WET, 10, 95 - i * 5));

		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 1, 60, Backpack.ROOMY, 1, 5);
		assertTrail(trail,
			NextAction.PLANT_SEED, 20, NextAction.WATER_PLANT, 1, NextAction.WATER_PLANT, 2,
			NextAction.WATER_PLANT, 3, NextAction.WATER_PLANT, 4);
	}

	@Test
	public void lastWaterLeadsIntoHarvest()
	{
		List<PlotInfo> plots = run(1, 19, STAGE3_WET);
		for (int i = 0; i < 19; i++)
			plots.set(i, PlotInfo.of(STAGE3_WET, 10, 97 - i * 3));

		plots.set(19, PlotInfo.of(STAGE3_DRY, 40));
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 0, 10, Backpack.ROOMY, 0, 5);
		assertTrail(trail,
			NextAction.WATER_PLANT, 20, NextAction.HARVEST, 1, NextAction.HARVEST, 2,
			NextAction.HARVEST, 3, NextAction.HARVEST, 4);
	}

	@Test
	public void deadPlantIsClearedThenPlantingMovesOn()
	{
		List<PlotInfo> plots = run(1, 13, STAGE1_WET);
		plots.set(8, PlotInfo.of(DEAD, 10));
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 8, 60, Backpack.ROOMY, 8, 5);
		assertTrail(trail,
			NextAction.CLEAR_DEAD, 9, NextAction.PLANT_SEED, 14, NextAction.PLANT_SEED, 15,
			NextAction.PLANT_SEED, 16, NextAction.PLANT_SEED, 17);
	}

	@Test
	public void eachPlotAppearsOnce()
	{
		List<PlotInfo> plots = run(1, 1, FRESH);
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 5, 60, Backpack.ROOMY, 5, 3);
		assertTrail(trail, NextAction.WATER_PLANT, 1, NextAction.PLANT_SEED, 2, NextAction.PLANT_SEED, 3);
	}

	@Test
	public void lengthOneIsJustTheCurrentAdvice()
	{
		List<PlotInfo> plots = run(1, 6, STAGE1_WET);
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 14, 60, Backpack.ROOMY, 14, 1);
		assertTrail(trail, NextAction.PLANT_SEED, 7);
	}

	@Test
	public void nonPlotAdviceGivesNoTrail()
	{
		List<PlotInfo> plots = run(1, 1, FRESH);
		assertTrue(ActionForecast.forecast(plots, 5, 0, Backpack.ROOMY, 5, 5).isEmpty());
	}

	@Test
	public void stopsWhenSeedsRunOut()
	{
		List<PlotInfo> plots = run(1, 6, STAGE1_WET);
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 2, 60, Backpack.ROOMY, 14, 5);
		assertTrail(trail, NextAction.PLANT_SEED, 7, NextAction.PLANT_SEED, 8);
	}

	@Test
	public void wateringKeepsTheStageClock()
	{
		List<PlotInfo> plots = new ArrayList<>();
		plots.add(PlotInfo.of(STAGE1_WET, 5, 98));
		ActionForecast.age(plots, 3);
		assertEquals(TithePlotState.UNWATERED, plots.get(0).getState());
		assertEquals(2, plots.get(0).getStage());
		assertEquals(1, plots.get(0).getStageAgeTicks());
	}

	@Test
	public void agingCarriesThePlantingAge()
	{
		List<PlotInfo> plots = new ArrayList<>();
		plots.add(PlotInfo.of(STAGE1_WET, 5, 98, 20));
		plots.add(PlotInfo.of(STAGE1_WET, 5, 50));
		ActionForecast.age(plots, 3);
		assertEquals(2, plots.get(0).getStage());
		assertEquals(23, plots.get(0).getPlantAgeTicks());
		assertEquals(PlotInfo.AGE_UNKNOWN, plots.get(1).getPlantAgeTicks());
	}

	@Test
	public void wateringPassFollowsPlantingOrder()
	{
		List<PlotInfo> plots = new ArrayList<>();
		plots.add(PlotInfo.of(STAGE3_DRY, 10, 10, 200));
		plots.add(PlotInfo.of(STAGE3_DRY, 10, 10, 300));
		plots.add(PlotInfo.of(STAGE3_DRY, 10, 10, 250));
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 0, 60, Backpack.ROOMY, 0, 5);
		assertTrail(trail, NextAction.WATER_PLANT, 2, NextAction.WATER_PLANT, 3, NextAction.WATER_PLANT, 1);
	}

	@Test
	public void shortOfWaterWatersWaitingPlantsUntilTheRefill()
	{
		List<PlotInfo> plots = run(3, 20, EMPTY);
		plots.set(0, PlotInfo.of(STAGE3_DRY, 10, 10, 200));
		plots.set(1, PlotInfo.of(STAGE3_DRY, 10, 10, 150));
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 18, 2, Backpack.ROOMY, 18, 5);
		assertTrail(trail, NextAction.WATER_PLANT, 1, NextAction.WATER_PLANT, 2);
	}

	@Test
	public void harvestFollowsPlantingOrder()
	{
		List<PlotInfo> plots = new ArrayList<>();
		plots.add(PlotInfo.of(GROWN, 10, 10, 300));
		plots.add(PlotInfo.of(GROWN, 10, 10, 400));
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 0, 60, Backpack.ROOMY, 0, 5);
		assertTrail(trail, NextAction.HARVEST, 2, NextAction.HARVEST, 1);
	}

	@Test
	public void depositsToMakeRoomAndKeepsHarvesting()
	{
		List<PlotInfo> plots = new ArrayList<>();
		plots.add(PlotInfo.of(GROWN, 10));
		plots.add(PlotInfo.of(GROWN, 10));
		Backpack oneSlotLeft = new Backpack(1, 1, 1, 0, false);
		List<ActionAdvisor.Advice> trail = ActionForecast.forecast(plots, 0, 60, oneSlotLeft, 0, 5);
		assertTrail(trail, NextAction.HARVEST, 1, NextAction.HARVEST, 2);
	}

	@Test
	public void noTrailWhileASlotMustBeFreed()
	{
		List<PlotInfo> plots = new ArrayList<>();
		plots.add(PlotInfo.of(GROWN, 10));
		Backpack full = new Backpack(0, 0, 0, 0, true);
		assertTrue(ActionForecast.forecast(plots, 0, 60, full, 0, 5).isEmpty());
	}
}
