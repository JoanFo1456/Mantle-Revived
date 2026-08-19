package modernmods.hilt.data.predicate.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import modernmods.hilt.data.loadable.Loadables;
import modernmods.hilt.data.loadable.record.RecordLoadable;
import modernmods.hilt.data.predicate.IJsonPredicate;

/**
 * Predicate that checks if an entity has the given mob effect.
 */
public record HasMobEffectPredicate(MobEffect effect) implements LivingEntityPredicate {
  public static final RecordLoadable<HasMobEffectPredicate> LOADER = RecordLoadable.create(Loadables.MOB_EFFECT.requiredField("effect", HasMobEffectPredicate::effect), HasMobEffectPredicate::new);

  public HasMobEffectPredicate(Holder<MobEffect> effect) {
    this(effect.value());
  }

  @Override
  public boolean matches(LivingEntity living) {
    return living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
  }

  @Override
  public RecordLoadable<? extends IJsonPredicate<LivingEntity>> getLoader() {
    return LOADER;
  }
}
