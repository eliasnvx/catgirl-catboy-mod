package dev.eliasnvx.catgirl.client.render.entity;

import dev.eliasnvx.catgirl.cosmetic.Coat;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Render state of a cat-person mob. */
public class NekoRenderState extends HumanoidRenderState {
    public Coat coat = Coat.DEFAULT;
    public int skin;
    /** Fisher Neko at the water. */
    public boolean sitting;
    /** Per-mob phase so ears and tails of a crowd don't move in step. */
    public float phase;
}
