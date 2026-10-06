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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Checks the trail fade, the glow pulse, color scaling, the plant cut out of a patch highlight, and the step
 * markers: number text and blips.
 */
public class HighlightStyleTest
{
	private static final double EPSILON = 1e-9;

	@Test
	public void trailFadesEvenlyToTenPercent()
	{
		assertEquals(1.0, HighlightStyle.fade(0), EPSILON);
		assertEquals(0.775, HighlightStyle.fade(1), EPSILON);
		assertEquals(0.55, HighlightStyle.fade(2), EPSILON);
		assertEquals(0.325, HighlightStyle.fade(3), EPSILON);
		assertEquals(0.10, HighlightStyle.fade(4), EPSILON);
		assertEquals(0.10, HighlightStyle.fade(9), EPSILON);
	}

	@Test
	public void pulseRisesAndFallsOncePerPeriod()
	{
		assertEquals(0.0, HighlightStyle.pulse(0, GlowSpeed.MEDIUM), EPSILON);
		assertEquals(1.0, HighlightStyle.pulse(600, GlowSpeed.MEDIUM), EPSILON);
		assertEquals(0.0, HighlightStyle.pulse(1200, GlowSpeed.MEDIUM), EPSILON);
		assertEquals(1.0, HighlightStyle.pulse(1200, GlowSpeed.SLOW), EPSILON);
		assertEquals(1.0, HighlightStyle.pulse(300, GlowSpeed.FAST), EPSILON);
	}

	@Test
	public void solidNeverPulses()
	{
		assertEquals(1.0, HighlightStyle.pulse(0, GlowSpeed.SOLID), EPSILON);
		assertEquals(1.0, HighlightStyle.pulse(777, GlowSpeed.SOLID), EPSILON);
	}

	@Test
	public void scaleKeepsTheHueAndScalesAlpha()
	{
		Color scaled = HighlightStyle.scale(new Color(255, 212, 0, 200), 0.5);
		assertEquals(new Color(255, 212, 0, 100), scaled);
		assertEquals(0, HighlightStyle.scale(Color.WHITE, -1).getAlpha());
		assertEquals(255, HighlightStyle.scale(Color.WHITE, 2).getAlpha());
	}

	@Test
	public void textIsDrawnOverADarkShadowAndFadesWithTheStrength()
	{
		Color color = new Color(255, 212, 0);
		BufferedImage image = textImage(color, 1);
		assertTrue(contains(image, color.getRGB()));
		assertTrue(contains(image, Color.BLACK.getRGB()));
		assertTrue(visible(image));
		assertFalse(visible(textImage(color, 0)));
	}

	@Test
	public void nothingHiddenKeepsTheShapeWhole()
	{
		Shape patch = new Rectangle(0, 0, 20, 20);
		assertSame(patch, HighlightStyle.without(patch, null));
	}

	@Test
	public void theHiddenPartIsCutOutOfTheShape()
	{
		Shape left = HighlightStyle.without(new Rectangle(0, 0, 20, 20), new Rectangle(5, 5, 10, 10));
		assertTrue(left.contains(2, 2));
		assertFalse(left.contains(10, 10));
	}

	@Test
	public void aPlantCoversItsPatchsFillAndBorder()
	{
		Color color = new Color(46, 219, 90);
		BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		HighlightStyle.draw(graphics, new Rectangle(2, 2, 20, 20), new Rectangle(14, 0, 10, 24), color, 1, true);
		graphics.dispose();

		assertEquals(color.getRGB(), image.getRGB(2, 12));
		assertTrue((image.getRGB(6, 12) >>> 24) != 0);
		assertEquals(0, image.getRGB(22, 12) >>> 24);
		assertEquals(0, image.getRGB(18, 12) >>> 24);
	}

	@Test
	public void blipsAreARowCentredOnTheMarkerPoint()
	{
		assertArrayEquals(new int[]{48}, HighlightStyle.blipLefts(1, 50));
		assertArrayEquals(new int[]{45, 51}, HighlightStyle.blipLefts(2, 50));
		assertArrayEquals(new int[]{36, 42, 48, 54, 60}, HighlightStyle.blipLefts(5, 50));
		assertArrayEquals(new int[0], HighlightStyle.blipLefts(0, 50));
	}

	@Test
	public void blipsAreDrawnOverADarkShadowAndFadeWithTheStrength()
	{
		Color color = new Color(255, 212, 0);
		BufferedImage image = blipImage(color, 1);
		assertTrue(contains(image, color.getRGB()));
		assertTrue(contains(image, Color.BLACK.getRGB()));
		assertFalse(visible(blipImage(color, 0)));
	}

	/** A small transparent image with three blips drawn on it at the given strength. */
	private static BufferedImage blipImage(Color color, double strength)
	{
		BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		HighlightStyle.drawBlips(graphics, 3, 12, 12, color, strength);
		graphics.dispose();
		return image;
	}

	/** A small transparent image with {@code 8} written on it at the given strength. */
	private static BufferedImage textImage(Color color, double strength)
	{
		BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		HighlightStyle.drawText(graphics, "8", 4, 18, color, strength);
		graphics.dispose();
		return image;
	}

	/** Whether any pixel of the image is exactly the given ARGB value. */
	private static boolean contains(BufferedImage image, int argb)
	{
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if (image.getRGB(x, y) == argb)
					return true;
			}
		}

		return false;
	}

	/** Whether any pixel of the image is not fully transparent. */
	private static boolean visible(BufferedImage image)
	{
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
					return true;
			}
		}

		return false;
	}
}
