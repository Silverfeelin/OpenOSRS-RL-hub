package net.runelite.client.plugins.friendsexporter;

import com.google.inject.Provides;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.Ignore;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Friends Exporter",
	description = "Adds a right click option to the friends tab that allows exporting of either friends or ignore list",
	type = PluginType.MISCELLANEOUS
)
@Slf4j
public class FriendsExporterPlugin extends Plugin
{
	private static final WidgetMenuOption FIXED_Friends_List;
	private static final WidgetMenuOption Resizable_Friends_List;
	private static final WidgetMenuOption Bottom_Friends_List;
	private static final WidgetMenuOption FIXED_Ignore_List;
	private static final WidgetMenuOption Resizable_Ignore_List;
	private static final WidgetMenuOption Bottom_Ignore_List;
	private static final WidgetMenuOption Fixed_Clan_List;
	private static final WidgetMenuOption Resizable_Clan_List;

	@Inject
	private Client client;

	@Inject
	private MenuManager menuManager;

	@Inject
	private FriendsExporterConfig config;

	private boolean clan = false;
	private boolean wid = false;

	@Override
	protected void startUp()
	{
		refreshShiftClickCustomizationMenus();
	}

	@Override
	protected void shutDown()
	{
		removeShiftClickCustomizationMenus();
	}

	@Subscribe
	public void onWidgetMenuOptionClicked(WidgetMenuOptionClicked event) throws Exception
	{
		if (event.getWidget() == WidgetInfo.FIXED_VIEWPORT_FRIENDS_TAB || event.getWidget() == WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_TAB || event.getWidget() == WidgetInfo.FIXED_VIEWPORT_CLAN_CHAT_TAB || event.getWidget() == WidgetInfo.RESIZABLE_VIEWPORT_CLAN_CHAT_TAB)
		{
			if (event.getMenuOption().equals("Export") && Text.removeTags(event.getMenuTarget()).equals("Friends List"))
			{
				exportFriendsList();
			}
			else if (event.getMenuOption().equals("Export") && Text.removeTags(event.getMenuTarget()).equals("Ignore List"))
			{
				exportIgnoreList();
			}
			else if (event.getMenuOption().equals("Export") && Text.removeTags(event.getMenuTarget()).equals("Rank List"))
			{
				exportRankList();
			}
			refreshShiftClickCustomizationMenus();
		}
	}

	private void refreshShiftClickCustomizationMenus()
	{
		this.removeShiftClickCustomizationMenus();
		this.menuManager.addManagedCustomMenu(FIXED_Friends_List);
		this.menuManager.addManagedCustomMenu(Resizable_Friends_List);
		this.menuManager.addManagedCustomMenu(Bottom_Friends_List);
		this.menuManager.addManagedCustomMenu(FIXED_Ignore_List);
		this.menuManager.addManagedCustomMenu(Resizable_Ignore_List);
		this.menuManager.addManagedCustomMenu(Bottom_Ignore_List);
		if (clan)
		{
			this.menuManager.addManagedCustomMenu(Fixed_Clan_List);
			this.menuManager.addManagedCustomMenu(Resizable_Clan_List);
		}
	}

	private void removeShiftClickCustomizationMenus()
	{
		this.menuManager.removeManagedCustomMenu(FIXED_Friends_List);
		this.menuManager.removeManagedCustomMenu(Resizable_Friends_List);
		this.menuManager.removeManagedCustomMenu(Bottom_Friends_List);
		this.menuManager.removeManagedCustomMenu(FIXED_Ignore_List);
		this.menuManager.removeManagedCustomMenu(Resizable_Ignore_List);
		this.menuManager.removeManagedCustomMenu(Bottom_Ignore_List);
		this.menuManager.removeManagedCustomMenu(Fixed_Clan_List);
		this.menuManager.removeManagedCustomMenu(Resizable_Clan_List);
	}

	private void exportFriendsList() throws Exception
	{
		String fileName = RuneLite.RUNELITE_DIR + "\\" + this.client.getLocalPlayer().getName() + " Friends " + LocalDate.now() + ".txt";
		purgeList(fileName);
		Friend[] array = this.client.getFriends();
		FileWriter writer = new FileWriter(fileName, true);
		for (int x = 0; x != this.client.getFriendsCount(); x++)
		{
			String friendName = Text.toJagexName(array[x].getName());
			String prevName = "";
			if (!StringUtils.isEmpty(array[x].getPrevName()))
			{
				prevName = Text.toJagexName(array[x].getPrevName());
			}
			String Writing = toWrite(x + 1, friendName, prevName, "");
			try
			{
				writer.write(Writing + "\r\n");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		writer.close();
	}

	private void exportRankList() throws Exception
	{
		String fileName = RuneLite.RUNELITE_DIR + "\\" + this.client.getLocalPlayer().getName() + " Ranks " + LocalDate.now() + ".txt";
		purgeList(fileName);
		Friend[] array = this.client.getFriends();
		Widget temp;
		Widget[] temp2;
		temp = this.client.getWidget(94, 28);
		temp2 = temp.getChildren();
		FileWriter writer = new FileWriter(fileName, true);
		for (int x = 0; x < temp2.length / 4; x++)
		{
			String rank = temp2[(x * 4) + 1].getText();
			if (!rank.equals("Not in clan") || this.config.showUnranked())
			{
				String prevName = "";
				for (int y = 0; y != this.client.getFriendsCount(); y++)
				{
					String friendName = Text.toJagexName(array[y].getName());
					if (friendName.equals(temp2[(x * 4) + 2].getText()))
					{
						if (!StringUtils.isEmpty(array[y].getPrevName()))
						{
							prevName = Text.toJagexName(array[y].getPrevName());
						}
						break;
					}
				}
				String Writing;
				if (!rank.equals("Not in clan"))
				{
					Writing = toWrite(x, temp2[(x * 4) + 2].getText(), prevName, rank);
				}
				else
				{
					Writing = toWrite(x, temp2[(x * 4) + 2].getText(), prevName, "No Rank");
				}
				try
				{
					writer.write(Writing + "\r\n");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		writer.close();
	}

	private void exportIgnoreList() throws Exception
	{
		String fileName = RuneLite.RUNELITE_DIR + "\\" + this.client.getLocalPlayer().getName() + " Ignore " + LocalDate.now() + ".txt";
		purgeList(fileName);
		Ignore[] array = this.client.getIgnores();
		FileWriter writer = new FileWriter(fileName, true);
		for (int x = 0; x != this.client.getIgnoreCount(); x++)
		{
			String friendName = Text.toJagexName(array[x].getName());
			String prevName = "";
			if (!StringUtils.isEmpty(array[x].getPrevName()))
			{
				prevName = Text.toJagexName(array[x].getPrevName());
			}
			String Writing = toWrite(x + 1, friendName, prevName, "");
			try
			{
				writer.write(Writing + "\r\n");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		writer.close();
	}

	private void purgeList(String fileName)
	{
		File purge = new File(fileName);
		purge.delete();
	}

	private String toWrite(Integer Num, String firstName, String lastName, String rank)
	{
		String export = "";
		String Separator;
		String Role = "";
		if (this.config.Separator().equals(""))
		{
			Separator = "-";
		}
		else
		{
			Separator = this.config.Separator();
		}
		if (this.config.newLine())
		{
			Separator = "\n" + Separator;
		}
		if (!rank.isEmpty())
		{
			Role = rank + Separator;
		}
		switch (this.config.Lineleads())
		{
			case None:
				if (!StringUtils.isEmpty(lastName))
				{
					export = Role + firstName + Separator + lastName;
				}
				else
				{
					export = Role + firstName;
				}
				break;
			case Number:
				if (!StringUtils.isEmpty(lastName))
				{
					export = Num.toString() + " " + Role + firstName + Separator + lastName;
				}
				else
				{
					export = Num.toString() + " " + Role + firstName;
				}
				break;
			case Number1:
				if (!StringUtils.isEmpty(lastName))
				{
					export = Num.toString() + ". " + Role + firstName + Separator + lastName;
				}
				else
				{
					export = Num.toString() + ". " + Role + firstName;
				}
				break;
			case Number2:
				if (!StringUtils.isEmpty(lastName))
				{
					export = Num.toString() + ") " + Role + firstName + Separator + lastName;
				}
				else
				{
					export = Num.toString() + ") " + Role + firstName;
				}
				break;
			case Number3:
				if (!StringUtils.isEmpty(lastName))
				{
					export = Num.toString() + ".) " + Role + firstName + Separator + lastName;
				}
				else
				{
					export = Num.toString() + ".) " + Role + firstName;
				}
		}
		return (export);
	}

	static
	{
		FIXED_Friends_List = new WidgetMenuOption("Export", "Friends List", WidgetInfo.FIXED_VIEWPORT_FRIENDS_TAB);
		Resizable_Friends_List = new WidgetMenuOption("Export", "Friends List", WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_TAB);
		Bottom_Friends_List = new WidgetMenuOption("Export", "Friends List", WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_ICON);
		FIXED_Ignore_List = new WidgetMenuOption("Export", "Ignore List", WidgetInfo.FIXED_VIEWPORT_FRIENDS_TAB);
		Resizable_Ignore_List = new WidgetMenuOption("Export", "Ignore List", WidgetInfo.RESIZABLE_VIEWPORT_FRIENDS_TAB);
		Bottom_Ignore_List = new WidgetMenuOption("Export", "Ignore List", WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_FRIEND_ICON);
		Fixed_Clan_List = new WidgetMenuOption("Export", "Rank List", WidgetInfo.FIXED_VIEWPORT_CLAN_CHAT_TAB);
		Resizable_Clan_List = new WidgetMenuOption("Export", "Rank List", WidgetInfo.RESIZABLE_VIEWPORT_CLAN_CHAT_TAB);
	}

	@Provides
	FriendsExporterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FriendsExporterConfig.class);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widget)
	{
		if (widget.getGroupId() == 94 && !this.client.isInInstancedRegion())
		{
			wid = true;
			clan = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (this.client.getWidget(94, 28) == null)
		{
			clan = false;
			refreshShiftClickCustomizationMenus();
		}

		if (wid)
		{
			refreshShiftClickCustomizationMenus();
			wid = false;
		}
	}
}