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

/** A Farmer Gricoller's Rewards item to save points toward, with its cost from the OSRS Wiki. */
enum RewardItem
{
	/** Farmer's strawhat, a piece of the farmer's outfit. */
	FARMERS_STRAWHAT("Farmer's strawhat", ItemID.TITHE_REWARD_HAT_MALE, 75, false),

	/** Farmer's jacket or shirt, a piece of the farmer's outfit. */
	FARMERS_JACKET("Farmer's jacket", ItemID.TITHE_REWARD_TORSO_MALE, 150, false),

	/** Farmer's boro trousers, a piece of the farmer's outfit. */
	FARMERS_TROUSERS("Farmer's boro trousers", ItemID.TITHE_REWARD_LEGS_MALE, 125, false),

	/** Farmer's boots, a piece of the farmer's outfit. */
	FARMERS_BOOTS("Farmer's boots", ItemID.TITHE_REWARD_FEET_MALE, 50, false),

	/** The seed box. */
	SEED_BOX("Seed box", ItemID.SEED_BOX, 250, false),

	/** The herb sack. */
	HERB_SACK("Herb sack", ItemID.SLAYER_HERB_SACK, 250, false),

	/** Gricoller's can, the 1,000-dose watering can. */
	GRICOLLERS_CAN("Gricoller's can", ItemID.ZEAH_WATERINGCAN, 200, false),

	/** The auto-weed unlock; it has no item, so weeds stand in for its icon. */
	AUTO_WEED("Auto-weed", ItemID.WEEDS, 50, false),

	/** A bucket of compost. */
	COMPOST("Compost", ItemID.BUCKET_COMPOST, 1, true),

	/** A bucket of supercompost. */
	SUPERCOMPOST("Supercompost", ItemID.BUCKET_SUPERCOMPOST, 5, true),

	/** A grape seed. */
	GRAPE_SEED("Grape seed", ItemID.GRAPE_SEED, 2, true),

	/** A stack of 20 Bologa's blessings. */
	BOLOGAS_BLESSING("Bologa's blessing", ItemID.GRAPE_BLESSING, 1, true),

	/** A herb box of seven grimy herbs. */
	HERB_BOX("Herb box", ItemID.NZONE_HERBBOX, 30, true),

	/** A seed pack, equal to a tier 3 farming contract pack. */
	SEED_PACK("Seed pack", ItemID.SEEDBOX, 30, true);

	private final String name;
	private final int itemId;
	private final int cost;
	private final boolean repeatable;

	RewardItem(String name, int itemId, int cost, boolean repeatable)
	{
		this.name = name;
		this.itemId = itemId;
		this.cost = cost;
		this.repeatable = repeatable;
	}

	/** The reward's name for the goal box. */
	String getName()
	{
		return name;
	}

	/** The item whose icon stands for the reward. */
	int getItemId()
	{
		return itemId;
	}

	/** The points one costs. */
	int getCost()
	{
		return cost;
	}

	/** Whether the shop sells it more than once, so it takes a quantity. */
	boolean isRepeatable()
	{
		return repeatable;
	}
}
