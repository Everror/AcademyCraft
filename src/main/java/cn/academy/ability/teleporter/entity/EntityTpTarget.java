package cn.academy.ability.teleporter.entity;

import cn.academy.ability.teleporter.skill.SkillRealtimeTeleport.State;
import cn.academy.core.AcademyCraftMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityTpTarget extends Entity {

	State state;
	
	public EntityTpTarget(State st) {
		super(st.player.worldObj);
		this.state = st;
		this.isImmuneToFire = true;
		//this.setSize(0.0F, 0.0F);
	}

	@Override
	protected void entityInit() {
		// TODO Auto-generated method stub
		AcademyCraftMod.log.info("entitytptgt.init" + worldObj);
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		this.setPosition(state.tx, state.ty, state.tz);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound var1) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound var1) {
		// TODO Auto-generated method stub

	}

}
