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
package cn.academy.ability.generic.skill;

import cn.academy.api.ability.SkillBase;

public class SkillBrainTrainingCourse2 extends SkillBase {
	public SkillBrainTrainingCourse2() {
		this.setName("gn_btc2");
		this.setLogo("generic/brain_training_course2.png");
	}
	
	@Override
	public boolean isDummy() {
		return true;
	}
}
