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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks the trail fade, the glow pulse, and color scaling. */
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
}
