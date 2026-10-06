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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies when a harvest fits in the backpack, by free slots and by fruit tier. */
public class BackpackTest
{
	private static final int GOLOVANOVA = 0;
	private static final int BOLOGANO = 1;

	/** A full 28-slot backpack of spades with one Golovanova fruit slot. */
	private static int[] fullWithGolovanova()
	{
		int[] ids = new int[TitheFarmIds.INVENTORY_SIZE];
		Arrays.fill(ids, TitheFarmIds.SPADE);
		ids[0] = TitheFarmIds.FRUIT_GOLOVANOVA;
		return ids;
	}

	/** One of each item, for backpacks where only the slots matter. */
	private static int[] ones(int size)
	{
		int[] counts = new int[size];
		Arrays.fill(counts, 1);
		return counts;
	}

	@Test
	public void aFreeSlotFitsAnyHarvest()
	{
		int[] ids = {TitheFarmIds.SPADE, -1, TitheFarmIds.FRUIT_GOLOVANOVA};
		Backpack backpack = Backpack.of(ids, new int[]{1, 0, 40}, true);
		assertTrue(backpack.hasFruit());
		assertEquals(40, backpack.getFruit());
		assertTrue(backpack.fits(BOLOGANO));
	}

	@Test
	public void aFullBackpackFitsOnlyItsOwnStack()
	{
		Backpack backpack = Backpack.of(fullWithGolovanova(), ones(TitheFarmIds.INVENTORY_SIZE), true);
		assertTrue(backpack.fits(GOLOVANOVA));
		assertFalse(backpack.fits(BOLOGANO));
		assertTrue(backpack.fits(PlotInfo.TIER_UNKNOWN));
	}

	@Test
	public void unstackedFruitNeedsAFreeSlot()
	{
		Backpack backpack = Backpack.of(fullWithGolovanova(), ones(TitheFarmIds.INVENTORY_SIZE), false);
		assertTrue(backpack.hasFruit());
		assertFalse(backpack.fits(GOLOVANOVA));
	}

	@Test
	public void aHarvestTakesTheLastSlotThenStartsItsStack()
	{
		Backpack backpack = new Backpack(1, 0, 0, 0, true).harvested(GOLOVANOVA);
		assertTrue(backpack.hasFruit());
		assertEquals(1, backpack.getFruit());
		assertTrue(backpack.fits(GOLOVANOVA));
		assertFalse(backpack.fits(BOLOGANO));
	}

	@Test
	public void aDepositFreesTheFruitSlots()
	{
		Backpack backpack = new Backpack(0, 2, 2, 0, false).deposited();
		assertFalse(backpack.hasFruit());
		assertTrue(backpack.fits(BOLOGANO));
	}
}
