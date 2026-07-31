package dev.satherov.epitaphs.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerSkin;

import com.mojang.authlib.GameProfile;

import java.util.function.Supplier;

public class PreviewAvatar extends RemotePlayer {
    
    private final Supplier<PlayerSkin> skin;
    
    public PreviewAvatar(ClientLevel level, GameProfile profile) {
        super(level, profile);
        this.skin = Minecraft.getInstance().getSkinManager().createLookup(profile, false);
    }
    
    @Override
    public PlayerSkin getSkin() {
        return this.skin.get();
    }
}
