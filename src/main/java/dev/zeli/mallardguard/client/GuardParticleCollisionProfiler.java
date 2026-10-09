package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardDiagnostics;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.Locale;

/** Opt-in collision measurements; tick counters share the particle-state monitor. */
@EventBusSubscriber(modid=MallardGuard.ID,value=Dist.CLIENT)
public final class GuardParticleCollisionProfiler {
    static final int SLOW=0, FAST_FIRST=1, FAST_SECOND=2;
    private static final int WINDOW_TICKS=100;
    private static final Logger LOGGER=LoggerFactory.getLogger("Mallard Guard/Particle Profile");
    private static final long[] CALLS=new long[9], NANOS=new long[9], HITS=new long[9];
    private static boolean enabled;
    private static ClientLevel currentLevel;
    private static long lastGameTick=Long.MIN_VALUE, windowStarted, previousTotal;
    private static long peakTickNanos;
    private static int ticks,peakHeads;
    private GuardParticleCollisionProfiler() {}

    static long begin() { return enabled?System.nanoTime():0; }
    static void record(GuardParryParticle.Type type,int path,long started,boolean hit) {
        recordBucket((type==GuardParryParticle.Type.FLYING_SPARK?0:3)+path,started,hit);
    }
    static void recordTracer(int contact,long started,boolean hit) { recordBucket(6+FAST_FIRST+contact,started,hit); }
    private static void recordBucket(int bucket,long started,boolean hit) {
        long elapsed=System.nanoTime()-started;
        CALLS[bucket]++;NANOS[bucket]+=Math.max(0,elapsed);
        if(hit)HITS[bucket]++;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            var mc=Minecraft.getInstance();
            boolean requested=mc.level!=null && (GuardConfig.DEBUG_CLIENT_FLAGS.get()&GuardDiagnostics.PARTICLE_COLLISIONS)!=0;
            if(!requested) {
                if(enabled && mc.level==currentLevel && ticks>0)report();
                if(enabled)clear();
                return;
            }
            if(!enabled || currentLevel!=mc.level) {
                clear();enabled=true;currentLevel=mc.level;
                lastGameTick=currentLevel.getGameTime();windowStarted=System.nanoTime();
                LOGGER.info("Particle collision profiling started; summaries every 100 game ticks. Timings include measurement overhead.");
                return;
            }
            long gameTick=currentLevel.getGameTime();
            if(gameTick==lastGameTick)return;
            if(gameTick<lastGameTick){clear();return;}
            lastGameTick=gameTick;ticks++;
            long total=totalNanos();peakTickNanos=Math.max(peakTickNanos,total-previousTotal);previousTotal=total;
            peakHeads=Math.max(peakHeads,GuardParryParticleRenderer.activeHeadCount(currentLevel)+GuardSparkTracerParticle.activeHeads(currentLevel));
            if(ticks>=WINDOW_TICKS){report();resetWindow();}
        }
    }
    private static long totalNanos() {
        long sum=0;for(long value:NANOS)sum+=value;return sum;
    }
    private static void report() {
        if(ticks==0)return;
        double seconds=(System.nanoTime()-windowStarted)/1_000_000_000.0;
        long total=totalNanos();
        LOGGER.info(String.format(Locale.ROOT,
            "Particle collisions: ticks=%d, wall_s=%.2f, peak_heads=%d, total_ms=%.3f, avg_ms_per_tick=%.4f, peak_ms_per_tick=%.4f; %s; %s; %s",
            ticks,seconds,peakHeads,total/1_000_000.0,total/1_000_000.0/ticks,peakTickNanos/1_000_000.0,
            effect("Flying Sparks",0),effect("Spark Debris",3),effect("Spark Tracers",6)));
    }
    private static String effect(String name,int offset) {
        long first=CALLS[offset+FAST_FIRST],second=CALLS[offset+FAST_SECOND];
        return String.format(Locale.ROOT,"%s [%s; %s; %s; second_check_pct=%.1f]",name,
            path("slow",offset+SLOW),path("fast_first",offset+FAST_FIRST),path("fast_second",offset+FAST_SECOND),
            first==0?0:100.0*second/first);
    }
    private static String path(String name,int bucket) {
        long calls=CALLS[bucket];
        return String.format(Locale.ROOT,"%s checks=%d hits=%d total_ms=%.3f avg_us=%.3f",name,calls,HITS[bucket],
            NANOS[bucket]/1_000_000.0,calls==0?0:NANOS[bucket]/1000.0/calls);
    }
    private static void resetWindow() {
        Arrays.fill(CALLS,0);Arrays.fill(NANOS,0);Arrays.fill(HITS,0);
        ticks=0;peakHeads=0;peakTickNanos=previousTotal=0;windowStarted=System.nanoTime();
    }
    static void clear() {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            enabled=false;currentLevel=null;lastGameTick=Long.MIN_VALUE;resetWindow();
        }
    }
}
