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
package cn.academy.ability.teleport.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.teleport.data.LocationData.Location;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Fired when client gui performs some action to modify the location data.
 * @author WeathFolD
 */
@RegistrationClass
public class ClientModifyMsg implements IMessage {
	
	int opcode;
	Object arg; //NULL if opcode==REMOVE

	//Opcodes
	public static final int 
		ADD = 0, //arg: Location
		REMOVE = 1, //arg: int
		CLEAR = 2; //arg: none
	
	public ClientModifyMsg(int _opcode, Object _arg) {
		opcode = _opcode;
		arg = _arg;
	}
	
	public ClientModifyMsg() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		opcode = buf.readByte();
		if(opcode == ADD) {
			arg = new Location(buf);
		} else if(opcode == REMOVE) {
			arg = (Integer) buf.readInt();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(opcode);
		if(opcode == ADD) {
			((Location)arg).toBuf(buf);
		} else if(opcode == REMOVE) {
			buf.writeInt((Integer) arg);
		}
	}
	
	@RegMessageHandler(msg = ClientModifyMsg.class, side = RegMessageHandler.Side.SERVER)
	public static class Handler implements IMessageHandler<ClientModifyMsg, IMessage> {

		@Override
		public IMessage onMessage(ClientModifyMsg msg, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			LocationData data = LocationData.get(player);
			
			switch(msg.opcode) {
			case ADD:
				data.locationList.add((Location) msg.arg);
				break;
			case REMOVE:
				data.locationList.remove((int)((Integer)msg.arg));
				break;
			case CLEAR:
				data.locationList.clear();
				break;
			}
			
			data.sync();
			return null;
		}
		
	}

}
