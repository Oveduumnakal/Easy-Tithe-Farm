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
import net.runelite.client.config.Range;

/** Settings for the Tithe Farm helper: crop count, what guidance is drawn, water guards, and colors. */
@ConfigGroup(TitheFarmConfig.GROUP)
public interface TitheFarmConfig extends Config
{
	/** Config group key, shared with the plugin so both agree on where settings are stored. */
	String GROUP = "tithefarm";

	/** How many crops to run at a time — drives the route length and the water math. */
	@ConfigSection(
		name = "Run",
		description = "How many crops to grow per run.",
		position = 0
	)
	String runSection = "run";

	/** Which guidance overlays are drawn: the static route and the live next-action highlight. */
	@ConfigSection(
		name = "Guidance",
		description = "The planting route and the next-action highlight.",
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

	/** Colors of the route, the highlight, and the water warnings. */
	@ConfigSection(
		name = "Colors",
		description = "Colors of the route, the highlight, and the warnings.",
		position = 3
	)
	String colorsSection = "colors";

	@Range(min = 1, max = 30)
	@ConfigItem(
		keyName = "cropCount",
		name = "Crops per run",
		description = "How many seeds you plant before looping back to water. The route and the water check "
			+ "both size themselves to this number.",
		section = runSection,
		position = 0
	)
	default int cropCount()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "showRoute",
		name = "Show planting route",
		description = "Draw the numbered plot layout and the order to plant and water them in.",
		section = guidanceSection,
		position = 0
	)
	default boolean showRoute()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightNextAction",
		name = "Highlight next action",
		description = "Highlight the single next thing to click — the next plot, the plant to water, a seed, "
			+ "or the water barrel.",
		section = guidanceSection,
		position = 1
	)
	default boolean highlightNextAction()
	{
		return true;
	}

	@ConfigItem(
		keyName = "waterRefillWarning",
		name = "Refill reminder",
		description = "Warn and highlight the water barrel when you do not have enough water to finish the run.",
		section = waterSection,
		position = 0
	)
	default boolean waterRefillWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyOnRefill",
		name = "Notify when low",
		description = "Fire a RuneLite notification when your water drops below what the run needs.",
		section = waterSection,
		position = 1
	)
	default boolean notifyOnRefill()
	{
		return false;
	}

	@ConfigItem(
		keyName = "blockPlantWhenShort",
		name = "Guard planting when low",
		description = "When you do not have enough water for the whole run, move \"Cancel\" to the top of the "
			+ "menu so a stray click cannot plant a seed you cannot finish watering. Never removes options.",
		section = waterSection,
		position = 2
	)
	default boolean blockPlantWhenShort()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "routeColor",
		name = "Route",
		description = "Color of the planting route and its plot numbers.",
		section = colorsSection,
		position = 0
	)
	default Color routeColor()
	{
		return new Color(255, 255, 255, 160);
	}

	@Alpha
	@ConfigItem(
		keyName = "nextActionColor",
		name = "Next action",
		description = "Color of the single next-action highlight.",
		section = colorsSection,
		position = 1
	)
	default Color nextActionColor()
	{
		return new Color(0, 255, 0, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "warningColor",
		name = "Warning",
		description = "Color of the low-water warning and the water barrel highlight.",
		section = colorsSection,
		position = 2
	)
	default Color warningColor()
	{
		return new Color(255, 60, 60, 200);
	}
}
