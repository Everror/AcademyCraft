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

import net.minecraftforge.common.config.Property;
import cn.academy.core.AcademyCraft;
import cn.academy.core.ctrl.EventHandlerClient;

/**
 * A set of key-skill binding. Manipulated by EventHandlerClient.
 * @author acaly
 */
public class Preset {
	
	/**
	 * Internal data. int[4].
	 * Also used in PresetManager to save presets.
	 */
	int[] data;
	
	Preset(Preset another) {
		data = another.data.clone();
	}

	@Override
	public Preset clone() {
		return new Preset(this);
	}
	
	/**
	 * Create a new preset with given property containing an int array.
	 * @param prop
	 */
	Preset(Property prop) {
		if (!prop.isIntList()) {
			AcademyCraft.log.error("Invalid preset in config file.");
			data = new int[EventHandlerClient.MAX_KEYS];
		}
		data = prop.getIntList();
		if (data.length != 4) {
			AcademyCraft.log.error("Invalid size of preset in config file.");
			data = new int[EventHandlerClient.MAX_KEYS];
		}
	}
	
	/**
	 * Create a new empty preset.
	 */
	Preset() {
		data = new int[EventHandlerClient.MAX_KEYS];
	}
	
	/**
	 * Get the skill id binded to the given key (0-3).
	 * @param keyId
	 * @return
	 */
	public int getSkillMapping(int keyId) {
		if (keyId >= data.length || keyId < 0) {
			AcademyCraft.log.warn("Index overflow in Preset.");
			return 0;
		}
		return data[keyId];
	}
	
	public boolean hasSkillMapping(int skillId) {
		for(int i = 0; i < 4; ++i) {
			if(data[i] == skillId)
				return true;
		}
		return false;
	}
	
	/**
	 * Set the skill mapping.
	 * @param keyId
	 * @param skillId
	 */
	public void setSkillMapping(int keyId, int skillId) {
		if (keyId >= data.length || keyId < 0) {
			AcademyCraft.log.warn("Index overflow in Preset.");
		} else {
			data[keyId] = skillId;
		}
		validateMapping();
	}

	private void validateMapping() {
		for (int i = 0; i < EventHandlerClient.MAX_KEYS; ++i) {
			if (data[i] == 0) continue;
			for (int j = i + 1; j < EventHandlerClient.MAX_KEYS; ++j) {
				if (data[j] == 0) continue;
				if (data[i] == data[j]) {
					AcademyCraft.log.error("Invalid skill mapping. Reset to default preset.");
					data = new int[EventHandlerClient.MAX_KEYS];
					return;
				}
			}
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Preset))
			return false;
		Preset p = (Preset) obj;
		for(int i = 0; i < 4; ++i) {
			if(data[i] != p.data[i])
				return false;
		}
		return true;
	}
}
