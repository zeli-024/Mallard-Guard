package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.zeli.mallardguard.GuardParticleColors;
import java.util.ArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Stationary, camera-facing six-frame impact with a matching tint mask. */
public final class GuardImpactParticle extends TextureSheetParticle {
    private static final int MAX_ACTIVE=64,FRAMES=6,LAST_SPRITE=FRAMES*2-1;
    private static final ArrayList<GuardImpactParticle> ACTIVE=new ArrayList<>(MAX_ACTIVE);
    private final SpriteSet sprites;
    private final float impactDuration,opacity,initialRotation,spinPerTick;
    private int startColor,middleColor,endColor;
    private long lastTick;

    private GuardImpactParticle(ClientLevel level,double x,double y,double z,SpriteSet sprites){
        super(level,x,y,z);
        this.sprites=sprites;
        impactDuration=GuardParticleColors.get(GuardParticleColors.IMPACT_DURATION);
        opacity=GuardParticleColors.get(GuardParticleColors.IMPACT_OPACITY)/100F;
        quadSize=.30F*GuardParticleColors.get(GuardParticleColors.IMPACT_SIZE)/100F;
        initialRotation=random.nextFloat()*(float)(Math.PI*2);
        spinPerTick=(random.nextBoolean()?1:-1)*(float)Math.toRadians(GuardParticleColors.get(GuardParticleColors.IMPACT_SPIN))/20F;
        startColor=GuardParticleColors.get(GuardParticleColors.TRACER_START_COLOR);
        middleColor=GuardParticleColors.get(GuardParticleColors.TRACER_MIDDLE_COLOR);
        endColor=GuardParticleColors.get(GuardParticleColors.TRACER_END_COLOR);
        lifetime=(int)impactDuration;
        hasPhysics=false;xd=yd=zd=0;lastTick=level.getGameTime();
        setSprite(sprites.get(0,LAST_SPRITE));
    }
    static boolean enabled(){
        return GuardParticleColors.get(GuardParticleColors.IMPACT_ENABLED)!=0&&GuardParticleColors.get(GuardParticleColors.IMPACT_SIZE)>0&&GuardParticleColors.get(GuardParticleColors.IMPACT_OPACITY)>0;
    }
    @Override public void tick(){
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            lastTick=level.getGameTime();if(++age>=lifetime)remove();
        }
    }
    void setPalette(int start,int middle,int end){
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            startColor=start;middleColor=middle;endColor=end;
        }
    }
    @Override public void remove() {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) { super.remove(); }
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.NO_RENDER;}
    private static void prune(ClientLevel level){
        int alive=0;
        for(int i=0;i<ACTIVE.size();i++){
            GuardImpactParticle p=ACTIVE.get(i);
            if(level==null||p.level!=level||!p.isAlive()||level.getGameTime()-p.lastTick>2)p.remove();
            else ACTIVE.set(alive++,p);
        }
        ACTIVE.subList(alive,ACTIVE.size()).clear();
    }
    static boolean hasActive(ClientLevel level){
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            prune(level);return !ACTIVE.isEmpty();
        }
    }
    public static void clear(){
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            for(int i=0;i<ACTIVE.size();i++)ACTIVE.get(i).remove();ACTIVE.clear();
        }
    }

    /** Two atlas quads at most: the white artwork and its matching, selectively tinted mask. */
    static void draw(VertexConsumer out,Camera camera,Vector3f right,Vector3f up,Vector3f towardCamera,float partial){
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            Vec3 eye=camera.getPosition();
            for(int i=0;i<ACTIVE.size();i++){
                GuardImpactParticle p=ACTIVE.get(i);float elapsed=p.age+partial;
                if(elapsed>=p.impactDuration||p.quadSize<=0||p.opacity<=0||eye.distanceToSqr(p.x,p.y,p.z)>128*128)continue;
                int frame=Math.min(FRAMES-1,(int)(elapsed*FRAMES/p.impactDuration));
                float angle=p.initialRotation+elapsed*p.spinPerTick,c=(float)Math.cos(angle),s=(float)Math.sin(angle),size=p.quadSize;
                float rx=(right.x*c+up.x*s)*size,ry=(right.y*c+up.y*s)*size,rz=(right.z*c+up.z*s)*size;
                float ux=(up.x*c-right.x*s)*size,uy=(up.y*c-right.y*s)*size,uz=(up.z*c-right.z*s)*size;
                float x=(float)(p.x-eye.x)+towardCamera.x*.004F,y=(float)(p.y-eye.y)+towardCamera.y*.004F,z=(float)(p.z-eye.z)+towardCamera.z*.004F;
                // The drawings disperse outward themselves; only their final interval gets an extra fade.
                float alpha=p.opacity*Math.min(1,(p.impactDuration-elapsed)*FRAMES/p.impactDuration);
                quad(out,p.sprites.get(frame,LAST_SPRITE),0xFFFFFF,alpha,x,y,z,rx,ry,rz,ux,uy,uz);
                int color=GuardParticleColors.tracer(elapsed/p.impactDuration,p.startColor,p.middleColor,p.endColor);
                if(color!=0xFFFFFF)
                    quad(out,p.sprites.get(FRAMES+frame,LAST_SPRITE),color,alpha,x,y,z,rx,ry,rz,ux,uy,uz);
            }
        }
    }
    private static void quad(VertexConsumer out,TextureAtlasSprite sprite,int color,float alpha,
        float x,float y,float z,float rx,float ry,float rz,float ux,float uy,float uz){
        float red=(color>>16&255)/255F,green=(color>>8&255)/255F,blue=(color&255)/255F;
        vertex(out,x-rx-ux,y-ry-uy,z-rz-uz,sprite.getU1(),sprite.getV1(),red,green,blue,alpha);
        vertex(out,x-rx+ux,y-ry+uy,z-rz+uz,sprite.getU1(),sprite.getV0(),red,green,blue,alpha);
        vertex(out,x+rx+ux,y+ry+uy,z+rz+uz,sprite.getU0(),sprite.getV0(),red,green,blue,alpha);
        vertex(out,x+rx-ux,y+ry-uy,z+rz-uz,sprite.getU0(),sprite.getV1(),red,green,blue,alpha);
    }
    private static void vertex(VertexConsumer out,float x,float y,float z,float u,float v,float red,float green,float blue,float alpha){
        out.addVertex(x,y,z).setColor(red,green,blue,alpha).setUv(u,v)
            .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,1,0);
    }
    record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>{
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz){
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            if(!enabled())return null;prune(level);if(ACTIVE.size()>=MAX_ACTIVE)return null;
                GuardImpactParticle p=new GuardImpactParticle(level,x,y,z,sprites);ACTIVE.add(p);return p;
        }
    }
    }
}
