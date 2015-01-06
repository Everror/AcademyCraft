package cn.academy.ability.teleporter.client.render;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.teleporter.entity.EntityTpTarget;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.proxy.ACClientProps;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

public class RenderTpTarget extends Render {

	@Override
	public void doRender(Entity entity, double x, double y, double z,
			float var8, float var9) {
		AcademyCraftMod.log.info("render");
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		RenderBlocks.getInstance().renderBlockAsItem(Blocks.tnt, 0, 1.0F);
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return ACClientProps.TEX_QUESTION_MARK;
	}

}
