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
package cn.academy.energy.block.tile.impl;

import cn.academy.energy.util.EnergyUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeathFolD
 *
 */
public class SlotElectricItem extends Slot {

	public SlotElectricItem(IInventory inv, int slot, int x, int y) {
		super(inv, slot, x, y);
	}
	
    @Override
	public boolean isItemValid(ItemStack stack) {
        return EnergyUtils.isElecItem(stack);
    }

}
