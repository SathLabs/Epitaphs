package dev.satherov.epitaphs;

import dev.satherov.epitaphs.client.screen.PreviewScreen;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Epitaphs.MOD_ID, dist = Dist.CLIENT)
public class EpitaphsClient {
    
    public EpitaphsClient(final IEventBus bus, final FMLModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        bus.addListener(RegisterMenuScreensEvent.class, event -> event.register(EPRegistry.PREVIEW_MENU.get(), PreviewScreen::new));
    }
}
