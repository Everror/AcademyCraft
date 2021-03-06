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
package cn.academy.ability.meltdowner.skill;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.meltdowner.entity.EntityMdBall;
import cn.academy.ability.meltdowner.entity.EntityMiningRayBase;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.api.entityx.EntityX.EntityCallback;

/**
 * @author WeathFolD
 *
 */
public abstract class SkillMiningBase extends SkillBase {
	
	/**
	 * 
	 */
	public SkillMiningBase() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new MiningState(player, SkillMiningBase.this);
			}
			
		}.setCooldown(0));
	}
	
	abstract float getConsume(int slv, int lv); //per tick
	
	protected abstract EntityMiningRayBase createEntity(AbilityData data);
	
	public static class MiningState extends State {
		
		int slv, lv;
		float ccp;
		int harvLevel, spawnRate;
		SkillMiningBase instance;
		AbilityData data;
		EntityMiningRayBase ray;
		
		public MiningState(EntityPlayer player, SkillMiningBase _instance) {
			super(player);
			instance = _instance;
			data = AbilityDataMain.getData(player);
			slv = data.getSkillLevel(instance);
			lv = data.getLevelID() + 1;
			ccp = instance.getConsume(slv, lv);
			ray = instance.createEntity(data);
		}

		public MiningState(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			if(!isRemote()) {
				player.worldObj.spawnEntityInWorld(ray);
			}
		}

		@Override
		public boolean onFinish(boolean ended) {
			if(!isRemote()) {
				ray.setDead();
			}
			return true;
		}

		@Override
		public void onHold() {}
		
		@Override
		public boolean onTick(int ticks) {
			if(isRemote())
				return false;
			return !data.decreaseCP(ccp, instance);
		}
		
	}

}
