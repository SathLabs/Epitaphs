package dev.satherov.epitaphs.common.item;

import dev.satherov.epitaphs.client.lang.EPMessageLang;
import dev.satherov.sathlib.common.item.SLItem;
import dev.satherov.sathlib.common.item.SLItemProperties;
import dev.satherov.sathlib.core.annotations.NothingNull;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

@NothingNull
public class SoulBottleItem extends SLItem {
    
    public SoulBottleItem(SLItemProperties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(EPMessageLang.MESSAGE_SOUL_BOTTLE_OBTAIN.translate(ChatFormatting.GRAY));
        builder.accept(EPMessageLang.MESSAGE_SOUL_BOTTLE_USE.translate(ChatFormatting.GRAY));
        builder.accept(EPMessageLang.MESSAGE_SOULBOUND_HINT.translate(ChatFormatting.GOLD));
        builder.accept(EPMessageLang.MESSAGE_EXPERIENCE_SOULBOUND_HINT.translate(ChatFormatting.DARK_GREEN));
    }
}
