package cn.academy.ability.teleporter.skill;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import cn.academy.ability.teleporter.entity.EntityTpTarget;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.core.proxy.ACClientProps;

public class SkillRealtimeTeleport extends SkillBase {

	public static final float dis = 100.0F;
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new State(player);
			}
			
		});
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_RAILGUN;
	}
	
	public static class State extends SkillState {

		public State(EntityPlayer player) {
			super(player);
		}
		
		public double tx, ty, tz;
		private EntityTpTarget entity;
		
		@Override
		protected void onStart() {
			player.addChatComponentMessage(new ChatComponentText("rttp.state.onStart: " + (this.isRemote ? "client" : "server")));
			trace();
			if (!this.isRemote) {
				entity = new EntityTpTarget(this);
				player.worldObj.spawnEntityInWorld(entity);
			}
		}
		
		@Override
		protected boolean onTick() {
			player.addChatComponentMessage(new ChatComponentText("rttp.state.onTick: " + (this.isRemote ? "client" : "server")));
			trace();
			return false;
		}
		
		@Override
		protected void onFinish() {
			player.addChatComponentMessage(new ChatComponentText("rttp.state.onFinish: " + (this.isRemote ? "client" : "server")));
			if (!this.isRemote) {
				entity.setDead();
				player.setPositionAndUpdate(tx, ty, tz);
			}
		}
		
		private void trace() {
			MovingObjectPosition mop = player.rayTrace(dis, 1.0F);
			if (mop.typeOfHit.equals(MovingObjectType.MISS)) {
				player.addChatComponentMessage(new ChatComponentText("rttp.trace.miss: " + (this.isRemote ? "client" : "server")));
				tx = player.posX;
				ty = player.posY;
				tz = player.posZ;
			}
			else {
				tx = mop.hitVec.xCoord;
				ty = mop.hitVec.yCoord;
				tz = mop.hitVec.zCoord;
			}
		}
		
	}
	
}
