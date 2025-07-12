package dev.satherov.epitaphs.common.tile;

import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@NothingNull
public class EPGraveBlockEntity extends BaseContainerBlockEntity {

    // Inventory: 36
    // Armor: 4
    // Offhand: 1
    public static final int SIZE = 41;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public EPGraveBlockEntity(BlockPos pos, BlockState blockState) {
        super(EPRegistry.GRAVE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return EPRegistry.GRAVE.get().getName();
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public void setItems(NonNullList<ItemStack> list) {
        this.items = list;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        this.setChanged();
        return stack;
    }

    public ItemStack removeItem(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public void clearContent() {
        items.clear();
        this.setChanged();
    }
}
