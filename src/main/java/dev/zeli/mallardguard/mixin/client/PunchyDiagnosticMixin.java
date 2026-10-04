package dev.zeli.mallardguard.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="punchy.client.modelparts.ModelPartsParticleSystem",remap=false)
public abstract class PunchyDiagnosticMixin {
    @Inject(method={"maybeLogGlowDefinition","maybeLogGlowRender"},at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void mallardguard$skipDiagnostic(CallbackInfo callback){callback.cancel();}
}
