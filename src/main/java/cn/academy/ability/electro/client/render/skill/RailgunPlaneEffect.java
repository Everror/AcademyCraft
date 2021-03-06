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
package cn.academy.ability.electro.client.render.skill;

import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cn.academy.api.client.render.SkillRenderer;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.draw.DrawObject;
import cn.liutils.api.draw.DrawObject.EventType;
import cn.liutils.api.draw.prop.AssignTexture;
import cn.liutils.api.draw.prop.DisableCullFace;
import cn.liutils.api.draw.prop.DisableLight;
import cn.liutils.api.draw.prop.Offset;
import cn.liutils.api.draw.prop.Transform;
import cn.liutils.api.draw.tess.Rect;
import cn.liutils.util.HudUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class RailgunPlaneEffect extends SkillRenderer {
	
	static final long ANIM_LEN = 1200; //animation length in milliseconds
	static final long DELAY = 100;
	
	public static RailgunPlaneEffect instance = new RailgunPlaneEffect();
	
	public static long getAnimLength() {
		return ANIM_LEN + DELAY;
	}
	
	private static DrawObject circle, line;
	static {
		//Setup pieces
		{
			circle = new DrawObject();
			
			Rect rect = new Rect(1.7, 1.7);
			rect.setCentered();
			rect.map.setAbs(0, 0, 1, 0.8533333333);
			
			Transform trans = new Transform().setRotation(90, 0, 0);
			
			circle.addHandlers(
				new AssignTexture(ACClientProps.EFF_RAILGUN_PREP_CC),
				DisableLight.instance(),
				rect,
				trans);
		}
		
		{
			line = new DrawObject();
			
			Rect rect = new Rect(44, 0.2);
			rect.map.setAbs(0, .9, 1, 1.0);
			
			Offset off = new Offset(EventType.PRE_TESS);
			off.set(0, 0, -31);
			
			line.addHandlers(
				rect,
				off,
				DisableCullFace.instance(),
				DisableLight.instance(),
				new AssignTexture(ACClientProps.EFF_RAILGUN_PREP_CC)
			);
		}
	}

	public RailgunPlaneEffect() {}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void renderHandEffect(EntityPlayer player, HandRenderType type, long dt) {
		if(type == HandRenderType.EQUIPPED) return;
		if(dt < DELAY) return;
		dt -= DELAY;
		
		double tz = dt * dt / 3e4;
		double TRANS_TIME = ANIM_LEN * 0.2;
		double alpha = (dt < TRANS_TIME ? dt / TRANS_TIME : (dt > ANIM_LEN - TRANS_TIME ? (ANIM_LEN - dt) / TRANS_TIME : 1));
		
		//Draw a screen-filling blackout
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GLU.gluOrtho2D(0, 255, 0, 255);
		
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix(); 
		GL11.glDepthMask(false);
		GL11.glLoadIdentity();
		{
			GL11.glTranslated(0, 0, 0);
			GL11.glColor4d(0, 0, 0, 0.2 * alpha);
			HudUtils.setZLevel(1);
			HudUtils.drawModalRect(0, 0, 255, 255);
			HudUtils.setZLevel(-90);
		}
		GL11.glDepthMask(true);
		
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
		
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glMatrixMode(GL11.GL_MODELVIEW); //Restore the matrix
		
		//Draw the real effect
		
		GL11.glColor4d(1, 1, 1, alpha * 0.6);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.0f);
		GL11.glPushMatrix(); {
			GL11.glTranslated(-.4, 0.85 + tz * 0.37, tz);
			GL11.glRotated(-20.4, 1, 0, 0);
			
			drawSingleSide(7);
			
//			GL11.glPushMatrix(); {
//				GL11.glTranslated(-2.3, 0, 0);
//				drawSingleSide(7);
//			} GL11.glPopMatrix();
		} GL11.glPopMatrix();
		
		GL11.glDisable(GL11.GL_ALPHA_TEST);
	}
	
	private void drawSingleSide(int n) {
		//line.draw();
		for(int i = n; i >= 0; --i) {
			double z = (4 + 6 * i * i) / 12d;
			GL11.glPushMatrix();
			GL11.glTranslated(0, 0, -z);
			circle.draw();
			GL11.glPopMatrix();
		}
	}

}
