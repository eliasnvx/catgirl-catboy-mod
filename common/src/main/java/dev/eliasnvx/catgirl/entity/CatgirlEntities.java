package dev.eliasnvx.catgirl.entity;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.mixin.MobAccessor;
import dev.eliasnvx.catgirl.set.FollowHouseCatGoal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import dev.eliasnvx.catgirl.entity.neko.Bakeneko;
import dev.eliasnvx.catgirl.entity.neko.CatSpirit;
import dev.eliasnvx.catgirl.entity.neko.FisherNeko;
import dev.eliasnvx.catgirl.entity.neko.Kitten;
import dev.eliasnvx.catgirl.entity.neko.NekoMob;
import dev.eliasnvx.catgirl.entity.neko.StrayNeko;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CatgirlEntities {

    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(CatgirlMod.MOD_ID, Registries.ENTITY_TYPE);

    /** Invisible seat in a cat bed or a box. Saved, but removes itself when empty. */
    public static final ResourceKey<EntityType<?>> SEAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, CatgirlMod.id("seat"));
    public static final RegistrySupplier<EntityType<CatSeat>> SEAT = REGISTER.register(SEAT_KEY.location(),
            () -> EntityType.Builder.<CatSeat>of(CatSeat::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).noSummon().clientTrackingRange(10).build(SEAT_KEY.location().toString()));

    public static final RegistrySupplier<EntityType<StrayNeko>> STRAY_NEKO = humanoid("stray_neko", StrayNeko::new, MobCategory.CREATURE);
    public static final RegistrySupplier<EntityType<FisherNeko>> FISHER_NEKO = humanoid("fisher_neko", FisherNeko::new, MobCategory.CREATURE);
    public static final RegistrySupplier<EntityType<Bakeneko>> BAKENEKO = humanoid("bakeneko", Bakeneko::new, MobCategory.MONSTER);
    public static final RegistrySupplier<EntityType<CatSpirit>> CAT_SPIRIT = humanoid("cat_spirit", CatSpirit::new, MobCategory.MONSTER);
    public static final ResourceKey<EntityType<?>> KITTEN_KEY = key("kitten");
    /** A real kitten (vanilla cat size and model). */
    public static final RegistrySupplier<EntityType<Kitten>> KITTEN = REGISTER.register(KITTEN_KEY.location(),
            () -> EntityType.Builder.<Kitten>of(Kitten::new, MobCategory.CREATURE).sized(0.6F, 0.7F).eyeHeight(0.35F)
                    .clientTrackingRange(8).build(KITTEN_KEY.location().toString()));

    // Natural spawning (move the weights to catgirl-common config once the config exists); biomes are data tags
    public static final TagKey<Biome> STRAY_NEKO_SPAWNS = biomeTag("spawns/stray_neko");
    public static final TagKey<Biome> FISHER_NEKO_SPAWNS = biomeTag("spawns/fisher_neko");
    public static final TagKey<Biome> BAKENEKO_SPAWNS = biomeTag("spawns/bakeneko");
    public static final TagKey<Biome> CAT_SPIRIT_SPAWNS = biomeTag("spawns/cat_spirit");
    private static final int STRAY_NEKO_WEIGHT = 8;
    private static final int FISHER_NEKO_WEIGHT = 4;
    private static final int BAKENEKO_WEIGHT = 3;
    private static final int CAT_SPIRIT_WEIGHT = 1;

    // Goal priorities on vanilla cats and ocelots: Cat's tempt goal is 4, so player-driven fun comes right after it
    private static final int CATNIP_PRIORITY = 2;
    private static final int YARN_PRIORITY = 5;
    private static final int FOLLOW_PRIORITY = 6;

    private CatgirlEntities() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, CatgirlMod.id(name));
    }

    private static TagKey<Biome> biomeTag(String name) {
        return TagKey.create(Registries.BIOME, CatgirlMod.id(name));
    }

    /** The shared cat-person rig: player-sized. */
    private static <T extends Mob> RegistrySupplier<EntityType<T>> humanoid(String name, EntityType.EntityFactory<T> factory, MobCategory category) {
        ResourceKey<EntityType<?>> key = key(name);
        return REGISTER.register(key.location(), () -> EntityType.Builder.of(factory, category)
                .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build(key.location().toString()));
    }

    private static <T extends Mob> void spawn(RegistrySupplier<EntityType<T>> type, MobCategory category, TagKey<Biome> biomes,
                                             int weight, SpawnPlacements.SpawnPredicate<T> rules) {
        SpawnPlacementsRegistry.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rules);
        BiomeModifications.addProperties(ctx -> ctx.hasTag(biomes), (ctx, props) -> props.getSpawnProperties()
                .addSpawn(category, new MobSpawnSettings.SpawnerData(type.get(), weight, 1, 1)));
    }

    /** Stray nekos live in villages: open ground inside one. */
    private static boolean inVillage(EntityType<? extends Mob> type, ServerLevelAccessor level, MobSpawnType reason,
                                     BlockPos pos, RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && (reason != MobSpawnType.NATURAL || level.getLevel().isVillage(pos));
    }

    public static void init() {
        REGISTER.register();
        EntityAttributeRegistry.register(STRAY_NEKO, StrayNeko::createAttributes);
        EntityAttributeRegistry.register(FISHER_NEKO, NekoMob::createAttributes);
        EntityAttributeRegistry.register(BAKENEKO, Bakeneko::createAttributes);
        EntityAttributeRegistry.register(CAT_SPIRIT, NekoMob::createAttributes);
        EntityAttributeRegistry.register(KITTEN, Cat::createAttributes);
        spawn(STRAY_NEKO, MobCategory.CREATURE, STRAY_NEKO_SPAWNS, STRAY_NEKO_WEIGHT, CatgirlEntities::inVillage);
        spawn(FISHER_NEKO, MobCategory.CREATURE, FISHER_NEKO_SPAWNS, FISHER_NEKO_WEIGHT, Mob::checkMobSpawnRules);
        spawn(BAKENEKO, MobCategory.MONSTER, BAKENEKO_SPAWNS, BAKENEKO_WEIGHT, Bakeneko::checkSpawnRules);
        spawn(CAT_SPIRIT, MobCategory.MONSTER, CAT_SPIRIT_SPAWNS, CAT_SPIRIT_WEIGHT, CatSpirit::checkSpawnRules);
        // Vanilla cats and ocelots get the mod's goals: catnip zoomies, chasing yarn balls, following House Cat wearers
        EntityEvent.ADD.register((entity, level) -> {
            if (!level.isClientSide() && (entity instanceof Cat || entity instanceof Ocelot)) {
                PathfinderMob mob = (PathfinderMob) entity;
                GoalSelector goals = ((MobAccessor) mob).catgirl$goalSelector();
                goals.addGoal(CATNIP_PRIORITY, new CatnipZoomiesGoal(mob));
                goals.addGoal(YARN_PRIORITY, new ChaseYarnGoal(mob));
                goals.addGoal(FOLLOW_PRIORITY, new FollowHouseCatGoal(mob));
            }
            return EventResult.pass();
        });
    }
}
