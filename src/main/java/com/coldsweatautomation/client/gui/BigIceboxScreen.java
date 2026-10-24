package com.coldsweatautomation.client.gui;

import com.coldsweatautomation.container.BigIceboxContainer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.common.event.HearthSaveDataHandler;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.network.message.DisableHearthParticlesMessage;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The expanded icebox screen, showing all 108 food slots in an 18 x 6 grid.
 *
 * <p>It extends vanilla's {@link AbstractContainerScreen} rather than Cold Sweat's
 * {@code AbstractHearthScreen}: the latter declares a package-private abstract
 * {@code getBlockEntity()}, so it can only be subclassed from inside Cold Sweat's own package -
 * and Java's module system forbids two mods from exporting the same package. The few extras of
 * Cold Sweat's screen (cold fuel gauge, power indicator, particle toggle) are re-implemented here
 * using Cold Sweat's public API and textures.</p>
 */
@OnlyIn(Dist.CLIENT)
public class BigIceboxScreen extends AbstractContainerScreen<BigIceboxContainer>
{
    private static final ResourceLocation BIG_ICEBOX_GUI =
            ResourceLocation.fromNamespaceAndPath("cold_sweat_automation", "textures/gui/screen/big_icebox_gui.png");
    /** The texture's real pixel size - it exceeds 256px, so blits must pass it explicitly. */
    private static final int TEXTURE_WIDTH = 232;
    private static final int TEXTURE_HEIGHT = 284;

    private static final ResourceLocation COLD_FUEL_GAUGE =
            ResourceLocation.fromNamespaceAndPath("cold_sweat", "textures/gui/sprites/hearth/fuel_gauge_cold.png");
    private static final ResourceLocation COLD_FUEL_GAUGE_EMPTY =
            ResourceLocation.fromNamespaceAndPath("cold_sweat", "textures/gui/sprites/hearth/fuel_gauge_cold_empty.png");
    // Sprite ATLAS ids - blitSprite() looks these up in the GUI atlas, so they must be the sprite
    // name (no "textures/" prefix, no ".png"), or the missing-texture checkerboard gets drawn.
    private static final ResourceLocation POWER_ON =
            ResourceLocation.fromNamespaceAndPath("cold_sweat", "hearth/power_indicator_on");
    private static final ResourceLocation POWER_OFF =
            ResourceLocation.fromNamespaceAndPath("cold_sweat", "hearth/power_indicator_off");

    private static final WidgetSprites PARTICLES_BUTTON_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("cold_sweat", "hearth/particle_button_on"),
            ResourceLocation.fromNamespaceAndPath("cold_sweat", "hearth/particle_button_off"),
            ResourceLocation.fromNamespaceAndPath("cold_sweat", "hearth/particle_button_on_focus"),
            ResourceLocation.fromNamespaceAndPath("cold_sweat", "hearth/particle_button_off_focus"));

    private final Pair<BlockPos, ResourceLocation> levelPos;
    private boolean hideParticles;
    private boolean hideParticlesOld;
    private ImageButton particleButton;

    public BigIceboxScreen(BigIceboxContainer screenContainer, Inventory inv, Component titleIn)
    {
        super(screenContainer, inv, titleIn);
        this.imageWidth = 232;
        this.imageHeight = 284;
        this.titleLabelX = this.imageWidth / 2 - Minecraft.getInstance().font.width(this.getTitle()) / 2;
        this.titleLabelY = 4;
        this.inventoryLabelX = BigIceboxContainer.INV_X;
        this.inventoryLabelY = 200;

        this.levelPos = Pair.of(this.menu.te.getBlockPos(), this.menu.te.getLevel().dimension().location());
        this.hideParticles = HearthSaveDataHandler.DISABLED_HEARTHS.contains(this.levelPos);
        this.hideParticlesOld = this.hideParticles;
    }

    @Override
    public void init()
    {
        super.init();
        if (this.menu.te.hasSmokestack())
        {
            this.particleButton = this.addRenderableWidget(new ImageButton(this.leftPos + this.imageWidth - 18, this.topPos + 4, 8, 7, PARTICLES_BUTTON_SPRITES, (button) ->
            {
                this.hideParticles = !this.hideParticles;
                if (this.hideParticles)
                {
                    HearthSaveDataHandler.DISABLED_HEARTHS.add(this.levelPos);
                    // Limit the number of disabled hearths to 64
                    if (HearthSaveDataHandler.DISABLED_HEARTHS.size() > 64)
                    {   HearthSaveDataHandler.DISABLED_HEARTHS.remove(HearthSaveDataHandler.DISABLED_HEARTHS.iterator().next());
                    }
                }
                else
                {   HearthSaveDataHandler.DISABLED_HEARTHS.remove(this.levelPos);
                }
            })
            {
                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button)
                {
                    if (this.active && this.visible && this.isValidClickButton(button) && this.clicked(mouseX, mouseY))
                    {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, !hideParticles ? 1.5f : 1.9f, 0.75f));
                        this.onClick(mouseX, mouseY);
                        this.setFocused(false);
                        return true;
                    }
                    return false;
                }

                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
                {
                    ResourceLocation resourcelocation = this.sprites.get(!hideParticles, this.isHoveredOrFocused());
                    guiGraphics.blitSprite(resourcelocation, this.getX(), this.getY(), this.width, this.height);
                }
            });
            this.particleButton.setTooltip(Tooltip.create(Component.translatable("cold_sweat.screen.hearth.show_particles")));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.children().forEach(child -> child.setFocused(false));
    }

    @Override
    public void onClose()
    {
        super.onClose();
        if (this.minecraft != null && this.minecraft.player != null && this.hideParticlesOld != this.hideParticles)
        {   PacketDistributor.sendToServer(new DisableHearthParticlesMessage(HearthSaveDataHandler.serializeDisabledHearths()));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        // The texture is larger than 256px, so the texture size MUST be passed explicitly -
        // the plain blit() overload assumes 256x256 and wraps the UVs, deforming the whole GUI.
        graphics.blit(BIG_ICEBOX_GUI, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // Cold fuel gauge, right of the fuel slot (mirrors Cold Sweat's own icebox layout)
        this.renderFuelGauge(graphics, this.leftPos + BigIceboxContainer.FUEL_X + 20, this.topPos + BigIceboxContainer.FUEL_Y + 1, this.menu.getFuel(), this.menu.te.getMaxFuel());

        if (!ConfigSettings.SMART_HEARTH.get() && this.menu.te.hasSmokestack())
        {
            boolean powered = this.menu.te.isCoolingOn();

            this.renderPowerIndicator(graphics, this.leftPos + BigIceboxContainer.FUEL_X + 21, this.topPos + BigIceboxContainer.FUEL_Y + 16, powered);

            if (CSMath.betweenInclusive(mouseX, this.leftPos + BigIceboxContainer.FUEL_X + 18, this.leftPos + BigIceboxContainer.FUEL_X + 37)
            && CSMath.betweenInclusive(mouseY, this.topPos + BigIceboxContainer.FUEL_Y + 13, this.topPos + BigIceboxContainer.FUEL_Y + 20))
            {   this.setTooltipForNextRenderPass(Component.translatable(powered ? "gui.cold_sweat.hearth.powered" : "gui.cold_sweat.hearth.unpowered"));
            }
        }
    }

    /**
     * Draws the vertical cold-fuel gauge, mirroring Cold Sweat's own hearth screens.
     */
    private void renderFuelGauge(GuiGraphics graphics, int x, int y, int fuel, int maxFuel)
    {
        int maxGaugeHeight = 14;
        int gaugeHeight = fuel <= 0 ? 0 : Math.round(CSMath.blend(2, 14, fuel, 0, maxFuel));

        graphics.blit(COLD_FUEL_GAUGE_EMPTY, x, y, 0, 0, 14, 14, 14, 14);
        graphics.blit(COLD_FUEL_GAUGE, x, y + (maxGaugeHeight - gaugeHeight), 0, maxGaugeHeight - gaugeHeight, 14, gaugeHeight, 14, 14);
    }

    private void renderPowerIndicator(GuiGraphics graphics, int x, int y, boolean powered)
    {   graphics.blitSprite(powered ? POWER_ON : POWER_OFF, x, y, 13, 4);
    }

    /**
     * Only used to keep the type parameter meaningful for callers.
     */
    public HearthBlockEntity getIcebox()
    {   return this.menu.te;
    }
}
