package cardejibka.noblind.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides blindness and darkness from the local player.
 *
 * <p>Three methods rather than one, because a shader mod reads the effect somewhere else.
 * Vanilla asks {@code hasEffect} for everything it draws itself: the fog comes out of
 * {@code MobEffectFogEnvironment.isApplicable}, the sky is suppressed from the flag
 * {@code Camera.extractRenderState} builds, and {@code FogRenderer} asks the same question about
 * night vision. Hiding the effect from {@code hasEffect} alone is enough for that.
 *
 * <p>Vitrail and Iris are not on that path. Each publishes the OptiFine uniforms {@code blindness},
 * {@code darknessFactor} and {@code darknessLightFactor} to the pack, and each fills them from
 * {@code getEffect(MobEffects.BLINDNESS)} and {@code getEffectBlendFactor(MobEffects.DARKNESS, ..)}
 * instead: Vitrail {@code render/FrameState.readPlayer}, over its own {@code blindness} and
 * {@code darknessFactor}, Iris {@code uniforms/CommonUniforms}. Neither of those calls
 * {@code hasEffect} at all, so with a pack drawing the frame the effect comes back. Vanilla has the
 * same gap in one place of its own, {@code LightmapRenderStateExtractor}, which reads the darkness
 * blend factor with no {@code hasEffect} guard in front of it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBlindnessMixin {

	@Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
	private void noblind$hideBlindness(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
		if (noblind$hidden(effect)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
	private void noblind$hideBlindnessInstance(Holder<MobEffect> effect,
			CallbackInfoReturnable<MobEffectInstance> cir) {
		if (noblind$hidden(effect)) {
			cir.setReturnValue(null);
		}
	}

	/**
	 * Redundant against the {@code getEffect} injection above, deliberately. Vanilla answers this
	 * by calling {@code getEffect} and blending whatever comes back, so a null instance already
	 * reads as zero here; a pack's uniform is worth more than the one line this saves, and this is
	 * what keeps the answer right if the vanilla body ever stops calling {@code getEffect}.
	 */
	@Inject(method = "getEffectBlendFactor", at = @At("HEAD"), cancellable = true)
	private void noblind$hideBlindnessBlend(Holder<MobEffect> effect, float partialTick,
			CallbackInfoReturnable<Float> cir) {
		if (noblind$hidden(effect)) {
			cir.setReturnValue(0.0F);
		}
	}

	/**
	 * The player and not the camera entity, matching what the mod has always done: a spectator
	 * riding someone else's eyes still sees their blindness.
	 */
	private boolean noblind$hidden(Holder<MobEffect> effect) {
		return (Object) this == Minecraft.getInstance().player
				&& (effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.DARKNESS));
	}
}
