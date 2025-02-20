package portablejim.bbw.core;

import java.util.LinkedList;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import portablejim.bbw.BetterBuildersWandsMod;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.core.conversion.CustomMapping;
import portablejim.bbw.core.items.IWandItem;
import portablejim.bbw.core.wands.IWand;
import portablejim.bbw.shims.BasicPlayerShim;
import portablejim.bbw.shims.BasicWorldShim;
import portablejim.bbw.shims.CreativePlayerShim;
import portablejim.bbw.shims.IPlayerShim;
import portablejim.bbw.shims.IWorldShim;

/**
 * Events for supporting wands.
 */
public class BlockEvents {

    @SubscribeEvent
    public void blockHighlightEvent(DrawBlockHighlightEvent event) {
        if (event.currentItem != null && event.currentItem.getItem() instanceof IWandItem
                && event.target != null
                && event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            IPlayerShim playerShim = new BasicPlayerShim(event.player);
            if (event.player.capabilities.isCreativeMode) {
                playerShim = new CreativePlayerShim(event.player);
            }
            IWorldShim worldShim = new BasicWorldShim(event.player.getEntityWorld());
            if (event.currentItem.getItem() instanceof IWandItem) {
                IWandItem wandItem = (IWandItem) event.currentItem.getItem();
                IWand wand = wandItem.getWand();

                WandWorker worker = new WandWorker(wand, playerShim, worldShim);

                Point3d clickedPos = new Point3d(event.target.blockX, event.target.blockY, event.target.blockZ);
                // ItemStack pickBlock = worldShim.getBlock(clickedPos).getPickBlock(event.target,
                // playerShim.getPlayer().getEntityWorld(), clickedPos.x, clickedPos.y, clickedPos.z,
                // playerShim.getPlayer());
                ItemStack sourceItems = worker.getProperItemStack(worldShim, playerShim, clickedPos);

                if (sourceItems != null && sourceItems.getItem() instanceof ItemBlock) {
                    Block targetedBlock = worldShim.getBlock(clickedPos);
                    int meta = worldShim.getMetadata(clickedPos);
                    CustomMapping customMapping = BetterBuildersWandsMod.instance.mappingManager
                            .getMapping(targetedBlock, meta);
                    int numBlocks = Math.min(
                            wand.getMaxBlocks(event.currentItem),
                            playerShim.countItems(
                                    sourceItems,
                                    customMapping != null && customMapping.shouldCopyTileNBT()));

                    LinkedList<Point3d> blocks = worker.getBlockPositionList(
                            clickedPos,
                            ForgeDirection.getOrientation(event.target.sideHit),
                            numBlocks,
                            wandItem.getMode(event.currentItem),
                            wandItem.getFaceLock(event.currentItem),
                            wandItem.getFluidMode(event.currentItem),
                            customMapping != null && customMapping.shouldCopyTileNBT());
                    if (blocks.size() > 0) {
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glDepthMask(true);
                        GL11.glLineWidth(2.5F);
                        for (Point3d block : blocks) {
                            AxisAlignedBB boundingBox = AxisAlignedBB
                                    .getBoundingBox(block.x, block.y, block.z, block.x + 1, block.y + 1, block.z + 1)
                                    .contract(0.005, 0.005, 0.005);
                            double interpolatedX = event.player.lastTickPosX
                                    + (event.player.posX - event.player.lastTickPosX) * event.partialTicks;
                            double interpolatedY = event.player.lastTickPosY
                                    + (event.player.posY - event.player.lastTickPosY) * event.partialTicks;
                            double interpolatedZ = event.player.lastTickPosZ
                                    + (event.player.posZ - event.player.lastTickPosZ) * event.partialTicks;
                            RenderGlobal.drawOutlinedBoundingBox(
                                    boundingBox.getOffsetBoundingBox(-interpolatedX, -interpolatedY, -interpolatedZ),
                                    0xC0C0C0);
                        }
                        GL11.glEnable(GL11.GL_TEXTURE_2D);
                        GL11.glDisable(GL11.GL_BLEND);
                    }
                }
            }
        }
        // FMLLog.info("Happened!" + event.target.toString());
    }
}
