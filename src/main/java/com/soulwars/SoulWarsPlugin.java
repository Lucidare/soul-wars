package com.soulwars;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.Optional;

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
	private SoulWarsTeam team = SoulWarsTeam.NONE;

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
	void onChatMessage(final ChatMessage event)
	{
		final ChatMessageType type = event.getType();

		if (type == ChatMessageType.SPAM || type == ChatMessageType.GAMEMESSAGE)
		{
			log.info(event.getMessage());
			soulWarsManager.parseChatMessage(event.getMessage(), getWorldPoint(), SoulWarsTeam.RED);
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		final int regionId = getWorldPoint().isPresent() ? getWorldPoint().get().getRegionID() : -1;
		if (currentRegionId != regionId) {
			currentRegionId = regionId;
		}
		// entered game
		if (inSoulWarsGame() && team == SoulWarsTeam.NONE) {
			checkTeam();
			soulWarsManager.reset();
			soulWarsManager.init();
		}

		// left game
		if (!inSoulWarsGame() && team != SoulWarsTeam.NONE) {
			team = SoulWarsTeam.NONE;
			soulWarsManager.reset();
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

	private void checkTeam()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		int capeId = player.getPlayerComposition().getEquipmentId(KitType.CAPE);
		log.info(Integer.toString(capeId));
		if (capeId == SoulWarsTeam.BLUE.getItemId()) {
			team = SoulWarsTeam.BLUE;
		} else if (capeId == SoulWarsTeam.RED.getItemId()) {
			team = SoulWarsTeam.RED;
		}
	}

	private Optional<WorldPoint> getWorldPoint()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return Optional.empty();
		}
		return Optional.of(WorldPoint.fromLocalInstance(client, player.getLocalLocation()));
	}
}
