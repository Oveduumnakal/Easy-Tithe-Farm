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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Verifies the next-action priority ordering. */
public class ActionAdvisorTest
{
	private static List<TithePlotState> states(TithePlotState... values)
	{
		return Arrays.asList(values);
	}

	@Test
	public void plantsIntoEmptyWhenHoldingSeeds()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(
			states(TithePlotState.EMPTY, TithePlotState.EMPTY), true, 60);
		assertEquals(NextAction.PLANT_SEED, advice.getAction());
		assertEquals(0, advice.getPlotIndex());
	}

	@Test
	public void collectsSeedsWhenEmptyPlotButNoSeeds()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(states(TithePlotState.EMPTY), false, 60);
		assertEquals(NextAction.GET_SEEDS, advice.getAction());
		assertEquals(-1, advice.getPlotIndex());
	}

	@Test
	public void watersFirstUnwateredWhenWaterAvailable()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(
			states(TithePlotState.WATERED, TithePlotState.UNWATERED, TithePlotState.UNWATERED), true, 5);
		assertEquals(NextAction.WATER_PLANT, advice.getAction());
		assertEquals(1, advice.getPlotIndex());
	}

	@Test
	public void refillsWhenUnwateredButNoWater()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(states(TithePlotState.UNWATERED), true, 0);
		assertEquals(NextAction.REFILL_WATER, advice.getAction());
		assertEquals(-1, advice.getPlotIndex());
	}

	@Test
	public void wateringOutranksHarvest()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(
			states(TithePlotState.GROWN, TithePlotState.UNWATERED), true, 3);
		assertEquals(NextAction.WATER_PLANT, advice.getAction());
		assertEquals(1, advice.getPlotIndex());
	}

	@Test
	public void harvestsGrownWhenNothingNeedsWater()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(
			states(TithePlotState.WATERED, TithePlotState.GROWN), true, 3);
		assertEquals(NextAction.HARVEST, advice.getAction());
		assertEquals(1, advice.getPlotIndex());
	}

	@Test
	public void plantsRemainingEmptyOverWaiting()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(
			states(TithePlotState.WATERED, TithePlotState.EMPTY), true, 3);
		assertEquals(NextAction.PLANT_SEED, advice.getAction());
		assertEquals(1, advice.getPlotIndex());
	}

	@Test
	public void waitsWhenAllWateredAndGrowing()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(
			states(TithePlotState.WATERED, TithePlotState.WATERED), true, 3);
		assertEquals(NextAction.WAIT, advice.getAction());
		assertEquals(-1, advice.getPlotIndex());
	}
}
