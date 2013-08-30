package me.javoris767.supachat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class VariableHandler {
	private SupaChat supachat;
	private dYamlConfiguration newConfig;
	
	// Values loaded from config -- Global
	private HashMap<String, HashMap<String, String>> groupVars = new HashMap<String, HashMap<String, String>>();
	private HashMap<String, HashMap<String, String>> userVars = new HashMap<String, HashMap<String, String>>();
	
	// Values loaded from config -- Per World
	private HashMap<String, HashMap<String, HashMap<String, String>>> wGroupVars = new HashMap<String, HashMap<String, HashMap<String, String>>>();
	private HashMap<String, HashMap<String, HashMap<String, String>>> wUserVars = new HashMap<String, HashMap<String, HashMap<String, String>>>();
	
	// Values loaded from config -- World Variables
	private HashMap<String, HashMap<String, String>> worldVars = new HashMap<String, HashMap<String, String>>();
	
	// Cached player data
	private ConcurrentHashMap<String, HashMap<String, String>> playerVars = new ConcurrentHashMap<String, HashMap<String, String>>();
	private ConcurrentHashMap<String, String> playerGroups = new ConcurrentHashMap<String, String>();

	public VariableHandler(SupaChat supachat) {
		this.supachat = supachat;
		checkConfig();
		loadConfig();
	}
	
	/*
	 * Check to see if variables.yml exists
	 */
	public void checkConfig() {
		File vFile = new File(supachat.getDataFolder(), "variables.yml");
		// Variables exists, don't touch it!
		if (vFile.exists()) return;
		// Copy defaults to variables.yml
		newConfig = dYamlConfiguration.loadConfiguration(vFile);
		newConfig.options().copyDefaults(true);
		InputStream defConfigStream = SupaChat.class.getClassLoader().getResourceAsStream("variables.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			
			newConfig.setDefaults(defConfig);
			
			try {
				newConfig.save(vFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void loadConfig() {
		groupVars.clear();
		userVars.clear();
		playerVars.clear();
		wGroupVars.clear();
		wUserVars.clear();
		worldVars.clear();
		
		reloadConfig();
		
		// Loop through nodes, "groups" and "users" are special cases, everything else is a world
		for (String key : newConfig.getKeys(false)) {
			if (key.equals("groups")) {
				ConfigurationSection groups = newConfig.getConfigurationSection("groups");
				loadNodes(groups, groupVars);
				continue;
			}
			if (key.equals("users")) {
				ConfigurationSection users = newConfig.getConfigurationSection("users");
				loadNodes(users, userVars);
				continue;
			}
			
			// Loop through world nodes, "groups" and "users" are special cases, everything else is a world variable
			HashMap<String, String> wVars = new HashMap<String, String>();
			ConfigurationSection world = newConfig.getConfigurationSection(key);
			if (world == null) {
				supachat.log.warning("[SupaChat::vh::loadConfig] There was an error loading configuration. world was null");
				continue;
			}
			for (String wKey : world.getKeys(false)) {
				if (wKey.equals("groups")) {
					ConfigurationSection groups = world.getConfigurationSection("groups");
					HashMap<String, HashMap<String, String>> wGroups = new HashMap<String, HashMap<String, String>>();
					loadNodes(groups, wGroups);
					wGroupVars.put(key.toLowerCase(), wGroups);
					continue;
				}
				if (wKey.equals("users")) {
					ConfigurationSection users = world.getConfigurationSection("users");
					HashMap<String, HashMap<String, String>> wUsers = new HashMap<String, HashMap<String, String>>();
					loadNodes(users, wUsers);
					wUserVars.put(key.toLowerCase(), wUsers);
					continue;
				}
				String value = world.getString(wKey);
				if (value == null) continue;
				wVars.put(wKey.toLowerCase(), value);
			}
			worldVars.put(key.toLowerCase(), wVars);
		}
	}
	
	public void addPlayer(Player player) {
		HashMap<String, String> tmpList = new HashMap<String, String>();
		
		String group = supachat.API.getGroup(player);
		String world = player.getWorld().getName().toLowerCase();
		
		// Check if the world the player is in has variables
		HashMap<String, String> wVars = worldVars.get(world);
		if (wVars != null && !wVars.isEmpty()) {
			tmpList.putAll(wVars);
		}
		
		if (group != null && !group.isEmpty()) {
			// Add the players cached group to the group list
			playerGroups.put(player.getName().toLowerCase(), group);
			
			group = group.toLowerCase();
			HashMap<String, String> gVars = groupVars.get(group);
			if (gVars != null)
				tmpList.putAll(gVars);
			
			// Check if this group has world-specific vars
			HashMap<String, HashMap<String, String>> worlds = wGroupVars.get(world);
			if (worlds != null) {
				HashMap<String, String> wgVars = worlds.get(group);
				if (wgVars != null)
					tmpList.putAll(wgVars);
			}
		} else {
			// Remove players cached group
			playerGroups.remove(player.getName().toLowerCase());
		}
		
		HashMap<String, String> uVars = userVars.get(player.getName().toLowerCase());
		if (uVars != null)
			tmpList.putAll(uVars);
		
		HashMap<String, HashMap<String, String>> worlds = wUserVars.get(world);
		if (worlds != null) {
			HashMap<String, String> wuVars = worlds.get(player.getName().toLowerCase());
			if (wuVars != null)
				tmpList.putAll(wuVars);
		}
		
		playerVars.put(player.getName().toLowerCase(), tmpList);
	}
	
	public void removePlayer(Player player) {
		playerGroups.remove(player.getName().toLowerCase());
		playerVars.remove(player.getName().toLowerCase());
	}
	
	public String getGroup(Player player) {
		String group = playerGroups.get(player.getName().toLowerCase());
		if (group == null) return "";
		return group;
	}
	
	public String getKey(Player player, String key) {
		HashMap<String, String> pVars = playerVars.get(player.getName().toLowerCase());
		if (pVars == null) return "";
		String var = pVars.get(key.toLowerCase());
		if (var == null) return "";
		return var;
	}
	
	public String getKey(String group, String key) {
		HashMap<String, String> gVars = groupVars.get(group.toLowerCase());
		if (gVars == null) return "";
		String var = gVars.get(key.toLowerCase());
		if (var == null) return "";
		return var;
	}

	private void reloadConfig() {
		File vFile = new File(supachat.getDataFolder(), "variables.yml");
		if (!vFile.exists()) {
			supachat.log.info("[SupaChat] variables.yml does not exist. Please create it.");
			return;
		}
		newConfig = dYamlConfiguration.loadConfiguration(vFile);
	}
	
	/*
	 * Load the nodes from root into map
	 */
	private void loadNodes(ConfigurationSection root, HashMap<String, HashMap<String, String>> map) {
		if (root == null) {
			supachat.log.warning("[SupaChat::vh::loadNodes] There was an error loading configuration nodes");
			return;
		}
		for (String key : root.getKeys(false)) {
			HashMap<String, String> tmpList = new HashMap<String, String>();
			ConfigurationSection vars = root.getConfigurationSection(key);
			if (vars == null) continue;
			// Store vars
			for (String vKey : vars.getKeys(false)) {
				String value = vars.getString(vKey);
				if (value == null) continue;
				tmpList.put(vKey.toLowerCase(),  value);
			}
			map.put(key.toLowerCase(), tmpList);
		}
	}
	
	/*
	 * DEBUG
	 */
	public void debug() {
		supachat.log.info("[SupaChat::vh::debug]");
		// Print out all group variables
		supachat.log.info("[SupaChat::vh::debug] Groups");
		for (String group : groupVars.keySet()) {
			supachat.log.info("  " + group);
			HashMap<String, String> gVars = groupVars.get(group);
			for (String key : gVars.keySet()) {
				supachat.log.info("    " + key + " => " + gVars.get(key));
			}
		}
		
		// Print out all user variables
		supachat.log.info("[SupaChat::vh::debug] Users");
		for (String user : userVars.keySet()) {
			supachat.log.info("  " + user);
			HashMap<String, String> uVars = userVars.get(user);
			for (String key : uVars.keySet()) {
				supachat.log.info("    " + key + " => " + uVars.get(key));
			}
		}
		
		// Print all world-specific group variables
		supachat.log.info("[SupaChat::vh::debug] World-Groups");
		for (String world : wGroupVars.keySet()) {
			supachat.log.info("  " + world);
			HashMap<String, HashMap<String, String>> groups = wGroupVars.get(world);
			for (String group : groups.keySet()) {
				supachat.log.info("    " + group);
				HashMap<String, String> gVars = groups.get(group);
				for (String key : gVars.keySet()) {
					supachat.log.info("      " + key + " => " + gVars.get(key));
				}
			}
		}
		
		// Print all world-specific user variables
		supachat.log.info("[SupaChat::vh::debug] World-Players");
		for (String world : wUserVars.keySet()) {
			supachat.log.info("  " + world);
			HashMap<String, HashMap<String, String>> users = wUserVars.get(world);
			for (String user : users.keySet()) {
				supachat.log.info("    " + user);
				HashMap<String, String> uVars = users.get(user);
				for (String key : uVars.keySet()) {
					supachat.log.info("      " + key + " => " + uVars.get(key));
				}
			}
		}
		
		// Print all world variables
		supachat.log.info("[SupaChat::vh::debug] World Variables");
		for (String world : worldVars.keySet()) {
			supachat.log.info("  " + world);
			HashMap<String, String> wVars = worldVars.get(world);
			for (String key : wVars.keySet()) {
				supachat.log.info("    " + key + " => " + wVars.get(key));
			}
		}
		
		// Print out all player variables
		supachat.log.info("[SupaChat::vh::debug] Players");
		for (String player : playerVars.keySet()) {
			supachat.log.info("  " + player);
			HashMap<String, String> pVars = playerVars.get(player);
			for (String key : pVars.keySet()) {
				supachat.log.info("    " + key + " => " + pVars.get(key));
			}
		}
	}
}
