package cn.mcbowser;

import cn.mcbowser.block.DisplayPanelBlock;
import cn.mcbowser.block.entity.DisplayPanelBlockEntity;
import cn.mcbowser.network.McBowserNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(McBowser.MOD_ID)
public final class McBowser {
    public static final String MOD_ID = "mc_bowser";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredBlock<DisplayPanelBlock> DISPLAY_PANEL = BLOCKS.registerBlock(
            "display_panel",
            DisplayPanelBlock::new,
            properties -> properties.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).lightLevel(state -> 4)
    );
    public static final DeferredItem<BlockItem> DISPLAY_PANEL_ITEM = ITEMS.registerSimpleBlockItem("display_panel", DISPLAY_PANEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisplayPanelBlockEntity>> DISPLAY_PANEL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("display_panel", () -> new BlockEntityType<>(DisplayPanelBlockEntity::new, DISPLAY_PANEL.get()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mc_bowser"))
            .icon(() -> DISPLAY_PANEL_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(DISPLAY_PANEL_ITEM.get()))
            .build());

    public McBowser(IEventBus modBus, ModContainer container) {
        // Rinku starts its downloader later, after the client readiness gate. An offline
        // build can therefore publish the exact embedded JCEF runtime during mod loading.
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            cn.mcbowser.client.OfflineJcefBootstrap.installIfBundled();
        }
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
        modBus.addListener(McBowserNetwork::register);
    }
}
