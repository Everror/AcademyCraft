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
package cn.academy.core.client.render;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import org.lwjgl.opengl.GL11;

import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.client.render.SkillRenderer.HandRenderType;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.api.render.IPlayerRenderHook;
import cn.liutils.registry.PlayerRenderHookRegistry.RegPlayerRenderHook;
import cn.liutils.registry.PlayerRenderHookRegistry.RegPlayerRenderHook.Pass;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The generic render pipeline of SkillRender. 
 * Multiple events are handled here, 
 * while multiple rendering routines sharing same util/drawing functions.
 * @author WeathFolD
 */
@RegistrationClass
@SideOnly(Side.CLIENT)
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
@RegEventHandler({Bus.Forge, Bus.FML})
public class SkillRenderManager {
	
	private static SkillRenderManager instance = new SkillRenderManager();
	
	/**
	 * Current alive renderers.
	 */
	private static Set<RenderNode> renderers = new HashSet();
	
	public static class RenderNode {
		public final SkillRenderer render;
		public final long createTime;
		public final long lifeTime;
		public boolean dead;
		
		public RenderNode(SkillRenderer sr, long _lifeTime) {
			render = sr;
			createTime = Minecraft.getSystemTime();
			lifeTime = _lifeTime;
		}
		
		public void setDead() {
			dead = true;
		}
	}
	
	public static RenderNode addEffect(SkillRenderer renderer, long time) {
		RenderNode ret = new RenderNode(renderer, time);
		renderers.add(ret);
		return ret;
	}
	
	public static RenderNode addEffect(SkillRenderer renderer) {
		return addEffect(renderer, Long.MAX_VALUE);
	}
	
	public static void init() {
	}
	
	@SubscribeEvent
	public void clientTick(ClientTickEvent event) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if(player == null) return;
		
		Iterator<RenderNode> iter = renderers.iterator();
		while(iter.hasNext()) {
			RenderNode node = iter.next();
			long dt = Minecraft.getSystemTime() - node.createTime;
			if(dt > node.lifeTime) {
				iter.remove();
				continue;
			}
			if(node.dead || 
			node.render.tickUpdate(player, dt)) {
				iter.remove();
			}
		}
	}
	
	@SubscribeEvent
	public void renderHudEvent(RenderGameOverlayEvent e) {
		ScaledResolution sr = e.resolution;
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if(e.type != ElementType.CROSSHAIRS)
			return;
		GL11.glDepthFunc(GL11.GL_ALWAYS);
		long time = Minecraft.getSystemTime();
		for(RenderNode node : renderers) {
			node.render.renderHud(player, sr, time - node.createTime);
		}
		RenderUtils.loadTexture(WIDGITS);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
	}
	
	private static final ResourceLocation WIDGITS = new ResourceLocation("textures/gui/widgets.png");
	
	public static void renderThirdPerson(EntityLivingBase ent, ItemStack stack, ItemRenderType type) {
		if(type == ItemRenderType.EQUIPPED_FIRST_PERSON || !(ent instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) ent;
		Item item = stack.getItem();
		Block block = Block.getBlockFromItem(stack.getItem());
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			
			GL11.glColor4d(1, 0.6, 0.6, 0.55);
			
			if(stack.getItemSpriteNumber() == 0 && item instanceof ItemBlock) { //block render routine
				GL11.glTranslated(0, 0, 1.5);
				GL11.glScalef(2F, 2F, 2F);
				GL11.glRotated(45, 1, 0, 1);
				GL11.glRotated(45, 0, 1, 0);
				GL11.glRotated(180, 1, 0, 0);
			} else if(item.isFull3D()) {
				GL11.glTranslated(0.1, 0.8, -.4);
				GL11.glScalef(.8F, .8F, .8F);
				GL11.glRotated(45, 0, 0, -1);
			} else {
				GL11.glTranslated(-.3, 1.2, -.6);
				GL11.glRotatef(90, 1, 0, 0);
				GL11.glScalef(1.5F, 1.5F, 1.5F);
			}
			
			traverseHandRender(player, HandRenderType.EQUIPPED);
			
		} GL11.glPopMatrix();
	}
	
	public static void renderFirstPerson() {
		//System.out.println("rfp");
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			GL11.glTranslated(0.28, -0.55, -.9);
			GL11.glColor4d(1, 0.6, 0.6, 0.55);
			GL11.glScalef(0.66F, 0.66F, 0.66F);
			GL11.glRotatef(20, 1, 0, 0);

			//RenderUtils.loadTexture(ACClientProps.TEX_ARC_SHELL[0]);
			//RenderUtils.drawCube(1, 1, 1, false);
			traverseHandRender(Minecraft.getMinecraft().thePlayer, HandRenderType.FIRSTPERSON);
			//rpe.renderHandEffect(Minecraft.getMinecraft().thePlayer, null, HandRenderType.FIRSTPERSON);
			
		} GL11.glPopMatrix();
	}
	
	//private static RailgunPlaneEffect rpe = new RailgunPlaneEffect(0);
	
	private static void traverseHandRender(EntityPlayer player, HandRenderType type) {
		long time = Minecraft.getSystemTime();
		for(RenderNode node : renderers) {
			node.render.renderHandEffect(player, type, time - node.createTime);
		}
	}
	
	@RegPlayerRenderHook(Pass.ALPHA)
	public static class PRHSkillRender implements IPlayerRenderHook {

		public PRHSkillRender() {}

		@Override
		public boolean isActivated(EntityPlayer player, World world) {
			return true;
		}

		@Override
		public void renderHead(EntityPlayer player, World world) {}

		@Override
		public void renderBody(EntityPlayer player, World world) {
			AbilityData data = AbilityDataMain.getData(player);
			long time = Minecraft.getSystemTime();
			for(RenderNode node : renderers) {
				GL11.glPushMatrix();
				node.render.renderSurroundings(player, time - node.createTime);
				GL11.glPopMatrix();
			}
		}

	}

	
	private static Vec3 vec(double x, double y, double z) {
		return Vec3.createVectorHelper(x, y, z);
	}
	
}
