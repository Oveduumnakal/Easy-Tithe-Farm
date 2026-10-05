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

/**
 * Checks the backpack for what the wiki says every Tithe Farm run needs — a spade, a seed dibber, and a watering
 * can — so a missing tool is caught before the first seed instead of mid-run. Pure and static.
 */
final class InventoryCheck
{
	/** Name shown when no spade is carried. */
	static final String SPADE = "spade";

	/** Name shown when no seed dibber is carried. */
	static final String DIBBER = "seed dibber";

	/** Name shown when no watering can of any kind is carried. */
	static final String CAN = "watering can";

	private InventoryCheck()
	{
	}

	/**
	 * The required tools missing from an inventory.
	 *
	 * @param itemIds    the inventory's item ids ({@code -1} for empty slots)
	 * @param barehanded whether the player plants without a dibber (Barbarian Training)
	 * @return the missing tools' names, in a fixed order; empty when nothing is missing
	 */
	static List<String> missing(int[] itemIds, boolean barehanded)
	{
		boolean spade = false;
		boolean dibber = false;
		boolean can = false;
		for (int id : itemIds)
		{
			if (id == TitheFarmIds.SPADE)
				spade = true;
			else if (id == TitheFarmIds.SEED_DIBBER)
				dibber = true;
			else if (TitheFarmIds.regularCanCharges(id) >= 0 || id == TitheFarmIds.GRICOLLER_CAN)
				can = true;
		}

		List<String> missing = new ArrayList<>();
		if (!spade)
			missing.add(SPADE);

		if (!dibber && !barehanded)
			missing.add(DIBBER);

		if (!can)
			missing.add(CAN);

		return missing;
	}

	/**
	 * Whether Gricoller's fertiliser is carried; the wiki's basic method cannot be run with it.
	 *
	 * @param itemIds the inventory's item ids
	 * @return true when the fertiliser is in the backpack
	 */
	static boolean hasFertiliser(int[] itemIds)
	{
		for (int id : itemIds)
		{
			if (id == TitheFarmIds.FERTILISER)
				return true;
		}

		return false;
	}
}
