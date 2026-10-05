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

/**
 * Water arithmetic for a run, across both watering-can setups. Pure and static so it can be unit-tested
 * without a client.
 *
 * <p>Regular watering cans encode their charge in their item id, so a backpack of cans sums to the total the
 * player can carry. Gricoller's watering can holds far more, tracked in a varbit rather than the item id; its
 * charge counts only while the can is actually in the backpack.
 *
 * <p>What a run still needs is read from the plots themselves: every planted plot owes the waters left in its
 * life, and every seed the run still has room for owes three more. Each watering lowers both the water carried
 * and the water needed by one, so the comparison stays truthful from the first seed to the last harvest.
 */
final class WaterTracker
{
	private WaterTracker()
	{
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

	/**
	 * Whether an item is a watering can with room for more water: a regular can under eight charges, or
	 * Gricoller's can under its capacity.
	 *
	 * @param itemId           the item id
	 * @param gricollerCharges the value of Gricoller's watering-can charge varbit
	 * @return true for a can that is not full
	 */
	static boolean needsFill(int itemId, int gricollerCharges)
	{
		int charges = TitheFarmIds.regularCanCharges(itemId);
		if (charges >= 0)
			return charges < TitheFarmIds.REGULAR_CAN_CAPACITY;

		return itemId == TitheFarmIds.GRICOLLER_CAN && gricollerCharges < TitheFarmIds.GRICOLLER_CAN_CAPACITY;
	}

	/**
	 * Whether every watering can in an inventory is full. An inventory with no cans counts as full, since
	 * there is nothing to refill.
	 *
	 * @param inventoryItemIds the inventory's item ids ({@code -1} for empty slots)
	 * @param gricollerCharges the value of Gricoller's watering-can charge varbit
	 * @return true when no can has room for more water
	 */
	static boolean cansFull(int[] inventoryItemIds, int gricollerCharges)
	{
		for (int id : inventoryItemIds)
		{
			if (needsFill(id, gricollerCharges))
				return false;
		}

		return true;
	}

	/**
	 * The waters the plants already in the ground still need before they are grown.
	 *
	 * @param plots the route plots
	 * @return the summed remaining waters of every planted plot
	 */
	static int plantedNeed(List<PlotInfo> plots)
	{
		int total = 0;
		for (PlotInfo plot : plots)
			total += plot.getWatersRemaining();

		return total;
	}

	/**
	 * The water needed to finish the run: what the planted plots owe, plus three for each seed the run still
	 * has room for, counting only as many as there are free plots for.
	 *
	 * @param plots the run's plots
	 * @param slots how many more seeds the run has room for
	 * @return the charges needed to see the run through to harvest
	 */
	static int runNeed(List<PlotInfo> plots, int slots)
	{
		return plantedNeed(plots) + toPlant(plots, slots) * TitheFarmIds.WATERS_PER_CROP;
	}

	/**
	 * Whether a seed can go in with water enough for the whole run: the water carried must cover what the
	 * planted plots owe plus three for every seed the run still has room for, and at least for this one seed.
	 *
	 * @param plots the run's plots
	 * @param water the charges carried
	 * @param slots how many more seeds the run has room for
	 * @return true when planting is safe for the rest of the run
	 */
	static boolean canAffordPlant(List<PlotInfo> plots, int water, int slots)
	{
		int seeds = Math.max(1, toPlant(plots, slots));
		return water >= plantedNeed(plots) + seeds * TitheFarmIds.WATERS_PER_CROP;
	}

	/** How many seeds the run will still plant: its free room, capped by the plots open for a seed. */
	private static int toPlant(List<PlotInfo> plots, int slots)
	{
		int open = 0;
		for (PlotInfo plot : plots)
		{
			if (plot.needsSeed())
				open++;
		}

		return Math.min(open, Math.max(0, slots));
	}
}
