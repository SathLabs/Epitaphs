package dev.satherov.epitaphs.common.menu;

import lombok.Getter;
import lombok.experimental.Accessors;

import dev.satherov.epitaphs.client.lang.EPMessageLang;
import dev.satherov.epitaphs.common.command.EPCommands;
import dev.satherov.epitaphs.common.container.InventoryContainer;
import dev.satherov.epitaphs.common.container.PlayerContainer;
import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public class PreviewMenu extends AbstractContainerMenu {
    
    private static final int ARMOR_SIZE = 4;
    private static final int OFFHAND_SIZE = 1;
    
    
    public static final int ARMOR_SLOT = Inventory.INVENTORY_SIZE;
    public static final int OFFHAND_SLOT = PreviewMenu.ARMOR_SLOT + PreviewMenu.ARMOR_SIZE;
    public static final int INVENTORY_SIZE = PreviewMenu.OFFHAND_SLOT + PreviewMenu.OFFHAND_SIZE;
    public static final int SLOT_SIZE = 18;
    
    
    private static final int MAX_ROWS = 8;
    private static final int MAX_COLUMNS = 8;
    private static final int MAX_SLOTS_PER_PAGE = 48;
    private static final int PANEL_PADDING = 14;
    private static final int PANEL_TOP = 8;
    private static final int PAGE_OFFSET = 8;
    
    private final PreviewData data;
    private final int panelWidth;
    private final int pages;
    private final boolean hasCosmetics;
    private final PreviewMenu.CurioView items;
    private final PreviewMenu.CurioView cosmetics;
    
    private boolean showCurios;
    private boolean showCosmetics;
    private int page;
    
    
    public PreviewMenu(int containerId, PreviewData data, PlayerContainer container) {
        this(containerId, data, PreviewMenu.inventoryOf(container), PreviewMenu.curiosOf(data, container));
    }
    
    
    public PreviewMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, PreviewData.STREAM_CODEC.decode(buffer));
    }
    
    private PreviewMenu(int containerId, PreviewData data) {
        this(containerId, data, new SimpleContainer(PreviewMenu.INVENTORY_SIZE), new SimpleContainer(PreviewMenu.curioSize(data)));
    }
    
    private PreviewMenu(int containerId, PreviewData data, Container inventory, Container curios) {
        super(EPRegistry.PREVIEW_MENU.get(), containerId);
        this.data = data;
        
        final List<PreviewMenu.CurioRange> itemRanges = PreviewMenu.ranges(data, false);
        final List<PreviewMenu.CurioRange> cosmeticRanges = PreviewMenu.ranges(data, true);
        this.panelWidth = PreviewMenu.panelWidth(Math.max(PreviewMenu.total(itemRanges), PreviewMenu.total(cosmeticRanges)));
        this.items = PreviewMenu.layout(itemRanges, this.panelWidth);
        this.cosmetics = PreviewMenu.layout(cosmeticRanges, this.panelWidth);
        this.pages = Math.max(this.items.pages(), this.cosmetics.pages());
        this.hasCosmetics = data.curios().stream().anyMatch(section -> section.cosmetics() > 0);
        
        this.addInventorySlots(inventory);
        this.addCurioSlots(curios, this.items, false);
        this.addCurioSlots(curios, this.cosmetics, true);
    }
    
    // ==================== SLOTS ====================
    
    private void addInventorySlots(Container inventory) {
        for (int column = 0; column < 9; column++) {
            this.addSlot(new PreviewSlot(inventory, column, 8 + column * PreviewMenu.SLOT_SIZE, 142, () -> true));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                final int slot = 9 + row * 9 + column;
                this.addSlot(new PreviewSlot(inventory, slot, 8 + column * PreviewMenu.SLOT_SIZE, 84 + row * PreviewMenu.SLOT_SIZE, () -> true));
            }
        }
        // The armor container is ordered feet first, the screen renders it head first.
        for (int slot = 0; slot < PreviewMenu.ARMOR_SIZE; slot++) {
            final int y = 8 + (PreviewMenu.ARMOR_SIZE - 1 - slot) * PreviewMenu.SLOT_SIZE;
            this.addSlot(new PreviewSlot(inventory, PreviewMenu.ARMOR_SLOT + slot, 8, y, () -> true));
        }
        this.addSlot(new PreviewSlot(inventory, PreviewMenu.OFFHAND_SLOT, 77, 62, () -> true));
    }
    
    private void addCurioSlots(Container curios, PreviewMenu.CurioView view, boolean cosmetic) {
        for (PreviewMenu.CurioEntry entry : view.entries()) {
            this.addSlot(new PreviewSlot(
                    curios,
                    entry.slot(),
                    entry.x(),
                    entry.y(),
                    () -> this.showCurios && this.showCosmetics == cosmetic && this.page == entry.page(),
                    entry.identifier(),
                    cosmetic
            ));
        }
    }
    
    private static Container inventoryOf(PlayerContainer container) {
        final InventoryContainer inventory = container.inventory();
        final SimpleContainer result = new SimpleContainer(PreviewMenu.INVENTORY_SIZE);
        
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) result.setItem(slot, inventory.items().getStack(slot).copy());
        for (int slot = 0; slot < PreviewMenu.ARMOR_SIZE; slot++) result.setItem(PreviewMenu.ARMOR_SLOT + slot, inventory.armor().getStack(slot).copy());
        result.setItem(PreviewMenu.OFFHAND_SLOT, inventory.offhand().getStack(0).copy());
        
        return result;
    }
    
    private static Container curiosOf(PreviewData data, PlayerContainer container) {
        final SimpleContainer result = new SimpleContainer(PreviewMenu.curioSize(data));
        
        int slot = 0;
        for (PreviewData.CurioSection section : data.curios()) {
            for (boolean cosmetic : new boolean[]{ false, true }) {
                final List<ItemStack> stacks = container.curios().stacks(section.identifier(), cosmetic);
                final int size = cosmetic ? section.cosmetics() : section.items();
                for (int index = 0; index < size; index++) {
                    if (index < stacks.size()) result.setItem(slot, stacks.get(index).copy());
                    slot++;
                }
            }
        }
        
        return result;
    }
    
    private static int curioSize(PreviewData data) {
        return data.curios().stream().mapToInt(PreviewData.CurioSection::size).sum();
    }
    
    // ==================== LAYOUT ====================
    
    private record CurioRange(String identifier, int start, int size) { }
    
    public record CurioEntry(int slot, String identifier, int page, int x, int y) { }
    
    public record CurioView(List<PreviewMenu.CurioEntry> entries, List<List<Integer>> grids, int pages) {
        
        public List<Integer> grid(int page) {
            return page >= 0 && page < this.grids.size() ? this.grids.get(page) : List.of();
        }
    }
    
    
    private static List<PreviewMenu.CurioRange> ranges(PreviewData data, boolean cosmetic) {
        final List<PreviewMenu.CurioRange> ranges = new ArrayList<>();
        
        int slot = 0;
        for (PreviewData.CurioSection section : data.curios()) {
            final int items = section.items();
            final int cosmetics = section.cosmetics();
            if (!cosmetic || cosmetics <= 0) {
                if (items > 0) ranges.add(new PreviewMenu.CurioRange(section.identifier(), slot, items));
            } else {
                ranges.add(new PreviewMenu.CurioRange(section.identifier(), slot + items, cosmetics));
            }
            slot += items + cosmetics;
        }
        
        return ranges;
    }
    
    private static int total(List<PreviewMenu.CurioRange> ranges) {
        return ranges.stream().mapToInt(PreviewMenu.CurioRange::size).sum();
    }
    
    private static int panelWidth(int total) {
        if (total <= 0) return 0;
        final int onPage = Math.min(total, PreviewMenu.MAX_SLOTS_PER_PAGE);
        final int columns = Mth.clamp(Mth.positiveCeilDiv(onPage, PreviewMenu.MAX_ROWS), 1, PreviewMenu.MAX_COLUMNS);
        return PreviewMenu.PANEL_PADDING + PreviewMenu.SLOT_SIZE * columns;
    }
    
    private static PreviewMenu.CurioView layout(List<PreviewMenu.CurioRange> ranges, int panelWidth) {
        final int total = PreviewMenu.total(ranges);
        if (total <= 0 || panelWidth <= 0) return new PreviewMenu.CurioView(List.of(), List.of(), 1);
        
        final int columns = (panelWidth - PreviewMenu.PANEL_PADDING) / PreviewMenu.SLOT_SIZE;
        final int pages = Math.max(1, Mth.positiveCeilDiv(total, PreviewMenu.MAX_SLOTS_PER_PAGE));
        final int top = PreviewMenu.PANEL_TOP + (pages > 1 ? PreviewMenu.PAGE_OFFSET : 0);
        
        final List<PreviewMenu.CurioEntry> entries = new ArrayList<>();
        final List<List<Integer>> grids = new ArrayList<>();
        for (int page = 0; page < pages; page++) grids.add(new ArrayList<>());
        
        int idx = 0;
        for (PreviewMenu.CurioRange range : ranges) {
            for (int offset = 0; offset < range.size(); offset++) {
                final int page = idx / PreviewMenu.MAX_SLOTS_PER_PAGE;
                final int onPage = idx % PreviewMenu.MAX_SLOTS_PER_PAGE;
                final int column = onPage % columns;
                final int row = onPage / columns;
                
                final List<Integer> grid = grids.get(page);
                while (grid.size() <= column) grid.add(0);
                grid.set(column, grid.get(column) + 1);
                
                entries.add(new PreviewMenu.CurioEntry(
                        range.start() + offset,
                        range.identifier(),
                        page,
                        column * PreviewMenu.SLOT_SIZE + 7 - panelWidth,
                        top + row * PreviewMenu.SLOT_SIZE
                ));
                idx++;
            }
        }
        
        return new PreviewMenu.CurioView(entries, grids, pages);
    }
    
    // ==================== VIEW ====================
    
    public PreviewMenu.CurioView view() {
        return this.showCosmetics ? this.cosmetics : this.items;
    }
    
    public int pageCount() {
        return this.view().pages();
    }
    
    public boolean hasCurios() {
        return this.panelWidth > 0;
    }
    
    public void toggleCurios() {
        this.showCurios = !this.showCurios;
    }
    
    public void toggleCosmetics() {
        this.showCosmetics = !this.showCosmetics;
        this.page = Mth.clamp(this.page, 0, this.pageCount() - 1);
    }
    
    public void flip(int direction) {
        this.page = Mth.clamp(this.page + direction, 0, this.pageCount() - 1);
    }
    
    // ==================== MENU ====================
    
    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        // nothing to do
    }
    
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer viewer)) return false;
        
        if (!EPCommands.PERMISSION_CHECK.check(viewer.permissions())) {
            viewer.sendOverlayMessage(EPMessageLang.MESSAGE_PREVIEW_DENIED.translate().style(ChatFormatting.RED));
            return false;
        }
        
        if (buttonId < 0 || buttonId >= this.slots.size()) return false;
        
        final ItemStack stack = this.getSlot(buttonId).getItem();
        if (stack.isEmpty()) return false;
        
        PreviewMenu.give(viewer, stack.copy());
        return true;
    }
    
    
    private static void give(ServerPlayer player, ItemStack stack) {
        if (player.getInventory().add(stack) && stack.isEmpty()) {
            player.inventoryMenu.broadcastChanges();
            return;
        }
        
        final ItemEntity drop = player.drop(stack, false);
        if (drop == null) return;
        drop.setNoPickUpDelay();
        drop.setTarget(player.getUUID());
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return false;
    }
    
    @Override
    public boolean canDragTo(Slot slot) {
        return false;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
