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
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

/**
 * Records the order the player plants in and saves it as their route.
 *
 * <p>While "Record route" is on, every seed that goes into an empty plot appends that plot's template tile.
 * Once "Crops per run" seeds are recorded — or the player turns recording off early — the order is saved to
 * the config and the route mode switches to the recorded route. Template tiles are used because the farm is a
 * fresh instance on every entry; the template tile is the same plot every time.
 */
@Singleton
class RouteRecorder
{
	private final TitheFarmConfig config;
	private final ConfigManager configManager;
	private final List<int[]> recording = new ArrayList<>();

	private String savedEncoded;
	private List<int[]> savedDecoded = Collections.emptyList();

	@Inject
	RouteRecorder(TitheFarmConfig config, ConfigManager configManager)
	{
		this.config = config;
		this.configManager = configManager;
	}

	/** Starts a fresh recording when the toggle turns on, and saves a partial one when it is turned off early. */
	void onConfigChanged(ConfigChanged event)
	{
		if (!TitheFarmConfig.GROUP.equals(event.getGroup()) || !TitheFarmConfig.RECORD_ROUTE.equals(event.getKey()))
			return;

		if (Boolean.parseBoolean(event.getNewValue()))
			recording.clear();
		else if (!recording.isEmpty())
			save();
	}

	/**
	 * Appends a freshly planted plot to the recording, finishing it once the run's crop count is reached.
	 *
	 * @param tile the planted plot's template tile
	 */
	void onPlanted(WorldPoint tile)
	{
		if (!config.recordRoute())
			return;

		int[] point = {tile.getX(), tile.getY()};
		for (int[] existing : recording)
		{
			if (existing[0] == point[0] && existing[1] == point[1])
				return;
		}

		recording.add(point);
		if (recording.size() >= config.cropCount())
		{
			save();
			configManager.setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.RECORD_ROUTE, false);
		}
	}

	/** Whether a recording is in progress. */
	boolean isRecording()
	{
		return config.recordRoute();
	}

	/** The plots recorded so far, in planting order. */
	List<int[]> getRecording()
	{
		return Collections.unmodifiableList(recording);
	}

	/** The saved route, decoded from the config, or an empty list when none is saved. */
	List<int[]> getSaved()
	{
		String encoded = config.recordedRoute();
		if (!encoded.equals(savedEncoded))
		{
			savedEncoded = encoded;
			savedDecoded = decode(encoded);
		}

		return savedDecoded;
	}

	/** Writes the recording to the config, switches to the recorded route, and clears the buffer. */
	private void save()
	{
		configManager.setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.RECORDED_ROUTE, encode(recording));
		configManager.setConfiguration(TitheFarmConfig.GROUP, TitheFarmConfig.ROUTE_MODE, RouteMode.RECORDED);
		recording.clear();
	}

	/**
	 * Encodes tiles as {@code x,y;x,y;...}.
	 *
	 * @param tiles the tiles as {@code int[]{x, y}}
	 * @return the encoded route
	 */
	static String encode(List<int[]> tiles)
	{
		StringBuilder out = new StringBuilder();
		for (int[] tile : tiles)
		{
			if (out.length() > 0)
				out.append(';');

			out.append(tile[0])
				.append(',')
				.append(tile[1]);
		}

		return out.toString();
	}

	/**
	 * Decodes a route written by {@link #encode}, skipping any malformed entry.
	 *
	 * @param encoded the encoded route, possibly empty or {@code null}
	 * @return the tiles as {@code int[]{x, y}}
	 */
	static List<int[]> decode(String encoded)
	{
		List<int[]> tiles = new ArrayList<>();
		if (encoded == null || encoded.trim().isEmpty())
			return tiles;

		for (String part : encoded.split(";"))
		{
			String[] xy = part.trim().split(",");
			if (xy.length != 2)
				continue;

			try
			{
				tiles.add(new int[]{Integer.parseInt(xy[0].trim()), Integer.parseInt(xy[1].trim())});
			}
			catch (NumberFormatException ignored)
			{
				// Malformed entry; skip it and keep the rest of the route.
			}
		}

		return tiles;
	}
}
