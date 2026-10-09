package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.MallardGuard;
import dev.zeli.mallardguard.GuardParticleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import java.util.ArrayList;

/** One bounded atlas batch for sparks and animated impacts. */
@EventBusSubscriber(modid=MallardGuard.ID,value=Dist.CLIENT)
public final class GuardParryParticleRenderer {
    // Custom world rendering bypasses Particle.render(); protect tick and draw state here.
    // A single monitor keeps registry, particle history and profiler lock ordering consistent.
    static final Object STATE_LOCK = new Object();
    private static final RenderType MATERIAL=RenderType.entityTranslucent(ResourceLocation.withDefaultNamespace("textures/atlas/particles.png"));
    private static final ArrayList<GuardParryParticle> ACTIVE=new ArrayList<>(GuardParticleConfig.MAX_ACTIVE_HEADS);
    private static final Vector3f RIGHT=new Vector3f(),UP=new Vector3f(),BACKWARD=new Vector3f();
    private static final boolean[] VISIBLE=new boolean[GuardParticleConfig.MAX_ACTIVE_HEADS];
    private static final double[] DISTANCE=new double[GuardParticleConfig.MAX_ACTIVE_HEADS];
    private static ClientLevel lastPruneLevel;
    private static long lastPruneTime=Long.MIN_VALUE;
    private GuardParryParticleRenderer() {}
    static void add(GuardParryParticle particle) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            ACTIVE.add(particle);
        }
    }
    static int availableCapacity(ClientLevel level) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            prune(level);return Math.max(0,GuardParticleConfig.MAX_ACTIVE_HEADS-ACTIVE.size()-GuardSparkTracerParticle.activeHeads(level));
        }
    }
    static int activeHeadCount(ClientLevel level) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            prune(level);return ACTIVE.size();
        }
    }
    static boolean hasRoom(ClientLevel level) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            return availableCapacity(level)>0;
        }
    }
    public static void clear() {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            GuardParticleCollisionProfiler.clear();
            for(int i=0;i<ACTIVE.size();i++)ACTIVE.get(i).remove();
            ACTIVE.clear();lastPruneLevel=null;lastPruneTime=Long.MIN_VALUE;
        }
    }
    private static void prune(ClientLevel level) {
        long time=level==null?Long.MIN_VALUE:level.getGameTime();if(level==lastPruneLevel && time==lastPruneTime)return;
        lastPruneLevel=level;lastPruneTime=time;
        int alive=0;
        for(int i=0;i<ACTIVE.size();i++) {
            var spark=ACTIVE.get(i);
            if(level==null || !spark.isAlive() || !spark.belongsTo(level) || time-spark.lastTick>2){spark.remove();}
            else ACTIVE.set(alive++,spark);
        }
        ACTIVE.subList(alive,ACTIVE.size()).clear();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            var mc=Minecraft.getInstance();prune(mc.level);boolean hasImpact=GuardImpactParticle.hasActive(mc.level);if(ACTIVE.isEmpty() && GuardSparkTracerParticle.activeHeads(mc.level)==0 && !hasImpact)return;
            var camera=event.getCamera();var eye=camera.getPosition();RIGHT.set(1,0,0).rotate(camera.rotation());UP.set(0,1,0).rotate(camera.rotation());
            BACKWARD.set(RIGHT.y*UP.z-RIGHT.z*UP.y,RIGHT.z*UP.x-RIGHT.x*UP.z,RIGHT.x*UP.y-RIGHT.y*UP.x);
            int visibleHeads=0;
            for(int i=0;i<ACTIVE.size();i++) {
                var spark=ACTIVE.get(i);DISTANCE[i]=spark.distanceSquared(eye);VISIBLE[i]=spark.visible(eye,BACKWARD,DISTANCE[i]);
                if(VISIBLE[i]){visibleHeads++;}
            }
            if(visibleHeads==0 && GuardSparkTracerParticle.activeHeads(mc.level)==0 && !hasImpact)return;
            var buffers=mc.renderBuffers().bufferSource();var vertices=buffers.getBuffer(MATERIAL);
            float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
            for(int i=0;i<ACTIVE.size();i++) {
                var spark=ACTIVE.get(i);if(!VISIBLE[i])continue;
                spark.prepare(camera,partial);
                spark.drawHead(vertices,RIGHT,UP,partial);
            }
            GuardSparkTracerParticle.draw(vertices,camera,RIGHT,UP,BACKWARD,partial);
            // The hit flash is the final layer at the shared origin. Keep the same atlas batch.
            GuardImpactParticle.draw(vertices,camera,RIGHT,UP,BACKWARD,partial);
            buffers.endBatch(MATERIAL);
        }
    }
}
