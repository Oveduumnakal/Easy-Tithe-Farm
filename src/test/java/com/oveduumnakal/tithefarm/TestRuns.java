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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mockito.Answers;

import net.runelite.api.GameObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Shared builders for tests that need plots, snapshots, or a config with its real defaults. */
final class TestRuns
{
	private TestRuns()
	{
	}

	/** A config mock that answers every setting with its declared default. */
	static TitheFarmConfig defaultConfig()
	{
		return mock(TitheFarmConfig.class, Answers.CALLS_REAL_METHODS);
	}

	/** A plot object mock with the given id and hash. */
	static GameObject plot(int id, long hash)
	{
		GameObject plot = mock(GameObject.class);
		when(plot.getId()).thenReturn(id);
		when(plot.getHash()).thenReturn(hash);
		return plot;
	}

	/** A snapshot over the given plots and their infos, with otherwise neutral values. */
	static RunSnapshot snapshot(List<GameObject> route, List<PlotInfo> plots, ActionAdvisor.Advice advice)
	{
		return new RunSnapshot(route, route.size(), plots, 60, 0, 0, true, advice, false, 0, RunStatus.NEUTRAL,
			Collections.emptyList());
	}

	/** One-element lists, for the common single-plot case. */
	static <T> List<T> listOf(T value)
	{
		List<T> list = new ArrayList<>();
		list.add(value);
		return list;
	}
}
