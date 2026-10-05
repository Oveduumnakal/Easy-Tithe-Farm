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

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Drives route recording through a mocked config manager. */
public class RouteRecorderMockTest
{
	private TitheFarmConfig config;
	private ConfigManager configManager;
	private RouteRecorder recorder;

	@Before
	public void setUp()
	{
		config = TestRuns.defaultConfig();
		configManager = mock(ConfigManager.class);
		when(config.recordRoute()).thenReturn(true);
		when(config.cropCount()).thenReturn(2);
		recorder = new RouteRecorder(config, configManager);
	}

	private static ConfigChanged toggle(boolean on)
	{
		ConfigChanged event = new ConfigChanged();
		event.setGroup(TitheFarmConfig.GROUP);
		event.setKey(TitheFarmConfig.RECORD_ROUTE);
		event.setNewValue(String.valueOf(on));
		return event;
	}

	@Test
	public void savesAndSwitchesToRecordedOnceTheCropCountIsPlanted()
	{
		recorder.onConfigChanged(toggle(true));
		recorder.onPlanted(new WorldPoint(1811, 3489, 0));
		recorder.onPlanted(new WorldPoint(1811, 3489, 0));
		verify(configManager, never()).setConfiguration(eq(TitheFarmConfig.GROUP), eq(TitheFarmConfig.RECORDED_ROUTE),
			anyString());
		recorder.onPlanted(new WorldPoint(1816, 3489, 0));
		verify(configManager).setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.RECORDED_ROUTE,
			"1811,3489;1816,3489");
		verify(configManager).setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.ROUTE_MODE, RouteMode.RECORDED);
		verify(configManager).setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.RECORD_ROUTE, false);
		assertEquals(0, recorder.getRecording().size());
	}

	@Test
	public void turningRecordingOffEarlySavesWhatWasPlanted()
	{
		recorder.onConfigChanged(toggle(true));
		recorder.onPlanted(new WorldPoint(1821, 3504, 0));
		recorder.onConfigChanged(toggle(false));
		verify(configManager).setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.RECORDED_ROUTE, "1821,3504");
	}

	@Test
	public void ignoresPlantingWhileNotRecording()
	{
		when(config.recordRoute()).thenReturn(false);
		recorder.onPlanted(new WorldPoint(1811, 3489, 0));
		assertEquals(0, recorder.getRecording().size());
	}

	@Test
	public void decodesTheSavedRouteFromConfig()
	{
		when(config.recordedRoute()).thenReturn("1,2;3,4");
		assertEquals(2, recorder.getSaved().size());
	}
}
