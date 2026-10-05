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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * The reward goal box: the first ticked reward's icon, centered beside the goal's name, progress, and runs left;
 * a points bar against the combined cost; and a warning when the goal is over the points cap. The points the
 * carried fruit would earn once deposited show as a {@code (+n)} after the progress and the points, and as a
 * second color in the bar after the points already banked. Drawn at the farm
 * and its lobby whenever a reward is ticked, in the top-left corner under the run panel.
 *
 * <p>Laid out by hand in one pass rather than with nested overlay components, so the box appears at its full
 * size on the first frame instead of growing into it, and labels sit right beside their values.
 */
class RewardGoalOverlay extends Overlay
{
	private static final int BORDER = ComponentConstants.STANDARD_BORDER;
	private static final int MIN_WIDTH = ComponentConstants.STANDARD_WIDTH;
	private static final int ICON_COLUMN = 36;
	private static final int ICON_GAP = 6;
	private static final int LABEL_GAP = 4;
	private static final int LINE_GAP = 1;
	private static final int BAR_GAP = 6;
	private static final int BAR_HEIGHT = 16;
	private static final int BAR_PADDING = 3;
	private static final Color BAR_COLOR = new Color(216, 177, 58);
	private static final Color BAR_BACKGROUND = new Color(61, 56, 49);
	private static final Color BAR_PENDING = new Color(110, 190, 90);

	private final TitheFarmConfig config;
	private final GoalTracker goals;
	private final ItemManager itemManager;
	private final TextComponent text = new TextComponent();

	private int cachedIconId = -1;
	private BufferedImage cachedIcon;

	@Inject
	RewardGoalOverlay(TitheFarmConfig config, GoalTracker goals, ItemManager itemManager)
	{
		this.config = config;
		this.goals = goals;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Goal goal = goals.getGoal();
		if (!goals.isInRegion() || goal.isEmpty())
			return null;

		FontMetrics metrics = graphics.getFontMetrics();
		int lineHeight = metrics.getHeight();
		List<Line> lines = lines(goal);
		int textWidth = 0;
		for (Line line : lines)
			textWidth = Math.max(textWidth, line.width(metrics));

		String capWarning = goal.isOverCap()
			? "Over the " + String.format("%,d", Goal.POINTS_CAP) + " point cap"
			: null;
		int contentWidth = ICON_COLUMN + ICON_GAP + textWidth;
		if (capWarning != null)
			contentWidth = Math.max(contentWidth, metrics.stringWidth(capWarning));

		int width = Math.max(MIN_WIDTH, contentWidth + 2 * BORDER);
		contentWidth = width - 2 * BORDER;

		BufferedImage icon = icon(goal.getIconItemId());
		int textHeight = lines.size() * lineHeight + (lines.size() - 1) * LINE_GAP;
		int iconHeight = icon == null ? 0 : icon.getHeight();
		int rowHeight = Math.max(textHeight, iconHeight);
		int height = BORDER + rowHeight;
		if (config.showPointsBar())
			height += BAR_GAP + BAR_HEIGHT;

		if (capWarning != null)
			height += BAR_GAP + lineHeight;

		height += BORDER;
		new BackgroundComponent(ComponentConstants.STANDARD_BACKGROUND_COLOR, new Rectangle(0, 0, width, height), true)
			.render(graphics);

		if (icon != null)
		{
			int iconX = BORDER + (ICON_COLUMN - icon.getWidth()) / 2;
			graphics.drawImage(icon, iconX, BORDER + (rowHeight - iconHeight) / 2, null);
		}

		int textX = BORDER + ICON_COLUMN + ICON_GAP;
		int baseline = BORDER + (rowHeight - textHeight) / 2 + metrics.getAscent();
		for (Line line : lines)
		{
			line.draw(graphics, metrics, textX, baseline);
			baseline += lineHeight + LINE_GAP;
		}

		int y = BORDER + rowHeight;
		if (config.showPointsBar())
		{
			y += BAR_GAP;
			drawBar(graphics, metrics, goal, new Rectangle(BORDER, y, contentWidth, BAR_HEIGHT));
			y += BAR_HEIGHT;
		}

		if (capWarning != null)
			drawText(graphics, capWarning, BORDER, y + BAR_GAP + metrics.getAscent(), config.warningColor());

		return new Dimension(width, height);
	}

	/** The name, progress, and runs-left lines stacked beside the icon. */
	private List<Line> lines(Goal goal)
	{
		List<Line> lines = new ArrayList<>();
		lines.add(new Line(goal.getDisplayName(), goal.getCountText(), Color.WHITE));
		String progress = percent(goal.getProgress());
		if (goal.getPending() > 0)
			progress += " (+" + percent(goal.getPendingProgress() - goal.getProgress()) + ")";

		lines.add(new Line("Progress:", progress, goal.isAffordable() ? Color.GREEN : Color.WHITE));
		if (config.showRunsLeft())
		{
			int runs = goal.getRunsLeft();
			lines.add(new Line("Runs left:", runs == 0 ? "ready!" : "~" + runs, runs == 0 ? Color.GREEN : Color.WHITE));
		}

		return lines;
	}

	/**
	 * The points bar: points on the left (with the carried fruit's points after them), the total on the right,
	 * what is still to earn in the middle. Banked points fill in gold; the carried fruit's points follow in green.
	 */
	private void drawBar(Graphics2D graphics, FontMetrics metrics, Goal goal, Rectangle bar)
	{
		int filled = (int) (bar.width * goal.getProgress());
		int pending = (int) (bar.width * goal.getPendingProgress()) - filled;
		graphics.setColor(BAR_BACKGROUND);
		graphics.fillRect(bar.x, bar.y, bar.width, bar.height);
		graphics.setColor(BAR_COLOR);
		graphics.fillRect(bar.x, bar.y, filled, bar.height);
		graphics.setColor(BAR_PENDING);
		graphics.fillRect(bar.x + filled, bar.y, pending, bar.height);

		int baseline = bar.y + (bar.height - metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getAscent();
		String right = String.valueOf(goal.getTotal());
		String left = goal.getPending() > 0
			? goal.getPoints() + " (+" + goal.getPending() + ")"
			: String.valueOf(goal.getPoints());
		drawText(graphics, left, bar.x + BAR_PADDING, baseline, Color.WHITE);
		drawText(graphics, right, bar.x + bar.width - BAR_PADDING - metrics.stringWidth(right), baseline,
			Color.WHITE);
		if (goal.getRemaining() > 0)
		{
			String center = "(" + goal.getRemaining() + ")";
			drawText(graphics, center, bar.x + (bar.width - metrics.stringWidth(center)) / 2, baseline, Color.WHITE);
		}
	}

	/** Draws shadowed overlay text with its baseline at the given point. */
	private void drawText(Graphics2D graphics, String value, int x, int baseline, Color color)
	{
		text.setText(value);
		text.setColor(color);
		text.setPosition(new Point(x, baseline));
		text.render(graphics);
	}

	/** The icon of the given item, cached until the goal's first reward changes. */
	private BufferedImage icon(int itemId)
	{
		if (itemId != cachedIconId)
		{
			cachedIconId = itemId;
			cachedIcon = itemManager.getImage(itemId);
		}

		return cachedIcon;
	}

	/**
	 * Progress as a percentage rounded down to one decimal, so 100% shows only once the goal is affordable.
	 *
	 * @param progress progress from 0 to 1
	 * @return the text, e.g. {@code 49.9%}
	 */
	static String percent(double progress)
	{
		return String.format("%.1f%%", Math.floor(progress * 1000) / 10);
	}

	/** One text line: a white label and, a small gap after it, a value in its own color. */
	private final class Line
	{
		private final String label;
		private final String value;
		private final Color valueColor;

		Line(String label, String value, Color valueColor)
		{
			this.label = label;
			this.value = value;
			this.valueColor = valueColor;
		}

		/** The line's width in the given font. */
		int width(FontMetrics metrics)
		{
			int width = metrics.stringWidth(label);
			return value.isEmpty() ? width : width + LABEL_GAP + metrics.stringWidth(value);
		}

		/** Draws the line with its baseline at the given point. */
		void draw(Graphics2D graphics, FontMetrics metrics, int x, int baseline)
		{
			drawText(graphics, label, x, baseline, Color.WHITE);
			if (!value.isEmpty())
				drawText(graphics, value, x + metrics.stringWidth(label) + LABEL_GAP, baseline, valueColor);
		}
	}
}
