/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.tithefarm;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Development entry point that launches a RuneLite client with the plugin loaded (used by {@code ./gradlew run}). */
public class TitheFarmPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TitheFarmPlugin.class);
		RuneLite.main(args);
	}
}
