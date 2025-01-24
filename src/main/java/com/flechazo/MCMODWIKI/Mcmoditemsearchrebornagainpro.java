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

/**
 * MCMODWIKI主类
 * @author Flechazo
 */
@Mod(Mcmoditemsearchrebornagainpro.MODID)
public class Mcmoditemsearchrebornagainpro {
    public static final String MODID = "mcmoditemsearchrebornagainpro";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Mcmoditemsearchrebornagainpro INSTANCE;

    /* 按键映射 */
    public static KeyMapping WIKI_KEY;
    public static KeyMapping WIKI_HAND_KEY;

    /**
     * 构造函数,注册事件和配置
     */
    public Mcmoditemsearchrebornagainpro(IEventBus modEventBus, ModContainer modContainer) {
        INSTANCE = this;
        modEventBus.addListener(this::registerKeyBindings);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // 注册按键事件处理器
        KeyHandler.init();
    }

    /**
     * 获取mod实例
     */
    public static Mcmoditemsearchrebornagainpro getInstance() {
        return INSTANCE;
    }

    /**
     * 注册按键映射
     */
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

    /**
     * 按键事件处理
     */
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
            
            // 检查是否超时
            if (currentTime - KeyHandler.lastClickTime > KeyHandler.CLICK_TIMEOUT) {
                KeyHandler.clickCount = 0;
            }
            
            KeyHandler.clickCount++;
            KeyHandler.lastClickTime = currentTime;
            
            // 检查是否达到配置的点击次数
            if (KeyHandler.clickCount >= Config.clickCount) {
                KeyHandler.clickCount = 0; // 重置计数
                
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
                    
                    // 史莱姆特判
                    if ("minecraft:slime".equals(entityId)) {
                        openUrl("https://www.mcmod.cn/item/46733.html");
                        return;
                    }
                    if ("minecraft:sunflower".equals (entityId)){
                        openUrl ("https://www.mcmod.cn/item/11126.html");
                        return;
                    }
                    if ("minecraft:short_grass".equals (entityId)){
                        openUrl ("https://www.mcmod.cn/item/10990.html");
                        return;
                    }
                    if ("minecraft:tall_grass".equals (entityId)){
                        openUrl ("https://www.mcmod.cn/item/11128.html");
                        return;
                    }
                    
                    openWikiByRegistryName(entityId, entity.getName().getString());
                }
            }
        }
    }

    /**
     * 打开物品wiki界面
     */
    public void openWiki(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        String regName = Objects.requireNonNull(
                BuiltInRegistries.ITEM.getKey(stack.getItem())).toString();
        String displayName = stack.getDisplayName().getString();

        if ("minecraft:sunflower".equals (regName)){
            openUrl ("https://www.mcmod.cn/item/11126.html");
            return;
        }
        if ("minecraft:short_grass".equals (regName)){
            openUrl ("https://www.mcmod.cn/item/10990.html");
            return;
        }
        if ("minecraft:tall_grass".equals (regName)){
            openUrl ("https://www.mcmod.cn/item/11128.html");
            return;
        }

        openWikiByRegistryName(regName, displayName);
    }

    /**
     * 根据注册名打开wiki界面
     */
    private void openWikiByRegistryName(String regName, String displayName) {
        try {
            String encodedRegName = URLEncoder.encode(regName, StandardCharsets.UTF_8);
            String encodedDisplayName = URLEncoder.encode(displayName, StandardCharsets.UTF_8);
LOGGER.info ("regname is" + regName);

            // 尝试获取精确的页面ID
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

    /**
     * 从MCMOD API获取ID
     */
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

    /**
     * 打开URL
     */
    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime runtime = Runtime.getRuntime();
                String osName = System.getProperty("os.name").toLowerCase();

                // 根据操作系统准备命令
                String[] cmd;
                if (osName.contains("win")) { // Windows
                    cmd = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
                } else if (osName.contains("mac")) { // macOS
                    cmd = new String[]{"open", url};
                } else { // Linux 或类似 Unix 的系统
                    cmd = new String[]{"xdg-open", url};
                }

                // 执行命令并处理潜在的IOException
                Process process = runtime.exec(cmd);
                process.waitFor(); // 等待进程完成

                // 将除0以外的任何退出值作为失败处理
                if (process.exitValue() != 0) {
                    throw new IOException("Command exited with non-zero status");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to open URL: {}", url, e);
        }
    }
} 