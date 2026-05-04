package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.Epitaphs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.common.DropRule;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("LoggingSimilarMessage")
public record CuriosContainer(Map<String, StackHandler> entries) implements SaveContainer<CuriosContainer> {
    
    public static final Codec<CuriosContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, StackHandler.CODEC).fieldOf("entries").forGetter(CuriosContainer::entries)
    ).apply(instance, CuriosContainer::new));
    
    public static CuriosContainer empty() {
        return new CuriosContainer(new HashMap<>());
    }
    
    // ==================== ONLINE ====================
    
    public static CuriosContainer create(ServerPlayer player) {
        final Map<String, StackHandler> entries = new HashMap<>();
        CuriosApi.getCuriosInventory(player).ifPresentOrElse(inventory -> {
            inventory.getCurios().forEach((key, value) -> {
                final IDynamicStackHandler itemStacks = value.getStacks();
                final IDynamicStackHandler cosmeticStacks = value.getCosmeticStacks();
                entries.put(key, StackHandler.create(itemStacks.getSlots(), itemStacks::getStackInSlot, cosmeticStacks.getSlots(), cosmeticStacks::getStackInSlot));
            });
        }, () -> Epitaphs.log.warn("No curios capability found on player {} - {}", player.getGameProfile().name(), player.getStringUUID()));
        return new CuriosContainer(entries);
    }
    
    @Override
    public void write(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresentOrElse(inventory -> {
            inventory.getCurios().forEach((key, value) -> {
                final StackHandler handler = this.entries.get(key);
                if (handler == null) return;
                if (handler.isEmpty()) return;
                
                final IDynamicStackHandler itemStacks = value.getStacks();
                final List<ItemStack> items = handler.items();
                for (int slot = 0; slot < items.size(); slot++) {
                    ItemStack stack = items.get(slot).copyAndClear();
                    if (stack.isEmpty()) continue;
                    if (slot < itemStacks.getSlots()) {
                        itemStacks.setStackInSlot(slot, stack);
                        continue;
                    }
                    
                    for (int i = 0; i < itemStacks.getSlots(); i++) {
                        if (itemStacks.getStackInSlot(i).isEmpty()) {
                            itemStacks.setStackInSlot(i, stack);
                            break;
                        }
                    }
                }
                
                final IDynamicStackHandler cosmeticStacks = value.getCosmeticStacks();
                final List<ItemStack> cosmetics = handler.cosmetics();
                for (int slot = 0; slot < cosmetics.size(); slot++) {
                    ItemStack stack = cosmetics.get(slot).copyAndClear();
                    if (stack.isEmpty()) continue;
                    if (slot < cosmeticStacks.getSlots()) {
                        cosmeticStacks.setStackInSlot(slot, stack);
                        continue;
                    }
                    
                    for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                        if (cosmeticStacks.getStackInSlot(i).isEmpty()) {
                            cosmeticStacks.setStackInSlot(i, stack);
                            break;
                        }
                    }
                }
            });
        }, () -> Epitaphs.log.warn("No curios capability found on player {} - {}", player.getGameProfile().name(), player.getStringUUID()));
    }
    
    public static CuriosContainer createSoulbound(ServerPlayer player) {
        final Map<String, StackHandler> entries = new HashMap<>();
        CuriosApi.getCuriosInventory(player).ifPresentOrElse(inventory -> {
            inventory.getCurios().forEach((key, value) -> {
                final IDynamicStackHandler itemStacks = value.getStacks();
                final IDynamicStackHandler cosmeticStacks = value.getCosmeticStacks();
                entries.put(key, StackHandler.createSoulbound(itemStacks.getSlots(), itemStacks::getStackInSlot, cosmeticStacks.getSlots(), cosmeticStacks::getStackInSlot));
            });
        }, () -> Epitaphs.log.warn("No curios capability found on player {} - {}", player.getGameProfile().name(), player.getStringUUID()));
        return new CuriosContainer(entries);
    }
    
    // ==================== OFFLINE ====================
    
    public static CuriosContainer create(ValueInput input) {
        final Map<String, StackHandler> entries = new HashMap<>();
        final ValueInput.ValueInputList curios = input.rawChildOrEmpty("neoforge:attachments")
                .rawChildOrEmpty("curios:inventory")
                .childrenListOrEmpty("Curios");
        
        curios.forEach(entry -> entry.keySet().forEach(identifier -> {
            if (identifier.isEmpty()) return;
            entries.put(identifier, StackHandler.create(entry.childOrEmpty(identifier)));
        }));
        
        return new CuriosContainer(entries);
    }
    
    @Override
    public void write(ValueInput input, ValueOutput output) {
        final ValueInput.ValueInputList curios = input.rawChildOrEmpty("neoforge:attachments")
                .rawChildOrEmpty("curios:inventory")
                .childrenListOrEmpty("Curios");
        if (curios.isEmpty()) return;
        
        final ValueOutput.ValueOutputList curiosOutput = output.child("neoforge:attachments")
                .child("curios:inventory")
                .childrenList("Curios");
        
        curios.forEach(entry -> {
            final ValueOutput entryOutput = curiosOutput.addChild();
            entry.keySet().forEach(identifier -> {
                if (identifier.isEmpty()) return;
                
                final ValueInput handlerInput = entry.childOrEmpty(identifier);
                StackHandler handler = this.entries.get(identifier);
                if (handler == null) {
                    Epitaphs.log.debug("Handler for type '{}' does not exist in Curio Container", identifier);
                    handler = StackHandler.create(handlerInput);
                }
                if (handler.isEmpty()) return;
                
                final ValueOutput handlerOutput = entryOutput.child(identifier);
                handlerOutput.putInt("BaseSize", handlerInput.getIntOr("BaseSize", handler.items().size()));
                handlerOutput.putInt("Size", handler.items().size());
                handler.write(handlerOutput);
                handlerOutput.store("Renders", Codec.BOOL.listOf(), handlerInput.read("Renders", Codec.BOOL.listOf()).orElse(List.of()));
                handlerOutput.store("ActiveStates", Codec.BOOL.listOf(), handlerInput.read("ActiveStates", Codec.BOOL.listOf()).orElse(List.of()));
                handlerOutput.putBoolean("Cosmetic", handlerInput.getBooleanOr("Cosmetic", !handler.cosmetics().isEmpty()));
                handlerOutput.putBoolean("Visible", handlerInput.getBooleanOr("Visible", true));
                handlerOutput.putBoolean("ToggleRender", handlerInput.getBooleanOr("ToggleRender", true));
                handlerOutput.store("DropRule", DropRule.CODEC, handlerInput.read("DropRule", DropRule.CODEC).orElse(DropRule.DEFAULT));
                handlerOutput.store(
                        "PermanentModifiers",
                        AttributeModifier.CODEC.listOf(),
                        handlerInput.read("PermanentModifiers", AttributeModifier.CODEC.listOf()).orElse(List.of())
                );
                handlerOutput.store(
                        "Modifiers",
                        AttributeModifier.CODEC.listOf(),
                        handlerInput.read("Modifiers", AttributeModifier.CODEC.listOf()).orElse(List.of())
                );
            });
        });
    }
    
    // ==================== OTHER ====================
    
    @Override
    public List<ItemStack> merge(CuriosContainer other) {
        final List<ItemStack> overflow = new ArrayList<>();
        for (Map.Entry<String, StackHandler> entry : other.entries.entrySet()) {
            final String identifier = entry.getKey();
            final StackHandler handler = entry.getValue();
            final StackHandler existing = this.entries.get(identifier);
            if (existing == null) this.entries.put(identifier, handler);
            else overflow.addAll(existing.merge(handler));
        }
        return overflow;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        this.entries.forEach((_, stackHandler) -> result.addAll(stackHandler.gather()));
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return this.entries.isEmpty() || this.entries.values().stream().allMatch(StackHandler::isEmpty);
    }
}
