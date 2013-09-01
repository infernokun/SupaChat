package me.javoris767.supachat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.massivecraft.factions.entity.UPlayer;

public class PlayerListener implements Listener
{
	private SupaChat plugin;

	public PlayerListener(SupaChat supachat)
	{
		plugin = supachat;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player p = event.getPlayer();
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				plugin.info.addPlayer(p);
			}
		}
		, 1L);
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		plugin.info.removePlayer(event.getPlayer());
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		String message = event.getMessage();
		if (message == null) return;
		if (Bukkit.getPluginManager().getPlugin("Factions") == null) {
			event.setFormat(plugin.API.parseChat(player, message, plugin.chatFormat));
		}else{
			UPlayer up = UPlayer.get(player);
			event.setFormat(plugin.fac.factionChat(player, message, plugin.chatFormat, up));
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!plugin.handleMe) return;
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		//UPlayer up = UPlayer.get(player);
		String command = event.getMessage();
		if (command == null) return;

		if (command.toLowerCase().startsWith("/me ")) {
			if ((plugin.mePerm) && (!player.hasPermission("plugin.me"))) {
				event.setCancelled(true);
				return;
			}
			plugin.info.addPlayer(player);
			String message = command.substring(command.indexOf(" ")).trim();

			String formatted = plugin.API.parseChat(player, message, plugin.meFormat);

			SupaChatMeEvent meEvent = new SupaChatMeEvent(player, message);
			plugin.getServer().getPluginManager().callEvent(meEvent);
			if (Bukkit.getPluginManager().getPlugin("Factions") == null) {
				plugin.getServer().broadcastMessage(formatted);
			}else{
				String fformatted = plugin.fac.factionChat(player, message, plugin.meFormat, plugin.fac.up);
				plugin.getServer().broadcastMessage(fformatted);
			}
			event.setCancelled(true);
		}
	}
}