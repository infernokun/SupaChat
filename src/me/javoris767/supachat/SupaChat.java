package me.javoris767.supachat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.logging.Logger;

import me.javoris767.supachat.Metrics.Metrics;
import me.javoris767.supachat.intergration.Factions;
import net.krinsoft.privileges.Privileges;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.platymuus.bukkit.permissions.PermissionsPlugin;

public class SupaChat extends JavaPlugin implements Runnable {

	public PluginManager pm;
	private String permHandler = "";
	public Plugin bPerm;
	public PermissionsPlugin pbPlug;
	public PermissionsEx pexPlug;
	public GroupManager gMan;
	public Privileges priv;
	public VariableHandler info;
	public HashMap<String, Long> connectList;
	public Logger log;
	FileConfiguration newConfig;
	public SupaChatAPI API;
	public Factions fac;
	public String iNameFormat = "[+prefix+group+suffix&f] +displayname";
	public String chatFormat = "+iname: +message";
	public String meFormat = "* +name +message";
	public String dateFormat = "HH:mm:ss";
	public Integer timeOffset = null;
	public boolean handleMe = true;
	public boolean mePerm = false;
	public int refreshTimeout = 100;
	public boolean factions;
	public String prefix = "[" + ChatColor.DARK_RED + "SupaChat" + ChatColor.WHITE + "]";
	File debugfolder = new File("plugins/SupaChat/debug/");
	
	public void onEnable() {
		this.API = new SupaChatAPI(this);
		this.pm = getServer().getPluginManager();
		this.log = getServer().getLogger();
		this.newConfig = getConfig();

		if (Bukkit.getPluginManager().getPlugin("Factions") != null) {
			fac = new Factions(this);
		}
		setupPermissions();
		loadConfig();
		debugfolder.mkdir();

		this.info = new VariableHandler(this);
		this.connectList = new HashMap<String, Long>();

		this.pm.registerEvents(new PlayerListener(this), this);

		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 0L, this.refreshTimeout);

		this.log.info(getDescription().getName() + " (v" + getDescription().getVersion() + ") enabled");
		try
		{
			Metrics metrics = new Metrics(this);

			Metrics.Graph phGraph = metrics.createGraph("Permission Handler");
			phGraph.addPlotter(new Metrics.Plotter(this.permHandler)
			{
				public int getValue() {
					return 1;
				}
			});
			metrics.start();
		}
		catch (IOException localIOException) {
		}
	}

	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		this.log.info("[SupaChat] SupaChat Disabled");
	}

	private void loadConfig() {
		reloadConfig();
		this.newConfig = getConfig();
		this.newConfig.options().copyDefaults(true);
		this.iNameFormat = this.newConfig.getString("iname-format");
		this.chatFormat = this.newConfig.getString("message-format");
		this.dateFormat = this.newConfig.getString("date-format");
		this.meFormat = this.newConfig.getString("me-format");
		this.handleMe = this.newConfig.getBoolean("handle-me");
		this.mePerm = this.newConfig.getBoolean("me-permissions");
		this.refreshTimeout = this.newConfig.getInt("variable-refresh");

		if (this.newConfig.isSet("time-offset")) {
			this.timeOffset = Integer.valueOf(this.newConfig.getInt("time-offset"));
		}
		saveConfig();

		getServer().getScheduler().cancelTasks(this);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 0L, this.refreshTimeout);
	}

	private void setupPermissions()
	{
		if ((this.bPerm != null) || (this.pbPlug != null) || (this.pexPlug != null) || (this.gMan != null) || (this.priv != null)) return;
		Plugin tmp = null;
		PluginManager pm = getServer().getPluginManager();

		tmp = pm.getPlugin("bPermissions");
		if ((tmp != null) && (tmp.isEnabled())) {
			this.permHandler = "bPermissions";
			this.log.info("[SupaChat] Found bPermissions v" + tmp.getDescription().getVersion());
			this.bPerm = tmp;
			return;
		}

		tmp = pm.getPlugin("PermissionsBukkit");
		if (tmp != null) {
			this.permHandler = "PermissionsBukkit";
			this.log.info("[SupaChat] Found PermissionsBukkit v" + tmp.getDescription().getVersion());
			this.pbPlug = ((PermissionsPlugin)tmp);
			return;
		}

		tmp = pm.getPlugin("PermissionsEx");
		if ((tmp != null) && (tmp.isEnabled())) {
			this.permHandler = "PermissionsEx";
			this.log.info("[SupaChat] Found PermissionsEx v" + tmp.getDescription().getVersion());
			this.pexPlug = ((PermissionsEx)tmp);
			return;
		}

		tmp = pm.getPlugin("GroupManager");
		if ((tmp != null) && (tmp.isEnabled())) {
			this.permHandler = "GroupManager";
			this.log.info("[SupaChat] Found GroupManager v" + tmp.getDescription().getVersion());
			this.gMan = ((GroupManager)tmp);
			return;
		}

		tmp = pm.getPlugin("Privileges");
		if ((tmp != null) && (tmp.isEnabled())) {
			this.permHandler = "Privileges";
			this.log.info("[SupaChat] Found Privileges v" + tmp.getDescription().getVersion());
			this.priv = ((Privileges)tmp);
			return;
		}

		this.permHandler = "SuperPerms";
		this.log.info("[SupaChat] Permissions not found, using SuperPerms");
	}

	/*
	 * Command Handler
	 */
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("supachat"))
			return handleCommands(sender, args);
		return true;
	}

	private boolean handleCommands(CommandSender sender, String[] args)
	{
		if(args.length == 0) 
		{
			sender.sendMessage("------------" + prefix + "------------");
			sender.sendMessage("/supachat reload - Reloads the config.");
			sender.sendMessage("/supachat debug - Sends a debug report to the console");
			return true;
		}
		if(args.length == 1)
		{
			if (args[0].equalsIgnoreCase("reload")) 
			{
				if(sender instanceof Player && !sender.hasPermission("supachat.reload")) {
					sender.sendMessage(prefix + ChatColor.RED + " Permission Denied");
					return true;
				}
				loadConfig();
				info.loadConfig();
				sender.sendMessage(prefix + " Config Reloaded");
			}
			else if (args[0].equalsIgnoreCase("debug")) 
			{
		        int debugs = 0;
		        debugs++;
		        File debug = new File(debugfolder + "debug" + debugs + ".txt");
		        try
		        {
		          FileWriter outfile = new FileWriter(debug, true);
		          PrintWriter out = new PrintWriter(outfile);
		          debug.createNewFile();
		          out.println("[supachat::debug]");
		          out.println("iNameFormat = " + this.newConfig.getString("iname-format"));
		          out.println();
		          out.println();
		          out.println();
		          out.println();
		          out.println();
		          out.close();
		        }
		        catch (IOException e) {
		          e.printStackTrace();
		        }
				info.debug();

				// Print config informations
				log.info("[supachat::debug]");
				log.info("iNameFormat = " + newConfig.getString("iname-format"));
				log.info("chatFormat = " + newConfig.getString("message-format"));
				log.info("dateFormat = " + newConfig.getString("date-format"));
				log.info("meFormat = " + newConfig.getString("me-format"));
				log.info("handleMe = " + newConfig.getBoolean("handle-me"));
				log.info("mePerm = " + newConfig.getBoolean("me-permissions"));

				log.info("plugins/SupaChat/config.yml exists: " + (new File(this.getDataFolder(), "config.yml")).exists());
				log.info("plugins/SupaChat/variables.yml exists: " + (new File(this.getDataFolder(), "variables.yml")).exists());
				sender.sendMessage(prefix + " Debug complete! See console.");
			}
		}
		return false;
	}


	// Reload player data every x seconds (Default 5)
	@Override
	public void run() {
		for (final Player p : getServer().getOnlinePlayers()) {
			info.addPlayer(p);
		}
	}
}