package com.djrapitops.plan.command.commands;

import com.djrapitops.plan.Phrase;
import com.djrapitops.plan.Plan;
import com.djrapitops.plan.PlanLiteHook;
import com.djrapitops.plan.utilities.UUIDFetcher;
import com.djrapitops.plan.command.CommandType;
import com.djrapitops.plan.command.SubCommand;

import com.djrapitops.plan.data.cache.InspectCacheHandler;
import com.djrapitops.plan.utilities.MiscUtils;
import java.util.UUID;
import main.java.com.djrapitops.plan.Settings;
import org.bukkit.Bukkit;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import static org.bukkit.Bukkit.getOfflinePlayer;

/**
 *
 * @author Rsl1122
 */
public class InspectCommand extends SubCommand {

    private Plan plugin;
    private InspectCacheHandler inspectCache;

    /**
     * Class Constructor.
     *
     * @param plugin Current instance of Plan
     */
    public InspectCommand(Plan plugin) {
        super("inspect", "plan.inspect", "Inspect Player's Data", CommandType.CONSOLE_WITH_ARGUMENTS, "<player>");

        this.plugin = plugin;
        inspectCache = plugin.getInspectCache();
    }

    /**
     * Subcommand inspect.
     *
     * Adds player's data from DataCache/DB to the InspectCache for amount of
     * time specified in the config, and clears the data from Cache with a timer
     * task.
     *
     * @param sender
     * @param cmd
     * @param commandLabel
     * @param args Player's name or nothing - if empty sender's name is used.
     * @return true in all cases.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        final boolean useAlternativeIP = Settings.SHOW_ALTERNATIVE_IP.isTrue();
        if (!Settings.WEBSERVER_ENABLED.isTrue()) {
            if (!useAlternativeIP) {
                PlanLiteHook planLiteHook = plugin.getPlanLiteHook();
                if (Settings.USE_ALTERNATIVE_UI.isTrue() && planLiteHook.isEnabled()) {
                    sender.sendMessage(Phrase.CMD_PASS_PLANLITE + "");
                    planLiteHook.passCommand(sender, cmd, commandLabel, args);
                } else {
                    sender.sendMessage(Phrase.ERROR_WEBSERVER_OFF_INSPECT + "");
                }
                return true;
            }
        }
        String playerName = MiscUtils.getPlayerDisplayname(args, sender);

        UUID uuid;
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName);
            if (uuid == null) {
                throw new Exception("Username doesn't exist.");
            }
        } catch (Exception e) {
            sender.sendMessage(Phrase.USERNAME_NOT_VALID.toString());
            return true;
        }
        OfflinePlayer p = getOfflinePlayer(uuid);
        if (!p.hasPlayedBefore()) {
            sender.sendMessage(Phrase.USERNAME_NOT_SEEN.toString());
            return true;
        }
        if (!plugin.getDB().wasSeenBefore(uuid)) {
            sender.sendMessage(Phrase.USERNAME_NOT_KNOWN.toString());
            return true;
        }
        sender.sendMessage(Phrase.GRABBING_DATA_MESSAGE + "");
        inspectCache.cache(uuid);
        final int port = Settings.WEBSERVER_PORT.getNumber();
        final String alternativeIP = Settings.ALTERNATIVE_IP.toString().replaceAll("%port%", "" + port);
        int configValue = Settings.CLEAR_INSPECT_CACHE.getNumber();
        if (configValue <= 0) {
            configValue = 4;
        }
        final int available = configValue;
        (new BukkitRunnable() {
            @Override
            public void run() {
                if (inspectCache.isCached(uuid)) {
                    sender.sendMessage(Phrase.CMD_INSPECT_HEADER + playerName);
                    // Link
                    String url = "http://" + (useAlternativeIP ? alternativeIP : plugin.getServer().getIp() + ":" + port)
                            + "/player/" + playerName;
                    String message = Phrase.CMD_LINK+"";
                    boolean console = !(sender instanceof Player);
                    if (console) {
                        sender.sendMessage(message + url);
                    } else {
                        sender.sendMessage(message);
                        Player player = (Player) sender;
                        Bukkit.getServer().dispatchCommand(
                                Bukkit.getConsoleSender(),
                                "tellraw " + player.getName() + " [\"\",{\"text\":\""+Phrase.CMD_CLICK_ME+"\",\"underlined\":true,"
                                + "\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + url + "\"}}]");
                    }

                    sender.sendMessage(Phrase.CMD_RESULTS_AVAILABLE.parse(available+""));
                    sender.sendMessage(Phrase.CMD_FOOTER+"");
                    this.cancel();
                }
            }
        }).runTaskTimer(plugin, 1 * 20, 5 * 20);
        return true;
    }
}