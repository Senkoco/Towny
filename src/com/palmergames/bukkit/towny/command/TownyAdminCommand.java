package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.conversation.SetupConversation;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.BackupTask;
import com.palmergames.bukkit.towny.tasks.NewDayScheduler;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Send a list of all general townyadmin help commands to player Command:
 * /townyadmin
 */

public class TownyAdminCommand extends BaseCommand implements CommandExecutor {

	private final Towny plugin;
	
	private static final List<String> adminTabCompletes = Arrays.asList(
		"plot",
		"resident",
		"town",
		"nation",
		"reset",
		"toggle",
		"set",
		"givebonus",
		"reload",
		"backup",
		"checkperm",
		"checkoutposts",
		"newday",
		"newhour",
		"unclaim",
		"purge",
		"mysqldump",
		"tpplot",
		"database",
		"townyperms",
		"depositall",
		"resetbanks",
		"install"
	);

	private static final List<String> adminTownTabCompletes = Arrays.asList(
		"new",
		"add",
		"kick",
		"rename",
		"spawn",
		"tpplot",
		"outpost",
		"delete",
		"rank",
		"toggle",
		"set",
		"meta",
		"deposit",
		"withdraw",
		"bankhistory",
		"outlaw",
		"leavenation",
		"invite",
		"unruin",
		"trust",
		"trusttown",
		"checkoutposts",
		"settownlevel",
		"giveboughtblocks",
		"merge",
		"forcemerge"
	);
	private static final List<String> adminTownToggleTabCompletes = Stream.concat(TownCommand.townToggleTabCompletes.stream(),
			Arrays.asList("forcepvp", "forcedisablepvp", "unlimitedclaims", "upkeep").stream()).collect(Collectors.toList()); 

	private static final List<String> adminNationTabCompletes = Arrays.asList(
		"add",
		"kick",
		"rename",
		"delete",
		"toggle",
		"set",
		"meta",
		"deposit",
		"withdraw",
		"bankhistory",
		"rank",
		"enemy",
		"ally",
		"merge",
		"transfer",
		"forcemerge"
	);

	private static final List<String> adminToggleTabCompletes = Arrays.asList(
		"wildernessuse",
		"regenerations",
		"neutral",
		"npc",
		"debug",
		"devmode",
		"townwithdraw",
		"nationwithdraw"
	);
	
	private static final List<String> adminPlotTabCompletes = Arrays.asList(
		"claim",
		"meta",
		"claimedat",
		"trust"
	);
	
	private static final List<String> adminMetaTabCompletes = Arrays.asList(
		"set",
		"add",
		"remove"
	);
	
	private static final List<String> adminDatabaseTabCompletes = Arrays.asList(
		"save",
		"load",
		"remove"
	);
	
	private static final List<String> adminResidentTabCompletes = Arrays.asList(
		"rename",
		"friend",
		"unjail",
		"delete"
	);
	
	private static final List<String> adminResidentFriendTabCompletes = Arrays.asList(
		"add",
		"remove",
		"list",
		"clear"
	);
	
	private static final List<String> adminSetCompletes = Arrays.asList(
		"mayor",
		"capital",
		"title",
		"surname",
		"nationzoneoverride",
		"plot"
	);
	
	private static final List<String> adminTownyPermsCompletes = Arrays.asList(
		"listgroups",
		"group",
		"townrank",
		"nationrank"
	);
	
	private static final List<String> adminReloadTabCompletes = Arrays.asList(
		"database",
		"db",
		"config",
		"perms",
		"permissions",
		"language",
		"lang",
		"townyperms",
		"all"
	);

	public TownyAdminCommand(Towny towny) {
		this.plugin = towny;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

		try {
			return parseTownyAdminCommand(sender, args);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args[0].toLowerCase()) {
			case "reload":
				if (args.length > 1)
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN_RELOAD, adminReloadTabCompletes), args[1]);
			case "purge":
				if (args.length == 3)
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("townless"), args[2], "+t");
				break;
			case "set":
				if (args.length > 1) {
					switch (args[1].toLowerCase()) {
						case "mayor":
							switch (args.length) {
								case 3:
									return getTownyStartingWith(args[2], "t");
								case 4:
									return filterByStartOrGetTownyStartingWith(Collections.singletonList("npc"), args[3], "+r");
								default:
									return Collections.emptyList();
							}
						case "capital":
						case "nationzoneoverride":
						case "plot":
							if (args.length == 3)
								return getTownyStartingWith(args[2], "t");
						case "title":
						case "surname":
							if (args.length == 3)
								return getTownyStartingWith(args[2], "r");
						default:
							if (args.length == 2)
								return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN_SET, adminSetCompletes), args[1]);
							else if (args.length > 2 && TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_SET, args[1]))
								return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
					}
				}
				break;
			case "plot":
				if (args.length == 2) {
					return NameUtil.filterByStart(adminPlotTabCompletes, args[1]);
				} else if (args.length > 2) {
					switch (args[1].toLowerCase()) {
						case "claim":
							return getTownyStartingWith(args[2], "r");
						case "meta":
							if (args.length == 3)
								return NameUtil.filterByStart(adminMetaTabCompletes, args[2]);
						case "trust":
							if (args.length == 3)
								return NameUtil.filterByStart(Arrays.asList("add", "remove"), args[2]);
							if (args.length == 4)
								return getTownyStartingWith(args[3], "r");
						default:
							return Collections.emptyList();
					}
				}
				break;
			case "givebonus":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "rt");
				break;
			case "toggle":
				if (args.length == 2) {
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN_TOGGLE, adminToggleTabCompletes), args[1]);
				} else if (args.length >= 3 && args[1].equalsIgnoreCase("npc")) {
					if (args.length == 3) {
						return getTownyStartingWith(args[2], "r");
					} else if (args.length == 4) {
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[3]);
					}
				} else if (args.length == 3) {
					return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
				}
				break;
			case "tpplot":
				if (args.length == 2) {
					return NameUtil.filterByStart(TownyUniverse.getInstance().getTownyWorlds()
						.stream()
						.map(TownyWorld::getName)
						.collect(Collectors.toList()), args[1]);
				}
				break;
			case "checkperm":
				if (args.length == 2)
					return NameUtil.filterByStart(BukkitTools.getVisibleOnlinePlayers(sender)
						.stream()
						.map(Player::getName)
						.collect(Collectors.toList()), args[1]);
				if (args.length == 3)
					return NameUtil.filterByStart(BukkitTools.getPluginManager().getPermissions()
						.stream()
						.map(Permission::getName)
						.collect(Collectors.toList()), args[2]);
			case "database":
				if (args.length == 2)
					return NameUtil.filterByStart(adminDatabaseTabCompletes, args[1]);
				if (args.length == 3 && args[1].equalsIgnoreCase("remove"))
					return Collections.singletonList("titles");
				break;
			case "resident":
				switch (args.length) {
					case 2:
						return getTownyStartingWith(args[1], "r");
					case 3:
						return NameUtil.filterByStart(adminResidentTabCompletes, args[2]);
					case 4:
						if (args[2].equalsIgnoreCase("friend"))
							return NameUtil.filterByStart(adminResidentFriendTabCompletes, args[3]);
					default:
						return Collections.emptyList();
				}
			case "town":
				if (args.length == 2) {
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("new"), args[1], "+t");
				} else if (args.length > 2 && !args[1].equalsIgnoreCase("new")) {
					switch (args[2].toLowerCase()) {
						case "add":
							if (args.length == 4)
								return null;
						case "kick":
							if (args.length == 4)
								return getResidentsOfTownStartingWith(args[1], args[3]);
						case "rank":
							switch (args.length) {
								case 4:
									return NameUtil.filterByStart(TownCommand.townAddRemoveTabCompletes, args[3]);
								case 5:
									return getResidentsOfTownStartingWith(args[1], args[4]);
								case 6:
									switch (args[3].toLowerCase()) {
										case "add":
											return NameUtil.filterByStart(TownyPerms.getTownRanks(), args[5]);
										case "remove": {
											Resident res = TownyUniverse.getInstance().getResident(args[4]);
											if (res != null)
												return NameUtil.filterByStart(res.getTownRanks(), args[5]);
											break;
										}
										default:
											return Collections.emptyList();
									}
								default:
									return Collections.emptyList();
							}
						case "set": {
							final Town town = TownyUniverse.getInstance().getTown(args[1]);
							if (town != null)
								return TownCommand.townSetTabComplete(sender, town, StringMgmt.remArgs(args, 2));
							break;
						}
						case "toggle":
							if (args.length == 4)
								return NameUtil.filterByStart(adminTownToggleTabCompletes, args[3]);
							else if (args.length == 5 && !args[3].equalsIgnoreCase("jail"))
								return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[4]);
						case "outlaw":
							switch (args.length) {
							case 4:
								return NameUtil.filterByStart(TownCommand.townAddRemoveTabCompletes, args[3]);
							case 5:
								switch (args[3].toLowerCase()) {
									case "add":
										return getTownyStartingWith(args[4], "r");
									case "remove": {
										final Town town = TownyUniverse.getInstance().getTown(args[1]);
										
										if (town != null)
											return NameUtil.filterByStart(NameUtil.getNames(town.getOutlaws()), args[4]);
										
										break;
									}
									default:
										return Collections.emptyList();
								}
							default:
								return Collections.emptyList();
							}
						case "invite":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "r");
						case "meta":
							if (args.length == 4) {
								return NameUtil.filterByStart(adminMetaTabCompletes, args[3]);
							}
							break;
						case "giveboughtblocks":
						case "settownlevel":
							if (args.length == 4)
								return NameUtil.filterByStart(StringMgmt.addToList(numbers, "unset"), args[3]);
							break;
						case "trust":
							if (args.length == 4)
								return NameUtil.filterByStart(Arrays.asList("add", "remove"), args[3]);
							if (args.length == 5)
								return getTownyStartingWith(args[4], "r");
						case "merge", "forcemerge":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "t");
						default:
							if (args.length == 3)
								return NameUtil.filterByStart(
									TownyCommandAddonAPI.getTabCompletes(
										CommandType.TOWNYADMIN_TOWN, adminTownTabCompletes
									), args[2]);
					}
				} else if (args.length == 4 && args[1].equalsIgnoreCase("new")) {
					return getTownyStartingWith(args[3], "r");
				}
				break;
			case "nation":
				if (args.length == 2) {
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("new"), args[1], "+n");
				} else if (args.length > 2 && !args[1].equalsIgnoreCase("new")) {
					switch (args[2].toLowerCase()) {
						case "add":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "t");
						case "toggle":
							if (args.length == 4) 
								return NameUtil.filterByStart(NationCommand.nationToggleTabCompletes, args[3]);
							else if (args.length == 5)
								return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[4]);
						case "set": {
							Nation nation = TownyUniverse.getInstance().getNation(args[1]);
							if (nation != null) {
								return NationCommand.nationSetTabComplete(sender, nation, StringMgmt.remArgs(args, 2));
							}
							else {
								return Collections.emptyList();
							}
						}
						case "merge", "forcemerge":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "n");
						case "transfer":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "t");
						case "rank":
							if (args.length == 4)
								return NameUtil.filterByStart(Arrays.asList("add","remove"), args[3]);
							else if (args.length == 5)
								return getTownyStartingWith(args[4], "r");
							else if (args.length == 6)
								return NameUtil.filterByStart(TownyPerms.getNationRanks(), args[5]);
						case "enemy":
						case "ally":
							if (args.length == 4)
								return Arrays.asList("add", "remove");
							if (args.length == 5)
								return getTownyStartingWith(args[4], "n");
						default:
							if (args.length == 3)
								return NameUtil.filterByStart(
									TownyCommandAddonAPI.getTabCompletes(
										CommandType.TOWNYADMIN_NATION, adminNationTabCompletes
									), args[2]);
					}
				} else if (args.length == 4 && args[1].equalsIgnoreCase("new")) {
					return getTownyStartingWith(args[3], "t");
				}
				break;
			case "unclaim":
				if (args.length == 2)
					return NameUtil.filterByStart(TownCommand.townUnclaimTabCompletes, args[1]);
			case "townyperms":
				if (args.length == 2)
					return NameUtil.filterByStart(adminTownyPermsCompletes, args[1]);
				if (args.length > 2) {
					switch (args[1].toLowerCase()) {
						case "group":
							if (args.length == 3)
								return NameUtil.filterByStart(TownyPerms.getGroupList(), args[2]);
							if (args.length == 4)
								return NameUtil.filterByStart(Arrays.asList("addperm","removeperm"), args[3]);
							break;
						case "townrank":
						case "nationrank":
							if (args.length == 3) 
								return NameUtil.filterByStart(Arrays.asList("addrank","removerank"), args[2]);
							if (args.length > 3 && args[2].equalsIgnoreCase("remove")) {
								if (args[1].equalsIgnoreCase("nationrank"))
									return NameUtil.filterByStart(TownyPerms.getNationRanks(), args[3]);
								if (args[1].equalsIgnoreCase("townrank"))
									return NameUtil.filterByStart(TownyPerms.getTownRanks(), args[3]);
							}
							break;
						default:
							return Collections.emptyList();
					}
				}
				break;
			default:
				if (args.length == 1)
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN, adminTabCompletes), args[0]);
				else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN, args[0]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN, args[0]).getTabCompletion(sender, args), args[args.length-1]);
		}
		
		return Collections.emptyList();
	}

	public boolean parseTownyAdminCommand(CommandSender sender, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SCREEN.getNode());
		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help"))
			HelpMenu.TA_HELP.send(sender);
		else {

			if (split[0].equalsIgnoreCase("set")) {

				adminSet(sender, StringMgmt.remFirstArg(split));
				return true;
			} else if (split[0].equalsIgnoreCase("resident")){
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT.getNode());
				parseAdminResidentCommand(sender, StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("town")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN.getNode());
				parseAdminTownCommand(sender, StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("nation")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION.getNode());
				parseAdminNationCommand(sender, StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("toggle")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE.getNode());
				parseToggleCommand(sender, StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("plot")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT.getNode());
				parseAdminPlotCommand(catchConsole(sender), StringMgmt.remFirstArg(split));
				return true;

			}

			if (split[0].equalsIgnoreCase("givebonus") || split[0].equalsIgnoreCase("giveplots")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_GIVEBONUS.getNode());
				giveBonus(sender, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("reload")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RELOAD.getNode());
				if (split.length == 2) {
					switch (split[1]) {
						case "db":
						case "database":
							reloadDatabase(sender);
							break;
						case "config":
							reloadConfig(sender, false);
							break;
						case "perms":
						case "townyperms":
						case "permissions":
							reloadPerms(sender);
							break;
						case "language":
						case "lang":
							reloadLangs(sender);
							break;
						case "all":
							// reloadDatabase() already reloads lang & config.
							reloadPerms(sender);
							reloadDatabase(sender);
							break;
						default:
							if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_RELOAD, split[1]))
								TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_RELOAD, split[1]).execute(sender, split);
							else
								HelpMenu.TA_RELOAD.send(sender);
					}
				} else {
					HelpMenu.TA_RELOAD.send(sender);
					return false;
				}
			} else if (split[0].equalsIgnoreCase("reset")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESET.getNode());
				Confirmation.runOnAccept(()-> reloadConfig(sender, true))
					.setTitle(Translatable.of("this_will_reset_your_config"))
					.sendTo(sender);

			} else if (split[0].equalsIgnoreCase("backup")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_BACKUP.getNode());
				CompletableFuture.runAsync(new BackupTask())
					.thenRun(()-> TownyMessaging.sendMsg(sender, Translatable.of("mag_backup_success")));
				
			} else if (split[0].equalsIgnoreCase("database")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_DATABASE.getNode());
				parseAdminDatabaseCommand(sender, StringMgmt.remFirstArg(split));
				return true;				
				
			} else if (split[0].equalsIgnoreCase("mysqldump")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_MYSQLDUMP.getNode());
				if (TownySettings.getSaveDatabase().equalsIgnoreCase("mysql") && TownySettings.getLoadDatabase().equalsIgnoreCase("mysql")) {
					TownyDataSource dataSource = new TownyFlatFileSource(plugin, townyUniverse);
					dataSource.saveAll();
					TownyMessaging.sendMsg(sender, Translatable.of("msg_mysql_dump_success"));
					return true;
				} else 
					throw new TownyException(Translatable.of("msg_err_mysql_not_being_used"));

			} else if (split[0].equalsIgnoreCase("newday")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NEWDAY.getNode());
				if (NewDayScheduler.isNewDayScheduled())
					throw new TownyException(Translatable.of("msg_newday_already_scheduled_soon"));

				// Turn the daily timer scheduler on if it wasn't running already.
				if (!NewDayScheduler.isNewDaySchedulerRunning())
					TownyTimerHandler.toggleDailyTimer(true);
				// Start a newday.
				NewDayScheduler.newDay();

			} else if (split[0].equalsIgnoreCase("newhour")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NEWHOUR.getNode());
				TownyTimerHandler.newHour();
				TownyMessaging.sendMsg(sender, Translatable.of("msg_newhour_success"));

			} else if (split[0].equalsIgnoreCase("purge")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PURGE.getNode());
				purge(sender, StringMgmt.remFirstArg(split));
			} else if (split[0].equalsIgnoreCase("unclaim")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_UNCLAIM.getNode());
				parseAdminUnclaimCommand(catchConsole(sender), StringMgmt.remFirstArg(split));
				/*
				 * else if (split[0].equalsIgnoreCase("seed") &&
				 * TownySettings.getDebug()) seedTowny(); else if
				 * (split[0].equalsIgnoreCase("warseed") &&
				 * TownySettings.getDebug()) warSeed(player);
				 */
				
			} else if (split[0].equalsIgnoreCase("checkperm")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_CHECKPERM.getNode());
				parseAdminCheckPermCommand(sender, StringMgmt.remFirstArg(split));
			} else if (split[0].equalsIgnoreCase("checkoutposts")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_CHECKOUTPOSTS.getNode());
				parseAdminCheckOutpostsCommand(sender, null);
			} else if (split[0].equalsIgnoreCase("townyperms")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWNYPERMS.getNode());
				parseAdminTownyPermsCommand(sender, StringMgmt.remFirstArg(split));
				
			} else if (split[0].equalsIgnoreCase("tpplot")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TPPLOT.getNode());
				parseAdminTpPlotCommand(catchConsole(sender), StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("depositall")) {
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translatable.of("msg_err_no_economy"));
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_DEPOSITALL.getNode());
				parseAdminDepositAllCommand(sender, StringMgmt.remFirstArg(split));
			} else if (split[0].equalsIgnoreCase("resetbanks")) {
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translatable.of("msg_err_no_economy"));
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESETBANKS.getNode());
				parseAdminResetBanksCommand(sender, StringMgmt.remFirstArg(split));
			} else if (split[0].equalsIgnoreCase("install")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_INSTALL.getNode());
				Towny.getAdventure().sender(sender).sendMessage(Component.text(Translatable.of("msg_setup_full_guide_link").forLocale(sender)).clickEvent(ClickEvent.openUrl("https://github.com/TownyAdvanced/Towny/wiki/Installation")));
				
				new SetupConversation(sender).runOnResponse(response -> {
					ConversationContext context = (ConversationContext) response;
					
					toggleWildernessUsage(parseBoolean(context.getSessionData(0)));
					toggleRevertUnclaim(parseBoolean(context.getSessionData(1)));
					TownySettings.setProperty(ConfigNodes.TOWN_TOWN_BLOCK_RATIO.getRoot(), Integer.parseInt((String) context.getSessionData(2)));
					
					if (TownyEconomyHandler.isActive()) {
						TownySettings.setProperty(ConfigNodes.ECO_PRICE_NEW_TOWN.getRoot(), Integer.parseInt((String) context.getSessionData(3)));
						TownySettings.setProperty(ConfigNodes.ECO_PRICE_NEW_NATION.getRoot(), Integer.parseInt((String) context.getSessionData(4)));
						TownySettings.setProperty(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK.getRoot(), Integer.parseInt((String) context.getSessionData(5)));
					}
					
					TownySettings.saveConfig();
					TownyMessaging.sendMsg(sender, Translatable.of("msg_setup_success"));
				});
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN, split[0]).execute(sender, "townyadmin", split);
			}  else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));
				return false;
			}
		}

		return true;
	}
	
	private boolean parseBoolean(Object object) {
		if (!(object instanceof String string))
			return false;

		return switch (string.toLowerCase()) {
			case "y", "yes", "true" -> true;
			default -> false;
		};
	}

	private void parseAdminTownyPermsCommand(CommandSender sender, String[] args) {

		if (args.length == 0 || args[0].equalsIgnoreCase("?") || !adminTownyPermsCompletes.contains(args[0])) {
			HelpMenu.TA_TOWNYPERMS.send(sender);
			return;
		}
		
		try {
			switch (args[0]) {
				case "listgroups":
					// Send an alphabetized list of ranks.
					TownyMessaging.sendMessage(sender, ChatTools.formatTitle("Groups"));
					TownyMessaging.sendMessage(sender, ChatTools.list(TownyPerms.getGroupList().stream().sorted().collect(Collectors.toList())));
					break;
				case "group":
					// Alter a groups permission nodes.
					parseAdminTownypermsGroupCommand(sender, StringMgmt.remFirstArg(args));
					break;
				case "townrank":
				case "nationrank":
					// Create and remove town and nation ranks.
					parseAdminTownypermsRankCommand(sender, args);
					break;
				default:
				}
		} catch (TownyException e) {
			// This will send back any errors to the sender.
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}
	}

	private void parseAdminTownypermsGroupCommand(CommandSender sender, String[] args) throws TownyException {
		//ta townyperms group GROUPNAME add|remove NODE
		//                    ^ args[0]
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_TOWNYPERMS.send(sender);
			return;
		}
		if (!TownyPerms.getGroupList().contains(args[0].toLowerCase()))
			throw new TownyException(Translatable.of("msg_err_group_not_found", args[0]));

		String group = args[0];
		List<String> groupNodes = TownyPerms.getPermsOfGroup(group);

		// /ta townyperms group GROUPNAME: display nodes held by group.
		if (args.length == 1) {
			displayNodesHelpByGroup(sender, group, groupNodes);
			return;
		}
		
		if ((!args[1].equalsIgnoreCase("addperm") && !args[1].equalsIgnoreCase("removeperm")) || 
			args.length != 3)
			throw new TownyException(Translatable.of("msg_err_expected_command_format", "/ta townyperms group add|remove node"));
		
		boolean add = args[1].equalsIgnoreCase("addperm");
		String node = args[2];
		boolean changed = false;
		switch (args[1].toLowerCase()) {
		case "addperm":
			if (groupNodes.contains(node))
				throw new TownyException(Translatable.of("msg_err_group_already_has_node", group, node));
			changed = groupNodes.add(node);
			break;
		case "removeperm":
			if (!groupNodes.contains(node))
				throw new TownyException(Translatable.of("msg_err_group_doesnt_have_node", group, node));
			changed = groupNodes.remove(node);
			break;
		default:
		}
		
		if (!changed)
			return;
		
		TownyPerms.getTownyPermsFile().set(group, groupNodes);
		TownyPerms.getTownyPermsFile().save();
		if (add)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_added_node_to_group", node, group));
		else 
			TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_removed_node_from_group", node, group));
		
		reloadPerms(sender);
	}
	
	private void displayNodesHelpByGroup(CommandSender sender, String group, List<String> groupNodes) {
		if (groupNodes.size() > 0) {
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle(Translatable.of("msg_title_group_permissions", StringMgmt.capitalize(group)).forLocale(sender)));
			for (String node : groupNodes)
				TownyMessaging.sendMessage(sender, " - " + node);

		} else {
			TownyMessaging.sendErrorMsg(Translatable.of("msg_err_group_has_no_nodes", group));
		}
	}

	private void parseAdminTownypermsRankCommand(CommandSender sender, String[] args) throws TownyException {
		//ta townyperms townrank|nationrank add|remove RANKNAME
		//              ^ args[0]
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_TOWNYPERMS.send(sender);
			return;
		}
		
		if ((!args[1].equalsIgnoreCase("addrank") && !args[1].equalsIgnoreCase("removerank")) || 
			args.length != 3)
			throw new TownyException(Translatable.of("msg_err_expected_command_format", "/ta townyperms townrank|nationrank add|remove [rank]"));
		
		boolean town = args[0].equalsIgnoreCase("townrank");		
		boolean add = args[1].equalsIgnoreCase("addrank");
		String rank = args[2];
		boolean changed = false;
		switch (args[1].toLowerCase()) {
		case "addrank":
			// Changing town ranks.
			if (town) {
				if (TownyPerms.getTownRanks().contains(rank))
					throw new TownyException(Translatable.of("msg_err_there_is_already_a_town_or_nationrank_called_x", args[0], rank));
				
				TownyPerms.getTownyPermsFile().createSection("towns.ranks." + rank);
				changed = true;
				
			// Changing nation ranks.
			} else {
				if (TownyPerms.getNationRanks().contains(rank))
					throw new TownyException(Translatable.of("msg_err_there_is_already_a_town_or_nationrank_called_x", args[0], rank));
				
				TownyPerms.getTownyPermsFile().createSection("nations.ranks." + rank);
				changed = true;
			}
			break;
		case "removerank":
			// Changing town ranks.
			if (town) {
				if (!TownyPerms.getTownRanks().contains(rank))
					throw new TownyException(Translatable.of("msg_err_there_is_no_town_or_nationrank_called_x", args[0], rank));
				
				TownyPerms.getTownyPermsFile().set("towns.ranks." + rank, null);				
				changed = true;
				
			// Changing nation ranks.
			} else {
				if (!TownyPerms.getNationRanks().contains(rank))
					throw new TownyException(Translatable.of("msg_err_there_is_no_town_or_nationrank_called_x", args[0], rank));
				
				TownyPerms.getTownyPermsFile().set("nations.ranks." + rank, null);
				changed = true;
			}
			break;
		default:
		}
		
		if (!changed)
			return;
		
		TownyPerms.getTownyPermsFile().save();
		if (add)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_add_rank_to_the_town_or_nation_rank", rank, args[0]));
		else 
			TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_removed_rank_from_the_town_or_nation_rank", rank, args[0]));
		
		reloadPerms(sender);
	}

	private void parseAdminDatabaseCommand(CommandSender sender, String[] split) {
	
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_DATABASE.send(sender);
			return;
		}
		
		if (split[0].equalsIgnoreCase("save")) {
			if (TownyUniverse.getInstance().getDataSource().saveAll())
				TownyMessaging.sendMsg(sender, Translatable.of("msg_save_success"));
	
		} else if (split[0].equalsIgnoreCase("load")) {
			TownyUniverse.getInstance().clearAllObjects();			
			if (TownyUniverse.getInstance().getDataSource().loadAll()) {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_load_success"));
				BukkitTools.fireEvent(new TownyLoadedDatabaseEvent());
			}
		} else if (split[0].equalsIgnoreCase("remove")) {
			parseAdminDatabaseRemoveCommand(sender, StringMgmt.remFirstArg(split));
		}
	}

	private void parseAdminDatabaseRemoveCommand(CommandSender sender, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/townyadmin database remove"));
			TownyMessaging.sendMessage(sender, ChatTools.formatCommand(Translatable.of("admin_sing").forLocale(sender), "/townyadmin database remove", "titles", "Removes all titles and surnames from every resident."));
			return;
		}
		
		if (split[0].equalsIgnoreCase("titles")) {
			TownyUniverse.getInstance().getResidents()
				.forEach(resident -> {
					resident.setTitle("");
					resident.setSurname("");
					resident.save();
				});
			TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_removed_all_titles_and_surnames_removed"));
		}
		
	}

	private void parseAdminPlotCommand(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length < 1 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_PLOT.send(player);
			return;
		}

		if (split[0].equalsIgnoreCase("meta")) {
			handlePlotMetaCommand(player, split);
			return;
		}
		
		if (split[0].equalsIgnoreCase("claim")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_CLAIM.getNode());

			if (split.length == 1) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_error_ta_plot_claim"));
				return;
			}			
			Optional<Resident> resOpt = townyUniverse.getResidentOpt(split[1]);
			
			if (!resOpt.isPresent()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_error_no_player_with_that_name", split[1]));
				return;
			}

			String world = player.getWorld().getName();
			List<WorldCoord> selection = new ArrayList<>();
			selection.add(new WorldCoord(world, Coord.parseCoord(player)));
			new PlotClaim(plugin, player, resOpt.get(), selection, true, true, false).start();
		} else if (split[0].equalsIgnoreCase("claimedat")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_CLAIMEDAT.getNode());
			
			WorldCoord wc = WorldCoord.parseWorldCoord(player);
			if (!wc.hasTownBlock() || wc.getTownBlock().getClaimedAt() == 0)
				throw new NotRegisteredException();
			
			TownyMessaging.sendMsg(player, Translatable.of("msg_plot_perm_claimed_at").append(" " + TownyFormatter.fullDateFormat.format(wc.getTownBlock().getClaimedAt())));
		} else if (split[0].equalsIgnoreCase("trust")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_TRUST.getNode());
			
			PlotCommand.parsePlotTrustCommand(player, StringMgmt.remFirstArg(split));
		}
	}

	private void parseAdminCheckPermCommand(CommandSender sender, String[] split) throws TownyException {
		
		if (split.length !=2 ) {
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: /ta checkperm {name} {node}"));
		}
		Player player = BukkitTools.getPlayerExact(split[0]);
		if (player == null) {
			throw new TownyException("Player couldn't be found");
		}
		String node = split[1];
		if (player.hasPermission(node))
			TownyMessaging.sendMsg(sender, Translatable.of("msg_perm_true"));
		else
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_perm_false"));
	}

	private void parseAdminCheckOutpostsCommand(CommandSender sender, @Nullable Town specificTown) {
		List<Town> towns = new ArrayList<>();
		if (specificTown == null) 
			towns.addAll(TownyAPI.getInstance().getTowns());
		else 
			towns.add(specificTown);
		int removed = 0;
		for (Town town : towns) {
			for (Location loc : town.getAllOutpostSpawns()) {
				boolean save = false;
				if (TownyAPI.getInstance().isWilderness(loc) || !TownyAPI.getInstance().getTown(loc).getUUID().equals(town.getUUID())) {
					town.removeOutpostSpawn(loc);
					save = true;
					removed++;
				}
				if (save)
					town.save();
			}
		}
		TownyMessaging.sendMsg(sender, Translatable.of("msg_removed_x_invalid_outpost_spawns", removed));
	}

	private void parseAdminTpPlotCommand(Player player, String[] split) throws TownyException {

		if (split.length != 3) {
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: /ta tpplot world x z"));
		}
		
		World world = Bukkit.getWorld(split[0]);

		if (world == null)
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: /ta tpplot world x z"));
		
		int x, z;
		try {
			x = Integer.parseInt(split[1]) * TownySettings.getTownBlockSize();
			z = Integer.parseInt(split[2]) * TownySettings.getTownBlockSize();
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_input_must_be_int", "x and z"));
		}
		
		PaperLib.getChunkAtAsync(world, x, z).thenAccept(chunk -> {
			int y = world.getHighestBlockYAt(x, z) + 1;
			
			Bukkit.getScheduler().runTask(plugin, () -> PaperLib.teleportAsync(player, new Location(world, x, y, z)));
		});
	}

	private void giveBonus(CommandSender sender, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Town town;
		Resident target = null;
		boolean isTown = false;

		if (split.length != 2)
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: givebonus [town/player] [n]"));

		if ((town = townyUniverse.getTown(split[0])) != null) {
			isTown = true;
		} else {
			target = getResidentOrThrow(split[0]);

			if (!target.hasTown())
				throw new TownyException(Translatable.of("msg_err_resident_doesnt_belong_to_any_town"));

			town = target.getTownOrNull();
		}
		
		int extraBlocks = MathUtil.getIntOrThrow(split[1]);

		town.setBonusBlocks(town.getBonusBlocks() + extraBlocks);
		TownyMessaging.sendMsg(sender, Translatable.of("msg_give_total", town.getName(), split[1], town.getBonusBlocks()));
		
		boolean isConsole = sender instanceof ConsoleCommandSender;
		if (!isConsole || isTown)
			TownyMessaging.sendTownMessagePrefixed(town, Translatable.of("msg_you_have_been_given_bonus_blocks", extraBlocks)); 
		if (isConsole && !isTown) {
			TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_given_bonus_blocks", extraBlocks)); 
			TownyMessaging.sendMessage(target, Translatable.of("msg_ptw_warning_1").forLocale(target));
			TownyMessaging.sendMessage(target, Translatable.of("msg_ptw_warning_2").forLocale(target));
			TownyMessaging.sendMessage(target, Translatable.of("msg_ptw_warning_3").forLocale(target));
		}
		town.save();

	}

	public void parseAdminUnclaimCommand(Player player, String[] split) {

		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_UNCLAIM.send(player);
		} else {
			try {
				List<WorldCoord> selection;
				selection = AreaSelectionUtil.selectWorldCoordArea(null, new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)), split);
				selection = AreaSelectionUtil.filterOutWildernessBlocks(selection);

				if (selection.isEmpty())
					throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

				new TownClaim(plugin, player, null, selection, false, false, true).start();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public void parseAdminResidentCommand(CommandSender sender, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_RESIDENT.send(sender);
			return;
		}

		try	{
			Resident resident = getResidentOrThrow(split[0]);

			if (split.length == 1) {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(resident, sender)));
				return;
			}

			if(split[1].equalsIgnoreCase("rename"))	{
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_RENAME.getNode());
				if (!NameValidation.isBlacklistName(split[2])) {
					townyUniverse.getDataSource().renamePlayer(resident, split[2]);
				} else
					throw new TownyException(Translatable.of("msg_invalid_name"));
				
			} else if(split[1].equalsIgnoreCase("friend"))	{
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_FRIEND.getNode());
				if (split.length == 2) {
					HelpMenu.TA_RESIDENT_FRIEND.send(sender);
					return;
				}

				ResidentCommand.residentFriend(catchConsole(sender), StringMgmt.remArgs(split, 2), true, resident);

			} else if(split[1].equalsIgnoreCase("unjail")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_UNJAIL.getNode());
				if (resident.isJailed())
					JailUtil.unJailResident(resident, UnJailReason.ADMIN);
				else 
					throw new TownyException(Translatable.of("msg_err_player_is_not_jailed"));
			} else if (split[1].equalsIgnoreCase("delete")) {
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_DELETE.getNode());
				residentDelete(sender, split[0]);
			}
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}
	}
	
	public void parseAdminTownCommand(CommandSender sender, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_TOWN.send(sender);
			return;
		}

		try {
			
			if (split[0].equalsIgnoreCase("new")) {
				/*
				 * Moved from TownCommand as of 0.92.0.13
				 */
				if (split.length != 3)
					throw new TownyException(Translatable.of("msg_err_not_enough_variables") + "/ta town new [name] [mayor]");

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_NEW.getNode());

				Player player = sender instanceof Player p ? p : null;
				Resident resident;
				if ("npc".equalsIgnoreCase(split[2]) && player != null) // Avoid creating a new npc resident if command is ran from console.
					resident = ResidentUtil.createAndGetNPCResident();
				else
					resident = TownyUniverse.getInstance().getResident(split[2]);
				
				if (resident == null) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", split[2]));
					return;
				}
				
				// If the command is being run from console, try to sub in the specfied player.
				if (player == null) {
					if (resident.isOnline()) {
						player = resident.getPlayer();
					} else {
						throw new TownyException(Translatable.of("msg_player_is_not_online", split[2]));
					}
				}
				TownCommand.newTown(player, split[1], resident, true);
				return;
			}
			
			Town town = townyUniverse.getTown(split[0]);
			
			if (town == null) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", split[0]));
				return;
			}
			
			
			if (split.length == 1) {
				/*
				 * This is run async because it will ping the economy plugin for the town bank value.
				 */
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(town, sender)));
				return;
			}

			if (split[1].equalsIgnoreCase("invite")) {
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_INVITE.getNode());
				// Give admins the ability to invite a player to town, invite still requires acceptance.
				if (split.length < 3)
					throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME invite PLAYERNAME"));
				TownCommand.townAdd(sender, town, StringMgmt.remArgs(split, 2));
				
			} else if (split[1].equalsIgnoreCase("add")) {
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_ADD.getNode());
				// Force-join command for admins to use to bypass invites system.
				if (split.length < 3)
					throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME add PLAYERNAME"));
				Resident resident = getResidentOrThrow(split[2]);
				TownCommand.townAddResident(town, resident);
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_join_town", resident.getName()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_join_town", resident.getName()));
				
			} else if (split[1].equalsIgnoreCase("kick")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_KICK.getNode());
				TownCommand.townKickResidents(sender, town.getMayor(), town, ResidentUtil.getValidatedResidentsOfTown(sender, town, StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE.getNode());
				Confirmation.runOnAccept(() -> {
					TownyMessaging.sendMsg(sender, Translatable.of("town_deleted_by_admin", town.getName()));
					TownyUniverse.getInstance().getDataSource().removeTown(town);
				}).sendTo(sender);

			} else if (split[1].equalsIgnoreCase("rename")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_RENAME.getNode());
				if (split.length < 3)
					throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME rename NEWNAME"));
				String name = String.join("_", StringMgmt.remArgs(split, 2));
				
				BukkitTools.ifCancelledThenThrow(new TownPreRenameEvent(town, name));

				if (!NameValidation.isBlacklistName(name) && (TownySettings.areNumbersAllowedInTownNames() || !NameValidation.containsNumbers(name))) {
					townyUniverse.getDataSource().renameTown(town, name);
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_name", getSenderFormatted(sender), town.getName()));
					TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_name", getSenderFormatted(sender), town.getName()));
				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_invalid_name"));
				}
				
			} else if (split[1].equalsIgnoreCase("spawn")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN.getNode());
				SpawnUtil.sendToTownySpawn(catchConsole(sender), StringMgmt.remArgs(split, 2), town, "", false, false, SpawnType.TOWN);

			} else if (split[1].equalsIgnoreCase("outpost")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_OUTPOST.getNode());
				SpawnUtil.sendToTownySpawn(catchConsole(sender), StringMgmt.remArgs(split, 2), town, "", true, false, SpawnType.TOWN);

			} else if (split[1].equalsIgnoreCase("rank")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_RANK.getNode());
				parseAdminTownRankCommand(sender, town, StringMgmt.remArgs(split, 2));
			} else if (split[1].equalsIgnoreCase("toggle")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_TOGGLE.getNode());
				if (split.length == 2 || split[2].equalsIgnoreCase("?")) {
					HelpMenu.TA_TOWN_TOGGLE.send(sender);
					return;
				}
				
				Optional<Boolean> choice = Optional.empty();
				if (split.length == 4) {
					choice = BaseCommand.parseToggleChoice(split[3]);
				}
				
				if (split[2].equalsIgnoreCase("forcepvp")) {
					
					town.setAdminEnabledPVP(choice.orElse(!town.isAdminEnabledPVP()));
					
					town.save();
					TownyMessaging.sendMsg(sender, Translatable.of("msg_town_forcepvp_setting_set_to", town.getName(), town.isAdminEnabledPVP()));
				} else if (split[2].equalsIgnoreCase("forcedisablepvp")) {
					
					town.setAdminDisabledPVP(choice.orElse(!town.isAdminDisabledPVP()));
					
					town.save();
					TownyMessaging.sendMsg(sender, Translatable.of("msg_town_forcedisabledpvp_setting_set_to", town.getName(), town.isAdminDisabledPVP()));
				} else if (split[2].equalsIgnoreCase("unlimitedclaims")) {
					
					town.setHasUnlimitedClaims(choice.orElse(!town.hasUnlimitedClaims()));
					town.save();
					TownyMessaging.sendMsg(sender, Translatable.of("msg_town_unlimitedclaims_setting_set_to", town.getName(), town.hasUnlimitedClaims()));
				} else if (split[2].equalsIgnoreCase("upkeep")) {
					
					town.setHasUpkeep(choice.orElse(!town.hasUpkeep()));
					town.save();
					TownyMessaging.sendMsg(sender, Translatable.of("msg_town_upkeep_setting_set_to", town.getName(), town.hasUpkeep()));
				} else
					TownCommand.townToggle(sender, StringMgmt.remArgs(split, 2), true, town);
				
			} else if (split[1].equalsIgnoreCase("set")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SET.getNode());
				TownCommand.townSet(sender, StringMgmt.remArgs(split, 2), true, town);
			} else if (split[1].equalsIgnoreCase("meta")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_META.getNode());
				handleTownMetaCommand(sender, town, split);
			} else if (split[1].equalsIgnoreCase("giveboughtblocks")) {

				giveTownBoughtTownBlocks(sender, town, StringMgmt.remArgs(split, 2));
			} else if (split[1].equalsIgnoreCase("settownlevel")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SETTOWNLEVEL.getNode());
				setTownLevel(sender, town, StringMgmt.remArgs(split, 2));
			} else if (split[1].equalsIgnoreCase("bankhistory")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_BANKHISTORY.getNode());
				int pages = 10;
				if (split.length > 2)
					pages = MathUtil.getIntOrThrow(split[2]);
				
				town.generateBankHistoryBook(catchConsole(sender), pages);
			} else if (split[1].equalsIgnoreCase("deposit")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DEPOSIT.getNode());
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translatable.of("msg_err_no_economy"));
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(Translatable.of("msg_err_invalid_input", "deposit [amount]"));
				
				int amount = MathUtil.getIntOrThrow(split[2]);
				
				if (town.getAccount().deposit(amount, "Admin Deposit")) {
					// Send notifications
					Translatable depositMessage = Translatable.of("msg_xx_deposited_xx", getSenderFormatted(sender), amount,  Translatable.of("town_sing"));
					TownyMessaging.sendMsg(sender, depositMessage);
					TownyMessaging.sendPrefixedTownMessage(town, depositMessage);
				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_unable_to_deposit_x", amount));
				}

			} else if (split[1].equalsIgnoreCase("withdraw")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_WITHDRAW.getNode());
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translatable.of("msg_err_no_economy"));

				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(Translatable.of("msg_err_invalid_input", "withdraw [amount]"));
				
				int amount = MathUtil.getIntOrThrow(split[2]);
				
				if (town.getAccount().withdraw(amount, "Admin Withdraw")) {				
					// Send notifications
					Translatable withdrawMessage = Translatable.of("msg_xx_withdrew_xx", getSenderFormatted(sender), amount,  Translatable.of("town_sing"));
					TownyMessaging.sendMsg(sender, withdrawMessage);
					TownyMessaging.sendPrefixedTownMessage(town, withdrawMessage);
				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_unable_to_withdraw_x", amount));
				}
			} else if (split[1].equalsIgnoreCase("outlaw")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_OUTLAW.getNode());
				TownCommand.parseTownOutlawCommand(sender, StringMgmt.remArgs(split, 2), true, town);
			} else if (split[1].equalsIgnoreCase("leavenation")) {
				Nation nation = null;
				if (town.hasNation())
					nation = town.getNation();
				else
					throw new TownyException(Translatable.of("That town does not belong to a nation."));
				
				town.removeNation();
				
				plugin.resetCache();

				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_town_left", StringMgmt.remUnderscore(town.getName())));
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));

			} else if (split[1].equalsIgnoreCase("unruin")) {
				// Sets the town to unruined with the existing NPC mayor still in place.
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_UNRUIN.getNode());
				TownRuinUtil.reclaimTown(town.getMayor(), town);
				town.save();
			} else if (split[1].equalsIgnoreCase("checkoutposts")) {
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_CHECKOUTPOSTS.getNode());
				parseAdminCheckOutpostsCommand(sender, town);
			} else if (split[1].equalsIgnoreCase("trust")) {
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_TRUST.getNode());
				TownCommand.parseTownTrustCommand(sender, StringMgmt.remArgs(split, 2), town);
			} else if (split[1].equalsIgnoreCase("trusttown")) {
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_TRUSTTOWN.getNode());
				TownCommand.parseTownTrustTownCommand(sender, StringMgmt.remArgs(split, 2), town);
			} else if (split[1].equalsIgnoreCase("merge")) {
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_MERGE.getNode());
				TownCommand.parseTownMergeCommand(sender, StringMgmt.remArgs(split, 2), town, true);
			} else if (split[1].equalsIgnoreCase("forcemerge")) {
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_FORCEMERGE.getNode());
				if (split.length < 3)
					throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME forcemerge OTHERTOWN"));
				Town remainingTown = townyUniverse.getTown(split[2]);

				if (remainingTown == null || remainingTown.equals(town))
					throw new TownyException(Translatable.of("msg_err_invalid_name", split[2]));
				Confirmation.runOnAccept(() -> {
					townyUniverse.getDataSource().mergeTown(town, remainingTown);
					TownyMessaging.sendGlobalMessage(Translatable.of("town1_has_merged_with_town2", town, remainingTown));
				}).sendTo(sender);
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_TOWN, split[1])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_TOWN, split[1]).execute(sender, split, town);
			} else {
				HelpMenu.TA_TOWN.send(sender);
				return;
			}

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}
		
	}

	private void giveTownBoughtTownBlocks(CommandSender sender, Town town, String[] split) throws TownyException {
		// The number is missing.
		if (split.length == 0)
			throw new TownyException("Eg: /townyadmin town [townname] giveboughtblocks 2");

		if (TownySettings.getMaxBonusBlocks(town) == 0)
			throw new TownyException(Translatable.of("msg_err_town_not_allowed_purchased_blocks", town));

		// Handle removing all bought blocks
		if (split[0].equalsIgnoreCase("unset")) {
			town.setPurchasedBlocks(0);
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_purchased_blocks_unset", town));
			return;
		}

		// Handle modifying the town's bought blocks.
		int blocks = MathUtil.getIntOrThrow(split[0]);
		town.setPurchasedBlocks(Math.max(town.getPurchasedBlocks() + blocks, 0)); // Don't go below 0 or something will probably break.
		town.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_purchased_blocks_changed", town, blocks, town.getPurchasedBlocks()));
	}

	private void setTownLevel(CommandSender sender, Town town, String[] split) throws TownyException {
		// The number is missing.
		if (split.length == 0)
			throw new TownyException("Eg: /townyadmin town [townname] settownlevel 2");
		
		// Handle un-setting the manual override.
		if (split[0].equalsIgnoreCase("unset")) {
			town.setManualTownLevel(-1);
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_level_unset", town, town.getLevel()));
			return;
		}

		// Handle applying a manual override.
		int level = MathUtil.getPositiveIntOrThrow(split[0]);
		if (level > town.getMaxLevel() - 1)
			level = town.getMaxLevel() - 1;
		town.setManualTownLevel(level);
		town.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_town_level_overridden_with", town, level));
	}

	private void parseAdminTownRankCommand(CommandSender sender, Town town, String[] split) throws TownyException {
		if (split.length < 3) {
			throw new TownyException("Eg: /townyadmin town [townname] rank add/remove [resident] [rank]");
		}

		Resident target = getResidentOrThrow(split[1]);

		if (!target.hasTown()) {
			throw new TownyException(Translatable.of("msg_err_resident_doesnt_belong_to_any_town"));
		}
		if (target.getTown() != town) {
			throw new TownyException(Translatable.of("msg_err_townadmintownrank_wrong_town"));
		}

		/*
		 * Match casing to an existing rank, returns null if Town rank doesn't exist.
		 */
		String rank = TownyPerms.matchTownRank(split[2]);
		if (rank == null)
			throw new TownyException(Translatable.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getTownRanks(), ", ")));

		if (split[0].equalsIgnoreCase("add")) {
			if (!target.hasTownRank(rank)) {
				BukkitTools.fireEvent(new TownAddResidentRankEvent(target, rank, town));
				target.addTownRank(rank);
				if (target.isOnline())
					TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_given_rank", "Town", rank));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_given_rank", "Town", rank, target.getName()));
			} else {
				// Must already have this rank
				TownyMessaging.sendMsg(sender, Translatable.of("msg_resident_already_has_rank", target.getName(), "Town"));
				return;
			}

		} else if (split[0].equalsIgnoreCase("remove")) {
			if (target.hasTownRank(rank)) {
				BukkitTools.fireEvent(new TownRemoveResidentRankEvent(target, rank, town));
				target.removeTownRank(rank);
				if (target.isOnline())
					TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_had_rank_taken", "Town", rank));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_taken_rank_from", "Town", rank, target.getName()));
			} else {
				// Doesn't have this rank
				TownyMessaging.sendMsg(sender, Translatable.of("msg_resident_doesnt_have_rank", target.getName(), "Town"));
				return;
			}

		} else {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", split[0]));
			return;
		}

		/*
		 * If we got here we have made a change Save the altered resident
		 * data.
		 */
		target.save();
		
	}

	public void parseAdminNationCommand(CommandSender sender, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_NATION.send(sender);
			return;
		}
		try {
			
			if (split[0].equalsIgnoreCase("new")) {
				if (split.length != 3)
					throw new TownyException(Translatable.of("msg_err_not_enough_variables") + "/ta nation new [name] [capital]");

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_NEW.getNode());
				
				final Town capitalTown = townyUniverse.getTown(split[2]);
				
				if (capitalTown == null)
					throw new TownyException(Translatable.of("msg_err_invalid_name", split[2]));

				NationCommand.newNation(sender, split[1], capitalTown, true);
				return;
			}
			
			Nation nation = townyUniverse.getNation(split[0]);
			
			if (nation == null) {
				throw new TownyException(Translatable.of("msg_err_no_nation_with_that_name", split[0]));
			}
			
			if (split.length == 1) {
				TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(nation, sender));
				return;
			}

			if (split[1].equalsIgnoreCase("add")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_ADD.getNode());
				if (split.length != 3)
					throw new TownyException(Translatable.of("msg_err_not_enough_variables") + "/ta nation [nationname] add [townname]");
				
				townyAdminNationAddTown(sender, nation, StringMgmt.remArgs(split, 2));

			} else if (split[1].equalsIgnoreCase("transfer")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_TRANSFER.getNode());
				if (split.length != 3)
					throw new TownyException(Translatable.of("msg_err_not_enough_variables") + "/ta nation [nationname] transfer [townname]");

				townyAdminNationTransfterTown(sender, nation, StringMgmt.remArgs(split, 2));

			} else if (split[1].equalsIgnoreCase("kick")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_KICK.getNode());
				NationCommand.nationKick(sender, nation, TownyAPI.getInstance().getTowns(StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE.getNode());
				Confirmation.runOnAccept(() -> {
					if (sender instanceof Player)
						TownyMessaging.sendMsg(sender, Translatable.of("nation_deleted_by_admin", nation.getName()));
					
					TownyUniverse.getInstance().getDataSource().removeNation(nation);
					TownyMessaging.sendGlobalMessage(Translatable.of("MSG_DEL_NATION", nation.getName()));
				}).sendTo(sender);

			} else if(split[1].equalsIgnoreCase("recheck")) {
				
				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_RECHECK.getNode());
				nation.removeOutOfRangeTowns();
				TownyMessaging.sendMsg(sender, Translatable.of("nation_rechecked_by_admin", nation.getName()));

			} else if (split[1].equalsIgnoreCase("rename")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_RENAME.getNode());
				String name = String.join("_", StringMgmt.remArgs(split, 2));

				BukkitTools.ifCancelledThenThrow(new NationPreRenameEvent(nation, name));

				if (!NameValidation.isBlacklistName(name) && (TownySettings.areNumbersAllowedInNationNames() || !NameValidation.containsNumbers(name))) {
					townyUniverse.getDataSource().renameNation(nation, name);
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_set_name", getSenderFormatted(sender), nation.getName()));
					TownyMessaging.sendMsg(sender, Translatable.of("msg_nation_set_name", getSenderFormatted(sender), nation.getName()));
				} else
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_invalid_name"));

			} else if (split[1].equalsIgnoreCase("merge")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_MERGE.getNode());
				NationCommand.mergeNation(sender, StringMgmt.remArgs(split, 2), nation, true);
			} else if (split[1].equalsIgnoreCase("forcemerge")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_FORCEMERGE.getNode());
				Nation remainingNation = townyUniverse.getNation(split[2]);
				
				if (remainingNation == null || remainingNation.equals(nation))
					throw new TownyException(Translatable.of("msg_err_invalid_name", split[2]));
				Confirmation.runOnAccept(() -> {
					townyUniverse.getDataSource().mergeNation(nation, remainingNation);
					TownyMessaging.sendGlobalMessage(Translatable.of("nation1_has_merged_with_nation2", nation, remainingNation));
				}).sendTo(sender);

			} else if (split[1].equalsIgnoreCase("set")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_SET.getNode());
				NationCommand.nationSet(sender, StringMgmt.remArgs(split, 2), true, nation);

			} else if (split[1].equalsIgnoreCase("toggle")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_TOGGLE.getNode());
				NationCommand.nationToggle(sender, StringMgmt.remArgs(split, 2), true, nation);
			} else if (split[1].equalsIgnoreCase("bankhistory")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_BANKHISTORY.getNode());
				int pages = 10;
				if (split.length > 2)
					try {
						pages = Integer.parseInt(split[2]);
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_int"));
						return;
					}

				nation.generateBankHistoryBook(catchConsole(sender), pages);
			} else if (split[1].equalsIgnoreCase("deposit")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DEPOSIT.getNode());
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translatable.of("msg_err_no_economy"));
				
				int amount;
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(Translatable.of("msg_err_invalid_input", "deposit [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_int"));
					return;
				}

				nation.getAccount().deposit(amount, "Admin Deposit");
				
				// Send notifications
				Translatable depositMessage = Translatable.of("msg_xx_deposited_xx", getSenderFormatted(sender), amount,  Translatable.of("nation_sing"));
				TownyMessaging.sendMsg(sender, depositMessage);
				TownyMessaging.sendPrefixedNationMessage(nation, depositMessage);
			}
			else if (split[1].equalsIgnoreCase("withdraw")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_WITHDRAW.getNode());
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translatable.of("msg_err_no_economy"));
				
				int amount;
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(Translatable.of("msg_err_invalid_input", "withdraw [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_int"));
					return;
				}

				nation.getAccount().withdraw(amount, "Admin Withdraw");
				
				// Send notifications
				Translatable withdrawMessage = Translatable.of("msg_xx_withdrew_xx", getSenderFormatted(sender), amount,  Translatable.of("nation_sing"));
				TownyMessaging.sendMsg(sender, withdrawMessage);
				TownyMessaging.sendPrefixedNationMessage(nation, withdrawMessage);
			}
			else if (split[1].equalsIgnoreCase("rank")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_RANK.getNode());
				if (split.length < 5) {
					HelpMenu.TA_NATION_RANK.send(sender);
					return;
				}
				Resident target;
				String rank;

				try {
					target = getResidentOrThrow(split[3]);
				} catch (TownyException exception) {
					TownyMessaging.sendMessage(sender, exception.getMessage());
					return;
				}
				
				if (!target.hasTown() || !target.getTownOrNull().hasNation() || !target.getTownOrNull().getNationOrNull().getUUID().equals(nation.getUUID()))
					throw new TownyException(Translatable.of("msg_err_that_resident_doesnt_belong_to_that_nation"));
				
				rank = TownyPerms.matchNationRank(split[4]);
				if (rank == null) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_unknown_rank_available_ranks", split[4], StringMgmt.join(TownyPerms.getNationRanks(), ", ")));
					return;
				}

				switch(split[2].toLowerCase()) {
					case "add":
						if (!target.hasNationRank(rank)) {
							BukkitTools.fireEvent(new NationRankAddEvent(nation, rank, target));
							target.addNationRank(rank);
							if (target.isOnline()) {
								TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_given_rank", "Nation", rank));
								plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
							}
							TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_given_rank", "Nation", rank, target.getName()));
							target.save();
							return;
						} else {
							// Already has the rank.
							TownyMessaging.sendMsg(sender, Translatable.of("msg_resident_already_has_rank", target.getName(), "Nation"));
							return;
						}
					case "remove":
						if (target.hasNationRank(rank)) {
							BukkitTools.fireEvent(new NationRankRemoveEvent(nation, rank, target));
							target.removeNationRank(rank);
							if (target.isOnline()) {
								TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_had_rank_taken", "Nation", rank));
								plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
							}
							TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_taken_rank_from", "Nation", rank, target.getName()));
							target.save();
							return;
						} else {
							// Doesn't have the rank.
							TownyMessaging.sendMsg(sender, Translatable.of("msg_resident_doesnt_have_rank", target.getName(), "Nation"));
							return;
						}
					default:
						HelpMenu.TA_NATION_RANK.send(sender);
						return;
				}
			} else if (split[1].equalsIgnoreCase("ally")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_ALLY.getNode());
				if (split.length < 4) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "/ta nation [nation] ally [add/remove] [nation]"));
					return;
				}
				
				Nation ally = townyUniverse.getNation(split[3]);
				if (ally == null || ally.equals(nation)) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_name", split[3]));
					return;
				}

				if (split[2].equalsIgnoreCase("add")) {
					if (!nation.hasAlly(ally)) {
						if (nation.hasEnemy(ally))
							nation.removeEnemy(ally);
						
						if (ally.hasEnemy(nation))
							ally.removeEnemy(nation);
						
						nation.addAlly(ally);
						nation.save();

						ally.addAlly(nation);
						ally.save();

						plugin.resetCache();
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_added_ally", ally.getName()));
						TownyMessaging.sendPrefixedNationMessage(ally, Translatable.of("msg_added_ally", nation.getName()));
						TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_allies_enemies_updated", nation.getName()));
					} else
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_already_allied_with_2", nation.getName(), ally.getName()));
				} else if (split[2].equalsIgnoreCase("remove")) {
					if (nation.hasAlly(ally)) {
						nation.removeAlly(ally);
						nation.save();

						ally.removeAlly(nation);
						ally.save();

						plugin.resetCache();
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_removed_ally", ally.getName()));
						TownyMessaging.sendPrefixedNationMessage(ally, Translatable.of("msg_removed_ally", nation.getName()));
						TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_allies_enemies_updated", nation.getName()));
					} else
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_not_allied_with_2", nation.getName(), ally.getName()));
				} else
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "/ta nation [nation] ally [add/remove] [nation]"));
			} else if (split[1].equalsIgnoreCase("enemy")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_ENEMY.getNode());
				if (split.length < 4) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "/ta nation [nation] enemy [add/remove] [nation]"));
					return;
				}
				
				Nation enemy = townyUniverse.getNation(split[3]);
				if (enemy == null || enemy.equals(nation)) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_name", split[3]));
					return;
				}

				if (split[2].equalsIgnoreCase("add")) {
					if (!nation.hasEnemy(enemy)) {
						if (nation.hasAlly(enemy)) {
							nation.removeAlly(enemy);
							enemy.removeAlly(nation);
							plugin.resetCache();
						}

						nation.addEnemy(enemy);
						nation.save();
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_enemy_nations", getSenderFormatted(sender), enemy.getName()));
						TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_allies_enemies_updated", nation.getName()));
					} else
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_already_enemies_with_2", nation.getName(), enemy.getName()));
				} else if (split[2].equalsIgnoreCase("remove")) {
					if (nation.hasEnemy(enemy)) {
						nation.removeEnemy(enemy);
						nation.save();
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_enemy_to_neutral", getSenderFormatted(sender), enemy.getName()));
						TownyMessaging.sendPrefixedNationMessage(enemy, Translatable.of("msg_removed_enemy", nation.getName()));
						TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_allies_enemies_updated", nation.getName()));
					} else
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_not_enemies_with_2", nation.getName(), enemy.getName()));
				} else
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "/ta nation [nation] enemy [add/remove] [nation]"));
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_NATION, split[1])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_NATION, split[1]).execute(sender, split, nation);
			}

		} catch (NotRegisteredException | AlreadyRegisteredException | InvalidNameException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}
	}

	private String getSenderFormatted(CommandSender sender) {
		return sender instanceof Player player ? player.getName() : "Console";
	}

	/**
	 * Force-join command for admins which will bypass the invite system.
	 * This also bypasses other limits Towny imposes on towns and nations
	 * such as the max-towns-per-nation and nation-proximity, and doesn't
	 * fire a cancellable pre-join event either. Any admin who runs this
	 * can be assumed to know what they want. 
	 * @param sender CommandSender
	 * @param nation Nation which will have a town added.
	 * @param townName Name of Town to add to Nation.
	 */
	private void townyAdminNationAddTown(CommandSender sender, Nation nation, String[] townName) {

		Town town = TownyUniverse.getInstance().getTown(townName[0]);
		
		if (town == null) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_name", townName[0]));
			return;
		}

		if (town.hasNation()) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_already_nation"));
			TownyMessaging.sendMessage(sender, "Suggestion: /townyadmin town " + town + "leavenation, or /ta nation " + nation + " transfer " + town);
		} else {
			try {
				town.setNation(nation);
			} catch (AlreadyRegisteredException ignored) {}
			town.save();
			TownyMessaging.sendNationMessagePrefixed(nation, Translatable.of("msg_join_nation", town.getName()));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_join_nation", town.getName()));
		} 
	}
	
	private void townyAdminNationTransfterTown(CommandSender sender, Nation nation, String[] townName) {
		Town town = TownyUniverse.getInstance().getTown(townName[0]);
		
		if (town == null) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_name", townName[0]));
			return;
		}
		
		if (town.hasNation()) {
			nation = town.getNationOrNull();
			town.removeNation();
			plugin.resetCache();
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_town_left", StringMgmt.remUnderscore(town.getName())));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));
		}
		try {
			town.setNation(nation);
		} catch (AlreadyRegisteredException ignored) {}
		town.save();
		TownyMessaging.sendNationMessagePrefixed(nation, Translatable.of("msg_join_nation", town.getName()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_join_nation", town.getName()));
	}

	private void adminSet(CommandSender sender, String[] split) throws TownyException {
		if (split.length == 0) {
			HelpMenu.TA_SET.send(sender);
			return;
		}

		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split[0].equalsIgnoreCase("mayor")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_MAYOR.getNode());
			
			if (split.length < 3) {
				HelpMenu.TA_SET_MAYOR.send(sender);
			} else
				try {
					Town town = getTownOrThrow(split[1]);
					@Nullable Resident oldMayor = town.getMayor();
					boolean deleteOldMayor = oldMayor != null && oldMayor.isNPC();
					
					// New mayor is either an NPC resident or a resident by name from split[2].
					Resident newMayor = split[2].equalsIgnoreCase("npc") 
							? ResidentUtil.createAndGetNPCResident()
							: getResidentOrThrow(split[2]);

					// Add the new Mayor to the town if need be.
					if (!town.hasResident(newMayor))
						TownCommand.townAddResident(town, newMayor);

					// Set the new mayor.
					town.setMayor(newMayor);
					
					// Reset caches and permissions.
					if (!deleteOldMayor && oldMayor.isOnline()) {
						Towny.getPlugin().deleteCache(oldMayor);
						TownyPerms.assignPermissions(oldMayor, oldMayor.getPlayer());
					}
					if (newMayor.isOnline() && !newMayor.isNPC())
						Towny.getPlugin().deleteCache(newMayor);

					// If the previous mayor was an NPC make sure they're removed from the database.
					if (deleteOldMayor)
						townyUniverse.getDataSource().removeResident(oldMayor);

					// NPC mayors set their towns to not pay any upkeep.
					town.setHasUpkeep(!newMayor.isNPC());

					town.save();
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_new_mayor", newMayor));
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(sender, e.getMessage());
				}

		} else if (split[0].equalsIgnoreCase("capital")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_CAPITAL.getNode());

			if (split.length < 2) {
				HelpMenu.TA_SET_CAPITAL.send(sender);
			} else {
				final Town newCapital = townyUniverse.getTown(split[1]);
				
				if (newCapital == null) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", split[1]));
					return;
				}
				
				try {
					Nation nation = newCapital.getNation();
					NationCommand.nationSet(sender, split, true, nation);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(sender, e.getMessage());
				}

			}
		} else if (split[0].equalsIgnoreCase("title")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_TITLE.getNode());
			
			Resident resident = null;
			// Give the resident a title
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(sender, "Eg: /townyadmin set title bilbo Jester");
			else
				resident = getResidentOrThrow(split[1]);

			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_input_too_long"));
				return;
			}

			String title = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setTitle(title + " ");
			resident.save();

			if (resident.hasTitle()) {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle())));
				TownyMessaging.sendMsg(resident, Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle())));
			} else {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_title_surname", "Title", resident.getName()));
				TownyMessaging.sendMsg(resident, Translatable.of("msg_clear_title_surname", "Title", resident.getName()));
			}

		} else if (split[0].equalsIgnoreCase("surname")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_SURNAME.getNode());
			
			Resident resident = null;
			// Give the resident a surname
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(sender, "Eg: /townyadmin set surname bilbo Jester");
			else
				resident = getResidentOrThrow(split[1]);

			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_input_too_long"));
				return;
			}

			String surname = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setSurname(surname + " ");
			resident.save();

			if (resident.hasSurname()) {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname())));
				TownyMessaging.sendMsg(resident, Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname())));
			} else {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_title_surname", "Surname", resident.getName()));
				TownyMessaging.sendMsg(resident, Translatable.of("msg_clear_title_surname", "Surname", resident.getName()));
			}
		} else if (split[0].equalsIgnoreCase("nationzoneoverride")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_NATIONZONE.getNode());
			
			if (split.length < 2) {
				HelpMenu.TA_SET_NATIONZONE.send(sender);
				return;
			}
			
			Town town = townyUniverse.getTown(split[1]);
			if (town == null)
				throw new TownyException(Translatable.of("msg_err_not_registered_1", split[1]));

			int size = MathUtil.getPositiveIntOrThrow(split[2]);
			
			town.setNationZoneOverride(size);
			town.save();
			if (size == 0)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_nationzone_override_removed", town.getName()));
			else 
				TownyMessaging.sendMsg(sender, Translatable.of("msg_nationzone_override_set_to", town.getName(), size));
			
		} else if (split[0].equalsIgnoreCase("plot")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_PLOT.getNode());
			
			if (split.length < 2) {
				HelpMenu.TA_SET_PLOT.send(sender);
				return;
			}
			
			final Player player = catchConsole(sender);
			TownBlock tb = TownyAPI.getInstance().getTownBlock(player);
			if (tb != null) {
				Town newTown = townyUniverse.getTown(split[1]);
				
				if (newTown != null) {
					tb.setResident(null);
					tb.setTown(newTown);
					tb.setType(TownBlockType.RESIDENTIAL);
					tb.setName("");
					tb.save();
					TownyMessaging.sendMsg(player, Translatable.of("changed_plot_town", newTown.getName()));
				}
				else {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", split[1]));
				}
			} else {
				Town town = townyUniverse.getTown(split[1]);
				
				if (town == null) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", split[1]));
					return;
				}
				
				TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld().getName());
				Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());
				List<WorldCoord> selection;
				if (split.length == 2)
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), new String[0], true);
				else  {
					String[] newSplit = StringMgmt.remFirstArg(split);
					newSplit = StringMgmt.remFirstArg(newSplit);
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), newSplit, true);
				}
				TownyMessaging.sendDebugMsg("Admin Initiated townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = AreaSelectionUtil.filterOutTownOwnedBlocks(selection);
				TownyMessaging.sendDebugMsg("Admin Initiated townClaim: Post-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				
				new TownClaim(plugin, player, town, selection, false, true, false).start();

			}
		} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_SET, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_SET, split[0]).execute(sender, "townyadmin", split);
		} else {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "administrative"));
		}
	}

	public void reloadLangs(CommandSender sender) {
		Translation.loadTranslationRegistry();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_reloaded_lang"));
	}
	
	public void reloadPerms(CommandSender sender) {
		try {
			plugin.loadPermissions(true);
		} catch (TownyInitException tie) {
			TownyMessaging.sendErrorMsg(sender, "Error Loading townyperms.yml!");
			TownyMessaging.sendErrorMsg(tie.getMessage());
			// Place Towny in Safe Mode while the townyperms.yml is unreadable.
			plugin.addError(tie.getError());
			return;
		}
		TownyMessaging.sendMsg(sender, Translatable.of("msg_reloaded_perms"));
		
	}

	/**
	 * Reloads only the config
	 * 
	 * @param reset Whether or not to reset the config.
	 */
	public void reloadConfig(CommandSender sender, boolean reset) {

		if (reset) {
			TownyUniverse.getInstance().getDataSource().deleteFile(plugin.getConfigPath());
			TownyMessaging.sendMsg(sender, Translatable.of("msg_reset_config"));
		}
		
		try {
			TownySettings.loadConfig(Paths.get(TownyUniverse.getInstance().getRootFolder()).resolve("settings").resolve("config.yml"), plugin.getVersion());
			TownySettings.loadTownLevelConfig();   // TownLevel and NationLevels are not loaded in the config,
			TownySettings.loadNationLevelConfig(); // but later so the config-migrator can do it's work on them if needed.
			Translation.loadTranslationRegistry();
			TownBlockTypeHandler.initialize();
		} catch (IOException e) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_reload_error"));
			e.printStackTrace();
			return;
		}
		
		TownyMessaging.sendMsg(sender, Translatable.of("msg_reloaded_config"));
	}

	/**
	 * Reloads both the database and the config. Used with a database reload command.
	 *
	 */
	public void reloadDatabase(CommandSender sender) {
		TownyUniverse.getInstance().getDataSource().finishTasks();
		try {
			plugin.loadFoundation(true);
		} catch (TownyInitException tie) {
			TownyMessaging.sendErrorMsg(tie.getMessage());
			
			plugin.addError(tie.getError());
			return;
		}
		// Register all child permissions for ranks
		TownyPerms.registerPermissionNodes();

		// Update permissions for all online players
		TownyPerms.updateOnlinePerms();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_reloaded_db"));
	}

	/**
	 * Remove residents who havn't logged in for X amount of days.
	 * 
	 * @param split - Current command arguments.
	 * @throws TownyException when an error message needs to be returned.
	 */
	public void purge(CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0) {
			// command was '/townyadmin purge'
			HelpMenu.TA_PURGE.send(sender);
			return;
		}

		checkPermOrThrowWithMessage(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PURGE.getNode(), Translatable.of("msg_err_admin_only"));

		boolean townless = split.length == 2 && split[1].equalsIgnoreCase("townless");
		Town town = !townless && split.length == 2 ? TownyUniverse.getInstance().getTown(split[1]) : null;
		if (!townless && town == null)
			throw new TownyException(Translatable.of("msg_err_not_registered_1", split[1]));

		try {
			int days = Integer.parseInt(split[0]);
			Confirmation.runOnAccept(() -> 
				new ResidentPurge(plugin, sender, TimeTools.getMillis(days + "d"), townless, town).start()
			).sendTo(sender);
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_int"));
		}
	}

	/**
	 * Delete a resident and it's data file (if not online) Available Only to
	 * players with the 'towny.admin' permission node.
	 * 
	 * @param sender - Sender who ran the command.
	 * @param name - Name of the resident to delete.
	 */
	public void residentDelete(CommandSender sender, String name) {

		Resident resident = TownyUniverse.getInstance().getResident(name);
		if (resident != null) {
			Player player = null;
			if (resident.isOnline())
				player = resident.getPlayer();

			TownyUniverse.getInstance().getDataSource().removeResident(resident);
			TownyMessaging.sendMsg(sender, Translatable.of("msg_del_resident", resident.getName()));
			
			if (player != null)
				Bukkit.getScheduler().runTask(plugin, new OnPlayerLogin(plugin, player));
		} else {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_name", name));
		}
	}

	public void parseToggleCommand(CommandSender sender, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2) {
			choice = BaseCommand.parseToggleChoice(split[1]);
		} else if (split.length == 3 && split[1].equalsIgnoreCase("npc")) {
			choice = BaseCommand.parseToggleChoice(split[2]);
		}

		if (split.length == 0) {
			// command was '/townyadmin toggle'
			HelpMenu.TA_TOGGLE.send(sender);
			return;
		}

		if (split[0].equalsIgnoreCase("wildernessuse")) {
			// Toggles build/destroy/switch/itemuse on or off in all worlds. True is the default, for installation setup to alter the defaulted false.
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_WILDERNESSUSE.getNode());
			toggleWildernessUsage(choice.orElse(true));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_wilderness_use_x_in_all_worlds", choice.orElse(true)));
		} else if (split[0].equalsIgnoreCase("regenerations")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_REGENERATIONS.getNode());
			toggleRegenerations(choice.orElse(false));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_regenerations_use_x_in_all_worlds", choice.orElse(false)));
		} else if (split[0].equalsIgnoreCase("devmode")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_DEVMODE.getNode());
			try {
				TownySettings.setDevMode(choice.orElse(!TownySettings.isDevMode()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_toggle_devmode", (TownySettings.isDevMode() ? Colors.Green + Translatable.of("enabled") : Colors.Red + Translatable.of("disabled"))));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("debug")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_DEBUG.getNode());
			try {
				TownySettings.setDebug(choice.orElse(!TownySettings.getDebug()));
				TownyLogger.getInstance().refreshDebugLogger();
				TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_toggle_debugmode", (TownySettings.getDebug() ? Colors.Green + Translatable.of("enabled") : Colors.Red + Translatable.of("disabled"))));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("townwithdraw")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_TOWNWITHDRAW.getNode());
			try {
				TownySettings.SetTownBankAllowWithdrawls(choice.orElse(!TownySettings.getTownBankAllowWithdrawls()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_toggle_townwithdraw", (TownySettings.getTownBankAllowWithdrawls() ? Colors.Green + Translatable.of("enabled") : Colors.Red + Translatable.of("disabled"))));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("nationwithdraw")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_NATIONWITHDRAW.getNode());
			try {
				TownySettings.SetNationBankAllowWithdrawls(choice.orElse(!TownySettings.getNationBankAllowWithdrawls()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_toggle_nationwithdraw", (TownySettings.getNationBankAllowWithdrawls() ? Colors.Green + Translatable.of("enabled") : Colors.Red + Translatable.of("disabled"))));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_choice"));
			}
			
		} else if (split[0].equalsIgnoreCase("npc")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_NPC.getNode());
			if (split.length != 2)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: toggle npc [resident]"));
			
			Resident resident = townyUniverse.getResident(split[1]);
			
			if (resident == null) {
				throw new TownyException(Translatable.of("msg_err_not_registered_1", split[1]));
			}

			resident.setNPC(!resident.isNPC());

			resident.save();

			TownyMessaging.sendMsg(sender, Translatable.of("msg_npc_flag", resident.isNPC(), resident.getName()));
		} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_TOGGLE, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_TOGGLE, split[0]).execute(sender, "townyadmin", split);
		} else {
			// parameter error message
			// peaceful/war/townmobs/worldmobs
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_choice"));
		}
	}

	private void toggleRegenerations(boolean choice) {
		for (TownyWorld world : new ArrayList<>(TownyUniverse.getInstance().getTownyWorlds())) {
			world.setUsingPlotManagementRevert(choice);
			world.setUsingPlotManagementWildBlockRevert(choice);
			world.setUsingPlotManagementWildEntityRevert(choice);
			world.save();
		}
	}
	
	private void toggleRevertUnclaim(boolean choice) {
		for (TownyWorld world : new ArrayList<>(TownyUniverse.getInstance().getTownyWorlds())) {
			world.setUsingPlotManagementRevert(choice);
			world.save();
		}
	}

	private void toggleWildernessUsage(boolean choice) {
		for (TownyWorld world : new ArrayList<>(TownyUniverse.getInstance().getTownyWorlds())) {
			world.setUnclaimedZoneBuild(choice);
			world.setUnclaimedZoneDestroy(choice);
			world.setUnclaimedZoneSwitch(choice);
			world.setUnclaimedZoneItemUse(choice);
			world.save();
		}
	}

	public static void handleTownMetaCommand(CommandSender sender, Town town, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_META.getNode());

		if (split.length == 2) {
			if (town.hasMeta()) {
				TownyMessaging.sendMessage(sender, ChatTools.formatTitle("Custom Meta Data"));
				for (CustomDataField<?> field : town.getMetadata()) {
					TownyMessaging.sendMessage(sender, field.getKey() + " = " + field.getValue());
				}
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_this_town_doesnt_have_any_associated_metadata"));
			}

			return;
		}

		if (split.length < 4) {
			HelpMenu.TA_TOWN_META.send(sender);
			return;
		}
		
		final String mdKey = split[3];
		
		if (split[2].equalsIgnoreCase("set")) {
			String val = split.length == 5 ? split[4] : null;
			
			if (town.hasMeta() && town.hasMeta(mdKey)) {
				CustomDataField<?> cdf = town.getMetadata(mdKey);

				// Check if the given value is valid for this field.
				try {
					if (val == null)
						throw new InvalidMetadataTypeException(cdf);

					cdf.isValidType(val);
				} catch (InvalidMetadataTypeException e) {
					TownyMessaging.sendErrorMsg(sender, e.getMessage());
					return;
				}

				// Change state
				cdf.setValueFromString(val);

				// Let user know that it was successful.
				TownyMessaging.sendMsg(sender, Translatable.of("msg_key_x_was_successfully_updated_to_x", mdKey, cdf.getValue()));

				// Save changes.
				town.save();
			}
			else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_key_x_is_not_part_of_this_town", mdKey));
			}
		} else if (split[2].equalsIgnoreCase("add")) {

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)){
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_the_metadata_for_key_is_not_registered", mdKey));
				return;
			}
			
			CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

			if (town.hasMeta() && town.hasMeta(md.getKey())) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_key_x_already_exists", mdKey));
				return;
			}

			TownyMessaging.sendMsg(sender, Translatable.of("msg_custom_data_was_successfully_added_to_town"));
			
			town.addMetaData(md.clone(), true);
			
		} else if (split[2].equalsIgnoreCase("remove")) {

			if (town.hasMeta() && town.hasMeta(mdKey)) {
				town.removeMetaData(mdKey, true);
				TownyMessaging.sendMsg(sender, Translatable.of("msg_data_successfully_deleted"));
			} else 
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_key_cannot_be_deleted"));
		}
	}
	
	public static boolean handlePlotMetaCommand(Player player, String[] split) throws TownyException {
		
		String world = player.getWorld().getName();
		TownBlock townBlock = null;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		try {
			townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
			return false;
		}

		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_META.getNode());
		
		if (split.length == 1) {
			if (townBlock.hasMeta()) {
				TownyMessaging.sendMessage(player, ChatTools.formatTitle("Custom Meta Data"));
				for (CustomDataField<?> field : townBlock.getMetadata()) {
					TownyMessaging.sendMessage(player, field.getKey() + " = " + field.getValue());
				}
			} else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_this_plot_doesnt_have_any_associated_metadata"));
			}

			return true;
		}

		if (split.length < 3) {
			HelpMenu.TA_PLOT_META.send(player);
			return false;
		}

		final String mdKey = split[2];
		
		if (split[1].equalsIgnoreCase("set")) {
			String val = split.length == 4 ? split[3] : null;
			
			if (townBlock.hasMeta() && townBlock.hasMeta(mdKey)) {
				CustomDataField<?> cdf = townBlock.getMetadata(mdKey);

				// Change state
				try {
					if (val == null)
						throw new InvalidMetadataTypeException(cdf); 
							
					cdf.isValidType(val);
				} catch (InvalidMetadataTypeException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
					return false;
				}

				cdf.setValueFromString(val);

				// Let user know that it was successful.
				TownyMessaging.sendMsg(player, Translatable.of("msg_key_x_was_successfully_updated_to_x", mdKey, cdf.getValue()));

				// Save changes.
				townBlock.save();
				return true;
			}
			else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_key_x_is_not_part_of_this_plot", mdKey));
				return false;
			}
		} else if (split[1].equalsIgnoreCase("add")) {

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_the_metadata_for_key_is_not_registered", mdKey));
				return false;
			}

			CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);
			if (townBlock.hasMeta() && townBlock.hasMeta(md.getKey())) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_key_x_already_exists", mdKey));
				return false;
			}

			TownyMessaging.sendMsg(player, Translatable.of("msg_custom_data_was_successfully_added_to_townblock"));

			townBlock.addMetaData(md.clone());
			
		} else if (split[1].equalsIgnoreCase("remove")) {

			if (townBlock.hasMeta() && townBlock.hasMeta(mdKey)) {
				CustomDataField<?> cdf = townBlock.getMetadata(mdKey);
				townBlock.removeMetaData(cdf);
				TownyMessaging.sendMsg(player, Translatable.of("msg_data_successfully_deleted"));
				return true;
			}

			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_key_cannot_be_deleted"));
			return false;
		}
		
		return true;
	}
	
	private void parseAdminDepositAllCommand(CommandSender sender, String[] split) throws TownyException {
		if (split.length != 1) {
			HelpMenu.TA_DEPOSITALL.send(sender);
			return;
		}
		String reason = "townyadmin depositall";
		double amount = MoneyUtil.getMoneyAboveZeroOrThrow(split[0]);
		for (Nation nation : TownyUniverse.getInstance().getNations())
			nation.getAccount().deposit(amount, reason);
		
		for (Town town : TownyUniverse.getInstance().getTowns())
			town.getAccount().deposit(amount, reason);

		TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_deposit_all_success", TownyEconomyHandler.getFormattedBalance(amount)));
	}

	private void parseAdminResetBanksCommand(CommandSender sender, String[] args) throws TownyException {
		final double amount = args.length == 1 ? MathUtil.getIntOrThrow(args[0]) : 0.0;
 		Confirmation.runOnAccept(() -> {
 			for (Town town : TownyUniverse.getInstance().getTowns())
 				town.getAccount().setBalance(amount, "Admin used /ta resetbanks");
 			
 			for (Nation nation : TownyUniverse.getInstance().getNations())
 				nation.getAccount().setBalance(amount, "Admin used /ta resetbanks");
 		})
 		.setTitle(Translatable.of("confirmation_are_you_sure_you_want_to_reset_all_banks", TownyEconomyHandler.getFormattedBalance(amount)))
 		.sendTo(sender);
	}
}
