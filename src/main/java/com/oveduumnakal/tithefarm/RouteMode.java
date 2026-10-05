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

import java.util.List;

/** Which planting route the plugin numbers and follows. */
public enum RouteMode
{
	/** The wiki's basic 20-plant route on the east side of the farm. */
	WIKI_BASIC("Wiki basic (20)", TitheRoutes.BASIC_20),

	/** The wiki's 20-plant combo route on the west side of the farm. */
	WIKI_COMBO("Wiki combo (20)", TitheRoutes.COMBO_20),

	/** The wiki's 23-plant simple route through the centre and west of the farm. */
	WIKI_SIMPLE_23("Wiki simple (23)", TitheRoutes.SIMPLE_23),

	/** The plugin's own route: a loop up one pair of columns and back down the next, sized to the crop count. */
	AUTOMATIC("Automatic", null),

	/** The order the player recorded with "Record route". */
	RECORDED("Recorded", null);

	private final String name;
	private final List<int[]> preset;

	RouteMode(String name, List<int[]> preset)
	{
		this.name = name;
		this.preset = preset;
	}

	/** The fixed wiki route for this mode, or {@code null} when the route is computed or recorded. */
	List<int[]> getPreset()
	{
		return preset;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
