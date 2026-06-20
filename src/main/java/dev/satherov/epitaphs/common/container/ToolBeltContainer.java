package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.common.component.SlotStackList;
import dev.satherov.epitaphs.common.data.SoulboundHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;

import dev.gigaherz.toolbelt.slot.BeltAttachment;

import java.util.Collections;
import java.util.List;

public record ToolBeltContainer(SlotStackList stack) implements SaveContainer<ToolBeltContainer> {
    
    public static final Codec<ToolBeltContainer> CODEC = SlotStackList.CODEC.xmap(ToolBeltContainer::new, ToolBeltContainer::stack);
    
    public static ToolBeltContainer empty() {
        return new ToolBeltContainer(new SlotStackList(1));
    }
    
    // ==================== ONLINE ====================
    
    public static ToolBeltContainer create(ServerPlayer player) {
        final SlotStackList list = new SlotStackList(1);
        list.add(BeltAttachment.get(player).getContents());
        return new ToolBeltContainer(list);
    }
    
    public static ToolBeltContainer createSoulbound(ServerPlayer player) {
        final SlotStackList list = new SlotStackList(1);
        BeltAttachment attachment = BeltAttachment.get(player);
        final ItemStack stack = attachment.getContents();
        if (SoulboundHandler.isSoulbound(stack)) {
            list.add(stack.copyAndClear());
            attachment.setContents(ItemStack.EMPTY);
        }
        return new ToolBeltContainer(list);
    }
    
    @Override
    public void write(ServerPlayer player) {
        final ItemStack stack = this.stack.getStack(0).copyAndClear();
        if (!stack.isEmpty()) BeltAttachment.get(player).setContents(stack);
    }
    
    // ==================== OFFLINE ====================
    
    public static ToolBeltContainer create(ValueInput input) {
        final List<ItemStack> toolbelt = input.rawChildOrEmpty("neoforge:attachments")
                .rawChildOrEmpty("toolbelt:belt")
                .listOrEmpty("stacks", ItemStack.OPTIONAL_CODEC)
                .stream()
                .toList();
        
        final SlotStackList list = new SlotStackList(toolbelt.size());
        for (int slot = 0; slot < toolbelt.size(); slot++) {
            list.add(slot, toolbelt.get(slot).copyAndClear());
        }
        return new ToolBeltContainer(list);
    }
    
    @Override
    public void write(ValueInput input, ValueOutput output) {
        ValueOutput out = output.child("neoforge:attachments")
                .child("toolbelt:belt")
                .childrenList("stacks")
                .addChild();
        final ItemStack stack = this.stack.getStack(0);
        if (stack.isEmpty()) return;
        
        out.putInt("count", stack.getCount());
        out.store("item", ItemStack.CODEC, stack);
        
    }
    
    @Override
    public List<ItemStack> merge(ToolBeltContainer other) {
        if (other.isEmpty()) return List.of();
        final ItemStack stack = other.stack.getStack(0).copyAndClear();
        if (this.stack.isEmpty()) {
            this.stack.add(0, stack);
            return List.of();
        }
        return List.of(stack);
    }
    
    @Override
    public List<ItemStack> gather() {
        final ItemStack stack = this.stack.getStack(0);
        return stack.isEmpty() ? List.of() : Collections.singletonList(stack);
    }
    
    @Override
    public boolean isEmpty() {
        return this.stack.isEmpty();
    }
}
