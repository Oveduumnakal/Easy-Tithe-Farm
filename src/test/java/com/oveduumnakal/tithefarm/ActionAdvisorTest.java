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
	private static final int EMPTY = 27383;
	private static final int FRESH = 27384;
	private static final int STAGE1_WET = 27385;
	private static final int DEAD = 27386;
	private static final int STAGE2_DRY = 27387;
	private static final int STAGE2_WET = 27388;
	private static final int GROWN = 27393;

	private static PlotInfo plot(int id)
	{
		return PlotInfo.of(id, 0);
	}

	private static PlotInfo aged(int id, int ticks)
	{
		return PlotInfo.of(id, ticks);
	}

	private static List<PlotInfo> plots(PlotInfo... values)
	{
		return Arrays.asList(values);
	}

	private static ActionAdvisor.Advice decide(List<PlotInfo> plots, int seeds, int water)
	{
		return decide(plots, seeds, water, 20);
	}

	private static ActionAdvisor.Advice decide(List<PlotInfo> plots, int seeds, int water, int cropCount)
	{
		return ActionAdvisor.decide(plots, seeds, water, false, false, ActionAdvisor.plantSlots(plots, cropCount),
			false);
	}

	private static void assertAdvice(NextAction action, int index, ActionAdvisor.Advice advice)
	{
		assertEquals(action, advice.getAction());
		assertEquals(index, advice.getPlotIndex());
	}

	@Test
	public void plantsIntoEmptyWhenHoldingSeeds()
	{
		assertAdvice(NextAction.PLANT_SEED, 0, decide(plots(plot(EMPTY), plot(EMPTY)), 20, 60));
	}

	@Test
	public void watersAFreshSeedBeforePlantingTheNext()
	{
		assertAdvice(NextAction.WATER_PLANT, 0, decide(plots(plot(FRESH), plot(EMPTY)), 19, 60));
	}

	@Test
	public void keepsPlantingWhenAnOldPlantJustAgedIntoItsNextStage()
	{
		List<PlotInfo> run = plots(aged(STAGE2_DRY, 5), plot(STAGE1_WET), plot(EMPTY));
		assertAdvice(NextAction.PLANT_SEED, 2, decide(run, 18, 60));
	}

	@Test
	public void leavesThePassForAPlantNearTheEndOfItsStage()
	{
		List<PlotInfo> run = plots(aged(STAGE2_DRY, ActionAdvisor.URGENT_TICKS), plot(STAGE1_WET), plot(EMPTY));
		assertAdvice(NextAction.WATER_PLANT, 0, decide(run, 18, 60));
	}

	@Test
	public void watersTheLongestWaitingPlantRatherThanTheStartOfTheRoute()
	{
		List<PlotInfo> run = plots(aged(STAGE2_DRY, 0), aged(STAGE2_DRY, 30), aged(STAGE2_DRY, 22));
		assertAdvice(NextAction.WATER_PLANT, 1, decide(run, 0, 60));
	}

	@Test
	public void watersInRouteOrderWhenPlantsHaveWaitedEqually()
	{
		List<PlotInfo> run = plots(plot(STAGE2_WET), plot(STAGE2_DRY), plot(STAGE2_DRY));
		assertAdvice(NextAction.WATER_PLANT, 1, decide(run, 0, 5));
	}

	@Test
	public void refillsWhenAPlantNeedsWaterAndNoneIsLeft()
	{
		assertAdvice(NextAction.REFILL_WATER, -1, decide(plots(plot(STAGE2_DRY)), 0, 0));
	}

	@Test
	public void refillsInsteadOfPlantingASeedItCannotWater()
	{
		List<PlotInfo> run = plots(plot(STAGE2_WET), plot(EMPTY));
		assertAdvice(NextAction.REFILL_WATER, -1, decide(run, 5, 3));
	}

	@Test
	public void clearsDeadPlantWhenItIsNextInTheRoute()
	{
		assertAdvice(NextAction.CLEAR_DEAD, 0, decide(plots(plot(DEAD), plot(EMPTY)), 5, 60));
		assertAdvice(NextAction.PLANT_SEED, 0, decide(plots(plot(EMPTY), plot(DEAD)), 5, 60));
	}

	@Test
	public void ignoresDeadPlantWithoutSeedsToReplant()
	{
		assertAdvice(NextAction.WAIT, -1, decide(plots(plot(DEAD), plot(STAGE1_WET)), 0, 60));
	}

	@Test
	public void harvestsGrownWhenNothingNeedsWater()
	{
		assertAdvice(NextAction.HARVEST, 1, decide(plots(plot(STAGE2_WET), plot(GROWN)), 0, 3));
	}

	@Test
	public void harvestsEveryGrownPlantBeforeReplanting()
	{
		assertAdvice(NextAction.HARVEST, 1, decide(plots(plot(EMPTY), plot(GROWN)), 20, 60));
		assertAdvice(NextAction.PLANT_SEED, 0, decide(plots(plot(EMPTY), plot(EMPTY)), 20, 60));
	}

	@Test
	public void refillsBeforeTheFirstSeedUnlessTheWholeRunIsCovered()
	{
		PlotInfo[] empty = new PlotInfo[20];
		Arrays.fill(empty, plot(EMPTY));
		assertAdvice(NextAction.REFILL_WATER, -1, decide(plots(empty), 20, 59));
		assertAdvice(NextAction.PLANT_SEED, 0, decide(plots(empty), 20, 60));
	}

	@Test
	public void depositsWhenBackpackIsFullAndAPlantIsGrown()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(GROWN)), 0, 60, true, true, 19, false);
		assertAdvice(NextAction.DEPOSIT_FRUIT, -1, advice);
	}

	@Test
	public void waitsInsteadOfDepositingMidRun()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(STAGE1_WET)), 0, 60, true, false, 19, false);
		assertAdvice(NextAction.WAIT, -1, advice);
	}

	@Test
	public void depositsBetweenRunsBeforeTheFirstSeed()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(EMPTY), plot(DEAD)), 20, 60, true, false, 20,
			false);
		assertAdvice(NextAction.DEPOSIT_FRUIT, -1, advice);
	}

	@Test
	public void collectsSeedsWhenEmptyPlotButNoSeeds()
	{
		assertAdvice(NextAction.GET_SEEDS, -1, decide(plots(plot(EMPTY)), 0, 60));
	}

	@Test
	public void waitsWhenAllWateredAndGrowing()
	{
		assertAdvice(NextAction.WAIT, -1, decide(plots(plot(STAGE1_WET), plot(STAGE2_WET)), 0, 3));
	}

	@Test
	public void watersAPlantOffTheRouteListedAfterIt()
	{
		List<PlotInfo> run = plots(plot(EMPTY), plot(STAGE1_WET), plot(STAGE2_DRY));
		assertAdvice(NextAction.WATER_PLANT, 2, decide(run, 0, 60, 2));
	}

	@Test
	public void stopsSuggestingSeedsOnceTheCropCountIsInTheGround()
	{
		List<PlotInfo> run = plots(plot(EMPTY), plot(STAGE1_WET), plot(STAGE2_WET));
		assertAdvice(NextAction.WAIT, -1, decide(run, 18, 60, 2));
		assertAdvice(NextAction.PLANT_SEED, 0, decide(run, 18, 60, 3));
	}

	@Test
	public void plantSlotsCountPlantsAnywhereButNotDeadOnes()
	{
		List<PlotInfo> run = plots(plot(EMPTY), plot(DEAD), plot(STAGE1_WET), plot(GROWN));
		assertEquals(18, ActionAdvisor.plantSlots(run, 20));
		assertEquals(0, ActionAdvisor.plantSlots(run, 1));
	}

	@Test
	public void lastRunNeverSendsYouForSeeds()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(EMPTY), plot(STAGE1_WET)), 0, 60, false, false,
			5, true);
		assertAdvice(NextAction.WAIT, -1, advice);
	}

	@Test
	public void lastRunSaysLeaveOnceEverythingIsInTheSack()
	{
		assertAdvice(NextAction.LEAVE, -1, ActionAdvisor.decide(plots(plot(EMPTY), plot(DEAD)), 5, 60, false, false,
			0, true));
		assertAdvice(NextAction.DEPOSIT_FRUIT, -1, ActionAdvisor.decide(plots(plot(EMPTY)), 0, 60, true, false,
			0, true));
	}

	@Test
	public void lastRunPlantsJustEnoughToReachTheNextHundred()
	{
		assertEquals(15, ActionAdvisor.wrapUpSeeds(80, 0, 5, 20));
		assertEquals(0, ActionAdvisor.wrapUpSeeds(40, 0, 20, 20));
		assertEquals(0, ActionAdvisor.wrapUpSeeds(100, 0, 0, 20));
		assertEquals(20, ActionAdvisor.wrapUpSeeds(60, 10, 10, 20));
	}

	@Test
	public void betweenRunsACanNotFullHoldsTheFirstSeed()
	{
		ActionAdvisor.Advice plant = new ActionAdvisor.Advice(NextAction.PLANT_SEED, 0);
		assertAdvice(NextAction.REFILL_WATER, -1, ActionAdvisor.topUpFirst(plant, true, false));
		assertAdvice(NextAction.PLANT_SEED, 0, ActionAdvisor.topUpFirst(plant, true, true));
		assertAdvice(NextAction.PLANT_SEED, 0, ActionAdvisor.topUpFirst(plant, false, false));
		ActionAdvisor.Advice seeds = new ActionAdvisor.Advice(NextAction.GET_SEEDS, -1);
		assertAdvice(NextAction.GET_SEEDS, -1, ActionAdvisor.topUpFirst(seeds, true, false));
	}
}
