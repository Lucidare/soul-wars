package com.soulwars;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(SoulWarsConfig.CONFIG_GROUP)
public interface SoulWarsConfig extends Config
{
	String CONFIG_GROUP = "soul-wars";

	@ConfigSection(
			name = "Tracking",
			description = "Configuration settings related to tracking.",
			position = 0
	)
	String trackingSection = "Tracking";

	@ConfigItem(
			keyName = "trackingMode",
			name = "Tracking Mode",
			description = "Increment or decrement resource counters.",
			position = 1,
			section = trackingSection
	)
	default TrackingMode trackingMode()
	{
		return TrackingMode.INCREMENT;
	}

	@ConfigItem(
			keyName = "removedWhenGoalReached",
			name = "Remove when goal reached",
			description = "Remove counters when desired number is reached.",
			position = 2,
			section = trackingSection
	)
	default boolean removedWhenGoalReached()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showFragmentsSacrificed",
			name = "Show fragments sacrificed",
			description = "Display number of fragments sacrificed",
			position = 3,
			section = trackingSection
	)
	default boolean showFragmentsSacrificed()
	{
		return true;
	}

	@ConfigItem(
			keyName = "fragmentsSacrificed",
			name = "Fragments Sacrificed",
			description = "The desired number of fragments to sacrifice.",
			position = 4,
			section = trackingSection
	)
	default int fragmentsSacrificed()
	{
		return 24;
	}

	@ConfigItem(
			keyName = "showAvatarDamage",
			name = "Show avatar damage",
			description = "Display amount of damage you've done to the avatar",
			position = 5,
			section = trackingSection
	)
	default boolean showAvatarDamage()
	{
		return true;
	}

	@ConfigItem(
			keyName = "avatarDamage",
			name = "Avatar Damage",
			description = "The desired number of avatar damage.",
			position = 6,
			section = trackingSection
	)
	default int avatarDamage()
	{
		return 260;
	}

	@ConfigItem(
			keyName = "showBonesBuried",
			name = "Show bones buried",
			description = "Display number of bones you've buried in your graveyard",
			position = 7,
			section = trackingSection
	)
	default boolean showBonesBuried()
	{
		return true;
	}

	@ConfigItem(
			keyName = "bonesBuried",
			name = "Bones Buried",
			description = "The desired number of bones buried.",
			position = 8,
			section = trackingSection
	)
	default int bonesBuried()
	{
		return 5;
	}

	@ConfigItem(
			keyName = "showCaptures",
			name = "Show captures",
			description = "Display number of times you've captured an obelisk or graveyard",
			position = 9,
			section = trackingSection
	)
	default boolean showCaptures()
	{
		return true;
	}

	@ConfigItem(
			keyName = "captures",
			name = "Captures",
			description = "The desired number of captures.",
			position = 10,
			section = trackingSection
	)
	default int captures()
	{
		return 6;
	}

	@ConfigSection(
			name = "Prevention",
			description = "Configuration settings related to prevention.",
			position = 2
	)
	String preventionSection = "Prevention";

	@ConfigItem(
			keyName = "preventIncorrectSacrifice",
			name = "Prevent incorrect sacrifice",
			description = "Prevent sacrificing fragments when obelisk is not captured.",
			position = 1,
			section = preventionSection
	)
	default boolean preventIncorrectSacrifice()
	{
		return true;
	}

	@ConfigItem(
			keyName = "preventIncorrectBury",
			name = "Prevent incorrect bone bury",
			description = "Prevent burying bones when graveyard is not captured.",
			position = 2,
			section = preventionSection
	)
	default boolean preventIncorrectBury()
	{
		return true;
	}

	@ConfigSection(
			name = "Capture Areas",
			description = "Configuration settings related to rendering capture areas.",
			position = 3
	)
	String captureAreas = "Capture Areas";

	@ConfigItem(
		keyName = "highlightCaptureAreas",
		name = "Highlight capture areas",
		description = "highlight the capture areas.",
		position = 1,
		section = captureAreas
	)
	default boolean highlightCaptureAreas()
	{
		return true;
	}

	@ConfigItem(
			keyName = "fillOpacity",
			name = "Fill Opacity",
			description = "Opacity of the tile fill color for capture areas",
			position = 2,
			section = captureAreas
	)
	default int fillOpacity() {
		return 50;
	}

	// Constants

	@Getter
	@AllArgsConstructor
	enum TrackingMode
	{
		DECREMENT("Decrement"),
		INCREMENT("Increment");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}
}
