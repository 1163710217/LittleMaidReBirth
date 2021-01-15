package com.sistr.littlemaidrebirth.setup;

import com.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import com.sistr.littlemaidrebirth.client.*;
import com.sistr.littlemaidrebirth.client.LittleMaidScreen;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = LittleMaidReBirthMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static void init(final FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
        ScreenManager.registerFactory(Registration.LITTLE_MAID_CONTAINER.get(), LittleMaidScreen::new);
    }

}
