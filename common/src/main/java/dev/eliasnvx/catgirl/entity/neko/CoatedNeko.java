package dev.eliasnvx.catgirl.entity.neko;

import dev.eliasnvx.catgirl.cosmetic.Coat;

/** A cat-person mob: its ears and tail wear a coat, and it has one of a few skins (4 per mob). */
public interface CoatedNeko {

    int SKINS = 4;

    Coat coat();

    /** 0..{@link #SKINS}-1 */
    int skin();
}
