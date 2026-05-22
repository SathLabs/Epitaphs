package dev.satherov.epitaphs.compat;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.data.pack.EPEnchantments;
import dev.satherov.sathlib.core.annotations.NothingNull;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JeiPlugin
@NothingNull
public class EpitaphsJeiPlugin implements IModPlugin {
    
    private static IJeiAnvilRecipe createSoulbound(IVanillaRecipeFactory factory, final HolderGetter<Item> items, final HolderGetter<Enchantment> enchantments) {
        final Holder<Enchantment> enchant = enchantments.getOrThrow(EPEnchantments.SOULBOUND);
        final HolderSet.Named<Item> tag = items.getOrThrow(EPRegistry.SOULBOUND_ENCHANTABLE);
        
        final List<ItemStack> inputs = new ArrayList<>();
        final List<ItemStack> outputs = new ArrayList<>();
        for (Holder<Item> holder : tag) {
            inputs.add(new ItemStack(holder.value()));
            ItemStack stack = new ItemStack(holder.value());
            final ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            mutable.set(enchant, 1);
            stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
            outputs.add(stack);
        }
        
        return factory.createAnvilRecipe(inputs, List.of(new ItemStack(EPRegistry.SOUL_BOTTLE.get())), outputs, Epitaphs.id("anvil/soulbound"));
    }
    
    private static IJeiAnvilRecipe createExperienceSoulbound(IVanillaRecipeFactory factory, final HolderGetter<Item> items, final HolderGetter<Enchantment> enchantments) {
        final Holder<Enchantment> enchant = enchantments.getOrThrow(EPEnchantments.SOULBOUND);
        final Holder<Enchantment> upgrade = enchantments.getOrThrow(EPEnchantments.EXPERIENCE_SOULBOUND);
        final HolderSet.Named<Item> tag = items.getOrThrow(EPRegistry.EXPERIENCE_SOULBOUND_ENCHANTABLE);
        
        final List<ItemStack> inputs = new ArrayList<>();
        final List<ItemStack> outputs = new ArrayList<>();
        for (Holder<Item> holder : tag) {
            ItemStack input = new ItemStack(holder.value());
            final ItemEnchantments.Mutable inputEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            inputEnchantments.set(enchant, 1);
            input.set(DataComponents.ENCHANTMENTS, inputEnchantments.toImmutable());
            inputs.add(input);
            
            ItemStack output = new ItemStack(holder.value());
            final ItemEnchantments.Mutable outputEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            outputEnchantments.set(upgrade, 1);
            output.set(DataComponents.ENCHANTMENTS, outputEnchantments.toImmutable());
            outputs.add(output);
        }
        
        return factory.createAnvilRecipe(inputs, List.of(new ItemStack(Items.EXPERIENCE_BOTTLE)), outputs, Epitaphs.id("anvil/experience_soulbound"));
    }
    
    @Override
    public Identifier getPluginUid() {
        return Epitaphs.id("jei");
    }
    
    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        final RegistryAccess access = Objects.requireNonNull(Minecraft.getInstance().level).registryAccess();
        final HolderGetter<Item> items = access.lookupOrThrow(Registries.ITEM);
        final HolderGetter<Enchantment> enchantments = access.lookupOrThrow(Registries.ENCHANTMENT);
        final IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
        final List<IJeiAnvilRecipe> recipes = new ArrayList<>(2);
        
        recipes.add(EpitaphsJeiPlugin.createSoulbound(factory, items, enchantments));
        recipes.add(EpitaphsJeiPlugin.createExperienceSoulbound(factory, items, enchantments));
        registration.addRecipes(RecipeTypes.ANVIL, recipes);
    }
}
