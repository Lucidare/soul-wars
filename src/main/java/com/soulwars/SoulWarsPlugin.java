package com.soulwars;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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

	private static final ImmutableSet<Integer> AVATARS_IDS = ImmutableSet.of(
			AVATAR_OF_CREATION_10531, AVATAR_OF_DESTRUCTION_10532
	);
	private static final int VARBIT_SOUL_WARS = 3815;
	private SoulWarsTeam team = SoulWarsTeam.NONE;
	private int numFragments = 0;
	//		int red_obelisk = 40451
	//		int blue_obelisk = 40450

	@Override
	protected void startUp() throws Exception
	{
		// Weird edge case: Turned on plugin while in game
		// determine team by checking cape instead
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		int capeId = player.getPlayerComposition().getEquipmentId(KitType.CAPE);
		if (capeId == SoulWarsTeam.BLUE.getItemId()) {
			team = SoulWarsTeam.BLUE;
		} else if (capeId == SoulWarsTeam.RED.getItemId()) {
			team = SoulWarsTeam.RED;
		}

		if (inSoulWarsGame()) {
			soulWarsManager.init(team);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		soulWarsManager.reset();
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
		}
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
			soulWarsManager.parseChatMessage(event.getMessage(), getWorldPoint(), numFragments);
		}
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged event) {
		// Update inventory, update shard count
		if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
			ItemContainer inventory = event.getItemContainer();
			for (final Item item : inventory.getItems()) {
				if (item.getId() == SoulWarsResource.FRAGMENTS_SACRIFICED.getItemId()) {
					int prevNumFragments = numFragments;
					numFragments = item.getQuantity();
					// num fragments decrease so sacrificed but potentially some remain due to low avatar strength
					if (prevNumFragments > numFragments) {
						soulWarsManager.decreaseFragmentsSacrificed(numFragments);
					}
				}
			}
		}
	}

	private boolean inSoulWarsGame()
	{
		return team != SoulWarsTeam.NONE;
	}

	@Subscribe
	void onVarbitChanged(final VarbitChanged event)
	{
		final int varbit = event.getVarbitId();

		if (varbit == VARBIT_SOUL_WARS)
		{
			int teamNum = event.getValue();

			// joined game
			if (teamNum != 0) {
				if (teamNum == SoulWarsTeam.BLUE.getVarbitNum()) {
					team = SoulWarsTeam.BLUE;
				} else if (teamNum == SoulWarsTeam.RED.getVarbitNum()) {
					team = SoulWarsTeam.RED;
				}
				soulWarsManager.reset();
				soulWarsManager.init(team);
			}
			// left game
			else {
				team = SoulWarsTeam.NONE;
				soulWarsManager.reset();
			}
		}
	}

	private WorldPoint getWorldPoint()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		return WorldPoint.fromLocalInstance(client, player.getLocalLocation());
	}
}
