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
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.Notifier;

/**
 * Keeps the reward goal current each tick while the player is at the farm or its lobby, and fires the goal
 * notification once when the points first reach the total. Independent of the session stats: it reads the points
 * varbit itself.
 */
@Singleton
class GoalTracker
{
	private final Client client;
	private final TitheFarmConfig config;
	private final Notifier notifier;

	private Goal goal = Goal.NONE;
	private boolean inRegion;
	private Boolean wasAffordable;

	@Inject
	GoalTracker(Client client, TitheFarmConfig config, Notifier notifier)
	{
		this.client = client;
		this.config = config;
		this.notifier = notifier;
	}

	/** Rebuilds the goal from the config and points, and notifies on the first tick it becomes affordable. */
	void onTick()
	{
		inRegion = inFarmRegion();
		if (!inRegion)
			return;

		int pending = DepositRewards.points(client.getVarbitValue(TitheFarmIds.SCORE_VARBIT), carriedFruit());
		goal = Goal.of(config, client.getVarbitValue(TitheFarmIds.POINTS_VARBIT), pending);
		if (shouldNotify(goal))
		{
			notifier.notify(config.goalNotification(),
				"Tithe Farm: you can afford every reward in your goal (" + goal.getTotal() + " points).");
		}
	}

	/**
	 * Whether the goal has just become affordable. The first goal seen only sets the baseline, so logging in with
	 * enough points does not notify; spending below the total, or ticking another reward, re-arms it.
	 *
	 * @param goal the current goal
	 * @return true on the tick the points first reach the total
	 */
	boolean shouldNotify(Goal goal)
	{
		if (goal.isEmpty())
		{
			wasAffordable = null;
			return false;
		}

		boolean affordable = goal.isAffordable();
		boolean crossed = Boolean.FALSE.equals(wasAffordable) && affordable;
		wasAffordable = affordable;
		return crossed;
	}

	/** Forgets the goal and the notification baseline, for plugin start, logout, or a world hop. */
	void reset()
	{
		goal = Goal.NONE;
		inRegion = false;
		wasAffordable = null;
	}

	/** The goal as of the last tick at the farm or lobby. */
	Goal getGoal()
	{
		return goal;
	}

	/** Whether the player stood at the farm or its lobby on the last tick. */
	boolean isInRegion()
	{
		return inRegion;
	}

	/** The Tithe Farm fruit in the backpack. */
	private int carriedFruit()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
			return 0;

		int fruit = 0;
		for (Item item : inventory.getItems())
		{
			if (item != null && TitheFarmIds.isFruit(item.getId()))
				fruit += item.getQuantity();
		}

		return fruit;
	}

	/** Whether the player's template tile lies in the farm's map region, which also holds the lobby. */
	private boolean inFarmRegion()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getLocalLocation() == null)
			return false;

		WorldView view = client.getTopLevelWorldView();
		WorldPoint tile = WorldPoint.fromLocalInstance(view.getScene(), player.getLocalLocation(), view.getPlane());
		return tile.getRegionID() == TitheFarmIds.FARM_REGION;
	}
}
