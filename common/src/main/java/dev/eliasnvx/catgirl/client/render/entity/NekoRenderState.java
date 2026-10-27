package dev.eliasnvx.catgirl.client.render.entity;

import dev.eliasnvx.catgirl.cosmetic.Coat;

/** What the ears and tail of a cat-person mob read this frame (1.21.1 has no render states: filled per draw). */
public class NekoRenderState {
    public Coat coat = Coat.DEFAULT;
    /** Per-mob phase so ears and tails of a crowd don't move in step. */
    public float phase;
    public float ageInTicks;
    public boolean hasRedOverlay;
}
