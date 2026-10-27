package dev.eliasnvx.catgirl.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Recipe results with NBT: {@code "result": {"item": ..., "nbt": {...}}}, the Forge format, which carries the coat of
 * the fur recipes (later versions use a components patch; adapted from Femboy Mod). Forge reads {@code nbt} itself;
 * vanilla (Fabric) ignores it, so it is applied here, only when the stack came back without a tag (never twice on
 * Forge). Shaped and shapeless results both go through this method.
 */
@Mixin(ShapedRecipe.class)
public abstract class RecipeResultNbtMixin {

    private static final String NBT_KEY = "nbt";

    @Inject(method = "itemStackFromJson", at = @At("RETURN"))
    private static void catgirl$resultNbt(JsonObject json, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (!json.has(NBT_KEY) || stack.hasTag()) {
            return;
        }
        JsonElement nbt = json.get(NBT_KEY);
        try {
            // Like Forge: a string is SNBT, an object is read as SNBT from its JSON text
            stack.setTag(TagParser.parseTag(GsonHelper.isStringValue(nbt) ? nbt.getAsString() : nbt.toString()));
        } catch (CommandSyntaxException e) {
            throw new JsonSyntaxException("Invalid result nbt: " + e.getMessage());
        }
    }
}
