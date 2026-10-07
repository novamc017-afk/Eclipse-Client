package com.eclipse.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends Screen {
    private static final int BG = 0x0A0A0F, CARD = 0x14141B, CARD_HOVER = 0x1D1D28,
            OFF = 0x2B2B36, ACCENT = 0x7C5CFF;
    private static final int CH = 46, GAP = 8, PAD = 18, HEADER = 70;
    private static String selected = Modules.CATEGORIES[0];

    private final long opened = System.currentTimeMillis();
    private long tabStart = opened;
    private long lastNanos = System.nanoTime();
    private int px, py, pw, ph, cols, cw, vpTop, vpH;
    private float scrollTarget, scrollCur, maxScroll;
    private final List<Card> cards = new ArrayList<>();

    public MenuScreen() { super(Text.literal("Eclipse")); }

    private static float ease(long since, float ms) {
        float t = Math.min(1f, (System.currentTimeMillis() - since) / ms);
        return 1f - (float) Math.pow(1f - t, 3);
    }
    float anim() { return ease(opened, 380f); }
    float cardAnim() { return ease(tabStart, 380f); }

    @Override
    protected void init() {
        cards.clear();
        cols = width >= 760 ? 4 : 3;
        int avail = Math.min(width - 24, 700);
        cw = Math.max(90, Math.min(150, (avail - 2 * PAD - (cols - 1) * GAP) / cols));
        pw = cols * cw + (cols - 1) * GAP + 2 * PAD;

        int maxMods = 0;
        for (String c : Modules.CATEGORIES) maxMods = Math.max(maxMods, Modules.inCategory(c).size());
        int rows = (maxMods + cols - 1) / cols;
        int content = rows * (CH + GAP) - GAP;
        ph = Math.max(HEADER + CH + PAD, Math.min(HEADER + content + PAD, height - 12));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        vpTop = py + HEADER;
        vpH = ph - HEADER - PAD;
        maxScroll = Math.max(0, content - vpH);
        scrollTarget = Math.min(scrollTarget, maxScroll);

        // category tabs (left) + Edit HUD button (right)
        int tx = px + PAD, ty = py + 36;
        for (String cat : Modules.CATEGORIES) {
            int w = textRenderer.getWidth(cat) + 18;
            addDrawableChild(new Tab(tx, ty, w, 18, cat, () -> {
                selected = cat;
                tabStart = System.currentTimeMillis();
                scrollTarget = 0; scrollCur = 0;
                clearAndInit();
            }, cat));
            tx += w + 4;
        }
        int ew = textRenderer.getWidth("Edit HUD") + 18;
        addDrawableChild(new Tab(px + pw - PAD - ew, ty, ew, 18, "Edit HUD",
                () -> client.setScreen(new EditorScreen(this)), null));

        List<Modules.Module> mods = Modules.inCategory(selected);
        for (int i = 0; i < mods.size(); i++) {
            Card c = new Card(px + PAD + (i % cols) * (cw + GAP), vpTop + (i / cols) * (CH + GAP), mods.get(i), i);
            cards.add(c);
            addDrawableChild(c);
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        float a = anim();
        ctx.fill(0, 0, width, height, Gfx.argb(0.66f * a, 0x000000));
        int slide = (int) ((1f - a) * 10);
        Gfx.roundRect(ctx, px, py + slide, pw, ph, 10, Gfx.argb(0.96f * a, BG));
        if (a > 0.05f) {
            ctx.drawTextWithShadow(textRenderer, "ECLIPSE", px + PAD, py + slide + 10, Gfx.argb(a, 0xFFFFFF));
            String key = KeyBindingHelper.getBoundKeyOf(EclipseClient.MENU).getLocalizedText().getString();
            ctx.drawTextWithShadow(textRenderer, key + " closes  |  rebind in Options > Controls",
                    px + PAD + 56, py + slide + 10, Gfx.argb(a * 0.45f, 0xB0B0C0));
            ctx.fill(px + PAD, py + slide + 60, px + pw - PAD, py + slide + 61, Gfx.argb(a * 0.25f, 0xFFFFFF));
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long n = System.nanoTime();
        float dt = Math.min(0.1f, (n - lastNanos) / 1_000_000_000f);
        lastNanos = n;
        scrollCur = Gfx.smooth(scrollCur, scrollTarget, dt, 16f);
        for (Card c : cards) c.setY(c.baseY - Math.round(scrollCur));

        super.render(ctx, mx, my, delta);

        if (maxScroll > 0) {
            float a = anim();
            int barH = Math.max(16, (int) (vpH * (vpH / (vpH + maxScroll))));
            int barY = vpTop + (int) ((vpH - barH) * (scrollCur / maxScroll));
            Gfx.roundRect(ctx, px + pw - 9, barY, 3, barH, 1, Gfx.argb(a * 0.45f, 0xFFFFFF));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) verticalAmount * 28f));
        return true;
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
    @Override public void removed() { Modules.save(); }

    private final class Tab extends ButtonWidget {
        private final String cat; // null for action buttons
        private float hov, sel;
        private long last = System.nanoTime();

        Tab(int x, int y, int w, int h, String label, Runnable action, String cat) {
            super(x, y, w, h, Text.literal(label), b -> action.run(), DEFAULT_NARRATION_SUPPLIER);
            this.cat = cat;
            this.sel = cat != null && cat.equals(selected) ? 1f : 0f;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
            long now = System.nanoTime();
            float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
            last = now;
            hov = Gfx.smooth(hov, isHovered() ? 1f : 0f, dt, 16f);
            sel = Gfx.smooth(sel, cat != null && cat.equals(selected) ? 1f : 0f, dt, 14f);
            float a = MenuScreen.this.anim();
            if (a < 0.05f) return;
            int bg = Gfx.lerpRgb(Gfx.lerpRgb(CARD, CARD_HOVER, hov), ACCENT, sel * 0.85f);
            Gfx.roundRect(ctx, getX(), getY(), getWidth(), getHeight(), 6, Gfx.argb(a, bg));
            String label = getMessage().getString();
            ctx.drawTextWithShadow(textRenderer, label, getX() + (getWidth() - textRenderer.getWidth(label)) / 2,
                    getY() + 5, Gfx.argb(a, 0xFFFFFF));
        }
    }

    private final class Card extends ButtonWidget {
        final int baseY;
        private final Modules.Module m;
        private final int index;
        private float hov, on;
        private long last = System.nanoTime();

        Card(int x, int baseY, Modules.Module m, int index) {
            super(x, baseY, cw, CH, Text.literal(m.name), b -> m.enabled = !m.enabled, DEFAULT_NARRATION_SUPPLIER);
            this.baseY = baseY;
            this.m = m;
            this.index = index;
            this.on = m.enabled ? 1f : 0f;
        }

        @Override
        public boolean isMouseOver(double mx, double my) {
            return my >= vpTop && my <= vpTop + vpH && super.isMouseOver(mx, my);
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
            int y0 = getY();
            if (y0 + CH < vpTop || y0 > vpTop + vpH) return;

            long now = System.nanoTime();
            float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
            last = now;
            hov = Gfx.smooth(hov, isHovered() && isMouseOver(mx, my) ? 1f : 0f, dt, 16f);
            on = Gfx.smooth(on, m.enabled ? 1f : 0f, dt, 14f);

            float a = Math.min(MenuScreen.this.anim(),
                    Math.max(0f, MenuScreen.this.cardAnim() * 1.6f - index * 0.04f));
            if (a < 0.03f) return;
            int x = getX(), y = y0 + (int) ((1f - a) * 8);

            ctx.enableScissor(px, vpTop, px + pw, vpTop + vpH);
            Gfx.roundRect(ctx, x, y, cw, CH, 7, Gfx.argb(a, Gfx.lerpRgb(CARD, CARD_HOVER, hov)));
            ctx.fill(x + 1, y + 10, x + 3, y + CH - 10, Gfx.argb(a * on, ACCENT));

            ctx.drawTextWithShadow(textRenderer, trim(m.name, cw - 52), x + 10, y + 9, Gfx.argb(a, 0xFFFFFF));
            ctx.drawTextWithShadow(textRenderer, trim(m.desc, cw - 20), x + 10, y + 25, Gfx.argb(a * 0.55f, 0xB8B8C8));

            int tw = 24, th = 12, tx = x + cw - tw - 10, ty = y + 8;
            Gfx.roundRect(ctx, tx, ty, tw, th, 6, Gfx.argb(a, Gfx.lerpRgb(OFF, ACCENT, on)));
            int kx = tx + 2 + (int) ((tw - th) * on);
            Gfx.roundRect(ctx, kx, ty + 2, th - 4, th - 4, 4, Gfx.argb(a, 0xFFFFFF));
            ctx.disableScissor();
        }

        private String trim(String s, int maxW) {
            if (textRenderer.getWidth(s) <= maxW) return s;
            while (s.length() > 1 && textRenderer.getWidth(s + "...") > maxW) s = s.substring(0, s.length() - 1);
            return s + "...";
        }
    }
}
