package com.flechazo.MCMODWIKI;

import com.mojang.logging.LogUtils;
import com.flechazo.MCMODWIKI.event.KeyHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
@Mod( MCMODItemSearchRebornAgainPro.MODID)
public class MCMODItemSearchRebornAgainPro {
    public static final String MODID = "mcmoditemsearchrebornagainpro";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MCMODItemSearchRebornAgainPro INSTANCE;

    public static KeyMapping WIKI_KEY;
    public static KeyMapping WIKI_HAND_KEY;

    public MCMODItemSearchRebornAgainPro (IEventBus modEventBus, ModContainer modContainer) {
        INSTANCE = this;
        modEventBus.addListener(this::registerKeyBindings);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        KeyHandler.init();
    }
    public static MCMODItemSearchRebornAgainPro getInstance() {
        return INSTANCE;
    }
    private void registerKeyBindings(RegisterKeyMappingsEvent event) {
        WIKI_KEY = new KeyMapping(
                "key.mcmodwiki.wiki",
                GLFW.GLFW_KEY_G,
                "key.categories.mcmodwiki"
        );
        WIKI_HAND_KEY = new KeyMapping(
                "key.mcmodwiki.wiki_hand",
                GLFW.GLFW_KEY_G,
                "key.categories.mcmodwiki"
        );
        WIKI_HAND_KEY.setKeyModifierAndCode(KeyModifier.CONTROL, WIKI_HAND_KEY.getKey());
        event.register(WIKI_KEY);
        event.register(WIKI_HAND_KEY);
    }
    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;

        if (WIKI_HAND_KEY.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();
            if (!stack.isEmpty()) {
                openWiki(stack);
            }
        } else if (WIKI_KEY.consumeClick()) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - KeyHandler.lastClickTime > KeyHandler.CLICK_TIMEOUT) {
                KeyHandler.clickCount = 0;
            }
            
            KeyHandler.clickCount++;
            KeyHandler.lastClickTime = currentTime;

            if (KeyHandler.clickCount >= Config.clickCount) {
                KeyHandler.clickCount = 0;
                
                HitResult hit = mc.hitResult;
                if (hit == null || hit.getType() == HitResult.Type.MISS) return;

                double distance = hit.getLocation().distanceTo(mc.player.position());
                if (distance > Config.maxDistance) return;

                if (hit instanceof BlockHitResult blockHit && mc.level != null) {
                    BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
                    if (!Config.allowFluid && !state.getFluidState().isEmpty()) return;
                    ItemStack stack = state.getBlock().asItem().getDefaultInstance();
                    openWiki(stack);
                } else if (hit instanceof EntityHitResult entityHit) {
                    Entity entity = entityHit.getEntity();
                    String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

                    switch (entityId) {
                        case "minecraft:slime" -> {
                            openUrl ("https://www.mcmod.cn/item/46733.html");
                            return;
                        }
                        case "minecraft:sunflower" -> {
                            openUrl ("https://www.mcmod.cn/item/11126.html");
                            return;
                        }
                        case "minecraft:short_grass" -> {
                            openUrl ("https://www.mcmod.cn/item/10990.html");
                            return;
                        }
                        case "minecraft:tall_grass" -> {
                            openUrl ("https://www.mcmod.cn/item/11128.html");
                            return;
                        }
                    }

                    openWikiByRegistryName(entityId, entity.getName().getString());
                }
            }
        }
    }
    public void openWiki(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        String regName = Objects.requireNonNull(
                BuiltInRegistries.ITEM.getKey(stack.getItem())).toString();
        String displayName = stack.getDisplayName().getString();

        switch (regName) {
            case "minecraft:sunflower" -> {
                openUrl ("https://www.mcmod.cn/item/11126.html");
                return;
            }
            case "minecraft:short_grass" -> {
                openUrl ("https://www.mcmod.cn/item/10990.html");
                return;
            }
            case "minecraft:tall_grass" -> {
                openUrl ("https://www.mcmod.cn/item/11128.html");
                return;
            }
        }

        openWikiByRegistryName(regName, displayName);
    }
    private void openWikiByRegistryName(String regName, String displayName) {
        try {
            String encodedRegName = URLEncoder.encode(regName, StandardCharsets.UTF_8);
            String encodedDisplayName = URLEncoder.encode(displayName, StandardCharsets.UTF_8);
            String apiUrl = String.format("https://api.mcmod.cn/getItem/?regname=%s", encodedRegName);
            Optional<Integer> mcModApiNum = getMcmodId(apiUrl);
            
            String url = mcModApiNum.map(id -> String.format("https://www.mcmod.cn/item/%d.html", id))
                    .orElseGet(() -> String.format("https://search.mcmod.cn/s?key=%s", encodedDisplayName));
            LOGGER.info ("打开的网址是：" + url);
            openUrl(url);
        } catch (Exception e) {
            LOGGER.error("Failed to open wiki for {}", regName, e);
        }
    }
    private Optional<Integer> getMcmodId(String apiUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new URI(apiUrl).toURL().openStream(), StandardCharsets.UTF_8))) {
            String response = reader.readLine();
            if (response != null && !response.trim().isEmpty()) {
                int id = Integer.parseInt(response.trim());
                return id > 0 ? Optional.of(id) : Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get mcmod id", e);
        }
        return Optional.empty();
    }
    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime runtime = Runtime.getRuntime();
                String osName = System.getProperty("os.name").toLowerCase();
                String[] cmd;
                if (osName.contains("win")) {
                    cmd = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
                } else if (osName.contains("mac")) {
                    cmd = new String[]{"open", url};
                } else {
                    cmd = new String[]{"xdg-open", url};
                }

                Process process = runtime.exec(cmd);
                process.waitFor();

                if (process.exitValue() != 0) {
                    throw new IOException("Command exited with non-zero status");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to open URL: {}", url, e);
        }
    }
} 