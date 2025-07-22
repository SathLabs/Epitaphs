package dev.satherov.epitaphs;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Epitaphs.MOD_ID, dist = Dist.CLIENT)
//@EventBusSubscriber(modid = Epitaphs.MOD_ID, value = Dist.CLIENT)
public class EpitaphsClient {

    public EpitaphsClient(ModContainer container) {

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
