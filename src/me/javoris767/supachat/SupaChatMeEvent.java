package me.javoris767.supachat;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SupaChatMeEvent extends Event {
	private String message;
	private Player player;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public SupaChatMeEvent(final Player player, final String message) {
		this.player = player;
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public Player getPlayer() {
		return player;
	}
}
