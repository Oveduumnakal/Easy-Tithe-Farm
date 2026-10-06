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
	private static final int GROWN_B = 27404;

	/** Room to spare and a small haul of fruit. */
	private static final Backpack WITH_FRUIT = new Backpack(27, 1, 12, 1, true);

	/** Room to spare and a hundred fruit, a full sack's worth. */
	private static final Backpack HUNDRED_FRUIT = new Backpack(27, 1, 100, 1, true);

	/** No free slot, with a Golovanova (tier A) fruit stack. */
	private static final Backpack FULL_WITH_A_STACK = new Backpack(0, 1, 12, 1, true);

	/** No free slot, with a Bologano (tier B) fruit stack. */
	private static final Backpack FULL_WITH_B_STACK = new Backpack(0, 1, 12, 1 << 1, true);

	/** No free slot and no fruit. */
	private static final Backpack FULL_NO_FRUIT = new Backpack(0, 0, 0, 0, true);

	private static PlotInfo plot(int id)
	{
		return PlotInfo.of(id, 0);
	}

	private static PlotInfo aged(int id, int ticks)
	{
		return PlotInfo.of(id, ticks);
	}

	/** A plot whose plant went in {@code plantedAgo} ticks ago and has sat {@code ticks} in its current state. */
	private static PlotInfo planted(int id, int ticks, int plantedAgo)
	{
		return PlotInfo.of(id, ticks, ticks, plantedAgo);
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
		return ActionAdvisor.decide(plots, seeds, water, Backpack.ROOMY, ActionAdvisor.plantSlots(plots, cropCount));
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
	public void watersInPlantingOrderRatherThanTimeWaiting()
	{
		List<PlotInfo> run = plots(planted(STAGE2_DRY, 30, 200), planted(STAGE2_DRY, 10, 260),
			planted(STAGE2_DRY, 20, 230));
		assertAdvice(NextAction.WATER_PLANT, 1, decide(run, 0, 60));
	}

	@Test
	public void harvestsInPlantingOrder()
	{
		List<PlotInfo> run = plots(planted(GROWN, 0, 300), planted(GROWN, 0, 340), planted(GROWN, 0, 320));
		assertAdvice(NextAction.HARVEST, 1, decide(run, 0, 60));
	}

	@Test
	public void urgentWateringJumpsTheQueueInPlantingOrder()
	{
		List<PlotInfo> run = plots(planted(STAGE2_DRY, ActionAdvisor.URGENT_TICKS + 10, 200),
			planted(STAGE2_DRY, ActionAdvisor.URGENT_TICKS, 250), planted(STAGE2_DRY, 10, 400), plot(EMPTY));
		assertAdvice(NextAction.WATER_PLANT, 1, decide(run, 18, 60));
	}

	@Test
	public void plantsSeededOnTheSameTickFallBackToTimeWaitingThenRoute()
	{
		List<PlotInfo> run = plots(planted(STAGE2_DRY, 5, 100), planted(STAGE2_DRY, 20, 100),
			planted(STAGE2_DRY, 20, 100));
		assertAdvice(NextAction.WATER_PLANT, 1, decide(run, 0, 60));
	}

	@Test
	public void aPlantNotSeenGoingInCountsAsPlantedFirst()
	{
		List<PlotInfo> run = plots(planted(STAGE2_DRY, 40, 300), aged(STAGE2_DRY, 5));
		assertAdvice(NextAction.WATER_PLANT, 1, decide(run, 0, 60));
	}

	@Test
	public void refillsWhenAPlantNeedsWaterAndNoneIsLeft()
	{
		assertAdvice(NextAction.REFILL_WATER, -1, decide(plots(plot(STAGE2_DRY)), 0, 0));
	}

	@Test
	public void spendsCarriedWaterOnWaitingPlantsBeforeRefilling()
	{
		List<PlotInfo> run = plots(planted(STAGE2_DRY, 5, 120), planted(STAGE2_DRY, 3, 110),
			planted(STAGE2_DRY, 1, 100), plot(EMPTY), plot(EMPTY));
		assertAdvice(NextAction.WATER_PLANT, 0, decide(run, 2, 5));
		assertAdvice(NextAction.REFILL_WATER, -1, decide(run, 2, 0));
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
		Backpack full = new Backpack(0, 1, 1, 0, false);
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(GROWN)), 0, 60, full, 19);
		assertAdvice(NextAction.DEPOSIT_FRUIT, -1, advice);
	}

	@Test
	public void harvestsIntoItsOwnStackWhenTheBackpackIsFull()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(GROWN)), 0, 60, FULL_WITH_A_STACK, 19);
		assertAdvice(NextAction.HARVEST, 0, advice);
	}

	@Test
	public void aStackOfAnotherTierLeavesNoRoom()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(GROWN)), 0, 60, FULL_WITH_B_STACK, 19);
		assertAdvice(NextAction.DEPOSIT_FRUIT, -1, advice);
	}

	@Test
	public void harvestsTheGrownPlantWhoseFruitFits()
	{
		List<PlotInfo> run = plots(plot(GROWN), plot(GROWN_B));
		assertAdvice(NextAction.HARVEST, 1, ActionAdvisor.decide(run, 0, 60, FULL_WITH_B_STACK, 18));
	}

	@Test
	public void asksToFreeASlotWhenFullWithNoFruitToDeposit()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(GROWN)), 0, 60, FULL_NO_FRUIT, 19);
		assertAdvice(NextAction.FREE_SLOT, -1, advice);
	}

	@Test
	public void waitsInsteadOfDepositingMidRun()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(STAGE1_WET)), 0, 60, WITH_FRUIT, 19);
		assertAdvice(NextAction.WAIT, -1, advice);
	}

	@Test
	public void depositsAHundredFruitBetweenRunsBeforeTheFirstSeed()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(EMPTY), plot(DEAD)), 20, 60, HUNDRED_FRUIT,
			20);
		assertAdvice(NextAction.DEPOSIT_FRUIT, -1, advice);
	}

	@Test
	public void keepsASmallHaulAndPlantsBetweenRuns()
	{
		ActionAdvisor.Advice advice = ActionAdvisor.decide(plots(plot(EMPTY), plot(DEAD)), 20, 60, WITH_FRUIT, 20);
		assertAdvice(NextAction.PLANT_SEED, 0, advice);
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
