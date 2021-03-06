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
package cn.academy.ability.teleport.entity.fx;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import cn.academy.ability.teleport.client.render.entity.MarkRender;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Spawn a position mark indicating where the player would be teleport to.
 * You should spawn this entity in both sides and it will not synchronize.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public abstract class EntityTPMarking extends EntityX {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static MarkRender render;
	
	final AbilityData data;
	protected final EntityPlayer player;

	public EntityTPMarking(EntityPlayer player) {
		super(player.worldObj);
		data = AbilityDataMain.getData(player);
		this.player = player;
		updatePos();
		this.ignoreFrustumCheck = true;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		rotationPitch = player.rotationPitch;
		rotationYaw = player.rotationYaw;
		this.updatePos();
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	protected void updatePos() {
		double md = getMaxDistance();
		MovingObjectPosition mop = GenericUtils.tracePlayer(player, md);
		
		
		if(mop != null) {
			double x = mop.hitVec.xCoord,
					y= mop.hitVec.yCoord,
					z= mop.hitVec.zCoord;
			switch(mop.sideHit) {
			case 0:
				y -= 1.0; break;
			case 1:
				y += 1.8; break;
			case 2:
				z -= .6; y = mop.blockY + 1.7; break;
			case 3:
				z += .6; y = mop.blockY + 1.7;  break;
			case 4:
				x -= .6; y = mop.blockY + 1.7;  break;
			case 5: 
				x += .6; y = mop.blockY + 1.7;  break;
			}
			//check head
			if(mop.sideHit > 1) {
				int hx = (int) x, hy = (int) (y + 1), hz = (int) z;
				if(!worldObj.isAirBlock(hx, hy, hz)) {
					y -= 1.25;
				}
			}
			
			setPosition(x, y, z);
		} else {
			Motion3D mo = new Motion3D(player, true);
			mo.move(md);
			setPosition(mo.posX, mo.posY, mo.posZ);
		}
	}
	
	public double getDist() {
		return this.getDistanceToEntity(player);
	}
	
	protected abstract double getMaxDistance();

}
