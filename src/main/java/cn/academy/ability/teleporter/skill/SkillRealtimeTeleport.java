package cn.academy.ability.teleporter.skill;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;

public class SkillRealtimeTeleport extends SkillBase {

	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new State(player);
			}
			
		});
	}
	
	private static class State extends SkillState {

		public State(EntityPlayer player) {
			super(player);
		}
		
		@Override
		public boolean onTick() {
			
		}
		
	}
	
}
