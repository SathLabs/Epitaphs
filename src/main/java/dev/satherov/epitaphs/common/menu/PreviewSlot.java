package dev.satherov.epitaphs.common.menu;

import lombok.Getter;
import lombok.experimental.Accessors;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

@Getter
@Accessors(fluent = true)
public class PreviewSlot extends Slot {
    
    private final BooleanSupplier visible;
    private final @Nullable String identifier;
    private final boolean cosmetic;
    
    public PreviewSlot(Container container, int slot, int x, int y, BooleanSupplier visible) {
        this(container, slot, x, y, visible, null, false);
    }
    
    public PreviewSlot(Container container, int slot, int x, int y, BooleanSupplier visible, @Nullable String identifier, boolean cosmetic) {
        super(container, slot, x, y);
        this.visible = visible;
        this.identifier = identifier;
        this.cosmetic = cosmetic;
    }
    
    @Override
    public boolean mayPickup(Player player) {
        return false;
    }
    
    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }
    
    @Override
    public boolean allowModification(Player player) {
        return false;
    }
    
    @Override
    public boolean isActive() {
        return this.visible.getAsBoolean();
    }
}
