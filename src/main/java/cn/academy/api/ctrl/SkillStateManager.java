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
package cn.academy.api.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import cn.academy.api.ctrl.pattern.Pattern;
import cn.academy.core.ctrl.DimensionSkillStateMessage;
import cn.academy.core.ctrl.SkillStateMessage;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This class manages all SkillStates, both on client and on server.
 * @author acaly
 *
 */
@RegistrationClass
@RegEventHandler
public class SkillStateManager {
	
	private static Map<String, List<SkillState>> client = new HashMap();
	private static Map<String, List<SkillState>> server = new HashMap();

	private static final Map<String, List<SkillState>> getMapForSide(EntityPlayer player) {
		if (player.worldObj.isRemote) {
			return client;
		} else {
			return server;
		}
	}
	
	/**
	 * Called by EventHandlerServer.
	 * Remove all states associated to this player
	 * @param player
	 */
	public static void removePlayerFromServer(EntityPlayer player) {
		if (server.containsKey(player.getCommandSenderName())) {
		    List<SkillState> removed = server.remove(player.getCommandSenderName());
		    
		    if (removed == null) return;
		    for (SkillState ss : removed) {
		        ss.reallyFinishSkill();
		    }
		}
	}
	
	/**
	 * Add a new state. Called by SkillState.
	 * @param state
	 */
	static void addState(SkillState state) {
		Map<String, List<SkillState>> stateMap = getMapForSide(state.player);
		
		if (stateMap.containsKey(state.player.getCommandSenderName())) {
			stateMap.get(state.player.getCommandSenderName()).add(state);
		} else {
			List<SkillState> list = new ArrayList();
			list.add(state);
			stateMap.put(state.player.getCommandSenderName(), list);
		}
	}

	/**
	 * Remove a finished state. Called by SkillState.
	 * @param state
	 */
	static void removeState(SkillState state) {
		Map<String, List<SkillState>> stateMap = getMapForSide(state.player);
		
		if (stateMap.containsKey(state.player.getCommandSenderName())) {
			stateMap.get(state.player.getCommandSenderName()).remove(state);
		}
	}
	
	/**
	 * Internal use only. Use SkillState.removeState instead.
	 * @param player
	 * @param clazz
	 */
	public static void removeStateWithClass(EntityPlayer player, Class<? extends SkillState> clazz) {
        Map<String, List<SkillState>> stateMap = getMapForSide(player);
	    if (!stateMap.containsKey(player.getCommandSenderName())) return;
	    
		List<SkillState> playerList = stateMap.get(player.getCommandSenderName());
		Iterator<SkillState> itor = playerList.iterator();
		while (itor.hasNext()) {
			SkillState state = itor.next();
			if (state.getClass().equals(clazz)) {
				state.reallyFinishSkill();
				itor.remove();
			}
		}
	}

	/**
	 * Get the first state with class of clazz for player.
	 * @param player
	 * @param clazz
	 * @return
	 */
	public static SkillState getStateWithClass(EntityPlayer player, 
			Class<? extends SkillState> clazz) {
		for (SkillState state : getState(player)) {
			if (state.getClass().equals(clazz)) {
				return state;
			}
		}
		return null;
	}
	
	/**
	 * Get all skill states of the given player. The result can not be modified.
	 * @param player
	 * @return
	 */
	public static List<SkillState> getState(EntityPlayer player) {
		Map<String, List<SkillState>> stateMap = getMapForSide(player);
		if (stateMap.containsKey(player.getCommandSenderName())) {
			return Collections.unmodifiableList(stateMap.get(player.getCommandSenderName()));
		} else {
			return Collections.unmodifiableList(new ArrayList<SkillState>());
		}
	}
	
	/**
	 * Send tick event to all active State in the server map. Called by EventHandlerServer.
	 */
	public static void tickServer() {
		for (List<SkillState> playerList : server.values()) {
			Iterator<SkillState> itor = playerList.iterator();
			while (itor.hasNext()) {
				SkillState state = itor.next();
				if (state.tickSkill()) {
					state.reallyFinishSkill();
					itor.remove();
				}
			}
		}
	}
	
	static Map<SkillState, Pattern> clientConnectMap = new HashMap();
	
	@SideOnly(Side.CLIENT)
	public static void regPatternFor(SkillState state, Pattern pattern) {
		clientConnectMap.put(state, pattern);
	}

	/**
	 * Send tick event to all active State in the client map. Called by EventHandlerClient.
	 */
	public static void tickClient() {
		for (List<SkillState> playerList : client.values()) {
			Iterator<SkillState> itor = playerList.iterator();
			while (itor.hasNext()) {
				SkillState state = itor.next();
				if (state.tickSkill()) {
					boolean res = state.reallyFinishSkill();
					Pattern pat = clientConnectMap.remove(state);
					if(pat != null) {
						pat.onStateEnd(res);
					}
					itor.remove();
				}
			}
		}
		if (++clientTickRemovePlayer == 20) {
		    updatePlayerOnClient();
		}
	}
	
	static int clientTickRemovePlayer = 0;
	
	@SideOnly(Side.CLIENT)
	private static void updatePlayerOnClient() {
		World world = Minecraft.getMinecraft().theWorld;
		for (List<SkillState> playerList : client.values()) {
			Iterator<SkillState> itor = playerList.iterator();
			while (itor.hasNext()) {
				SkillState state = itor.next();
				if (world.getEntityByID(state.player.getEntityId()) != state.player) {
					state.reallyFinishSkill();
					itor.remove();
				}
			}
		}
		
		//remove empty lists
		Iterator<Map.Entry<String, List<SkillState>>> itor = client.entrySet().iterator();
		while (itor.hasNext()) {
		    if (itor.next().getValue().isEmpty()) itor.remove();
		}
	}
	
	//Only used by SkillStateMessage.
	//Should NEVER be called on server.
	public static SkillState getStateById(String player, int id) {
		List<SkillState> playerList = client.get(player);
		//client.get(player);
		if (playerList == null) return null;
		for (SkillState s : playerList) {
			if (s.stateID == id) return s;
		}
		return null;
	}
	
	static DimensionSkillStateMessage constructDimensionMessage(int dimension) {
	    DimensionSkillStateMessage ret = new DimensionSkillStateMessage();
	    ret.dimension = dimension;

        for (List<SkillState> playerList : server.values()) {
            for (SkillState state : playerList) {
                if (state.player.worldObj.provider.dimensionId != dimension) {
                    break;
                }
                ret.states.add(new SkillStateMessage(state, SkillStateMessage.Action.SYNC));
            }
        }
	    
	    return ret;
	}
	
	@SideOnly(Side.CLIENT)
	public static boolean isClientStateEmpty() {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if(player == null) return true;
		//System.out.println(getMapForSide(player));
		List<SkillState> list = getMapForSide(player).get(player.getCommandSenderName());
		return list == null ? true : list.isEmpty();
	}

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.entity;
            //AcademyCraft.netHandler.sendTo(constructDimensionMessage(event.world.provider.dimensionId), player);
        }
    }
}
