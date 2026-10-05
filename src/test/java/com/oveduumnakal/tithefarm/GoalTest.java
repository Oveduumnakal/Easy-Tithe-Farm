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

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/** Verifies the reward goal arithmetic, names, and how ticks and quantities are read from the config. */
public class GoalTest
{
	private TitheFarmConfig config;

	@Before
	public void setUp()
	{
		config = TestRuns.defaultConfig();
	}

	@Test
	public void nothingTickedIsEmpty()
	{
		Goal goal = Goal.of(config, 500);
		assertTrue(goal.isEmpty());
		assertFalse(goal.isAffordable());
		assertEquals("", goal.getDisplayName());
		assertEquals(0, goal.getTotal());
	}

	@Test
	public void sumsCostTimesQuantityOverTickedRewards()
	{
		when(config.trackStrawhat()).thenReturn(true);
		when(config.trackGricollersCan()).thenReturn(true);
		when(config.trackSeedPack()).thenReturn(true);
		when(config.seedPackQuantity()).thenReturn(5);
		Goal goal = Goal.of(config, 212);
		assertEquals(75 + 200 + 5 * 30, goal.getTotal());
		assertEquals(213, goal.getRemaining());
		assertEquals("Selected rewards (3)", goal.getDisplayName());
		assertEquals("", goal.getCountText());
		assertEquals(RewardItem.FARMERS_STRAWHAT.getItemId(), goal.getIconItemId());
		assertFalse(goal.isAffordable());
	}

	@Test
	public void ignoresQuantityOfAnUntickedReward()
	{
		when(config.compostQuantity()).thenReturn(1000);
		when(config.trackBoots()).thenReturn(true);
		assertEquals(50, Goal.of(config, 0).getTotal());
	}

	@Test
	public void namesASingleRewardWithItsQuantity()
	{
		when(config.trackHerbBox()).thenReturn(true);
		when(config.herbBoxQuantity()).thenReturn(5);
		Goal goal = Goal.of(config, 95);
		assertEquals("Herb box x5", goal.getDisplayName());
		assertEquals("3/5", goal.getCountText());
	}

	@Test
	public void singleOneOffRewardHasNoCount()
	{
		when(config.trackHerbSack()).thenReturn(true);
		Goal goal = Goal.of(config, 300);
		assertEquals("Herb sack", goal.getDisplayName());
		assertEquals("", goal.getCountText());
		assertTrue(goal.isAffordable());
		assertEquals(1.0, goal.getProgress(), 0);
		assertEquals(0, goal.getRunsLeft());
	}

	@Test
	public void keepsShopOrder()
	{
		when(config.trackSeedPack()).thenReturn(true);
		when(config.trackBoots()).thenReturn(true);
		List<Goal.Selection> selections = Goal.selections(config);
		assertEquals(RewardItem.FARMERS_BOOTS, selections.get(0).getItem());
		assertEquals(RewardItem.SEED_PACK, selections.get(1).getItem());
	}

	@Test
	public void flagsATotalAboveThePointsCap()
	{
		when(config.trackHerbBox()).thenReturn(true);
		when(config.herbBoxQuantity()).thenReturn(600);
		assertTrue(Goal.of(config, 0).isOverCap());
		when(config.herbBoxQuantity()).thenReturn(500);
		assertFalse(Goal.of(config, 0).isOverCap());
	}

	@Test
	public void runsLeftUsesTheCropCount()
	{
		assertEquals(0, Goal.runsToGoal(80, 75, 20));
		assertEquals(5, Goal.runsToGoal(0, 30, 20));
		assertEquals(4, Goal.runsToGoal(0, 28, 20));
		when(config.trackGricollersCan()).thenReturn(true);
		assertEquals(15, Goal.of(config, 100).getRunsLeft());
	}

	@Test
	public void percentRoundsDownSoFullMeansAffordable()
	{
		assertEquals("49.8%", RewardGoalOverlay.percent(212 / 425.0));
		assertEquals("99.9%", RewardGoalOverlay.percent(0.99999));
		assertEquals("100.0%", RewardGoalOverlay.percent(1));
	}

	@Test
	public void pendingPointsRaiseOnlyThePendingProgress()
	{
		when(config.trackStrawhat()).thenReturn(true);
		Goal goal = Goal.of(config, 30, 15);
		assertEquals(15, goal.getPending());
		assertEquals(0.4, goal.getProgress(), 1e-9);
		assertEquals(0.6, goal.getPendingProgress(), 1e-9);
		assertEquals(45, goal.getRemaining());
		assertFalse(goal.isAffordable());
	}

	@Test
	public void pendingPointsStopAtThePointsCap()
	{
		when(config.trackStrawhat()).thenReturn(true);
		assertEquals(10, Goal.of(config, Goal.POINTS_CAP - 10, 35).getPending());
	}
}
