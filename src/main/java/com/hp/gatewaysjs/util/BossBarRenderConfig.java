package com.hp.gatewaysjs.util;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

/**
 * Boss条渲染配置，控制自定义Boss条的每个部分的绘制参数。
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li><b>单纹理模式</b>：外表和进度在同一张纹理的不同区域（如原版bars.png）</li>
 *   <li><b>双纹理模式</b>：外表和进度是两张独立的纹理（如灾变模组的base+overlay）</li>
 * </ul>
 * </p>
 */
public class BossBarRenderConfig {

    /** 外表纹理（空血条背景） */
    private final Identifier texture;
    /** 进度纹理（选填，不填则使用texture） */
    @Nullable
    private final Identifier progressTexture;

    private final float bgU;
    private final float bgV;
    private final float fillU;
    private final float fillV;
    private final int barWidth;
    private final int barHeight;
    private final int texWidth;
    private final int texHeight;

    /**
     * 使用默认UV布局（和原版bars.png一致），仅替换纹理。
     * @param texture 外表纹理路径（进度也使用此纹理）
     */
    public BossBarRenderConfig(Identifier texture) {
        this(texture, null, 0, 60, 0, 65, 182, 5, 256, 256);
    }

    /**
     * 双纹理模式——外表和进度使用不同的纹理图片。
     * @param texture        外表纹理（空血条背景）
     * @param progressTexture 进度纹理（血条填充）
     */
    public BossBarRenderConfig(Identifier texture, Identifier progressTexture) {
        this(texture, progressTexture, 0, 60, 0, 65, 182, 5, 256, 256);
    }

    /**
     * 单纹理模式——完整配置渲染参数，外表和进度共用同一纹理。
     * @param texture   纹理路径
     * @param bgU       背景部分的u坐标
     * @param bgV       背景部分的v坐标
     * @param fillU     进度填充部分的u坐标
     * @param fillV     进度填充部分的v坐标
     * @param barWidth  Boss条绘制宽度（像素）
     * @param barHeight Boss条绘制高度（像素）
     * @param texWidth  纹理总宽度
     * @param texHeight 纹理总高度
     */
    public BossBarRenderConfig(Identifier texture, float bgU, float bgV, float fillU, float fillV,
                               int barWidth, int barHeight, int texWidth, int texHeight) {
        this(texture, null, bgU, bgV, fillU, fillV, barWidth, barHeight, texWidth, texHeight);
    }

    /**
     * 双纹理模式——完整配置渲染参数，外表和进度使用不同纹理。
     * @param texture         外表纹理（空血条背景）
     * @param progressTexture 进度纹理（血条填充，null则使用 texture）
     * @param bgU             背景部分的u坐标
     * @param bgV             背景部分的v坐标
     * @param fillU           进度填充部分的u坐标
     * @param fillV           进度填充部分的v坐标
     * @param barWidth        Boss条绘制宽度（像素）
     * @param barHeight       Boss条绘制高度（像素）
     * @param texWidth        纹理总宽度
     * @param texHeight       纹理总高度
     */
    public BossBarRenderConfig(Identifier texture, @Nullable Identifier progressTexture,
                               float bgU, float bgV, float fillU, float fillV,
                               int barWidth, int barHeight, int texWidth, int texHeight) {
        this.texture = texture;
        this.progressTexture = progressTexture;
        this.bgU = bgU;
        this.bgV = bgV;
        this.fillU = fillU;
        this.fillV = fillV;
        this.barWidth = barWidth;
        this.barHeight = barHeight;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    /** @return 外表纹理路径 */
    public Identifier texture() { return texture; }

    /** @return 进度纹理路径，null表示与外表共用同一纹理 */
    @Nullable
    public Identifier progressTexture() { return progressTexture; }

    /** @return 实际渲染进度时使用的纹理 */
    public Identifier getActualProgressTexture() {
        return progressTexture != null ? progressTexture : texture;
    }

    public float bgU() { return bgU; }
    public float bgV() { return bgV; }
    public float fillU() { return fillU; }
    public float fillV() { return fillV; }
    public int barWidth() { return barWidth; }
    public int barHeight() { return barHeight; }
    public int texWidth() { return texWidth; }
    public int texHeight() { return texHeight; }
}

