package com.soulwars;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Soul Wars",
	description = "Keeps track of fragments, avatar damage, bones buried, and captures.",
	tags = {"soul", "wars"}
)

public class SoulWarsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SoulWarsConfig config;

	@Override
	protected void startUp() throws Exception
	{
	}

	@Override
	protected void shutDown() throws Exception
	{
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
	}

	@Provides
	SoulWarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SoulWarsConfig.class);
	}
}
