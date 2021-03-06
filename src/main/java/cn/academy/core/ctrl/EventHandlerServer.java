/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.ctrl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerEvent;
import cn.academy.api.ability.Category;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.ctrl.SkillStateManager;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * The event handler in server side. It's actually a network handler.
 * Interact with EventHandlerClient through network and sent events to the RawEventHandler.
 * This class only has one instance on the server side.
 * @author acaly
 *
 */
@RegistrationClass
@RegSubmoduleInit
public class EventHandlerServer {
	
	private static final String NBT_PLAYER_WORLD_ID = "ap_mod_ctrl_player_world_id";
	
	/**
	 * The network handler used to handle client messages.
	 * @author acaly
	 *
	 */
	@RegMessageHandler(msg = ControlMessage.class, side = RegMessageHandler.Side.SERVER)
	public static class NetworkHandler implements IMessageHandler<ControlMessage, IMessage> {

		@Override
		public IMessage onMessage(ControlMessage msg, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			switch (msg.eventType) {
			case RAW_DOWN:
			case RAW_UP:
			case RAW_CLIENT_DOWN:
			case RAW_CLIENT_UP:
				//These are valid messages.
				INSTANCE.onEvent(player, msg.skillId, msg.eventType, msg.time);
				break;
			case INIT_QUERY_WORLD_ID:
				//Client is querying world id.
				NBTTagCompound tag = player.getEntityData();
				int worldId = msg.time;
				if (tag.hasKey(NBT_PLAYER_WORLD_ID)) {
					//id found.
					worldId = tag.getInteger(NBT_PLAYER_WORLD_ID);
				} else {
					//Not found. Use default value provided by client and store it in NBT.
					tag.setInteger(NBT_PLAYER_WORLD_ID, worldId);
				}
				//Send the id back to client.
				AcademyCraft.netHandler.sendTo(
						new ControlMessage(0, msg.eventType, worldId), player);
				break;
			case CLIENT_STOP_ALL:
				INSTANCE.skillEventAll(player, SkillEventType.RAW_CANCEL);
				break;
			default:
				AcademyCraft.log.error("An unexpected packet is received from client.");
			}
			return null;
		}
		
	}
	
	/**
	 * Class used to handle per skill state.
	 * This is where every event on server side is processed.
	 * @author acaly
	 *
	 */
	private static class SingleSkill {
		
		/*
		 * Information used to find the skill.
		 */
		private EntityPlayerMP player;
		private int skillId;
		private RawEventHandler reh;
		
		/*
		 * Internal state
		 */
		
		/**
		 * How many ticks before a RAW_CLIENT_DOWN message must be received.
		 */
		private int tickToSetDead = 0;
		
		/**
		 * How many ticks before a RAW_CLIENT_UP or RAW_DOWN message must be received.
		 */
		private int tickToFinishClick = 0;
		
		public SingleSkill(EntityPlayer player, int skillId) {
			this.player = (EntityPlayerMP) player;
			this.skillId = skillId;
			this.reh = GenericUtils.assertObj(INSTANCE.rehMap.get(player).get(skillId));
		}
		
		/**
		 * On event without a client time.
		 * @param type The event type.
		 * @return Same as boolean onEvent(SkillEventType type, int time).
		 */
		public boolean onEvent(SkillEventType type) {
			//Use server time as client time if not provided.
			return onEvent(type, reh.getTime());
		}
		
		/**
		 * On event provided a client time.
		 * @param type The event type.
		 * @param time The client time.
		 * @return True if a RAW_TICK is needed next time.
		 */
		public boolean onEvent(SkillEventType type, int time) {
			switch (type) {
			case RAW_TICK:
				if (tickToSetDead == 1) {
					//Reset counter.
					tickToSetDead = 1;
					//Time out. Set dead (to client and server).
					setDead();
				} else if (tickToSetDead > 1) {
					--tickToSetDead; 
					//Send tick event to server.
					reh.onEvent(SkillEventType.RAW_TICK_DOWN, time, false);
				}
				if (tickToFinishClick == 1) {
					//Reset counter.
					tickToFinishClick = 0;
					//Time out. Set dead.
					setDead();
				} else if (tickToFinishClick > 1) {
					--tickToFinishClick;
					//Send tick to server.
					//TODO needed?
					reh.onEvent(SkillEventType.RAW_TICK_UP, time, false);
				}
				return tickToSetDead > 0 || tickToFinishClick > 0;
			case RAW_DOWN:
				//First check if the skill is open.
				if (!AbilityDataMain.getData(player).isSkillLearned(skillId)) {
					AcademyCraft.log.warn("Player is trying to use a skill that has not been learnt.");
					return false;
				}
				if (tickToSetDead > 0) {
					//Already down? Abort.
					AcademyCraft.log.error("Unexpected RAW_DOWN event.");
					setDead();
					return false;
				}
				reh.onEvent(type, time, false);
				tickToSetDead = RawEventHandler.KA_INTERVAL + RawEventHandler.KA_DELAY;
				if (tickToFinishClick > 0) {
					//Still waiting for CLIENT_UP, so that's a double click.
					tickToFinishClick = 0;
					reh.onEvent(SkillEventType.RAW_DBLCLK, time, false);
				}
				return true;
			case RAW_UP:
				//TODO check time
				if (tickToFinishClick > 0) {
					//Already up?
					setDead();
					return false;
				}
				if (tickToSetDead == 0) {
					//Not in the down state (maybe due to network timeout).
					//Do nothing.
					return false;
				}
				//Stop waiting for CLIENT_DOWN and start waiting for CLIENT_UP (or DOWN).
				tickToSetDead = 0;
				tickToFinishClick = RawEventHandler.DBL_DELAY + RawEventHandler.KA_DELAY;
				if (time > reh.getTime()) {
					//Client time is greater. Need an adjust event.
					reh.onEvent(SkillEventType.RAW_ADJUST, time, false);
					reh.onEvent(SkillEventType.RAW_UP, time, false);
				} else {
					reh.onEvent(SkillEventType.RAW_UP, reh.getTime(), false);
				}
				return true;
			case RAW_CLIENT_DOWN:
				//TODO check time
				if (tickToSetDead == 0) {
					//Already timeout. Do nothing (setDead has been called).
					return false;
				}
				//Reset counter.
				tickToSetDead = RawEventHandler.KA_INTERVAL + RawEventHandler.KA_DELAY;
				return true;
			case RAW_CLIENT_UP:
				if (tickToFinishClick == 0) {
					//Already timeout. Do nothing (setDead has been called).
					return false;
				}
				//Reset counter.
				tickToFinishClick = 0;
				//Invoke CLICK on server.
				reh.onEvent(SkillEventType.RAW_CLICK, reh.getTime(), false);
				return false;
			case RAW_CANCEL:
                reh.onEvent(SkillEventType.RAW_CANCEL, reh.getTime(), false);
                AcademyCraft.netHandler.sendTo(
                        new ControlMessage(skillId, SkillEventType.RAW_CANCEL, time),
                        player);
			    return false;
			default:
				AcademyCraft.log.error("Unexpcected event in EventHandlerServer.");
				return false;
			}
		}
		
		/**
		 * Send RAW_CANCEL to server and client to abort current skill.
		 */
		private void setDead() {
			//AcademyCraft.log.warn("Delay in client side. Skill cancelled.");
			int time = reh.getTime();
			reh.onEvent(SkillEventType.RAW_CANCEL, time, false);
			AcademyCraft.netHandler.sendTo(
					new ControlMessage(skillId, SkillEventType.RAW_CANCEL, time),
					player);
		}
		
	}
	
	@RegEventHandler
	public static final EventHandlerServer INSTANCE = new EventHandlerServer();

	/**
	 * Make it private.
	 */
	private EventHandlerServer() {}
	
	/**
	 * Setup the key bindings and network.
	 */
	public static void init() {
	}
	
	/**
	 * Called by data part on server side, after the ability data is changed.
	 * @param name The name of player joined.
	 */
	public static void resetPlayerSkillData(EntityPlayer player) {
		//AcademyCraft.log.info("EventHandlerServer: Reset player.");
		
        //Cancel all skills.
        INSTANCE.skillEventAll(player, SkillEventType.RAW_CANCEL);
        //Remove this player.
        INSTANCE.rehMap.remove(player);
        INSTANCE.kaMap.remove(player);
        SkillStateManager.removePlayerFromServer(player);
		
		Category cat = GenericUtils.assertObj(AbilityDataMain.getData(player).getCategory());
		
		//Create every raw event handler for this player.
		Map<Integer, RawEventHandler> rehMap = new HashMap();
		for (int i = 0; i < cat.getSkillCount(); ++i) {
			rehMap.put(i, new RawEventHandler(player, cat.getSkill(i)));
		}
		
		INSTANCE.rehMap.put(player, rehMap);
		INSTANCE.kaMap.put(player, new HashMap());
	}

	private void onEvent(EntityPlayer player, int skillId, SkillEventType type, int timeForSkill) {
		skillEvent(player, skillId, type, timeForSkill);
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		skillEventAll(SkillEventType.RAW_TICK);
		SkillStateManager.tickServer();
	}
	
	@SubscribeEvent
	public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
		EntityPlayer player = event.player;
		
		//Cancel all skills.
		//Remove this player.
		clearPlayerStates(player);
		
		SkillStateManager.removePlayerFromServer(player);
	}
	
	@SubscribeEvent
	public void onPlayerCloned(PlayerEvent.Clone event) {
	    //Dead or to another dimension
        //Cancel all skills.
        skillEventAll(event.original, SkillEventType.RAW_CANCEL);
	    
	    //Copy data to the new instance.
	    NBTTagCompound abilityData = new NBTTagCompound();
	    AbilityDataMain.getData(event.original).saveNBTData(abilityData);
	    AbilityDataMain.getData(event.entityPlayer).loadNBTData(abilityData);

	    //Update both maps.
	    if (kaMap.containsKey(event.original)) {
	        kaMap.remove(event.original);
	    }
	    if (rehMap.containsKey(event.original)) {
	        rehMap.remove(event.original);
	    }
	}
	
	/**
	 * Clear all the player's current states.
	 * @param player
	 */
	public void clearPlayerStates(EntityPlayer player) {
		skillEventAll(player, SkillEventType.RAW_CANCEL);
		rehMap.remove(player);
		kaMap.remove(player);
	}

	/**
	 * Raw event handlers. (All created in resetPlayerSkillData.)
	 */
	private Map<EntityPlayer, Map<Integer, RawEventHandler>> rehMap = new HashMap();
	/**
	 * Active skills.
	 */
	private Map<EntityPlayer, Map<Integer, SingleSkill>> kaMap = new HashMap();
	
	/**
	 * Send an event to a single skill.
	 * @param player The player.
	 * @param skillId The skill.
	 * @param type The event type.
	 * @param time The client time (if available).
	 */
	private void skillEvent(EntityPlayer player, int skillId, SkillEventType type, int time) {
		Map<Integer, SingleSkill> playerMap = kaMap.get(player);
		SingleSkill ka = playerMap.get(skillId);
		if (ka == null) {
			ka = new SingleSkill(player, skillId);
			playerMap.put(skillId, ka);
		}
		
		if (ka.onEvent(type, time) == false) {
			playerMap.remove(skillId);
		}
	}

	/**
	 * Send an event to all skills of the given player.
	 * @param player The player.
	 * @param type The event type.
	 */
	private void skillEventAll(EntityPlayer player, SkillEventType type) {
		Map<Integer, SingleSkill> playerMap = kaMap.get(player);
		if (playerMap == null) return;
		Iterator<SingleSkill> itor = playerMap.values().iterator();
		while (itor.hasNext()) {
			if (itor.next().onEvent(type) == false) {
				itor.remove();
			}
		}
	}
	
	/**
	 * Send an event to all skills of every player on the server.
	 * @param type The event type.
	 */
	private void skillEventAll(SkillEventType type) {
		for (Map<Integer, SingleSkill> playerMap : kaMap.values()) {
			Iterator<SingleSkill> itor = playerMap.values().iterator();
			while (itor.hasNext()) {
				if (itor.next().onEvent(type) == false) {
					itor.remove();
				}
			}
		}
	}
}
