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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the highlight and timer overlays stay silent when they should, without touching the run. */
public class OverlayGatingTest
{
	private TitheFarmConfig config;
	private TithePlotTracker tracker;
	private TitheRun run;
	private Graphics2D graphics;

	@Before
	public void setUp()
	{
		config = TestRuns.defaultConfig();
		tracker = mock(TithePlotTracker.class);
		run = mock(TitheRun.class);
		graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		when(tracker.inTitheFarm()).thenReturn(true);
	}

	@Test
	public void nothingIsDrawnOutsideTheFarm()
	{
		when(tracker.inTitheFarm()).thenReturn(false);
		assertNull(new TitheHighlightOverlay(config, tracker, run).render(graphics));
		assertNull(new TitheTimerOverlay(config, tracker, run).render(graphics));
		verify(run, never()).snapshot();
	}

	@Test
	public void timersCanBeTurnedOff()
	{
		when(config.showTimers()).thenReturn(false);
		assertNull(new TitheTimerOverlay(config, tracker, run).render(graphics));
		verify(run, never()).snapshot();
	}
}
