package com.soulwars;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("soulwars")
public interface SoulWarsConfig extends Config
{
	@ConfigItem(
			keyName = "trackingMode",
			name = "Tracking Mode",
			description = "Increment or decrement resource counters.",
			position = 1
	)
	default TrackingMode trackingMode()
	{
		return TrackingMode.DECREMENT;
	}

	@ConfigItem(
			keyName = "removedWhenCompleted",
			name = "Remove when completed",
			description = "Remove counters when desired number is reached.",
			position = 2
	)
	default boolean removedWhenCompleted()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showFragmentsSacrificed",
			name = "Show fragments sacrificed",
			description = "Display number of fragments sacrificed",
			position = 3
	)
	default boolean showFragmentsSacrificed()
	{
		return true;
	}

	@ConfigItem(
			keyName = "fragmentsSacrificed",
			name = "Fragments Sacrificed",
			description = "The desired number of fragments to sacrifice.",
			position = 4
	)
	default int fragmentsSacrificed()
	{
		return 24;
	}

	@ConfigItem(
			keyName = "showAvatarDamage",
			name = "Show avatar damage",
			description = "Display amount of damage you've done to the avatar",
			position = 5
	)
	default boolean showAvatarDamage()
	{
		return true;
	}

	@ConfigItem(
			keyName = "avatarDamage",
			name = "Avatar Damage",
			description = "The desired number of avatar damage.",
			position = 6
	)
	default int avatarDamage()
	{
		return 260;
	}

	@ConfigItem(
			keyName = "showBonesBuried",
			name = "Show bones buried",
			description = "Display number of bones you've buried in your graveyard",
			position = 7
	)
	default boolean showBonesBuried()
	{
		return true;
	}

	@ConfigItem(
			keyName = "bonesBuried",
			name = "Bones Buried",
			description = "The desired number of bones buried.",
			position = 8
	)
	default int bonesBuried()
	{
		return 5;
	}

	@ConfigItem(
			keyName = "showCaptures",
			name = "Show captures",
			description = "Display number of times you've captured an obelisk or graveyard",
			position = 9
	)
	default boolean showCaptures()
	{
		return true;
	}

	@ConfigItem(
			keyName = "captures",
			name = "Captures",
			description = "The desired number of captures.",
			position = 10
	)
	default int captures()
	{
		return 6;
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
