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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks deposit points and experience against the OSRS Wiki's sack rules. */
public class DepositRewardsTest
{
	private static final int[] NONE = {};

	@Test
	public void aPointForEveryThirdPlaceInTheSack()
	{
		assertEquals(0, DepositRewards.points(0, 0));
		assertEquals(3, DepositRewards.points(0, 10));
		assertEquals(4, DepositRewards.points(2, 10));
		assertEquals(3, DepositRewards.points(3, 10));
	}

	@Test
	public void aFullSackIsWorthThirtyFive()
	{
		assertEquals(35, DepositRewards.points(0, 100));
		assertEquals(13 + 2, DepositRewards.points(60, 40));
		assertEquals(13, DepositRewards.points(60, 39));
	}

	@Test
	public void theSackEmptiesAndTheCountStartsOver()
	{
		assertEquals(35, DepositRewards.points(0, 101));
		assertEquals(36, DepositRewards.points(0, 103));
		assertEquals(19 + 35 + 16, DepositRewards.points(150, 200));
		assertEquals(DepositRewards.points(0, 10), DepositRewards.points(100, 10));
	}

	@Test
	public void experienceDoublesFromTheSeventyFifthWithItsBonus()
	{
		int[] golovanova = {TitheFarmIds.FRUIT_GOLOVANOVA};
		assertEquals(74 * 6, DepositRewards.xp(0, golovanova, new int[]{74}, 0));
		assertEquals(74 * 6 + 12 + 1500, DepositRewards.xp(0, golovanova, new int[]{75}, 0));
		assertEquals(74 * 6 + 26 * 12 + 1500, DepositRewards.xp(0, golovanova, new int[]{100}, 0));
		assertEquals(74 * 6 + 26 * 12 + 1500 + 6, DepositRewards.xp(0, golovanova, new int[]{101}, 0));
	}

	@Test
	public void experienceFollowsEachFruitTypeAndSkipsOtherItems()
	{
		int[] ids = {TitheFarmIds.SEED_LOGAVANO, TitheFarmIds.FRUIT_LOGAVANO, -1};
		assertEquals(23 + 46 + 5750, DepositRewards.xp(73, ids, new int[]{20, 2, 0}, 0));
		assertEquals(0, DepositRewards.xp(0, NONE, NONE, 0));
		assertEquals(28, DepositRewards.xp(80, new int[]{TitheFarmIds.FRUIT_BOLOGANO}, new int[]{1}, 0));
	}

	@Test
	public void outfitPiecesAddUpWithASetBonus()
	{
		assertEquals(0, DepositRewards.outfitBoost(new int[]{-1, TitheFarmIds.SPADE}), 1e-9);
		assertEquals(0.004, DepositRewards.outfitBoost(new int[]{TitheFarmIds.FARMERS_HAT_FEMALE}), 1e-9);
		assertEquals(0.008 + 0.006, DepositRewards.outfitBoost(
			new int[]{TitheFarmIds.FARMERS_TORSO, TitheFarmIds.FARMERS_LEGS_FEMALE}), 1e-9);
		assertEquals(0.025, DepositRewards.outfitBoost(new int[]{TitheFarmIds.FARMERS_HAT, TitheFarmIds.FARMERS_TORSO,
			TitheFarmIds.FARMERS_LEGS, TitheFarmIds.FARMERS_BOOTS}), 1e-9);
	}

	@Test
	public void outfitBoostSkipsTheSeventyFifthBonus()
	{
		int[] logavano = {TitheFarmIds.FRUIT_LOGAVANO};
		assertEquals(Math.round(46 * 1.025) + 5750, DepositRewards.xp(74, logavano, new int[]{1}, 0.025));
		assertEquals(2970 + 5750, DepositRewards.xp(0, logavano, new int[]{100}, 0.025));
	}
}
