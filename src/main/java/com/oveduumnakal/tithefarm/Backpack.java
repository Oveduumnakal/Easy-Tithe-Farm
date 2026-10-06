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

/**
 * The backpack as far as fruit is concerned: how many slots are free, how much fruit is carried and in how many
 * slots, and which fruit tiers sit in a stack a harvest of the same tier can join. Pure and immutable, so
 * {@link ActionAdvisor} can ask whether a grown plant's fruit fits or a deposit is due, and {@link ActionForecast}
 * can play harvests and deposits forward.
 *
 * <p>When fruit stacks, a harvest fits into a full backpack only if a stack of that plant's own tier is already
 * carried — a Golovanova stack leaves no room for a Bologano harvest. When fruit does not stack, every harvest
 * needs a free slot.
 */
final class Backpack
{
	/** A backpack with room to spare and no fruit. */
	static final Backpack ROOMY = new Backpack(TitheFarmIds.INVENTORY_SIZE, 0, 0, 0, false);

	private final int freeSlots;
	private final int fruitSlots;
	private final int fruit;
	private final int stackedTiers;
	private final boolean fruitStacks;

	/**
	 * Creates a backpack.
	 *
	 * @param freeSlots    the empty slots
	 * @param fruitSlots   the slots holding Tithe Farm fruit
	 * @param fruit        the Tithe Farm fruit carried, across every slot
	 * @param stackedTiers a bit per fruit tier carried as a stack ({@code 1 << tier}); always 0 when fruit does
	 *                     not stack
	 * @param fruitStacks  whether Tithe Farm fruit stacks
	 */
	Backpack(int freeSlots, int fruitSlots, int fruit, int stackedTiers, boolean fruitStacks)
	{
		this.freeSlots = Math.max(0, freeSlots);
		this.fruitSlots = Math.max(0, fruitSlots);
		this.fruit = Math.max(0, fruit);
		this.stackedTiers = fruitStacks ? stackedTiers : 0;
		this.fruitStacks = fruitStacks;
	}

	/**
	 * Reads a backpack from its item ids and quantities.
	 *
	 * @param itemIds     the inventory's item ids ({@code -1} for empty slots), possibly shorter than 28
	 * @param counts      the quantity in each slot, index for index with {@code itemIds}
	 * @param fruitStacks whether Tithe Farm fruit stacks
	 * @return the backpack
	 */
	static Backpack of(int[] itemIds, int[] counts, boolean fruitStacks)
	{
		int occupied = 0;
		int fruitSlots = 0;
		int fruit = 0;
		int stackedTiers = 0;
		for (int slot = 0; slot < itemIds.length; slot++)
		{
			if (itemIds[slot] < 0)
				continue;

			occupied++;
			int tier = TitheFarmIds.fruitTier(itemIds[slot]);
			if (tier >= 0)
			{
				fruitSlots++;
				fruit += counts[slot];
				stackedTiers |= 1 << tier;
			}
		}

		return new Backpack(TitheFarmIds.INVENTORY_SIZE - occupied, fruitSlots, fruit, stackedTiers, fruitStacks);
	}

	/** Whether any Tithe Farm fruit is carried. */
	boolean hasFruit()
	{
		return fruitSlots > 0;
	}

	/** The Tithe Farm fruit carried, across every slot. */
	int getFruit()
	{
		return fruit;
	}

	/**
	 * Whether a harvest of the given tier fits: a slot is free, or a stack of that tier is already carried. A
	 * plant of unknown tier fits into any carried stack.
	 *
	 * @param tier the plant's seed tier, or {@link PlotInfo#TIER_UNKNOWN}
	 * @return true when the fruit would go into the backpack
	 */
	boolean fits(int tier)
	{
		return freeSlots > 0 || hasStackOf(tier);
	}

	/**
	 * The backpack after a harvest of the given tier: the fruit joins its stack, or takes a free slot.
	 *
	 * @param tier the harvested plant's seed tier, or {@link PlotInfo#TIER_UNKNOWN}
	 * @return the backpack afterwards
	 */
	Backpack harvested(int tier)
	{
		if (hasStackOf(tier))
			return new Backpack(freeSlots, fruitSlots, fruit + 1, stackedTiers, fruitStacks);

		int stacked = tier < 0 ? stackedTiers : stackedTiers | 1 << tier;
		return new Backpack(freeSlots - 1, fruitSlots + 1, fruit + 1, stacked, fruitStacks);
	}

	/** The backpack after depositing every fruit in the sack. */
	Backpack deposited()
	{
		return new Backpack(freeSlots + fruitSlots, 0, 0, 0, fruitStacks);
	}

	/** Whether a stack the given tier's fruit can join is carried; any stack will do for an unknown tier. */
	private boolean hasStackOf(int tier)
	{
		return tier < 0 ? stackedTiers != 0 : (stackedTiers & (1 << tier)) != 0;
	}
}
