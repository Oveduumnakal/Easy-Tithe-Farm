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

import java.awt.Color;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

/** Settings for Easy Tithe Farm: crop count, what guidance is drawn, water guards, and colors. */
@ConfigGroup(TitheFarmConfig.GROUP)
public interface TitheFarmConfig extends Config
{
	/** Config group key, shared with the plugin so both agree on where settings are stored. */
	String GROUP = "tithefarm";

	/** Key of the route-mode setting, written by {@link RouteRecorder} when a recording is saved. */
	String ROUTE_MODE = "routeMode";

	/** Key of the record-route toggle, switched off by {@link RouteRecorder} when a recording completes. */
	String RECORD_ROUTE = "recordRoute";

	/** Key of the hidden saved route, encoded by {@link RouteRecorder#encode}. */
	String RECORDED_ROUTE = "recordedRoute";

	/** How many crops to run at a time — drives the route length and the water math. */
	@ConfigSection(
		name = "Run",
		description = "How many crops to grow per run.",
		position = 0
	)
	String runSection = "run";

	/** The planting route and how the next-action highlights look. */
	@ConfigSection(
		name = "Guidance",
		description = "The planting route and the next-action highlights.",
		position = 1
	)
	String guidanceSection = "guidance";

	/** Water tracking: the refill reminder and the insufficient-water menu guard. */
	@ConfigSection(
		name = "Water",
		description = "Refill reminders and the insufficient-water guard.",
		position = 2
	)
	String waterSection = "water";

	/** Alerts that catch trouble while you are looking away: dying plants, missing tools, low run energy. */
	@ConfigSection(
		name = "Alerts",
		description = "Warnings for dying plants, missing tools, and low run energy.",
		position = 3
	)
	String alertsSection = "alerts";

	/** Colors of each kind of highlight, one per action, and of the warnings. */
	@ConfigSection(
		name = "Colors",
		description = "Highlight colors, one per action, and the warning color.",
		position = 4
	)
	String colorsSection = "colors";

	/** The rewards you are saving points toward, shown in their own goal box at the farm and its lobby. */
	@ConfigSection(
		name = "Reward goals",
		description = "Tick the rewards you are saving for. The goal box shows progress toward their combined cost; "
			+ "the notification fires when you can afford them all.",
		position = 5
	)
	String rewardsSection = "rewards";

	@Range(min = 1, max = 30)
	@ConfigItem(
		keyName = "cropCount",
		name = "Crops per run",
		description = "How many seeds you plant before looping back to water. Wiki routes are cut to this "
			+ "number (or extended automatically past their length); recording stops at it.",
		section = runSection,
		position = 0
	)
	default int cropCount()
	{
		return 20;
	}

	@ConfigItem(
		keyName = ROUTE_MODE,
		name = "Route",
		description = "Wiki routes follow the OSRS Wiki strategy guide, cut to your crop count. Automatic loops up "
			+ "one pair of columns and back down the next. Recorded uses the order you saved with \"Record route\".",
		section = guidanceSection,
		position = 0
	)
	default RouteMode routeMode()
	{
		return RouteMode.WIKI_BASIC;
	}

	@ConfigItem(
		keyName = RECORD_ROUTE,
		name = "Record route",
		description = "Turn on, then plant a run in the order you like. Once you have planted \"Crops per run\" "
			+ "seeds the order is saved, the route switches to Recorded, and this turns itself off.",
		section = guidanceSection,
		position = 1
	)
	default boolean recordRoute()
	{
		return false;
	}

	@ConfigItem(
		keyName = RECORDED_ROUTE,
		name = "Recorded route",
		description = "The saved planting order.",
		hidden = true
	)
	default String recordedRoute()
	{
		return "";
	}

	@ConfigItem(
		keyName = "highlightNextAction",
		name = "Highlight next action",
		description = "Light up the plot to click now and the four after it, brightest first, colored by what to do "
			+ "there. The seed or watering can to use glows with it.",
		section = guidanceSection,
		position = 2
	)
	default boolean highlightNextAction()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showStepNumbers",
		name = "Show step numbers",
		description = "Write each highlighted plot's place in the order (1 to 5) on its north-east tile, glowing with "
			+ "its border.",
		section = guidanceSection,
		position = 3
	)
	default boolean showStepNumbers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glowSpeed",
		name = "Glow",
		description = "How fast the highlights pulse. Solid turns the pulse off.",
		section = guidanceSection,
		position = 4
	)
	default GlowSpeed glowSpeed()
	{
		return GlowSpeed.MEDIUM;
	}

	@ConfigItem(
		keyName = "waterRefillWarning",
		name = "Refill reminder",
		description = "Warn when the water you carry will not finish the run. Between runs, light up every can "
			+ "that is not full and the water barrels, and hold off planting until the cans are full.",
		section = waterSection,
		position = 0
	)
	default boolean waterRefillWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyWhenLow",
		name = "Notify when low",
		description = "Fires once when your water drops below what the run still needs.",
		section = waterSection,
		position = 1
	)
	default Notification notifyWhenLow()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "blockPlantWhenShort",
		name = "Prevent planting (low water)",
		description = "Move \"Cancel\" to the top of the plant menu when your water would not finish the run: what "
			+ "your planted crops still need plus 3 for every seed still to plant. Never removes options.",
		section = waterSection,
		position = 2
	)
	default boolean blockPlantWhenShort()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "plantColor",
		name = "Plant",
		description = "Plots to plant, the seed to plant with, and the seed table.",
		section = colorsSection,
		position = 0
	)
	default Color plantColor()
	{
		return new Color(255, 212, 0, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "waterColor",
		name = "Water",
		description = "Plots to water, the watering can to use, and the cans and water barrels when a refill is due.",
		section = colorsSection,
		position = 1
	)
	default Color waterColor()
	{
		return new Color(75, 156, 255, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "harvestColor",
		name = "Harvest",
		description = "Plots ready to harvest.",
		section = colorsSection,
		position = 2
	)
	default Color harvestColor()
	{
		return new Color(46, 219, 90, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "clearColor",
		name = "Clear dead",
		description = "Dead plants to clear.",
		section = colorsSection,
		position = 3
	)
	default Color clearColor()
	{
		return new Color(255, 59, 59, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "depositColor",
		name = "Deposit 100+",
		description = "Your fruit and the sacks, once you carry 100 or more fruit.",
		section = colorsSection,
		position = 4
	)
	default Color depositColor()
	{
		return new Color(255, 163, 26, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "nextActionColor",
		name = "Other",
		description = "Anything else to click: the sacks when a full backpack needs a deposit mid-run, and the "
			+ "next-action text in the panel.",
		section = colorsSection,
		position = 5
	)
	default Color nextActionColor()
	{
		return new Color(255, 255, 255, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "warningColor",
		name = "Warning",
		description = "Color of the low-water and run-energy warnings.",
		section = colorsSection,
		position = 6
	)
	default Color warningColor()
	{
		return new Color(255, 0, 0, 200);
	}

	@ConfigItem(
		keyName = "showTimers",
		name = "Plant timers",
		description = "Show a countdown over every plant that needs water: the time left before it dies. Turns "
			+ "yellow, then red, as it gets close.",
		section = guidanceSection,
		position = 5
	)
	default boolean showTimers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyBeforeDeath",
		name = "Notify before a plant dies",
		description = "Fire a RuneLite notification when a plant is about to die for lack of water.",
		section = alertsSection,
		position = 0
	)
	default boolean notifyBeforeDeath()
	{
		return true;
	}

	@Range(min = 5, max = 50)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "deathWarnSeconds",
		name = "Warn this early",
		description = "How many seconds before a plant dies to warn about it.",
		section = alertsSection,
		position = 1
	)
	default int deathWarnSeconds()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "minimalView",
		name = "Minimal view",
		description = "Show only the next thing to click and the warnings that matter: no trail of plots after it, "
			+ "no inventory boxes, and a panel cut down to the next action.",
		section = guidanceSection,
		position = 6
	)
	default boolean minimalView()
	{
		return false;
	}

	@ConfigItem(
		keyName = "blockWrongPlant",
		name = "Prevent planting wrong plot",
		description = "Move \"Cancel\" to the top of the plant menu on any plot except the route's next one, so "
			+ "seeds only go in route order. Never removes options.",
		section = guidanceSection,
		position = 7
	)
	default boolean blockWrongPlant()
	{
		return false;
	}

	@ConfigItem(
		keyName = "blockOutOfOrderWater",
		name = "Prevent watering out of order",
		description = "While the next action is watering, move \"Cancel\" to the top of the menu on every other "
			+ "plant, so the highlighted plant is watered first. Never removes options.",
		section = guidanceSection,
		position = 8
	)
	default boolean blockOutOfOrderWater()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSession",
		name = "Show tonight's progress",
		description = "Show points earned, experience gained, and time spent this session in the panel.",
		section = runSection,
		position = 1
	)
	default boolean showSession()
	{
		return true;
	}

	@ConfigItem(
		keyName = "inventoryCheck",
		name = "Check my tools",
		description = "Warn in the panel when the spade, seed dibber, or watering can is missing, or when "
			+ "Gricoller's fertiliser is in your backpack.",
		section = alertsSection,
		position = 2
	)
	default boolean inventoryCheck()
	{
		return true;
	}

	@ConfigItem(
		keyName = "barehandedPlanting",
		name = "I plant barehanded",
		description = "Turn on if you finished the Barbarian Training farming step, so a missing seed dibber is "
			+ "not flagged.",
		section = alertsSection,
		position = 3
	)
	default boolean barehandedPlanting()
	{
		return false;
	}

	@Range(max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "lowEnergyPercent",
		name = "Low run energy below",
		description = "Warn in the panel, and box any energy or stamina potion you carry, when run energy drops "
			+ "below this. 0 turns the warning off.",
		section = alertsSection,
		position = 4
	)
	default int lowEnergyPercent()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "goalNotification",
		name = "Goal notification",
		description = "Fires once when your points reach the combined cost of every ticked reward.",
		section = rewardsSection,
		position = 0
	)
	default Notification goalNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "showPointsBar",
		name = "Show points bar",
		description = "Show the points progress bar in the goal box.",
		section = rewardsSection,
		position = 1
	)
	default boolean showPointsBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRunsLeft",
		name = "Show runs left",
		description = "Show roughly how many runs of your crop count are left until you can afford the goal.",
		section = rewardsSection,
		position = 2
	)
	default boolean showRunsLeft()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackStrawhat",
		name = "Farmer's strawhat",
		description = "Include Farmer's strawhat in the goal.",
		section = rewardsSection,
		position = 3
	)
	default boolean trackStrawhat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackJacket",
		name = "Farmer's jacket",
		description = "Include Farmer's jacket in the goal.",
		section = rewardsSection,
		position = 4
	)
	default boolean trackJacket()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackTrousers",
		name = "Farmer's boro trousers",
		description = "Include Farmer's boro trousers in the goal.",
		section = rewardsSection,
		position = 5
	)
	default boolean trackTrousers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackBoots",
		name = "Farmer's boots",
		description = "Include Farmer's boots in the goal.",
		section = rewardsSection,
		position = 6
	)
	default boolean trackBoots()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackSeedBox",
		name = "Seed box",
		description = "Include Seed box in the goal.",
		section = rewardsSection,
		position = 7
	)
	default boolean trackSeedBox()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackHerbSack",
		name = "Herb sack",
		description = "Include Herb sack in the goal.",
		section = rewardsSection,
		position = 8
	)
	default boolean trackHerbSack()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackGricollersCan",
		name = "Gricoller's can",
		description = "Include Gricoller's can in the goal.",
		section = rewardsSection,
		position = 9
	)
	default boolean trackGricollersCan()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackAutoWeed",
		name = "Auto-weed",
		description = "Include Auto-weed in the goal.",
		section = rewardsSection,
		position = 10
	)
	default boolean trackAutoWeed()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackCompost",
		name = "Compost",
		description = "Include Compost in the goal.",
		section = rewardsSection,
		position = 11
	)
	default boolean trackCompost()
	{
		return false;
	}

	@Range(min = 1, max = Goal.POINTS_CAP)
	@ConfigItem(
		keyName = "compostQuantity",
		name = "  Compost qty",
		description = "How many to include when ticked.",
		section = rewardsSection,
		position = 12
	)
	default int compostQuantity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "trackSupercompost",
		name = "Supercompost",
		description = "Include Supercompost in the goal.",
		section = rewardsSection,
		position = 13
	)
	default boolean trackSupercompost()
	{
		return false;
	}

	@Range(min = 1, max = Goal.POINTS_CAP)
	@ConfigItem(
		keyName = "supercompostQuantity",
		name = "  Supercompost qty",
		description = "How many to include when ticked.",
		section = rewardsSection,
		position = 14
	)
	default int supercompostQuantity()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "trackGrapeSeed",
		name = "Grape seed",
		description = "Include Grape seed in the goal.",
		section = rewardsSection,
		position = 15
	)
	default boolean trackGrapeSeed()
	{
		return false;
	}

	@Range(min = 1, max = Goal.POINTS_CAP)
	@ConfigItem(
		keyName = "grapeSeedQuantity",
		name = "  Grape seed qty",
		description = "How many to include when ticked.",
		section = rewardsSection,
		position = 16
	)
	default int grapeSeedQuantity()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "trackBologasBlessing",
		name = "Bologa's blessing",
		description = "Include Bologa's blessing (stacks of 20) in the goal.",
		section = rewardsSection,
		position = 17
	)
	default boolean trackBologasBlessing()
	{
		return false;
	}

	@Range(min = 1, max = Goal.POINTS_CAP)
	@ConfigItem(
		keyName = "bologasBlessingQuantity",
		name = "  Bologa's blessing qty",
		description = "How many stacks of 20 to include when ticked.",
		section = rewardsSection,
		position = 18
	)
	default int bologasBlessingQuantity()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "trackHerbBox",
		name = "Herb box",
		description = "Include Herb box in the goal.",
		section = rewardsSection,
		position = 19
	)
	default boolean trackHerbBox()
	{
		return false;
	}

	@Range(min = 1, max = Goal.POINTS_CAP)
	@ConfigItem(
		keyName = "herbBoxQuantity",
		name = "  Herb box qty",
		description = "How many to include when ticked.",
		section = rewardsSection,
		position = 20
	)
	default int herbBoxQuantity()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "trackSeedPack",
		name = "Seed pack",
		description = "Include Seed pack in the goal.",
		section = rewardsSection,
		position = 21
	)
	default boolean trackSeedPack()
	{
		return false;
	}

	@Range(min = 1, max = Goal.POINTS_CAP)
	@ConfigItem(
		keyName = "seedPackQuantity",
		name = "  Seed pack qty",
		description = "How many to include when ticked.",
		section = rewardsSection,
		position = 22
	)
	default int seedPackQuantity()
	{
		return 5;
	}
}
