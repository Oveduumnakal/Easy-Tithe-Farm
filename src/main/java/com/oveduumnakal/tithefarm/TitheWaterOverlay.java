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
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A small panel with the next action, the water carried against what the run still needs, warnings, the points
 * with what the carried fruit would add, tonight's time and experience, and the progress of a route recording.
 * Drawn only at the Tithe Farm. The next-action line
 * doubles as a hint when the target is not in view, such as the seed table outside the farm. In minimal view the
 * panel keeps only the next action, the last-run line, a recording in progress, and the red warnings.
 */
class TitheWaterOverlay extends OverlayPanel
{
	private final TitheFarmConfig config;
	private final TithePlotTracker plotTracker;
	private final TitheRun run;
	private final SessionTracker session;

	@Inject
	TitheWaterOverlay(TitheFarmConfig config, TithePlotTracker plotTracker, TitheRun run, SessionTracker session)
	{
		this.config = config;
		this.plotTracker = plotTracker;
		this.run = run;
		this.session = session;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plotTracker.inTitheFarm())
			return null;

		RunSnapshot snapshot = run.snapshot();
		boolean warn = config.waterRefillWarning() && snapshot.isShort();
		Color waterColor = warn ? config.warningColor() : Color.WHITE;
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Tithe Farm")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Next")
			.right(nextText(snapshot))
			.build());
		RunStatus status = snapshot.getStatus();
		boolean minimal = config.minimalView();

		if (status.isWrapUp())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Last run")
				.right(wrapUpText(snapshot))
				.rightColor(config.nextActionColor())
				.build());
		}

		if (!minimal || warn)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Water")
				.right(snapshot.getWater() + "/" + snapshot.getRunNeed())
				.leftColor(waterColor)
				.rightColor(waterColor)
				.build());
		}

		if (snapshot.isRecording())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Recording")
				.right(snapshot.getRecordedCount() + " / " + config.cropCount())
				.rightColor(config.nextActionColor())
				.build());
		}

		addWarnings(status);
		if (!minimal)
			addSession(status);

		return super.render(graphics);
	}

	/** Red lines for missing tools, a carried fertiliser, and low run energy. */
	private void addWarnings(RunStatus status)
	{
		if (status.isEnergyLow(config.lowEnergyPercent()))
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Run energy")
				.right(status.getEnergyPercent() + "%")
				.leftColor(config.warningColor())
				.rightColor(config.warningColor())
				.build());
		}

		if (!status.getMissingTools().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Missing")
				.right(String.join(", ", status.getMissingTools()))
				.leftColor(config.warningColor())
				.rightColor(config.warningColor())
				.build());
		}

		if (status.hasFertiliser())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Fertiliser")
				.right("drop it for wiki routes")
				.leftColor(config.warningColor())
				.rightColor(config.warningColor())
				.build());
		}
	}

	/**
	 * The points and tonight's time and experience, each with what the carried fruit would add once deposited,
	 * when enabled. The reward goal has its own box.
	 */
	private void addSession(RunStatus status)
	{
		if (!config.showSession())
			return;

		int points = session.points();
		int pending = Math.min(DepositRewards.points(status.getDeposited(), status.getCarried()),
			Math.max(0, Goal.POINTS_CAP - points));
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Points")
			.right(pending > 0 ? points + " (+" + pending + ")" : String.valueOf(points))
			.build());
		String xp = SessionTracker.compactXp(session.xpTonight());
		if (status.getPendingXp() > 0)
			xp += " (+" + SessionTracker.compactXp(status.getPendingXp()) + ")";

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Tonight")
			.right(TitheTime.format(session.ticksInFarm()) + "  " + xp + " xp")
			.build());
	}

	/** What the last run still asks for: a few more seeds, finishing up, or leaving. */
	private static String wrapUpText(RunSnapshot snapshot)
	{
		if (snapshot.getAdvice().getAction() == NextAction.LEAVE)
			return "done, leave";

		int seeds = snapshot.getStatus().getPlantLimit();
		return seeds > 0 ? "plant " + seeds + " more" : "finish up";
	}

	/** The next-action word, with the route number for a plot action. */
	private static String nextText(RunSnapshot snapshot)
	{
		NextAction action = snapshot.getAdvice().getAction();
		String label = action.getLabel();
		int number = snapshot.getTargetNumber();
		return number > 0 ? label + " " + number : label;
	}
}
