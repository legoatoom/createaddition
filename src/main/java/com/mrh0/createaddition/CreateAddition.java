package com.mrh0.createaddition;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mrh0.createaddition.commands.CCApiCommand;
import com.mrh0.createaddition.config.Config;
import com.mrh0.createaddition.groups.ModGroup;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.index.CAEffects;
import com.mrh0.createaddition.index.CAFluids;
import com.mrh0.createaddition.index.CAItemProperties;
import com.mrh0.createaddition.index.CAItems;
import com.mrh0.createaddition.index.CAPonder;
import com.mrh0.createaddition.index.CAPotatoCannonProjectiles;
import com.mrh0.createaddition.index.CARecipes;
import com.mrh0.createaddition.index.CATileEntities;
import com.mrh0.createaddition.network.EnergyNetworkPacket;
import com.mrh0.createaddition.network.ObservePacket;
import com.mrh0.createaddition.network.RemoveConnectorPacket;
import com.simibubi.create.foundation.block.BlockStressValues;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.repack.registrate.util.nullness.NonNullSupplier;

@Mod(CreateAddition.MODID)
public class CreateAddition {
    public static final Logger LOGGER = LogManager.getLogger();
    
    public static final String MODID = "createaddition";
    
    public static boolean IE_ACTIVE = false;
    public static boolean CC_ACTIVE = false;
    public static boolean AE2_ACTIVE = false;
    
    private static final NonNullSupplier<CreateRegistrate> registrate = CreateRegistrate.lazy(CreateAddition.MODID);
    
    private static final String PROTOCOL = "1";
	public static final SimpleChannel Network = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(MODID, "main"))
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .networkProtocolVersion(() -> PROTOCOL)
            .simpleChannel();

    public CreateAddition() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::postInit);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(MobEffect.class, CreateAddition::onRegisterEffectEvent);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(RecipeSerializer.class, CARecipes::register);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        Config.loadConfig(Config.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("createaddition-common.toml"));
        
        IE_ACTIVE = ModList.get().isLoaded("immersiveengineering");
        CC_ACTIVE = ModList.get().isLoaded("computercraft");
        AE2_ACTIVE = ModList.get().isLoaded("ae2");
        
        new ModGroup("main");
        
        CABlocks.register();
        CATileEntities.register();
        CAItems.register();
        CAFluids.register();
    }

    private void setup(final FMLCommonSetupEvent event) {
    	CAPotatoCannonProjectiles.register();
    	BlockStressValues.registerProvider(MODID, AllConfigs.SERVER.kinetics.stressValues);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
    	event.enqueueWork(CAPonder::register);
        event.enqueueWork(CAItemProperties::register);
        
        RenderType cutout = RenderType.cutoutMipped();       
		
        ItemBlockRenderTypes.setRenderLayer(CABlocks.TESLA_COIL.get(), cutout);
    }
    
    public void postInit(FMLLoadCompleteEvent evt) {
    	int i = 0;
        Network.registerMessage(i++, ObservePacket.class, ObservePacket::encode, ObservePacket::decode, ObservePacket::handle);
        Network.registerMessage(i++, EnergyNetworkPacket.class, EnergyNetworkPacket::encode, EnergyNetworkPacket::decode, EnergyNetworkPacket::handle);
        Network.registerMessage(i++, RemoveConnectorPacket.class, RemoveConnectorPacket::encode, RemoveConnectorPacket::decode, RemoveConnectorPacket::handle);
        
    	System.out.println("Create Crafts & Addition Initialized!");
    }
    
    @SubscribeEvent
    public void onRegisterCommandEvent(RegisterCommandsEvent event) {
    	CommandDispatcher<CommandSourceStack> dispather = event.getDispatcher();
    	CCApiCommand.register(dispather);
    }
    
    public static void onRegisterEffectEvent(Register<MobEffect> event) {
    	CAEffects.register(event.getRegistry());
    }
    
    public static CreateRegistrate registrate() {
		return registrate.get();
	}
}
