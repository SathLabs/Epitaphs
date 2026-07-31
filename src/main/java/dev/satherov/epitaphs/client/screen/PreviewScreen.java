package dev.satherov.epitaphs.client.screen;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPScreenLang;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.common.menu.PreviewData;
import dev.satherov.epitaphs.common.menu.PreviewMenu;
import dev.satherov.epitaphs.common.menu.PreviewSlot;
import dev.satherov.epitaphs.compat.CuriosHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.mojang.authlib.GameProfile;

import org.jspecify.annotations.Nullable;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PreviewScreen extends AbstractContainerScreen<PreviewMenu> {
    
    private static final Identifier BACKGROUND = Epitaphs.id("textures/gui/container/preview.png");
    private static final Identifier PANEL_SPRITE = Epitaphs.id("preview/panel");
    
    
    private static final Identifier CURIOS_INVENTORY = PreviewScreen.curios("inventory");
    private static final Identifier CURIOS_BUTTON = PreviewScreen.curios("button");
    private static final Identifier CURIOS_BUTTON_HOVERED = PreviewScreen.curios("button_highlighted");
    private static final Identifier COSMETICS_OFF = PreviewScreen.curios("cosmetic_off");
    private static final Identifier COSMETICS_OFF_HOVERED = PreviewScreen.curios("cosmetic_off_highlighted");
    private static final Identifier COSMETICS_ON = PreviewScreen.curios("cosmetic_on");
    private static final Identifier COSMETICS_ON_HOVERED = PreviewScreen.curios("cosmetic_on_highlighted");
    
    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier HEART_ABSORB_FULL = Identifier.withDefaultNamespace("hud/heart/absorbing_full");
    private static final Identifier HEART_ABSORB_HALF = Identifier.withDefaultNamespace("hud/heart/absorbing_half");
    private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier EXPERIENCE_BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier EXPERIENCE_PROGRESS = Identifier.withDefaultNamespace("hud/experience_bar_progress");
    
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;
    private static final int INFO_WIDTH = 175;
    private static final int GAP = 4;
    private static final int FOOTER_HEIGHT = 52;
    
    private static final int INFO_X = PreviewScreen.PANEL_WIDTH + PreviewScreen.GAP;
    private static final int INFO_LEFT = PreviewScreen.INFO_X + 8;
    private static final int INFO_INNER_WIDTH = PreviewScreen.INFO_WIDTH - 16;
    private static final int FOOTER_TOP = PreviewScreen.PANEL_HEIGHT + PreviewScreen.GAP;
    private static final int HEIGHT = PreviewScreen.FOOTER_TOP + PreviewScreen.FOOTER_HEIGHT;
    
    private static final int ICON_SIZE = 9;
    private static final int ICON_STEP = 8;
    private static final int ICONS_PER_ROW = 10;
    private static final int HEART_ROWS = 2;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 5;
    
    private static final int ACTION_TOP = 142;
    private static final int ACTION_HEIGHT = 16;
    private static final int ACTION_GAP = 4;
    private static final int ACTION_WIDTH = (PreviewScreen.INFO_INNER_WIDTH - PreviewScreen.ACTION_GAP) / 2;
    private static final int BACK_WIDTH = 44;
    
    private static final int LIST_TOP = 22;
    private static final int LIST_ROWS = 11;
    private static final int ROW_HEIGHT = 12;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int ROW_HOVER_COLOR = 0x33000000;
    private static final int ROW_CURRENT_COLOR = 0x55000000;
    private static final int TRACK_COLOR = 0x30000000;
    private static final int THUMB_COLOR = 0xFF6E6E6E;
    
    private static final int LINE_HEIGHT = 10;
    private static final int LABEL_COLOR = 0xFF7A7A7A;
    private static final int VALUE_COLOR = 0xFF404040;
    private static final int HEADER_COLOR = 0xFF404040;
    private static final int SEPARATOR_COLOR = 0xFF8B8B8B;
    private static final float SCALE_PADDING = 0.98F;
    
    private final @Nullable PreviewAvatar avatar;
    private final List<PreviewScreen.Backup> backups;
    
    private @Nullable TextureButton previous;
    private @Nullable TextureButton next;
    private int shownPage = -1;
    private boolean listing;
    private int scroll;
    
    private record Backup(String file, Component taken, Component type) { }
    
    public PreviewScreen(PreviewMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.panelWidth() + PreviewScreen.INFO_X + PreviewScreen.INFO_WIDTH, PreviewScreen.HEIGHT);
        
        final ClientLevel level = Minecraft.getInstance().level;
        final PreviewData.Profile profile = menu.data().profile();
        this.avatar = level == null ? null : new PreviewAvatar(level, new GameProfile(profile.uuid(), profile.name()));
        this.backups = menu.data().backups().stream().map(file -> {
            return new PreviewScreen.Backup(file, Component.literal(PreviewScreen.taken(file)), PreviewScreen.type(file));
        }).toList();
    }
    
    private static String taken(String file) {
        final Matcher matcher = DataHandler.DATE_PATTERN.matcher(file);
        if (!matcher.find()) return file;
        
        try {
            return DataHandler.SYSTEM_FORMATTER.format(Instant.from(DataHandler.FORMATTER.parse(matcher.group())));
        } catch (DateTimeException e) {
            return file;
        }
    }
    
    private static Identifier curios(String name) {
        return Identifier.fromNamespaceAndPath(CuriosHandler.MOD_ID, "textures/gui/curios/" + name + ".png");
    }
    
    // ==================== SETUP ====================
    
    private int visibleWidth() {
        return (this.menu.showCurios() ? this.menu.panelWidth() : 0) + PreviewScreen.INFO_X + PreviewScreen.INFO_WIDTH;
    }
    
    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.visibleWidth()) / 2 + (this.menu.showCurios() ? this.menu.panelWidth() : 0);
        this.previous = null;
        this.next = null;
        this.shownPage = -1;
        
        if (CuriosHandler.isLoaded()) {
            TextureButton curios = this.addRenderableWidget(new TextureButton(
                    this.leftPos + 28,
                    this.topPos + 10,
                    10,
                    10,
                    (_, hovered) -> new TextureButton.Skin(hovered ? PreviewScreen.CURIOS_BUTTON_HOVERED : PreviewScreen.CURIOS_BUTTON, 0, 0, 10, 10),
                    _ -> {
                        this.menu.toggleCurios();
                        this.rebuildWidgets();
                    },
                    EPScreenLang.PREVIEW_CURIOS.translate()
            ));
            curios.active = this.menu.hasCurios();
            curios.setTooltip(Tooltip.create(this.menu.hasCurios()
                    ? EPScreenLang.PREVIEW_CURIOS_TOGGLE.translate()
                    : EPScreenLang.PREVIEW_CURIOS_NONE.translate()));
        }
        
        this.addInfoButtons();
        
        if (!this.menu.showCurios() || !this.menu.hasCurios()) return;
        
        if (this.menu.hasCosmetics()) {
            TextureButton cosmetics = this.addRenderableWidget(new TextureButton(
                    this.leftPos - 27,
                    this.topPos - 18,
                    20,
                    17,
                    (_, hovered) -> new TextureButton.Skin(PreviewScreen.cosmeticsSkin(this.menu.showCosmetics(), hovered), 0, 0, 20, 17),
                    _ -> {
                        this.menu.toggleCosmetics();
                        this.rebuildWidgets();
                    },
                    EPScreenLang.PREVIEW_COSMETICS_TOGGLE.translate()
            ));
            cosmetics.setTooltip(Tooltip.create(EPScreenLang.PREVIEW_COSMETICS_TOGGLE.translate()));
        }
        
        if (this.menu.pageCount() > 1) {
            this.previous = this.addRenderableWidget(this.pageButton(this.leftPos - 28, 32, -1));
            this.next = this.addRenderableWidget(this.pageButton(this.leftPos - 17, 43, 1));
        }
    }
    
    private TextureButton pageButton(int x, int u, int direction) {
        return new TextureButton(
                x,
                this.topPos + 2,
                11,
                12,
                (active, hovered) -> new TextureButton.Skin(
                        PreviewScreen.CURIOS_INVENTORY,
                        u + (active && hovered ? 22 : 0),
                        active ? 25 : 37,
                        256,
                        256
                ),
                _ -> this.menu.flip(direction),
                this.pageLabel()
        );
    }
    
    private void addInfoButtons() {
        final int left = this.leftPos + PreviewScreen.INFO_LEFT;
        
        if (this.listing) {
            final Button back = this.addRenderableWidget(Button.builder(EPScreenLang.PREVIEW_BACKUPS_BACK.translate(), _ -> this.list(false))
                    .bounds(left + PreviewScreen.INFO_INNER_WIDTH - PreviewScreen.BACK_WIDTH, this.topPos + 5, PreviewScreen.BACK_WIDTH, PreviewScreen.ACTION_HEIGHT)
                    .build());
            back.setTooltip(Tooltip.create(EPScreenLang.PREVIEW_BACKUPS_BACK_HINT.translate()));
            return;
        }
        
        final Button restore = this.addRenderableWidget(Button.builder(EPScreenLang.PREVIEW_RESTORE.translate(), _ -> this.restore())
                .bounds(left, this.topPos + PreviewScreen.ACTION_TOP, PreviewScreen.ACTION_WIDTH, PreviewScreen.ACTION_HEIGHT)
                .build());
        restore.setTooltip(Tooltip.create(EPScreenLang.PREVIEW_RESTORE_HINT.translate()));
        
        final Button backups = this.addRenderableWidget(Button.builder(EPScreenLang.PREVIEW_BACKUPS.translate(), _ -> this.list(true))
                .bounds(left + PreviewScreen.ACTION_WIDTH + PreviewScreen.ACTION_GAP, this.topPos + PreviewScreen.ACTION_TOP, PreviewScreen.ACTION_WIDTH, PreviewScreen.ACTION_HEIGHT)
                .build());
        backups.setTooltip(Tooltip.create(EPScreenLang.PREVIEW_BACKUPS_HINT.translate()));
    }
    
    ///
    /// Swaps the info panel between the backup details and the list of every backup, scrolling the
    /// list so the backup that is open right now is in view.
    ///
    private void list(boolean listing) {
        this.listing = listing;
        if (listing) this.scroll = Mth.clamp(this.current() - PreviewScreen.LIST_ROWS / 2, 0, this.maxScroll());
        this.rebuildWidgets();
    }
    
    ///
    /// Closes the preview and drops the command that recovers this backup into the chat, ready to be
    /// looked over before it is sent.
    ///
    private void restore() {
        final PreviewData.Profile profile = this.menu.data().profile();
        this.onClose();
        this.minecraft.setScreen(new ChatScreen("/epitaphs recover uuid " + profile.uuid() + " " + profile.file(), false));
    }
    
    ///
    /// Swaps this preview out for another one of the same player's backups by re-running the command,
    /// which keeps the whole thing behind the same permission check.
    ///
    private void open(PreviewScreen.Backup backup) {
        if (backup.file().equals(this.menu.data().profile().file())) {
            this.list(false);
            return;
        }
        if (this.minecraft.player == null) return;
        this.minecraft.player.connection.sendCommand("epitaphs preview uuid " + this.menu.data().profile().uuid() + " " + backup.file());
    }
    
    private int current() {
        final String file = this.menu.data().profile().file();
        for (int index = 0; index < this.backups.size(); index++) {
            if (this.backups.get(index).file().equals(file)) return index;
        }
        return 0;
    }
    
    private int maxScroll() {
        return Math.max(0, this.backups.size() - PreviewScreen.LIST_ROWS);
    }
    
    private Component pageLabel() {
        return EPScreenLang.PREVIEW_PAGE.translate(String.valueOf(this.menu.page() + 1), String.valueOf(this.menu.pageCount()));
    }
    
    private static Identifier cosmeticsSkin(boolean on, boolean hovered) {
        if (on) return hovered ? PreviewScreen.COSMETICS_ON_HOVERED : PreviewScreen.COSMETICS_ON;
        return hovered ? PreviewScreen.COSMETICS_OFF_HOVERED : PreviewScreen.COSMETICS_OFF;
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.shownPage != this.menu.page()) {
            this.shownPage = this.menu.page();
            final Tooltip label = Tooltip.create(this.pageLabel());
            if (this.previous != null) {
                this.previous.active = this.menu.page() > 0;
                this.previous.setTooltip(label);
            }
            if (this.next != null) {
                this.next.active = this.menu.page() + 1 < this.menu.pageCount();
                this.next.setTooltip(label);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
    
    // ==================== BACKGROUND ====================
    
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        
        final int xo = this.leftPos;
        final int yo = this.topPos;
        
        graphics.blit(RenderPipelines.GUI_TEXTURED, PreviewScreen.BACKGROUND, xo, yo, 0.0F, 0.0F, PreviewScreen.PANEL_WIDTH, PreviewScreen.PANEL_HEIGHT, 256, 256);
        this.extractAvatar(graphics, mouseX, mouseY);
        if (this.menu.showCurios() && this.menu.hasCurios()) this.extractCurioPanel(graphics);
        
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.PANEL_SPRITE, xo + PreviewScreen.INFO_X, yo, PreviewScreen.INFO_WIDTH, PreviewScreen.PANEL_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.PANEL_SPRITE, xo, yo + PreviewScreen.FOOTER_TOP, PreviewScreen.PANEL_WIDTH, PreviewScreen.FOOTER_HEIGHT);
    }
    
    ///
    /// Renders whoever the backup belongs to instead of whoever opened the preview, wearing the gear
    /// the backup was taken with.
    ///
    private void extractAvatar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.avatar == null) return;
        
        this.avatar.setItemSlot(EquipmentSlot.HEAD, this.stack(PreviewMenu.ARMOR_SLOT + 3), true);
        this.avatar.setItemSlot(EquipmentSlot.CHEST, this.stack(PreviewMenu.ARMOR_SLOT + 2), true);
        this.avatar.setItemSlot(EquipmentSlot.LEGS, this.stack(PreviewMenu.ARMOR_SLOT + 1), true);
        this.avatar.setItemSlot(EquipmentSlot.FEET, this.stack(PreviewMenu.ARMOR_SLOT), true);
        this.avatar.setItemSlot(EquipmentSlot.OFFHAND, this.stack(PreviewMenu.OFFHAND_SLOT), true);
        this.avatar.setItemSlot(EquipmentSlot.MAINHAND, this.stack(Mth.clamp(this.menu.data().selectedSlot(), 0, 8)), true);
        
        InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics,
                this.leftPos + 26,
                this.topPos + 8,
                this.leftPos + 75,
                this.topPos + 78,
                30,
                0.0625F,
                mouseX,
                mouseY,
                this.avatar
        );
    }
    
    private ItemStack stack(int slot) {
        return this.menu.getSlot(slot).getItem();
    }
    
    private void extractCurioPanel(GuiGraphicsExtractor graphics) {
        final List<Integer> grid = this.menu.view().grid(this.menu.page());
        if (grid.isEmpty()) return;
        
        final boolean paged = this.menu.pageCount() > 1;
        int x = -33;
        int y = this.topPos;
        
        if (this.menu.hasCosmetics()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, PreviewScreen.CURIOS_INVENTORY, this.leftPos + x + 2, y - 23, 32, 0, 28, 24, 256, 256);
        }
        
        x -= (grid.size() - 1) * PreviewMenu.SLOT_SIZE;
        for (int column = 0; column < grid.size(); column++) {
            final int height = 7 + grid.getFirst() * PreviewMenu.SLOT_SIZE + (paged ? 8 : 0);
            int u = column == 0 ? 91 : 98;
            
            graphics.blit(RenderPipelines.GUI_TEXTURED, PreviewScreen.CURIOS_INVENTORY, this.leftPos + x, y, u, 0, 25, height, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, PreviewScreen.CURIOS_INVENTORY, this.leftPos + x, y + height, u, 159, 25, 7, 256, 256);
            
            if (grid.size() == 1) {
                u += 7;
                graphics.blit(RenderPipelines.GUI_TEXTURED, PreviewScreen.CURIOS_INVENTORY, this.leftPos + x + 7, y, u, 0, 25, height, 256, 256);
                graphics.blit(RenderPipelines.GUI_TEXTURED, PreviewScreen.CURIOS_INVENTORY, this.leftPos + x + 7, y + height, u, 159, 25, 7, 256, 256);
            }
            
            x += column == 0 ? 25 : PreviewMenu.SLOT_SIZE;
        }
        
        x -= grid.size() * PreviewMenu.SLOT_SIZE;
        if (paged) y += 8;
        
        for (int rows : grid) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, PreviewScreen.CURIOS_INVENTORY, this.leftPos + x, y + 7, 7, 7, 18, rows * PreviewMenu.SLOT_SIZE, 256, 256);
            x += PreviewMenu.SLOT_SIZE;
        }
        
        for (Slot slot : this.menu.slots) {
            if (!(slot instanceof PreviewSlot preview) || !preview.cosmetic() || !slot.isActive()) continue;
            graphics.blit(RenderPipelines.GUI_TEXTURED, PreviewScreen.CURIOS_INVENTORY, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 32, 50, 18, 18, 256, 256);
        }
    }
    
    // ==================== FOREGROUND ====================
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        if (this.listing) this.extractBackups(graphics, xm - this.leftPos, ym - this.topPos);
        else this.extractInfo(graphics);
        this.extractFooter(graphics);
    }
    
    private void extractBackups(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(
                this.font,
                EPScreenLang.PREVIEW_BACKUPS_TITLE.translate(String.valueOf(this.backups.size())),
                PreviewScreen.INFO_LEFT,
                9,
                PreviewScreen.HEADER_COLOR,
                false
        );
        
        if (this.backups.isEmpty()) {
            graphics.text(this.font, EPScreenLang.PREVIEW_BACKUPS_EMPTY.translate(), PreviewScreen.INFO_LEFT, PreviewScreen.LIST_TOP + 4, PreviewScreen.LABEL_COLOR, false);
            return;
        }
        
        final boolean scrollable = this.backups.size() > PreviewScreen.LIST_ROWS;
        final int width = PreviewScreen.INFO_INNER_WIDTH - (scrollable ? PreviewScreen.SCROLLBAR_WIDTH + 2 : 0);
        final int hovered = this.rowAt(mouseX, mouseY);
        final int current = this.current();
        
        for (int row = 0; row < PreviewScreen.LIST_ROWS; row++) {
            final int index = this.scroll + row;
            if (index >= this.backups.size()) break;
            
            final PreviewScreen.Backup backup = this.backups.get(index);
            final int y = PreviewScreen.LIST_TOP + row * PreviewScreen.ROW_HEIGHT;
            
            if (index == hovered) graphics.fill(PreviewScreen.INFO_LEFT, y, PreviewScreen.INFO_LEFT + width, y + PreviewScreen.ROW_HEIGHT, PreviewScreen.ROW_HOVER_COLOR);
            else if (index == current) graphics.fill(PreviewScreen.INFO_LEFT, y, PreviewScreen.INFO_LEFT + width, y + PreviewScreen.ROW_HEIGHT, PreviewScreen.ROW_CURRENT_COLOR);
            
            graphics.text(this.font, backup.taken(), PreviewScreen.INFO_LEFT + 3, y + 2, PreviewScreen.VALUE_COLOR, false);
            graphics.text(this.font, backup.type(), PreviewScreen.INFO_LEFT + width - 3 - this.font.width(backup.type()), y + 2, PreviewScreen.LABEL_COLOR, false);
        }
        
        if (scrollable) this.extractScrollbar(graphics);
    }
    
    private void extractScrollbar(GuiGraphicsExtractor graphics) {
        final int x = PreviewScreen.INFO_LEFT + PreviewScreen.INFO_INNER_WIDTH - PreviewScreen.SCROLLBAR_WIDTH;
        final int height = PreviewScreen.LIST_ROWS * PreviewScreen.ROW_HEIGHT;
        graphics.fill(x, PreviewScreen.LIST_TOP, x + PreviewScreen.SCROLLBAR_WIDTH, PreviewScreen.LIST_TOP + height, PreviewScreen.TRACK_COLOR);
        
        final int thumb = Math.max(8, height * PreviewScreen.LIST_ROWS / this.backups.size());
        final int y = PreviewScreen.LIST_TOP + (height - thumb) * this.scroll / this.maxScroll();
        graphics.fill(x, y, x + PreviewScreen.SCROLLBAR_WIDTH, y + thumb, PreviewScreen.THUMB_COLOR);
    }
    
    private int rowAt(int x, int y) {
        if (!this.listing || this.backups.isEmpty()) return -1;
        if (x < PreviewScreen.INFO_LEFT || x >= PreviewScreen.INFO_LEFT + PreviewScreen.INFO_INNER_WIDTH) return -1;
        if (y < PreviewScreen.LIST_TOP || y >= PreviewScreen.LIST_TOP + PreviewScreen.LIST_ROWS * PreviewScreen.ROW_HEIGHT) return -1;
        
        final int index = this.scroll + (y - PreviewScreen.LIST_TOP) / PreviewScreen.ROW_HEIGHT;
        return index < this.backups.size() ? index : -1;
    }
    
    private void extractInfo(GuiGraphicsExtractor graphics) {
        final PreviewData data = this.menu.data();
        final PreviewData.Profile profile = data.profile();
        final PreviewData.Location location = data.location();
        
        int y = 8;
        graphics.text(this.font, Component.literal(profile.name()), PreviewScreen.INFO_LEFT, y, PreviewScreen.HEADER_COLOR, false);
        y += PreviewScreen.LINE_HEIGHT;
        this.small(graphics, PreviewScreen.INFO_LEFT, y, Component.literal(profile.uuid().toString()), PreviewScreen.LABEL_COLOR, 0.65F);
        y += 6;
        
        y = this.section(graphics, y, EPScreenLang.PREVIEW_SECTION_BACKUP);
        y = this.entry(graphics, y, EPScreenLang.PREVIEW_ENTRY_TAKEN, Component.literal(DataHandler.ISO8601_FORMATTER.format(Instant.ofEpochMilli(profile.timestamp()))), false);
        y = this.entry(graphics, y, EPScreenLang.PREVIEW_ENTRY_TYPE, PreviewScreen.type(profile.file()), false);
        
        y = this.section(graphics, y, EPScreenLang.PREVIEW_SECTION_LOCATION);
        y = this.entry(graphics, y, EPScreenLang.PREVIEW_ENTRY_DIMENSION, location.dimension()
                .map(key -> (Component) Component.literal(key.identifier().toString()))
                .orElseGet(EPScreenLang.PREVIEW_VALUE_UNKNOWN::translate), true);
        y = this.entry(graphics, y, EPScreenLang.PREVIEW_ENTRY_POSITION, EPScreenLang.PREVIEW_VALUE_POSITION.translate(
                String.valueOf(Mth.floor(location.position().x())),
                String.valueOf(Mth.floor(location.position().y())),
                String.valueOf(Mth.floor(location.position().z()))
        ), true);
        
        y = this.section(graphics, y, EPScreenLang.PREVIEW_SECTION_STATUS);
        y = this.entry(graphics, y, EPScreenLang.PREVIEW_ENTRY_GAME_MODE, location.gameType().getLongDisplayName(), false);
        this.entry(graphics, y, EPScreenLang.PREVIEW_ENTRY_SCORE, Component.literal(String.valueOf(location.score())), false);
    }
    
    private static Component type(String file) {
        if (file.contains("-death")) return EPScreenLang.PREVIEW_VALUE_DEATH.translate();
        if (file.contains("-save")) return EPScreenLang.PREVIEW_VALUE_SAVE.translate();
        return EPScreenLang.PREVIEW_VALUE_UNKNOWN.translate();
    }
    
    private int section(GuiGraphicsExtractor graphics, int y, EPScreenLang header) {
        y += 4;
        graphics.text(this.font, header.translate(), PreviewScreen.INFO_LEFT, y, PreviewScreen.HEADER_COLOR, false);
        y += PreviewScreen.LINE_HEIGHT - 1;
        graphics.fill(PreviewScreen.INFO_LEFT, y, PreviewScreen.INFO_LEFT + PreviewScreen.INFO_INNER_WIDTH, y + 1, PreviewScreen.SEPARATOR_COLOR);
        return y + 3;
    }
    
    private int entry(GuiGraphicsExtractor graphics, int y, EPScreenLang label, Component value, boolean squish) {
        final Component text = label.translate();
        graphics.text(this.font, text, PreviewScreen.INFO_LEFT, y, PreviewScreen.LABEL_COLOR, false);
        
        final int available = PreviewScreen.INFO_INNER_WIDTH - this.font.width(text) - 4;
        final int width = this.font.width(value);
        
        if ((width <= available) || !squish) {
            graphics.text(this.font, value, PreviewScreen.INFO_LEFT + PreviewScreen.INFO_INNER_WIDTH - width, y, PreviewScreen.VALUE_COLOR, false);
        } else {
            final float scale = (available / (float) width) * PreviewScreen.SCALE_PADDING;
            final int scaledWidth = Mth.ceil(width * scale);
            final int yOffset = Mth.floor(this.font.lineHeight * (1.0F - scale) / 2.0F);
            
            this.small(graphics,
                    PreviewScreen.INFO_LEFT + PreviewScreen.INFO_INNER_WIDTH - scaledWidth,
                    y + yOffset,
                    value,
                    PreviewScreen.VALUE_COLOR,
                    scale
            );
        }
        
        return y + PreviewScreen.LINE_HEIGHT;
    }
    
    private void small(GuiGraphicsExtractor graphics, int x, int y, Component text, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }
    
    // ==================== FOOTER ====================
    
    private void extractFooter(GuiGraphicsExtractor graphics) {
        final PreviewData.Vitals vitals = this.menu.data().vitals();
        
        this.extractHearts(graphics, 8, PreviewScreen.FOOTER_TOP + 8, vitals);
        this.extractFood(graphics, 8, PreviewScreen.FOOTER_TOP + 22, vitals);
        this.extractExperience(graphics, 8, PreviewScreen.FOOTER_TOP + 36, vitals);
    }
    
    private void extractHearts(GuiGraphicsExtractor graphics, int x, int y, PreviewData.Vitals vitals) {
        final int hearts = Math.min(Mth.ceil(vitals.maxHealth() / 2.0F), PreviewScreen.ICONS_PER_ROW * PreviewScreen.HEART_ROWS);
        final int absorption = Mth.ceil(vitals.absorption() / 2.0F);
        
        for (int heart = 0; heart < hearts; heart++) {
            final int xo = x + heart % PreviewScreen.ICONS_PER_ROW * PreviewScreen.ICON_STEP;
            final int yo = y + heart / PreviewScreen.ICONS_PER_ROW * PreviewScreen.LINE_HEIGHT;
            
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.HEART_CONTAINER, xo, yo, PreviewScreen.ICON_SIZE, PreviewScreen.ICON_SIZE);
            
            final float health = vitals.health() - heart * 2.0F;
            if (health >= 2.0F) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.HEART_FULL, xo, yo, PreviewScreen.ICON_SIZE, PreviewScreen.ICON_SIZE);
            else if (health >= 1.0F) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.HEART_HALF, xo, yo, PreviewScreen.ICON_SIZE, PreviewScreen.ICON_SIZE);
        }
        
        for (int heart = 0; heart < absorption && hearts + heart < PreviewScreen.ICONS_PER_ROW * PreviewScreen.HEART_ROWS; heart++) {
            final int index = hearts + heart;
            final int xo = x + index % PreviewScreen.ICONS_PER_ROW * PreviewScreen.ICON_STEP;
            final int yo = y + index / PreviewScreen.ICONS_PER_ROW * PreviewScreen.LINE_HEIGHT;
            
            final float absorbed = vitals.absorption() - heart * 2.0F;
            final Identifier sprite = absorbed >= 2.0F ? PreviewScreen.HEART_ABSORB_FULL : PreviewScreen.HEART_ABSORB_HALF;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, xo, yo, PreviewScreen.ICON_SIZE, PreviewScreen.ICON_SIZE);
        }
    }
    
    private void extractFood(GuiGraphicsExtractor graphics, int x, int y, PreviewData.Vitals vitals) {
        for (int index = 0; index < PreviewScreen.ICONS_PER_ROW; index++) {
            final int xo = x + index * PreviewScreen.ICON_STEP;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.FOOD_EMPTY, xo, y, PreviewScreen.ICON_SIZE, PreviewScreen.ICON_SIZE);
            
            final int food = vitals.food() - index * 2;
            if (food >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.FOOD_FULL, xo, y, PreviewScreen.ICON_SIZE, PreviewScreen.ICON_SIZE);
            else if (food == 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.FOOD_HALF, xo, y, PreviewScreen.ICON_SIZE, PreviewScreen.ICON_SIZE);
        }
    }
    
    private void extractExperience(GuiGraphicsExtractor graphics, int x, int y, PreviewData.Vitals vitals) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PreviewScreen.EXPERIENCE_BACKGROUND, x, y, PreviewScreen.BAR_WIDTH, PreviewScreen.BAR_HEIGHT);
        
        final int progress = Math.round(Mth.clamp(vitals.experienceProgress(), 0.0F, 1.0F) * PreviewScreen.BAR_WIDTH);
        if (progress > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    PreviewScreen.EXPERIENCE_PROGRESS,
                    PreviewScreen.BAR_WIDTH,
                    PreviewScreen.BAR_HEIGHT,
                    0,
                    0,
                    x,
                    y,
                    progress,
                    PreviewScreen.BAR_HEIGHT
            );
        }
        
        final Component level = Component.translatable("gui.experience.level", vitals.experienceLevel());
        graphics.text(this.font, level, PreviewScreen.PANEL_WIDTH - 8 - this.font.width(level), y - 13, PreviewScreen.VALUE_COLOR, false);
    }
    
    // ==================== INTERACTION ====================
    
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot instanceof PreviewSlot slot && !slot.hasItem() && slot.identifier() != null) {
            final Component name = Component.translatable("curios.identifier." + slot.identifier()).withStyle(ChatFormatting.YELLOW);
            final Component display = slot.cosmetic() ? EPScreenLang.PREVIEW_CURIOS_COSMETIC.translate(name) : name;
            final Component curio = EPScreenLang.PREVIEW_CURIOS_SLOT.translate(display);
            graphics.setTooltipForNextFrame(this.font, curio, mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
    
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        final List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(itemStack));
        tooltip.add(Component.empty());
        tooltip.add(EPScreenLang.PREVIEW_GIVE_HINT.translate(ChatFormatting.DARK_GRAY));
        return tooltip;
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        final int row = this.rowAt((int) event.x() - this.leftPos, (int) event.y() - this.topPos);
        if (row >= 0) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.open(this.backups.get(row));
            return true;
        }
        
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            this.give(this.hoveredSlot.index);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    
    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0.0) return super.mouseScrolled(x, y, scrollX, scrollY);
        
        if (this.listing) {
            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, this.maxScroll());
            return true;
        }
        
        if (this.menu.showCurios() && this.menu.pageCount() > 1) {
            this.menu.flip(scrollY > 0.0 ? -1 : 1);
            return true;
        }
        
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
    
    private void give(int slot) {
        if (this.minecraft.gameMode == null) return;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, slot);
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
    }
}
