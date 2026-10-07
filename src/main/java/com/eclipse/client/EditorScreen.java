package com.eclipse.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Drag any enabled HUD module around. Positions are saved as screen fractions. */
public class EditorScreen extends Screen {
    private final Screen parent;
    private Modules.Module dragging;
    private int offX, offY;
    private boolean lastDown;

    public EditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Hud.editing = true;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset positions"), b -> Modules.resetPositions())
                .dimensions(width / 2 - 104, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 + 4, height - 28, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, Gfx.argb(0.35f, 0x000000));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        long win = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        List<Modules.Module> all = Modules.ALL;

        if (down && !lastDown && my < height - 36) {
            for (int i = all.size() - 1; i >= 0; i--) {
                Modules.Module m = all.get(i);
                if (m.enabled && m.isHud() && m.bw > 0
                        && mx >= m.bx && mx <= m.bx + m.bw && my >= m.by && my <= m.by + m.bh) {
                    dragging = m; offX = mx - m.bx; offY = my - m.by;
                    break;
                }
            }
        }
        if (!down) dragging = null;
        if (dragging != null) {
            int nx = Math.max(0, Math.min(width - dragging.bw, mx - offX));
            int ny = Math.max(0, Math.min(height - dragging.bh, my - offY));
            dragging.px = nx / (float) width;
            dragging.py = ny / (float) height;
            dragging.bx = nx; dragging.by = ny;
        }
        lastDown = down;

        for (Modules.Module m : all) {
            if (!m.enabled || !m.isHud() || m.bw <= 0) continue;
            boolean hot = m == dragging || (mx >= m.bx && mx <= m.bx + m.bw && my >= m.by && my <= m.by + m.bh);
            int c = Gfx.argb(hot ? 1f : 0.55f, 0x7C5CFF);
            ctx.fill(m.bx - 1, m.by - 1, m.bx + m.bw + 1, m.by, c);
            ctx.fill(m.bx - 1, m.by + m.bh, m.bx + m.bw + 1, m.by + m.bh + 1, c);
            ctx.fill(m.bx - 1, m.by, m.bx, m.by + m.bh, c);
            ctx.fill(m.bx + m.bw, m.by, m.bx + m.bw + 1, m.by + m.bh, c);
            if (hot) ctx.drawTextWithShadow(textRenderer, m.name, m.bx, Math.max(0, m.by - 11), 0xFFFFFFFF);
        }
        ctx.drawCenteredTextWithShadow(textRenderer, "Drag modules to move them", width / 2, 8, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == KeyBindingHelper.getBoundKeyOf(EclipseClient.MENU).getCode()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public void removed() { Hud.editing = false; Modules.save(); }
}
