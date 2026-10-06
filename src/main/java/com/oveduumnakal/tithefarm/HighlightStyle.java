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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

/**
 * The strength math behind the highlights: how much each step of the trail fades, how the glow pulses, and how a
 * strength scales a color. Pure and static so it can be unit-tested without a client.
 */
final class HighlightStyle
{
	/** Strength of the last trail step; the steps between fade evenly down to it from full strength. */
	static final double LAST_STEP = 0.10;

	/** Share of the border's strength the fill gets, so the shape reads as tinted rather than painted over. */
	static final double FILL_SHARE = 0.25;

	/** Diameter in pixels of one step-marker blip. */
	static final int BLIP_SIZE = 4;

	/** Pixels between neighbouring blips. */
	static final int BLIP_GAP = 2;

	/** The border every highlight is drawn with. */
	private static final Stroke BORDER = new BasicStroke(2f);

	private HighlightStyle()
	{
	}

	/**
	 * The strength of one step of the trail: 1 for the current target, falling evenly to {@link #LAST_STEP} at
	 * the last of {@link ActionForecast#TRAIL_LENGTH} steps.
	 *
	 * @param step the step, 0 for the current target
	 * @return the strength 0 to 1
	 */
	static double fade(int step)
	{
		int last = ActionForecast.TRAIL_LENGTH - 1;
		if (step <= 0)
			return 1;

		if (step >= last)
			return LAST_STEP;

		return 1 - step * (1 - LAST_STEP) / last;
	}

	/**
	 * The glow at a moment in time: rises from 0 to 1 and back once per period, easing in and out. Always 1 for
	 * {@link GlowSpeed#SOLID}.
	 *
	 * @param millis the current time in milliseconds
	 * @param speed  the pulse speed
	 * @return the strength 0 to 1
	 */
	static double pulse(long millis, GlowSpeed speed)
	{
		int period = speed.getPeriodMillis();
		if (period <= 0)
			return 1;

		double phase = (double) (millis % period) / period;
		return 0.5 - 0.5 * Math.cos(2 * Math.PI * phase);
	}

	/**
	 * Fills a shape with a light tint of the color and borders it, both at the given strength. Draws nothing for
	 * a missing shape or a strength that leaves the color fully transparent.
	 *
	 * @param graphics the graphics to draw on
	 * @param shape    the shape, or {@code null}
	 * @param color    the base color
	 * @param strength the strength 0 to 1
	 */
	static void draw(Graphics2D graphics, Shape shape, Color color, double strength)
	{
		draw(graphics, shape, color, strength, true);
	}

	/**
	 * Borders a shape in the color at the given strength, and fills it with a light tint of the color when asked.
	 * The trail fills only the current target, so it stands apart from the plots after it.
	 *
	 * @param graphics the graphics to draw on
	 * @param shape    the shape, or {@code null}
	 * @param color    the base color
	 * @param strength the strength 0 to 1
	 * @param fill     whether to tint the inside as well as draw the border
	 */
	static void draw(Graphics2D graphics, Shape shape, Color color, double strength, boolean fill)
	{
		Color edge = scale(color, strength);
		if (shape == null || edge.getAlpha() == 0)
			return;

		if (fill)
		{
			graphics.setColor(scale(edge, FILL_SHARE));
			graphics.fill(shape);
		}

		graphics.setColor(edge);
		graphics.setStroke(BORDER);
		graphics.draw(shape);
	}

	/**
	 * Writes text in the color at the given strength over a thin dark shadow that fades with it, so the text
	 * stays readable over a tinted fill. Draws nothing for a strength that leaves the color fully transparent.
	 *
	 * @param graphics the graphics to draw on, with its font already set
	 * @param text     the text
	 * @param x        the x of the text's baseline start
	 * @param y        the y of the text's baseline
	 * @param color    the base color
	 * @param strength the strength 0 to 1
	 */
	static void drawText(Graphics2D graphics, String text, int x, int y, Color color, double strength)
	{
		Color face = scale(color, strength);
		if (face.getAlpha() == 0)
			return;

		graphics.setColor(new Color(0, 0, 0, face.getAlpha()));
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(face);
		graphics.drawString(text, x, y);
	}

	/**
	 * Draws a centred row of small dots in the color at the given strength, each over a thin dark shadow that
	 * fades with it, like {@link #drawText}. Draws nothing for a strength that leaves the color fully transparent.
	 *
	 * @param graphics the graphics to draw on
	 * @param count    how many dots
	 * @param centreX  the x the row is centred on
	 * @param centreY  the y the row is centred on
	 * @param color    the base color
	 * @param strength the strength 0 to 1
	 */
	static void drawBlips(Graphics2D graphics, int count, int centreX, int centreY, Color color, double strength)
	{
		Color face = scale(color, strength);
		if (face.getAlpha() == 0)
			return;

		int top = centreY - BLIP_SIZE / 2;
		Color shadow = new Color(0, 0, 0, face.getAlpha());
		for (int left : blipLefts(count, centreX))
		{
			graphics.setColor(shadow);
			graphics.fillOval(left + 1, top + 1, BLIP_SIZE, BLIP_SIZE);
			graphics.setColor(face);
			graphics.fillOval(left, top, BLIP_SIZE, BLIP_SIZE);
		}
	}

	/**
	 * The left edge of each dot in a row of blips centred on an x.
	 *
	 * @param count   how many dots
	 * @param centreX the x the row is centred on
	 * @return the left x of each dot, left to right; empty for no dots
	 */
	static int[] blipLefts(int count, int centreX)
	{
		int[] lefts = new int[Math.max(0, count)];
		int width = lefts.length * BLIP_SIZE + Math.max(0, lefts.length - 1) * BLIP_GAP;
		int first = centreX - width / 2;
		for (int i = 0; i < lefts.length; i++)
			lefts[i] = first + i * (BLIP_SIZE + BLIP_GAP);

		return lefts;
	}

	/**
	 * A color with its alpha scaled by a strength.
	 *
	 * @param color    the base color
	 * @param strength the strength 0 to 1
	 * @return the color at that strength
	 */
	static Color scale(Color color, double strength)
	{
		double clamped = Math.max(0, Math.min(1, strength));
		int alpha = (int) Math.round(color.getAlpha() * clamped);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}
