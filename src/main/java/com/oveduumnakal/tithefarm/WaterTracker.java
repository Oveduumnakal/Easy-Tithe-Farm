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

import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/**
 * Counts the water the player can still pour, across both watering-can setups.
 *
 * <p>Regular watering cans encode their charge in their item id, so a backpack of cans sums to the total the
 * player can carry. Gricoller's watering can holds far more, tracked in a varbit rather than the item id; its
 * charge counts only while the can is actually in the backpack. The arithmetic is a pure static method so it
 * can be unit-tested without a client; the instance methods read the live inventory and varbit and delegate.
 */
@Singleton
class WaterTracker
{
	@Inject
	private Client client;

	/** The water charges the player can currently pour, or 0 when the inventory is unavailable. */
	int availableCharges()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
			return 0;

		Item[] items = inventory.getItems();
		int[] ids = new int[items.length];
		for (int i = 0; i < items.length; i++)
			ids[i] = items[i] == null ? -1 : items[i].getId();

		int gricollerCharges = client.getVarbitValue(TitheFarmIds.GRICOLLER_CHARGES_VARBIT);
		return totalCharges(ids, gricollerCharges);
	}

	/** How many charges a run of the given crop count needs — three per crop, one per growth stage. */
	int chargesNeeded(int crops)
	{
		return Math.max(0, crops) * TitheFarmIds.WATERS_PER_CROP;
	}

	/** Whether the player holds enough water to see a fresh run of {@code crops} plants through to harvest. */
	boolean enoughFor(int crops)
	{
		return availableCharges() >= chargesNeeded(crops);
	}

	/**
	 * Sums the water charges represented by an inventory, given the item ids it holds and Gricoller's can's
	 * varbit charge. Every regular watering can adds its own charge; Gricoller's varbit charge is added once,
	 * and only when Gricoller's can is present in the ids.
	 *
	 * @param inventoryItemIds the inventory's item ids ({@code -1} for empty slots)
	 * @param gricollerCharges the value of Gricoller's watering-can charge varbit
	 * @return the total pourable charges
	 */
	static int totalCharges(int[] inventoryItemIds, int gricollerCharges)
	{
		int total = 0;
		boolean hasGricoller = false;
		for (int id : inventoryItemIds)
		{
			int charges = TitheFarmIds.regularCanCharges(id);
			if (charges >= 0)
				total += charges;
			else if (id == TitheFarmIds.GRICOLLER_CAN)
				hasGricoller = true;
		}

		if (hasGricoller)
			total += Math.max(0, gricollerCharges);

		return total;
	}
}
