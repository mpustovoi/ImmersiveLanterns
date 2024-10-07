package toni.immersivelanterns;

import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import toni.immersivelanterns.foundation.config.AllConfigs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.wispforest.accessories.api.client.*;

#if AFTER_21_1
import net.minecraft.client.DeltaTracker;
#endif

#if FABRIC
    import net.fabricmc.api.ClientModInitializer;
    import net.fabricmc.api.ModInitializer;
    #if after_21_1
    import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
    import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
    import net.neoforged.neoforge.client.gui.ConfigurationScreen;
    #endif

    #if current_20_1
    import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
    #endif
#endif

#if FORGE
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
#endif


#if NEO
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
#endif


#if FORGELIKE
@Mod("immersivelanterns")
#endif
public class ImmersiveLanterns #if FABRIC implements ModInitializer, ClientModInitializer #endif
{
    public static final String MODNAME = "Immersive Lanterns";
    public static final String ID = "immersivelanterns";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);
    public static String debugString = "";

    public ImmersiveLanterns(#if NEO IEventBus modEventBus, ModContainer modContainer #endif) {
        #if FORGE
        var context = FMLJavaModLoadingContext.get();
        var modEventBus = context.getModEventBus();
        #endif

        #if FORGELIKE
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        AllConfigs.register((type, spec) -> {
            #if FORGE
            ModLoadingContext.get().registerConfig(type, spec);
            #elif NEO
            modContainer.registerConfig(type, spec);
            #endif
        });
        #endif
    }

    public static boolean isEquipped(Player player) {
        var accessories = (AccessoriesAPIAccess) player;
        return accessories.accessoriesCapability().isEquipped(stack -> stack.getItem() == Items.LANTERN || stack.getItem() == Items.SOUL_LANTERN);
    }

    public static SlotReference getEquipped(Player player) {
        if (!isEquipped(player))
            return null;

        var accessories = (AccessoriesAPIAccess) player;
        var equipped = accessories.accessoriesCapability().getEquipped(stack -> stack.getItem() == Items.LANTERN || stack.getItem() == Items.SOUL_LANTERN);

        return equipped.get(0).reference();
    }

    #if FABRIC @Override #endif
    public void onInitialize() {
        #if FABRIC
            AllConfigs.register((type, spec) -> {
                #if AFTER_21_1
                NeoForgeConfigRegistry.INSTANCE.register(ImmersiveLanterns.ID, type, spec);
                #else
                ForgeConfigRegistry.INSTANCE.register(ImmersiveLanterns.ID, type, spec);
                #endif
            });
        #endif
    }

    #if FABRIC @Override #endif
    public void onInitializeClient() {
        AccessoriesRendererRegistry.registerRenderer(Items.LANTERN, LanternRenderer::new);
        AccessoriesRendererRegistry.registerRenderer(Items.SOUL_LANTERN, LanternRenderer::new);

        #if AFTER_21_1
            #if FABRIC
            ConfigScreenFactoryRegistry.INSTANCE.register(ImmersiveLanterns.ID, ConfigurationScreen::new);
            #endif
        #endif
    }


    // Forg event stubs to call the Fabric initialize methods, and set up cloth config screen
    #if FORGELIKE
    public void commonSetup(FMLCommonSetupEvent event) { onInitialize(); }
    public void clientSetup(FMLClientSetupEvent event) { onInitializeClient(); }
    #endif
}
