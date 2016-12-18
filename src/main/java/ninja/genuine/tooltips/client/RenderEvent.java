package ninja.genuine.tooltips.client;

import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ninja.genuine.tooltips.WorldTooltips;
import ninja.genuine.tooltips.system.Tooltip;

public class RenderEvent {

	private Minecraft mc;
	private EntityItem entity;
	private Tooltip cache;

	public RenderEvent() {}

	public void post() {
		mc = Minecraft.getMinecraft();
	}

	public void syncColors() {
		if (!Objects.isNull(cache))
			cache.syncSettings();
	}

	@SubscribeEvent
	public void render(final RenderWorldLastEvent event) {
		entity = getMouseOver(mc, event.partialTicks);
		if (!Objects.isNull(entity)) {
			if (Objects.isNull(cache) || cache.getEntity() != entity)
				cache = new Tooltip(Minecraft.getMinecraft().thePlayer, entity);
			cache.renderTooltip3D(mc, event.partialTicks);
		}
	}

	@SubscribeEvent
	public void render(final RenderGameOverlayEvent.Post event) {
		// TODO Let's make it a choice to do 2D or 3D tooltips.
		//      Just need to make a nice anchoring gui first.
		// renderer.renderTooltip2D(mc, item, generateTooltip(mc, mc.player, item.getEntityItem()), event.getPartialTicks());
	}

	public static EntityItem getMouseOver(Minecraft mc, float partialTicks) {
		Entity viewer = mc.getRenderViewEntity();
		mc.mcProfiler.startSection("world-tooltips");
		double distanceLook = WorldTooltips.maxDistance;
		Vec3 eyes = viewer.getPositionEyes(partialTicks);
		Vec3 look = viewer.getLook(partialTicks);
		Vec3 eyesLook = eyes.addVector(look.xCoord * distanceLook, look.yCoord * distanceLook, look.zCoord * distanceLook);
		float distanceMax = 1;
		List<EntityItem> entityList = mc.theWorld.getEntitiesWithinAABB(EntityItem.class,
				viewer.getEntityBoundingBox().addCoord(look.xCoord * distanceLook, look.yCoord * distanceLook, look.zCoord * distanceLook).expand(distanceMax, distanceMax, distanceMax));
		double difference = 0;
		EntityItem target = null;
		for (int i = 0; i < entityList.size(); i++) {
			EntityItem entity = entityList.get(i);
			float boundSize = 0.15F;
			AxisAlignedBB aabb1 = entity.getEntityBoundingBox();
			AxisAlignedBB aabb2 = new AxisAlignedBB(aabb1.minX, aabb1.minY, aabb1.minZ, aabb1.maxX, aabb1.maxY, aabb1.maxZ);
			AxisAlignedBB expandedAABB = aabb2.offset(0, 0.25, 0).expand(0.15, 0.1, 0.15).expand(boundSize, boundSize, boundSize);
			MovingObjectPosition objectInVector = expandedAABB.calculateIntercept(eyes, eyesLook);
			if (expandedAABB.isVecInside(eyes)) {
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
