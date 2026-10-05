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
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies the required-tools check and the fertiliser warning. */
public class InventoryCheckTest
{
	@Test
	public void fullKitIsMissingNothing()
	{
		int[] items = {952, 5343, 5340, 13425, -1};
		assertEquals(Collections.emptyList(), InventoryCheck.missing(items, false));
	}

	@Test
	public void listsEveryMissingToolInOrder()
	{
		int[] items = {13425, -1};
		assertEquals(Arrays.asList(InventoryCheck.SPADE, InventoryCheck.DIBBER, InventoryCheck.CAN),
			InventoryCheck.missing(items, false));
	}

	@Test
	public void barehandedPlantingNeedsNoDibber()
	{
		int[] items = {952, 13353};
		assertEquals(Collections.emptyList(), InventoryCheck.missing(items, true));
	}

	@Test
	public void anEmptyRegularCanStillCountsAsACan()
	{
		int[] items = {952, 5343, 5331};
		assertEquals(Collections.emptyList(), InventoryCheck.missing(items, false));
	}

	@Test
	public void spotsGricollersFertiliser()
	{
		assertTrue(InventoryCheck.hasFertiliser(new int[]{952, 13420}));
		assertFalse(InventoryCheck.hasFertiliser(new int[]{952, 5343}));
	}

	@Test
	public void recognisesRunEnergyPotions()
	{
		assertTrue(TitheFarmIds.isRunRestore(12625));
		assertTrue(TitheFarmIds.isRunRestore(3022));
		assertFalse(TitheFarmIds.isRunRestore(2434));
	}

	@Test
	public void energyWarningRespectsItsThreshold()
	{
		RunStatus tired = new RunStatus(false, 0, 0, Collections.emptyList(), false, 12, 0, 0, true);
		assertTrue(tired.isEnergyLow(20));
		assertFalse(tired.isEnergyLow(10));
		assertFalse("0 turns it off", tired.isEnergyLow(0));
	}
}
