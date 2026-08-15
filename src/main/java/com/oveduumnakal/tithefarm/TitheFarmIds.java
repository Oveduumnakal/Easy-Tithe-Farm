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

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * Game ids for the Tithe Farm minigame, taken from RuneLite's {@code gameval} constants and its built-in
 * Tithe Farm plugin's plant tables.
 *
 * <p>Plots come in three seed tiers (A Golovanova, B Bologano, C Logavano). Each tier owns a run of eleven
 * consecutive object ids: three growth stages of dry/wet/dead, then a grown id, then a dead-grown id. The
 * empty plot is one shared id. {@link TithePlotState} decodes an object id into a state; this class only
 * holds the raw numbers and simple membership tests.
 */
final class TitheFarmIds
{
	/** Empty, plantable plot ({@code HOSIDIUS_TITHE_EMPTY}). */
	static final int PLOT_EMPTY = ObjectID.HOSIDIUS_TITHE_EMPTY;

	/** First growth object id, the tier-A stage-1 dry plot ({@code HOSIDIUS_TITHE_A_1_DRY}). */
	static final int PLOT_GROWTH_FIRST = ObjectID.HOSIDIUS_TITHE_A_1_DRY;

	/** Last growth object id, the tier-C dead-grown plot ({@code HOSIDIUS_TITHE_C_4_DEAD}). */
	static final int PLOT_GROWTH_LAST = ObjectID.HOSIDIUS_TITHE_C_4_DEAD;

	/** Grown (harvestable) object id for tier A ({@code HOSIDIUS_TITHE_A_4}). */
	static final int PLOT_A_GROWN = ObjectID.HOSIDIUS_TITHE_A_4;

	/** Grown (harvestable) object id for tier B ({@code HOSIDIUS_TITHE_B_4}). */
	static final int PLOT_B_GROWN = ObjectID.HOSIDIUS_TITHE_B_4;

	/** Grown (harvestable) object id for tier C ({@code HOSIDIUS_TITHE_C_4}). */
	static final int PLOT_C_GROWN = ObjectID.HOSIDIUS_TITHE_C_4;

	/** Number of growth object ids per tier: three stages of three, a grown, and a dead-grown. */
	static final int TIER_SPAN = 11;

	/** The water barrels players refill cans at (two-object barrel: {@code WATER_BARREL1} / {@code WATER_BARREL2}). */
	static final int WATER_BARREL_A = ObjectID.WATER_BARREL1;

	/** The second half of the two-object water barrel. */
	static final int WATER_BARREL_B = ObjectID.WATER_BARREL2;

	/** The sack of fruit deposit point, once produce has been added. */
	static final int SACK_OF_FRUIT = ObjectID.TITHE_SACK_OF_FRUIT;

	/** The sack of fruit deposit point while still empty. */
	static final int SACK_OF_FRUIT_EMPTY = ObjectID.TITHE_SACK_OF_FRUIT_EMPTY;

	/** The seed table where a run's seeds are collected. */
	static final int SEED_TABLE = ObjectID.TITHE_PLANT_SEED_TABLE;

	/** Golovanova seed (tier A). */
	static final int SEED_GOLOVANOVA = ItemID.HOSIDIUS_TITHE_SEED_A;

	/** Bologano seed (tier B). */
	static final int SEED_BOLOGANO = ItemID.HOSIDIUS_TITHE_SEED_B;

	/** Logavano seed (tier C). */
	static final int SEED_LOGAVANO = ItemID.HOSIDIUS_TITHE_SEED_C;

	/** Gricoller's fertiliser (reduces watering; optional). */
	static final int FERTILISER = ItemID.HOSIDIUS_TITHE_FERTILISER;

	/** Empty regular watering can ({@code WATERING_CAN_0}); the eight filled ids follow it non-contiguously. */
	static final int WATERING_CAN_EMPTY = ItemID.WATERING_CAN_0;

	/** Regular watering can with one charge ({@code WATERING_CAN_1}); each further charge is the next id. */
	static final int WATERING_CAN_ONE = ItemID.WATERING_CAN_1;

	/** Regular watering can with eight charges, the full can ({@code WATERING_CAN_8}). */
	static final int WATERING_CAN_FULL = ItemID.WATERING_CAN_8;

	/** Charges a full regular watering can holds. */
	static final int REGULAR_CAN_CAPACITY = 8;

	/** Gricoller's watering can ({@code ZEAH_WATERINGCAN}); its charges live in {@link #GRICOLLER_CHARGES_VARBIT}. */
	static final int GRICOLLER_CAN = ItemID.ZEAH_WATERINGCAN;

	/** Varbit holding the charge count of Gricoller's watering can. */
	static final int GRICOLLER_CHARGES_VARBIT = VarbitID.ZEAH_WATERINGCAN_CHARGES;

	/** Waters a single plant needs across its life — one per growth stage. */
	static final int WATERS_PER_CROP = 3;

	private TitheFarmIds()
	{
	}

	/** Whether an object id is any Tithe Farm plot — the empty plot or one of the three tiers' growth ids. */
	static boolean isPlot(int objectId)
	{
		return objectId == PLOT_EMPTY || (objectId >= PLOT_GROWTH_FIRST && objectId <= PLOT_GROWTH_LAST);
	}

	/** Whether an object id is one of the two water-barrel halves. */
	static boolean isWaterBarrel(int objectId)
	{
		return objectId == WATER_BARREL_A || objectId == WATER_BARREL_B;
	}

	/** Whether an object id is the fruit-deposit sack in either its empty or filled form. */
	static boolean isSack(int objectId)
	{
		return objectId == SACK_OF_FRUIT || objectId == SACK_OF_FRUIT_EMPTY;
	}

	/** Whether an item id is a Tithe Farm fruit seed of any tier. */
	static boolean isSeed(int itemId)
	{
		return itemId == SEED_GOLOVANOVA || itemId == SEED_BOLOGANO || itemId == SEED_LOGAVANO;
	}

	/**
	 * The charge count of a regular watering can from its item id, or {@code -1} if the id is not a regular
	 * can. The empty can is its own id; the filled cans run one id apart starting one past the empty id.
	 *
	 * @param itemId the inventory item id
	 * @return charges 0 to {@link #REGULAR_CAN_CAPACITY}, or {@code -1} when not a regular watering can
	 */
	static int regularCanCharges(int itemId)
	{
		if (itemId == WATERING_CAN_EMPTY)
			return 0;

		if (itemId >= WATERING_CAN_ONE && itemId <= WATERING_CAN_FULL)
			return itemId - WATERING_CAN_ONE + 1;

		return -1;
	}
}
