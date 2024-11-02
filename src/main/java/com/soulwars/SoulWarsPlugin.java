package com.soulwars;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import static net.runelite.api.NpcID.*;

@Slf4j
@PluginDescriptor(
	name = "Soul Wars",
	description = "Keeps track of fragments, avatar damage, bones buried, and captures.",
	tags = {"soul", "wars"}
)

public class SoulWarsPlugin extends Plugin
{

//		[int] souls = [10534 10535, 10536, 10537]
//		int wolf = 10533,
public static final int WOLF_10533 = 10533;
//		int bones = 25199,
//		int fragment = 25201,
//		int red_obe = 40451,
//		int blue_obe = 40450,
	private static final ImmutableSet<Integer> AVATARS_IDS = ImmutableSet.of(
			AVATAR_OF_CREATION_10531, AVATAR_OF_DESTRUCTION_10532
	);
	private static final ImmutableSet<Integer> SOUL_WARS_ARENA_REGIONS = ImmutableSet.of(
			8493, 8749, 9005
	);
	// 2167, 2893 | 2157, 2893 | 2167, 2903 | 2157, 2903 blue graveyard
	// 2248, 2930 | 2258, 2930 | 2248, 2920 | 2258, 2920 red graveyard

	// 2199 - 2214 - 2919 - 2904 Soul


	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SoulWarsConfig config;

	@Inject
	private PartyService partyService;
	@Inject
	private SoulWarsManager soulWarsManager;

	private int currentRegionId = -1;
	private boolean currentInGame = false;

	@Override
	protected void startUp() throws Exception
	{
		if (inSoulWarsGame()) {
			soulWarsManager.init();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		soulWarsManager.reset();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
	{
		Actor actor = hitsplatApplied.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		Hitsplat hitsplat = hitsplatApplied.getHitsplat();
		final int npcId = ((NPC) actor).getId();

		if (!AVATARS_IDS.contains(npcId))
		{
			// only track avatars
			return;
		}

		if (hitsplat.isMine()) {
			int hit = hitsplat.getAmount();
			soulWarsManager.dealtAvatarDamage(hit);
		};
	}

	@Provides
	SoulWarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SoulWarsConfig.class);
	}

	@Subscribe
	void onConfigChanged(final ConfigChanged event)
	{
		if (!event.getGroup().equals(SoulWarsConfig.CONFIG_GROUP))
		{
			return;
		}

		if (inSoulWarsGame()) {
			soulWarsManager.updateInfoBoxes();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		final int regionId = getRegionId();
		if (currentRegionId != regionId) {
			currentRegionId = regionId;
		}
		// Either entered game or left game
		if (currentInGame != inSoulWarsGame()) {
			currentInGame = inSoulWarsGame();
			soulWarsManager.reset();

			// new game reset
			if (currentInGame) {
				soulWarsManager.init();
			}
		}
	}

	private boolean inSoulWarsGame()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}
		return player.getWorldView().isInstance() && SOUL_WARS_ARENA_REGIONS.contains(currentRegionId);
	}

	private int getRegionId()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return -1;
		}
		return WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID();
	}
}
