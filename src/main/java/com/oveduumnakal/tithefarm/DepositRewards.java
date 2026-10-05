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
 * What depositing fruit in the sack is worth, following the OSRS Wiki's Tithe Farm rules. Pure and static so it can
 * be unit-tested without a client.
 *
 * <p>The sack holds 100 fruit and empties as the 100th goes in, so every rule counts the fruit's place in the
 * current sack, 1 to 100: a point for every third place (3, 6, ... 99), 2 bonus points at the 100th, the fruit's
 * harvest experience per fruit up to the 74th and double that from the 75th to the 100th, and a flat bonus of 250
 * times the harvest experience at the 75th. A deposit that overflows the sack fills it, the sack empties, and the
 * rest starts the next sack.
 *
 * <p>Farmer's outfit raises the per-fruit experience by 0.4% for the strawhat, 0.8% for the jacket or shirt, 0.6%
 * for the boro trousers, and 0.2% for the boots, plus 0.5% more for all four: 2.5% for the full set. The 75th
 * fruit's flat bonus is not raised.
 */
final class DepositRewards
{
	/** Fruit one sack holds before it empties. */
	static final int SACK_SIZE = 100;

	/** Fruit per point within a sack. */
	private static final int FRUIT_PER_POINT = 3;

	/** Bonus points for the 100th fruit in a sack. */
	private static final int FULL_SACK_POINTS = 2;

	/** The first place in the sack that earns double experience. */
	private static final int DOUBLE_XP_FROM = 75;

	/** The 75th fruit's flat bonus, as a multiple of the fruit's harvest experience. */
	private static final int BONUS_XP_MULTIPLIER = 250;

	/** Farmer's outfit experience boost per piece, and the extra for wearing all four. */
	private static final double HAT_BOOST = 0.004;
	private static final double TORSO_BOOST = 0.008;
	private static final double LEGS_BOOST = 0.006;
	private static final double BOOTS_BOOST = 0.002;
	private static final double SET_BOOST = 0.005;

	/** Harvest experience of one Golovanova, Bologano, and Logavano fruit. */
	private static final int GOLOVANOVA_XP = 6;
	private static final int BOLOGANO_XP = 14;
	private static final int LOGAVANO_XP = 23;

	private DepositRewards()
	{
	}

	/**
	 * The points one fruit earns on average over full sacks: 33 for every third place plus 2 for the 100th, over
	 * 100 fruit.
	 *
	 * @return the average points per fruit
	 */
	static double pointsPerFruit()
	{
		return (double) (SACK_SIZE / FRUIT_PER_POINT + FULL_SACK_POINTS) / SACK_SIZE;
	}

	/**
	 * The points depositing the carried fruit would earn.
	 *
	 * @param deposited the fruit deposited this game, read as a running count or the sack's fill
	 * @param carried   the fruit in the backpack
	 * @return the points the deposit would add
	 */
	static int points(int deposited, int carried)
	{
		int place = sackFill(deposited);
		int points = 0;
		for (int i = 0; i < Math.max(0, carried); i++)
		{
			place = place % SACK_SIZE + 1;
			if (place % FRUIT_PER_POINT == 0)
				points++;

			if (place == SACK_SIZE)
				points += FULL_SACK_POINTS;
		}

		return points;
	}

	/**
	 * The Farming experience depositing the carried fruit would earn, the fruit going in in backpack order.
	 *
	 * @param deposited the fruit deposited this game, read as a running count or the sack's fill
	 * @param itemIds   the backpack's item ids ({@code -1} for empty slots)
	 * @param counts    the quantity in each slot, index for index with {@code itemIds}
	 * @param boost     the Farmer's outfit boost from {@link #outfitBoost}, e.g. {@code 0.025}
	 * @return the experience the deposit would add, rounded to the nearest whole point
	 */
	static int xp(int deposited, int[] itemIds, int[] counts, double boost)
	{
		int place = sackFill(deposited);
		int perFruit = 0;
		int bonus = 0;
		for (int slot = 0; slot < itemIds.length; slot++)
		{
			int each = fruitXp(itemIds[slot]);
			for (int i = 0; each > 0 && i < counts[slot]; i++)
			{
				place = place % SACK_SIZE + 1;
				perFruit += place >= DOUBLE_XP_FROM ? each * 2 : each;
				if (place == DOUBLE_XP_FROM)
					bonus += each * BONUS_XP_MULTIPLIER;
			}
		}

		return (int) Math.round(perFruit * (1 + boost)) + bonus;
	}

	/**
	 * The experience boost from the Farmer's outfit pieces worn.
	 *
	 * @param wornIds the item ids of the equipment worn ({@code -1} for empty slots)
	 * @return the boost as a fraction, 0 to {@code 0.025}
	 */
	static double outfitBoost(int[] wornIds)
	{
		boolean hat = false;
		boolean torso = false;
		boolean legs = false;
		boolean boots = false;
		for (int id : wornIds)
		{
			hat |= id == TitheFarmIds.FARMERS_HAT || id == TitheFarmIds.FARMERS_HAT_FEMALE;
			torso |= id == TitheFarmIds.FARMERS_TORSO || id == TitheFarmIds.FARMERS_TORSO_FEMALE;
			legs |= id == TitheFarmIds.FARMERS_LEGS || id == TitheFarmIds.FARMERS_LEGS_FEMALE;
			boots |= id == TitheFarmIds.FARMERS_BOOTS || id == TitheFarmIds.FARMERS_BOOTS_FEMALE;
		}

		double boost = (hat ? HAT_BOOST : 0) + (torso ? TORSO_BOOST : 0) + (legs ? LEGS_BOOST : 0)
			+ (boots ? BOOTS_BOOST : 0);
		return hat && torso && legs && boots ? boost + SET_BOOST : boost;
	}

	/**
	 * The harvest experience of one fruit, or 0 for an item that is not Tithe Farm fruit.
	 *
	 * @param itemId the item id
	 * @return the experience per fruit
	 */
	static int fruitXp(int itemId)
	{
		if (itemId == TitheFarmIds.FRUIT_GOLOVANOVA)
			return GOLOVANOVA_XP;

		if (itemId == TitheFarmIds.FRUIT_BOLOGANO)
			return BOLOGANO_XP;

		if (itemId == TitheFarmIds.FRUIT_LOGAVANO)
			return LOGAVANO_XP;

		return 0;
	}

	/** The fruit in the current sack, 0 to 99. */
	private static int sackFill(int deposited)
	{
		return Math.max(0, deposited) % SACK_SIZE;
	}
}
