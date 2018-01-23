package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.util.TimeTools;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class ConfirmationHandler {
	private static Towny plugin;

	public static void initialize(Towny plugin) {
		ConfirmationHandler.plugin = plugin;
	}

	private static HashMap<Resident, Town> towndeleteconfirmations = new HashMap<Resident, Town>();
	private static HashMap<Resident, Town> townunclaimallconfirmations = new HashMap<Resident, Town>();
	private static HashMap<Resident, Nation> nationdeleteconfirmations = new HashMap<Resident, Nation>();
	private static HashMap<Resident, Integer> townypurgeconfirmations = new HashMap<Resident, Integer>();

	public static void addConfirmation(final Resident r, final ConfirmationType type, Object extra) throws TownyException {
		// We use "extra" in certain instances like the number of days for something e.t.c
		if (type == ConfirmationType.TOWNDELETE) {
			r.setConfirmationType(type);
			towndeleteconfirmations.put(r, r.getTown()); // The good thing is, using the "put" option we override the past one!

			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						removeConfirmation(r, type);
					} catch (TownyException e) {
						// Shouldn't be possible since we added it in the first place!
					}
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.PURGE) {
			r.setConfirmationType(type);
			townypurgeconfirmations.put(r, (Integer) extra); // However an add option doesn't overridee so, we need to check if it exists first.
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						removeConfirmation(r, type);
					} catch (TownyException e) {
						// Shouldn't be possible since we added it in the first place!
					}
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.UNCLAIMALL) {

			r.setConfirmationType(type);
			townunclaimallconfirmations.put(r, r.getTown());

			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						removeConfirmation(r, type);
					} catch (TownyException e) {
						// Shouldn't be possible since we added it in the first place!
					}
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.NATIONDELETE) {

			r.setConfirmationType(type);
			nationdeleteconfirmations.put(r, r.getTown().getNation());

			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						removeConfirmation(r, type);
					} catch (TownyException e) {
						// Shouldn't be possible since we added it in the first place!
					}
				}
			}.runTaskLater(plugin, 400);
		}
	}

	public static void removeConfirmation(Resident r, ConfirmationType type) throws TownyException {
		if (type == ConfirmationType.TOWNDELETE) {
			towndeleteconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.PURGE) {
			townypurgeconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.UNCLAIMALL) {
			townunclaimallconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.NATIONDELETE) {
			nationdeleteconfirmations.remove(r);
			r.setConfirmationType(null);
		}
	}

	public static void handleConfirmation(Resident r, ConfirmationType type) throws TownyException {
		if (type == ConfirmationType.TOWNDELETE) {
			if (towndeleteconfirmations.containsKey(r)) {
				if (towndeleteconfirmations.get(r).equals(r.getTown())) {
					TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(towndeleteconfirmations.get(r)));
					TownyUniverse.getDataSource().removeTown(towndeleteconfirmations.get(r));
					return;
				}
			}
		}
		if (type == ConfirmationType.PURGE) {
			if (townypurgeconfirmations.containsKey(r)) {
				if (!TownyUniverse.getPermissionSource().testPermission(TownyUniverse.getPlayer(r), PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PURGE.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only"));
				}
				int days = townypurgeconfirmations.get(r);

				new ResidentPurge(plugin, null, TimeTools.getMillis(days + "d")).start();

			}
		}
		if (type == ConfirmationType.UNCLAIMALL) {
			if (townunclaimallconfirmations.containsKey(r)) {
				if (townunclaimallconfirmations.get(r).equals(r.getTown())) {
					TownClaim.townUnclaimAll(plugin, townunclaimallconfirmations.get(r));
					removeConfirmation(r, type);
					return;
				}
			}
		}
		if (type == ConfirmationType.NATIONDELETE) {
			if (nationdeleteconfirmations.containsKey(r)) {
				if (nationdeleteconfirmations.get(r).equals(r.getTown().getNation())) {
					TownyUniverse.getDataSource().removeNation(nationdeleteconfirmations.get(r));
					TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nationdeleteconfirmations.get(r)));
					return;
				}
			}
		}
	}

}
