package dev.eliasnvx.catgirl.client.render.entity;

/** A renderer that tints its model (1.20.1 has no tint hook: LivingEntityRendererMixin asks the renderer). */
public interface ModelTint {

    /** @param color the ARGB color vanilla would draw the model with (white, or see-through for invisible mobs) */
    int catgirl$tint(int color);
}
