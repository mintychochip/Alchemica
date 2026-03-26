package org.aincraft.providers;

import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.CauldronLevelChangeEvent;

/**
 * CauldronProvider for Minecraft 1.8.
 *
 * <p>On 1.8 the cauldron block uses numeric block data (byte) for its fill level:
 * 0 = empty, 1 = one-third, 2 = two-thirds, 3 = full. This is the same encoding
 * used in 1.9–1.12, so the implementation mirrors {@code LegacyCauldronProvider}
 * from the v1_12_R1 module.
 *
 * <p>{@link CauldronLevelChangeEvent} does not exist in 1.8, so
 * {@link #getOldLevel(CauldronLevelChangeEvent)} and
 * {@link #getNewLevel(CauldronLevelChangeEvent)} are never called at runtime and
 * throw {@link UnsupportedOperationException} as a guard.
 */
public final class v1_8_CauldronProvider extends AbstractCauldronProvider {

    @SuppressWarnings("deprecation")
    @Override
    public boolean isWaterCauldron(Block block) {
        Material material = block.getType();
        if (material != Material.CAULDRON) {
            return false;
        }
        // Block data > 0 means the cauldron has some water in it
        return block.getData() > 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getCauldronLevel(Block block) throws IllegalArgumentException {
        Preconditions.checkArgument(isWaterCauldron(block), "block is not a water cauldron");
        return block.getData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setCauldronLevel(Block block, int level) throws IllegalArgumentException {
        Preconditions.checkArgument(level >= 0 && level <= 3, "level must be 0-3");
        Preconditions.checkArgument(isWaterCauldron(block), "block is not a water cauldron");
        block.setData((byte) level);
    }

    /**
     * Not reachable on 1.8 — CauldronLevelChangeEvent does not fire.
     */
    @Override
    public int getOldLevel(CauldronLevelChangeEvent event) {
        throw new UnsupportedOperationException(
                "CauldronLevelChangeEvent does not exist in Minecraft 1.8");
    }

    /**
     * Not reachable on 1.8 — CauldronLevelChangeEvent does not fire.
     */
    @Override
    public int getNewLevel(CauldronLevelChangeEvent event) {
        throw new UnsupportedOperationException(
                "CauldronLevelChangeEvent does not exist in Minecraft 1.8");
    }
}
