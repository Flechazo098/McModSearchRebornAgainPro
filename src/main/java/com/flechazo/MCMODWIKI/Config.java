package com.flechazo.MCMODWIKI;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * MCMODWIKI的配置类
 * @author Flechazo
 */
@EventBusSubscriber(modid = Mcmoditemsearchrebornagainpro.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    /* 最大交互距离 */
    public static final ModConfigSpec.DoubleValue MAX_DISTANCE = BUILDER
            .comment("最大交互距离(格)")
            .defineInRange("maxDistance", 5.2D, 1.0D, 20.0D);

    /* 是否允许液体交互 */
    public static final ModConfigSpec.BooleanValue ALLOW_FLUID = BUILDER
            .comment("是否允许与液体交互")
            .define("allowFluid", false);

    public static final ModConfigSpec.IntValue CLICK_COUNT = BUILDER
            .comment("触发查询所需的按键次数(1-2)")
            .defineInRange("clickCount", 2, 1, 2);

    static {
        BUILDER.comment("MCMODWIKI配置").push("general");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /* 配置值的公共访问接口 */
    public static double maxDistance;
    public static boolean allowFluid;
    public static int clickCount;

    /**
     * 配置加载事件处理
     * @param event 配置加载事件
     */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        maxDistance = MAX_DISTANCE.get();
        allowFluid = ALLOW_FLUID.get();
        clickCount = CLICK_COUNT.get();
    }
}
