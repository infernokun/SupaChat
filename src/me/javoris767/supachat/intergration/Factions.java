package me.javoris767.supachat.intergration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.javoris767.supachat.SupaChat;

import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.UConf;
import com.massivecraft.factions.entity.UPlayer;
import com.massivecraft.mcore.util.Txt;
import com.platymuus.bukkit.permissions.Group;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;

public class Factions {
	private SupaChat plugin;
	public UPlayer up;

	public Factions(SupaChat supaChat) {
		plugin = supaChat;
	}

	public String FactionName(Player p) 
	{
		// Check disabled
		if(UConf.isDisabled(p)) return "";

		// Get entities
		UPlayer up = UPlayer.get(p);

		// No force
		Faction faction = up.getFaction();
		if(faction.isNone()) return "";

		return faction.getName();
	}

	public String FactionNameForce(Player p) 
	{
		UPlayer up = UPlayer.get(p);

		Faction faction = up.getFaction();
		return faction.getName();
	}

	public String Relcolor(Player p, Player rp) 
	{
		if(rp == null) return "";

		if(UConf.isDisabled(p)) return "";

		for(Player pp : Bukkit.getOnlinePlayers()) {

			UPlayer up = UPlayer.get(p);
			UPlayer urp = UPlayer.get(pp);
			return up.getRelationTo(urp).getColor().toString();
		}
		return null;
	}

	public String FactionRole(Player p)
	{
		if(UConf.isDisabled(p)) return "";

		UPlayer up = UPlayer.get(p);

		return Txt.upperCaseFirst(up.getRole().toString().toLowerCase());
	}

	public String RolePrefix(Player p) 
	{
		if(UConf.isDisabled(p)) return "";

		UPlayer up = UPlayer.get(p);

		Faction faction = up.getFaction();
		if(faction.isNone()) return "";

		return up.getRole().getPrefix();
	}

	public String RolePrefixForce(Player p)
	{
		if(UConf.isDisabled(p)) return "";

		UPlayer up = UPlayer.get(p);

		return up.getRole().getPrefix();
	}

	public String FactionTitle(Player p) 
	{
		if(UConf.isDisabled(p)) return "";

		UPlayer up = UPlayer.get(p);

		if(!up.hasTitle()) return "";
		return up.getTitle();
	}

	public String factionChat(Player p, String msg, String chatFormat, UPlayer up) {
		return factionChat(p, msg, chatFormat, false, up);
	}

	public String factionChat(Player p, String msg, String chatFormat, Boolean parseName, UPlayer up) {
		// Variables we can use in a message
		String group = plugin.info.getGroup(p);
		String prefix = getPrefix(p);
		String suffix = getSuffix(p);

		String factionName = FactionName(p);
		String factionNameForce = FactionNameForce(p);
		String relColor = Relcolor(p, p);
		String factionRole = FactionRole(p);
		String rolePrefix = RolePrefix(p);
		String rolePrefixForce = RolePrefixForce(p);
		String factiontitle = FactionTitle(p);

		if (prefix == null) prefix = "";
		if (suffix == null) suffix = "";
		if (group == null) group = "";
		String healthbar = healthBar(p);
		Damageable damag = p;
		String health = String.valueOf(damag.getHealth());
		String world = p.getWorld().getName();

		if (world.contains("_nether"))
			world = world.replace("_nether", " Nether");
		if (world.contains("_the_end"))
			world = world.replace("_the_end", " End");

		// Timestamp support
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(plugin.dateFormat);
		// Offset the time if needed
		if (plugin.timeOffset != null) {
			String offset = Integer.toString(plugin.timeOffset);
			if (plugin.timeOffset == 0) offset = "";
			if (plugin.timeOffset > 0) offset = "+" + offset;
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT" + offset));
		}
		String time = dateFormat.format(now);

		// We're sending this to String.format, so we need to escape those pesky % symbols
		msg = msg.replaceAll("%", "%%");

		// Add coloring if the player has permission
		if (!checkPermissions(p, "plugin.color")) {
			boolean hasColor = true;
			boolean hasFormat = true;
			// Strip color if they lack permission
			if (!checkPermissions(p, "plugin.format.color")) {
				msg = msg.replaceAll("(&+([a-fA-F0-9]))", "");
				hasColor = false;
			}
			// Strip formatting if they lack permission
			if (!checkPermissions(p, "plugin.format.formatting")) {
				msg = msg.replaceAll("(&+([k-oK-OrR]))", "");
				hasFormat = false;
			}
			// Legacy: Strip all if they lack any permission
			if (!hasColor && !hasFormat) {
				msg = msg.replaceAll("(&+([a-fA-Fk-oK-OrR0-9]))", "");
			}
		}

		String format = parseVars(chatFormat, p);
		if (format == null) return msg;

		String iName = p.getDisplayName();
		if (!parseName.booleanValue()) {
			iName = factionChat(p, "", plugin.iNameFormat, true, up);
		}

		// Order is important, this allows us to use all variables in the suffix and prefix! But no variables in the message
		String[] search = new String[] {"+factions_relcolor", "+factions_roleprefix", "+ffactions_roleprefix", "+factions_title", "+factions_role", "+factions_name", "+ffactions_name", "+suffix,+s", "+prefix,+p", "+group,+g", "+healthbar,+hb", "+health,+h", "+world,+w", "+time,+t", "+name,+n", "+displayname,+d", "+iname,+in", "+message", "+m"};
		String[] replace = new String[] {relColor, rolePrefix, rolePrefixForce, factiontitle, factionRole, factionName, factionNameForce, suffix, prefix, group, healthbar, health, world, time, p.getName(), p.getDisplayName(), iName, "+m", msg, };
		return replaceVars(format, search, replace);
	}

	public Long getConnectTime(Player p) {
		Long conTime = plugin.connectList.get(p.getName());
		if (conTime == null) return -1L;
		return conTime;
	}

	public Long getOnlineTime(Player p) {
		Long conTime = getConnectTime(p);
		if (conTime == null) return -1L;
		Long onTime = (System.currentTimeMillis() / 1000L) - conTime;
		return onTime;
	}

	public String prettyTime(Long time) {
		StringBuilder sb = new StringBuilder();
		sb.append(time % 60).append("s");
		if (time >= 60) {
			Long min = time / 60;
			sb.insert(0, " " + min % 60 + "m");
		}
		if (time >= 3600) {
			Long hour = time / 3600;
			sb.insert(0, " " + hour + "h");
		}
		return sb.toString();
	}

	/**
	 * Permissions handling from mChat by MiracleM4n with modification by Drakia
	 **/

	public String factionChat(Player player, String msg, UPlayer up) {
		//UPlayer up = UPlayer.get(player);
		return factionChat(player, msg, plugin.chatFormat, up);
	}

	public String factionPlayerName(Player player, UPlayer up) {
		return factionChat(player, "", plugin.iNameFormat, true, up);
	}

	/*
	 * Info Stuff
	 */
	public String getRawInfo(Player player, String info) {
		if (info.equals("group")) {
			if (plugin.bPerm != null) {
				return getbPermGroup(player);
			}
			if (plugin.pbPlug != null) {
				return getPermBGroup(player);
			}
			if (plugin.pexPlug != null) {
				return getPexGroup(player);
			}
			if (plugin.gMan != null) {
				return getGManGroup(player);
			}
			if (plugin.priv != null) {
				return getPrivilegesGroup(player);
			}
			return getSuperPermGroup(player);
		}

		return getVariable(player, info);
	}

	public String getRawPrefix(Player player) {
		return getRawInfo(player, "prefix");
	}

	public String getRawSuffix(Player player) {
		return getRawInfo(player, "suffix");
	}

	public String getRawGroup(Player player) {
		return getRawInfo(player, "group");
	}

	public String getInfo(Player player, String info) {
		return addColor(getRawInfo(player, info));
	}

	public String getPrefix(Player player) {
		return getInfo(player, "prefix");
	}

	public String getSuffix(Player player) {
		return getInfo(player, "suffix");
	}

	public String getGroup(Player player) {
		return getInfo(player, "group");
	}

	/*
	 * Return a health bar string.
	 */
	public String healthBar(Player player) {
		float maxHealth = 20;
		float barLength = 10;
		Damageable damag = player;
		double health = damag.getHealth();
		long fill = Math.round( (health / maxHealth) * barLength );
		String barColor = "&2";
		// 0-40: Red  40-70: Yellow  70-100: Green
		if (fill <= 4) barColor = "&4";
		else if (fill <= 7) barColor = "&e";
		else barColor = "&2";

		StringBuilder out = new StringBuilder();
		out.append(barColor);
		for (int i = 0; i < barLength; i++) {
			if (i == fill) out.append("&8");
			out.append("|");
		}
		out.append("&f");
		return out.toString();
	}

	public String addColor(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

	public Boolean checkPermissions(Player player, String node) {
		// SuperPerms
		if (player.hasPermission(node)) {
			return true;
			// Op Fallback
		} else if (player.isOp()) {
			return true;
		}
		return false;
	}

	/*
	 * Private non-API functions
	 */
	/*
	 * Bukkit Permission Stuff
	 */
	private String getVariable(Player player, String info) {
		return plugin.info.getKey(player, info);
	}

	private String getbPermGroup(Player player) {
		String[] groups = ApiLayer.getGroups(player.getWorld().getName(), CalculableType.USER, player.getName());
		if (groups.length == 0) return "";
		return groups[0];
	}

	private String getPermBGroup(Player player) {
		// Because of a softdepend issue in Bukkit, this plugin may not be enabled
		if (!plugin.pbPlug.isEnabled()) return "";
		List<Group> groups = plugin.pbPlug.getGroups(player.getName());
		if (groups.size() == 0) return "";
		return groups.get(0).getName();
	}

	private String getPexGroup(Player player) {
		PermissionUser user = PermissionsEx.getUser(player);
		if (user == null) return "";
		PermissionGroup[] groups = user.getGroups(player.getWorld().getName());
		if (groups.length == 0) return "";
		return groups[0].getName();
	}

	private String getGManGroup(Player player) {
		if (!plugin.gMan.isEnabled()) return "";
		AnjoPermissionsHandler handler = plugin.gMan.getWorldsHolder().getWorldPermissions(player);
		if (handler == null) return "";
		return handler.getGroup(player.getName());
	}

	private String getSuperPermGroup(Player player) {
		Set<PermissionAttachmentInfo> perms = player.getEffectivePermissions();
		for (PermissionAttachmentInfo perm : perms) {
			if (perm.getPermission().startsWith("group.") &&
					perm.getValue()) {
				String group = perm.getPermission().substring(6);
				return plugin.info.getKey(group, "name");
			}
		}
		return "";
	}

	/*
	 * Privileges Stuff
	 */
	private String getPrivilegesGroup(Player player) {
		if (!plugin.priv.isEnabled()) return "";
		net.krinsoft.privileges.groups.Group group = plugin.priv.getGroupManager().getGroup((OfflinePlayer)player);
		if (group == null || group.getName() == null) return "";
		return group.getName();
	}

	/*
	 * Parse given text string for permissions variables
	 */
	private String parseVars(String format, Player p) {
		Pattern pattern = Pattern.compile("\\+\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(format);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String var = getRawInfo(p, matcher.group(1));
			matcher.appendReplacement(sb, Matcher.quoteReplacement(var));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/*
	 * Parse a given text string and replace the variables/color codes.
	 */
	private String replaceVars(String format, String[] search, String[] replace) {
		if (search.length != replace.length) return "";
		for (int i = 0; i < search.length; i++) {
			if (search[i].contains(",")) {
				for (String s : search[i].split(",")) {
					if (s == null || replace[i] == null) continue;
					format = format.replace(s, replace[i]);
				}
			} else {
				format = format.replace(search[i], replace[i]);
			}
		}
		return addColor(format);
	}
}
