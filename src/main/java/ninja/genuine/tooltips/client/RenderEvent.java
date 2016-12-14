package ninja.genuine.tooltips.client;

import java.lang.reflect.Method;
import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ninja.genuine.tooltips.client.render.TooltipRenderer;

public class RenderEvent {

	private static Class<?> nei;

	private static Method info;

	private static boolean useNei = false;

	private Minecraft mc;

	private TooltipRenderer renderer;

	private boolean enabled = true;

	public RenderEvent() {}

	@SubscribeEvent
	public void render(final RenderWorldLastEvent event) {
		EntityPlayer player = mc.thePlayer;
		EntityItem item = getMouseOver(mc, event.getPartialTicks());
		if (item != null && enabled)
			renderer.renderTooltip(mc, item, generateTooltip(mc, player, item), event.getPartialTicks());
	}

	public void post() {
		mc = Minecraft.getMinecraft();
		renderer = new TooltipRenderer();
		try {
			nei = Class.forName("codechicken.nei.guihook.GuiContainerManager");
			if (nei != null) {
				info = nei.getDeclaredMethod("itemDisplayNameMultiline", ItemStack.class, GuiContainer.class, boolean.class);
				useNei = true;
			}
		}
		catch (final Exception e) {}
	}

	public void disable() {
		enabled = false;
		System.out.println("World Tooltips is now disabled.");
	}

	public List<String> generateTooltip(Minecraft mc, EntityPlayer player, EntityItem item) {
		List<String> list = null;
		if (useNei)
			try {
				list = (List<String>) info.invoke(null, item.getEntityItem(), null, mc.gameSettings.advancedItemTooltips);
			}
			catch (final Exception e) {}
		if (list == null)
			list = item.getEntityItem().getTooltip(player, mc.gameSettings.advancedItemTooltips);
		// list.add(ChatFormatting.BLUE.toString() + ChatFormatting.ITALIC.toString() + modNameFromStack(item.getEntityItem()) + ChatFormatting.RESET.toString());
		if (item.getEntityItem().func_190916_E() > 1)
			list.set(0, item.getEntityItem().func_190916_E() + " x " + list.get(0));
		return list;
	}

	public static String modNameFromStack(final ItemStack stack) {
		// TODO Find out where to get the mod name from a stack.
		return "Minecraft";
	}

	public static void addDebugInfo(final EntityItem entityItem, final List<String> list) {
		if (entityItem.getEntityItem().getItem() instanceof ItemArmor) {
			final ItemArmor item = (ItemArmor) entityItem.getEntityItem().getItem();
			list.add("Armor Strength: " + item.damageReduceAmount);
		} else if (entityItem.getEntityItem().getItem() instanceof ItemTool) {
			final ItemTool item = (ItemTool) entityItem.getEntityItem().getItem();
			list.add("Material: " + item.getToolMaterialName());
		} else if (entityItem.getEntityItem().getItem() instanceof ItemFood) {
			final ItemFood item = (ItemFood) entityItem.getEntityItem().getItem();
			list.add("Hunger: " + item.getHealAmount(entityItem.getEntityItem()));
			list.add("Saturation: " + item.getSaturationModifier(entityItem.getEntityItem()));
		} else if (entityItem.getEntityItem().getItem() instanceof ItemPotion) {
			final ItemPotion item = (ItemPotion) entityItem.getEntityItem().getItem();
			final List<PotionEffect> effects = PotionUtils.getEffectsFromStack(entityItem.getEntityItem());
			if (effects != null)
				for (final PotionEffect effect : effects)
					list.add("Potion Effect: " + I18n.format(effect.getEffectName()));
		}
	}

	public static EntityItem getMouseOver(Minecraft mc, float partialTicks) {
		Entity viewer = mc.getRenderViewEntity();
		mc.mcProfiler.startSection("tooltip");
		final double distanceLook = 16;
		final Vec3d eyes = viewer.getPositionEyes(partialTicks);
		final Vec3d look = viewer.getLook(partialTicks);
		final Vec3d eyesLook = eyes.addVector(look.xCoord * distanceLook, look.yCoord * distanceLook, look.zCoord * distanceLook);
		final float distanceMax = 5;
		final List<EntityItem> entityList = mc.theWorld.getEntitiesWithinAABB(EntityItem.class,
				viewer.getEntityBoundingBox().addCoord(look.xCoord * distanceLook, look.yCoord * distanceLook, look.zCoord * distanceLook).expand(distanceMax, distanceMax, distanceMax));
		double difference = 0;
		EntityItem target = null;
		for (int i = 0; i < entityList.size(); i++) {
			final EntityItem entity = entityList.get(i);
			final float boundSize = 0.2f;
			final AxisAlignedBB entityCollisionBox = entity.getEntityBoundingBox().expand(boundSize, boundSize, boundSize);
			final RayTraceResult objectInVector = entityCollisionBox.calculateIntercept(eyes, eyesLook);
			if (entityCollisionBox.isVecInside(eyes)) {
				if (0.0D <= difference) {
					target = entity;
					difference = 0;
				}
			} else if (objectInVector != null) {
				final double distance = eyes.distanceTo(objectInVector.hitVec);
				if (distance < difference || difference == 0.0D) {
					target = entity;
					difference = distance;
				}
			}
		}
		mc.mcProfiler.endSection();
		return target;
	}
}
