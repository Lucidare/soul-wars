package com.soulwars;

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
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.Arrays;

import static com.soulwars.SoulWarsManager.*;

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
	@Inject
	private OverlayManager overlayManager;

	private CaptureAreaOverlay overlay;
	private static final int INVENTORY_CLICK = 57;
	private SoulWarsTeam team = SoulWarsTeam.NONE;

	@Override
	protected void startUp() throws Exception
	{
		overlay = new CaptureAreaOverlay(soulWarsManager, client, config);
		overlayManager.add(overlay);

		// Weird edge case: Turned on plugin while in game
		// determine team by checking cape instead
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		int capeId = player.getPlayerComposition().getEquipmentId(KitType.CAPE);
		if (capeId == SoulWarsTeam.BLUE.itemId) {
			team = SoulWarsTeam.BLUE;
		} else if (capeId == SoulWarsTeam.RED.itemId) {
			team = SoulWarsTeam.RED;
		}

		if (inSoulWarsGame()) {
			soulWarsManager.init(team);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		soulWarsManager.reset();
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		Player player = client.getLocalPlayer();
		if (player == null) {
			return;
		}
		int wv = player.getLocalLocation().getWorldView();
		WorldView worldView = client.getWorldView(wv);

		int[] loadedRegionIds = worldView.getScene().getMapRegions();
		int[] loadedSoulWarsRegion = Arrays.stream(loadedRegionIds).filter(SOUL_WARS_ARENA_REGIONS::contains).toArray();
		soulWarsManager.highlightCaptureAreas(loadedSoulWarsRegion);
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
			soulWarsManager.parseChatMessage(event.getMessage(), getWorldPoint());
		}
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged event) {
		// Update inventory, update shard count
		boolean foundFragments = false;
		if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
			ItemContainer inventory = event.getItemContainer();
			for (final Item item : inventory.getItems()) {
				if (item.getId() == SoulWarsResource.FRAGMENTS_SACRIFICED.itemId) {
					foundFragments = true;
					soulWarsManager.updateFragmentInInventoryCount(item.getQuantity());
				}
			}
			// no fragments in inventory
			if (!foundFragments) {
				soulWarsManager.updateFragmentInInventoryCount(0);
			}
		}
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
				if (teamNum == SoulWarsTeam.BLUE.varbitNum) {
					team = SoulWarsTeam.BLUE;
				} else if (teamNum == SoulWarsTeam.RED.varbitNum) {
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

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!inSoulWarsGame()) {
			return;
		}

		int eventId = event.getId();
		int actionId = event.getMenuAction().getId();
		int itemId = event.getItemId();

		if (config.preventIncorrectSacrifice()) {
			boolean isObeliskClick = eventId == SoulWarsTeam.NONE.obeliskId
					|| eventId == SoulWarsTeam.RED.obeliskId
					|| eventId == SoulWarsTeam.BLUE.obeliskId;

			if (isObeliskClick) {
				 if (!soulWarsManager.shouldSacrificeObelisk()) {
					event.consume();
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Preventing sacrificing on incorrect obelisk.", "Soul Wars");
				}
			}
		}
		if (config.preventIncorrectBury()) {
			boolean isBoneClick = actionId == INVENTORY_CLICK && itemId == SoulWarsResource.BONES_BURIED.itemId;
			if (isBoneClick) {
				if (!soulWarsManager.shouldBuryBone(getWorldPoint())) {
					event.consume();
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Preventing burying on incorrect graveyard.", "Soul Wars");
				}
			}
		}
	}

	private boolean inSoulWarsGame()
	{
		return team != SoulWarsTeam.NONE;
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
