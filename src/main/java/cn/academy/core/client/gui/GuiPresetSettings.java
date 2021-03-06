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
package cn.academy.core.client.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.Preset;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.client.ACLangs;
import cn.academy.core.ctrl.EventHandlerClient;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cn.liutils.api.register.Configurable;
import cn.liutils.registry.AttachKeyHandlerRegistry.RegAttachKeyHandler;
import cn.liutils.registry.ConfigurableRegistry.RegConfigurable;
import cn.liutils.util.ClientUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * @author WeAthFolD
 *
 */
@RegistrationClass
@RegConfigurable
@SideOnly(Side.CLIENT)
public class GuiPresetSettings extends LIGuiScreen {

	@Configurable(category = "Control", key = "keyPresetSettings", defValueInt = Keyboard.KEY_N)
	@RegAttachKeyHandler(clazz = KeyHandler.class)
	public static int KEY_ID_PRESET_SETTINGS;
	
	public static class KeyHandler implements IKeyHandler {
		@Override
		public void onKeyDown(int keyCode, boolean tickEnd) {
			if(tickEnd || !ClientUtils.isPlayerInGame()) return;
			Minecraft mc = Minecraft.getMinecraft();
			GuiHandlers.handlerPresetSettings.openClientGui();
		}
		@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
		@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
	}
	

	
	private boolean isSetting;

	private int currentPage;
	private static final int MAX_PAGE = PresetManager.PRESET_COUNT;
	
	private Preset tempPreset; //Used for current setting
	private boolean isModifying;
	private int modKey;
	private PageMain pageMain;
	
	public GuiPresetSettings() {
		super();
		tempPreset = PresetManager.getCurrentPreset().clone();
		gui.addWidget(pageMain = new PageMain());
	}
	
	private class PageMain extends Widget {
		private static final float 
			RATIO = 1.75F, 
			HEIGHT = 80, 
			WIDTH = 140,
			PAGE_STEP = 16;
		
		private class SelectPage extends Widget {
			
			int id;

			public SelectPage(int _id, float x) {
				super(x, 0, PAGE_STEP, HEIGHT / 6);
				id = _id;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				boolean draw = false;
				float color = 0F;
				if(id == currentPage) {
					draw = true;
					color = 0.1F;
				} else if(mouseHovering) {
					draw = true;
					color = 0.5F;
				}
				if(draw) {
					RenderUtils.bindGray(color, .6);
					HudUtils.drawModalRect(0, 0, width, height);
				}
				RenderUtils.bindGray(.8, .8);
				drawText(String.valueOf(id), 6, 2.5, 6);
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				if(id == currentPage) return;
				tempPreset = PresetManager.getPreset(id).clone();
				currentPage = id;
			}
			
		}
		
		private class PartKeyInfo extends Widget {
			
			int id;
			static final float STEP = 28.67F, WIDTH = STEP + 5.3F, LOGO_SIZE = 23.3F, HEIGHT = 46.7F;

			public PartKeyInfo(int _id) {
				super(5 + WIDTH * _id, 18.5F, STEP, HEIGHT);
				id = _id;
				doesDraw = true;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				double tx = WIDTH / 2, ty = 4;
				RenderUtils.bindGray(.8, .8);
				String str = LIKeyProcess.getKeyName(EventHandlerClient.getKeyId(id));
				drawText(str, STEP / 2, ty, 6.5, Align.CENTER);
				
				tx = 2.5;
				ty = 20;
				HudUtils.drawRectOutline(tx, ty, LOGO_SIZE, LOGO_SIZE, 2);
				EntityPlayer player = Minecraft.getMinecraft().thePlayer;
				AbilityData data = AbilityDataMain.getData(player);
				ResourceLocation logo = data.getSkill(tempPreset.getSkillMapping(id)).getLogo();
				if(logo != null) {
					RenderUtils.loadTexture(logo);
					HudUtils.drawRect(tx, ty, LOGO_SIZE, LOGO_SIZE);
				}
				if(mouseHovering) {
					RenderUtils.bindGray(.6, .6);
					HudUtils.drawModalRect(0, 0, STEP, HEIGHT);
				}
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				modKey = id;
				isModifying = true;
				PageMain.this.doesDraw = false;
				gui.addWidget(new PageModify());
			}
			
		}
		
		private class ButtonGeneric extends Widget {
			
			static final float WIDTH = 28f, HEIGHT = 10F;
			static final float 
				HOVERING_COLOR = 0.6F,
				ORDINARY_COLOR = 0.3F,
				DISABLED_COLOR = 0.2F,
				TEXT_COLOR = 0.6F,
				TEXT_DISABLE_COLOR = 0.4F;

			protected boolean disabled = false;
			private final String name;
			
			public ButtonGeneric(String name, float x, float y) {
				super(x, y, WIDTH, HEIGHT);
				doesDraw = true;
				this.name = name;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				disabled = PresetManager.getPreset(currentPage).equals(tempPreset);
				
				float color;
				if(disabled) {
					color = DISABLED_COLOR;
				} else if(mouseHovering) {
					color = HOVERING_COLOR;
				} else color = ORDINARY_COLOR;
				GL11.glDepthFunc(GL11.GL_ALWAYS);
				RenderUtils.bindGray(color, .6);
				HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT);
				
				float fsize = 5F;
				color = disabled ? TEXT_DISABLE_COLOR : TEXT_COLOR;
				GL11.glColor4f(color, color, color, 0.9F);
				String translated = StatCollector.translateToLocal(name);
				drawText(translated,
						WIDTH / 2,
						HEIGHT / 2 - fsize / 1.8, 
						fsize, 
						Align.CENTER);
				GL11.glDepthFunc(GL11.GL_LEQUAL);
			}
			
		}
		
		public PageMain() {
			super(0, 0, WIDTH, HEIGHT);
			this.alignStyle = AlignStyle.CENTER;
		}
		
		@Override
		public void onAdded() {
			for(int i = 0; i < MAX_PAGE; ++i) {
				addWidget(new SelectPage(i, PAGE_STEP * i));
			}
			for(int i = 0; i < EventHandlerClient.MAX_KEYS; ++i) {
				addWidget(new PartKeyInfo(i));
			}
			
			addWidgets(
			new ButtonGeneric("ac.accept", 70, 67.5F) {
				@Override
				public void onMouseDown(double mx, double my) {
					PresetManager.setPreset(currentPage, tempPreset.clone());
				}
			},
			new ButtonGeneric("ac.restore", 105.5F, 67.5F) {
				@Override
				public void onMouseDown(double mx, double my) {
					tempPreset = PresetManager.getPreset(currentPage).clone();
				}
			});
		}

		@Override
		public void draw(double mx, double my, boolean hover) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			HudUtils.setZLevel(zLevel);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glPushMatrix(); {
				RenderUtils.bindGray(.15, .6);
				HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT + 2);
				
				RenderUtils.bindGray(.25, .6);
				HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT / 6);
			} GL11.glPopMatrix(); 
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			
			//page text
			RenderUtils.bindGray(.8, .8);
			drawText(ACLangs.presetSettings(), 130, 2.8, 6F, Align.RIGHT);
		}
		
	}
	
	private class PageModify extends Widget {
		
		double mAlpha = 0.0;
		long createTime;
		
		private class PartSkillInfo extends Widget {
			
			SkillBase skill;
			public final int id;
			final boolean used; //If this skill already have a mapping

			public PartSkillInfo(SkillBase _skill, int i, int j, double beg) {
				super(beg + STEP * j , HEIGHT / 2 - WIDTH / 2,
					WIDTH, WIDTH);
				doesDraw = true;
				skill = _skill;
				this.id = i;
				
				boolean u = false;
				if(i != 0) {
					for(int k = 0; k < 4; ++k) {
						if(tempPreset.getSkillMapping(k) == i) 
							u = true;
					}
				}
				used = u;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				final float lsize = 24;
			
				float tx = WIDTH / 2 - lsize / 2;
				RenderUtils.bindGray(.8, .8 * mAlpha);
				HudUtils.drawRectOutline(tx, tx, lsize, lsize, 2);
				ResourceLocation logo = skill.getLogo();
				RenderUtils.bindGray(1, mAlpha);
				RenderUtils.loadTexture(logo);
				HudUtils.drawRect(tx, tx, lsize, lsize);
				
				if(mouseHovering || used) {
					RenderUtils.bindGray(used ? .1 : .4, .5 * mAlpha);
					HudUtils.drawModalRect(0, 0, WIDTH, WIDTH);
				}
				
				if(mouseHovering) {
					String tit = skill.getDisplayName();
					RenderUtils.bindGray(.7, .8);
					drawText(tit, WIDTH / 2, 32, 6, Align.CENTER);
				}
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				if(used) return;
				tempPreset.setSkillMapping(modKey, id);
				isModifying = false;
				pageMain.doesDraw = true;
				PageModify.this.dispose();
			}
			
		}

		static final float HEIGHT = 50, WIDTH = 30, STEP = WIDTH + 10;

		public PageModify() {
			super(0, 0, GuiPresetSettings.this.width, HEIGHT);
			this.alignStyle = AlignStyle.CENTER;
			createTime = Minecraft.getSystemTime();
		}
		
		@Override
		public void onAdded() {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			AbilityData data = AbilityDataMain.getData(player);
			
			List<Integer> learnedSkills = data.getControlSkillList();
			double beg = width / 2 - ((learnedSkills.size() - 1) * STEP + WIDTH) / 2;
			int j = 0;
			for(int i : learnedSkills) {
				addWidget(new PartSkillInfo(data.getSkill(i), i, j++,  beg));
			}
		}
		
		@Override
		public void draw(double mx, double my, boolean hover) {
			mAlpha = Math.min(1.0, (Minecraft.getSystemTime() - createTime) / 300D);
			
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			HudUtils.setZLevel(zLevel);
			GL11.glDepthFunc(GL11.GL_ALWAYS);
			GL11.glPushMatrix(); {
				RenderUtils.bindGray(.1f, .6f * mAlpha);
				
				RenderUtils.loadTexture(ACClientProps.TEX_GUI_KS_MASK);
				HudUtils.drawRect(0, 0, width, HEIGHT);
				
			} GL11.glPopMatrix();
			GL11.glDepthFunc(GL11.GL_ALWAYS);
			
			String str = ACLangs.chooseSkill();
			RenderUtils.bindGray(.8, .8 * mAlpha);
			drawText(str, 5, 2, 7);
		}
		
	}
	
	private void drawText(String text, double x, double y, double size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	private void drawText(String text, double x, double y, double size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}
	
}