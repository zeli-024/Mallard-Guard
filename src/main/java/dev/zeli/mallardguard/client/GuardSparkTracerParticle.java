package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.zeli.mallardguard.GuardSparkTracerConfig;
import dev.zeli.mallardguard.GuardParticleColors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.ArrayList;
import static dev.zeli.mallardguard.GuardSparkTracerConfig.*;

/** One physical head with stationary upright squares stored in its bounded history. */
public final class GuardSparkTracerParticle extends TextureSheetParticle {
    private static final ArrayList<GuardSparkTracerParticle> HEADS=new ArrayList<>();
    private static ClientLevel pruneLevel;
    private static long pruneTime=Long.MIN_VALUE;
    private final GuardSparkPath path=new GuardSparkPath();
    private final GuardSparkTrail trail;
    private final int squaresPerTick,headShrink,trailShrink;
    private int startColor,middleColor,endColor;
    private double minX,minY,minZ,maxX,maxY,maxZ;
    private final int headLife,headFade,trailLife,trailFade,dropDelay;
    private final double acceleration,retention,bounce,decay,surface;
    private final float width,headWidth,headOpacity,trailOpacity;

    private final float hu0,hu1,hv0,hv1,tu0,tu1,tv0,tv1;
    private int impacts,groundImpacts;
    private boolean stopped;
    private long lastTick;
    private GuardSparkTracerParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites) {
        super(level,x,y,z);xd=dx;yd=dy;zd=dz;hasPhysics=false;
        double lifeFactor=GuardParryParticleMath.varianceFactor(random.nextDouble(),get(LIFETIMEVARIANCE));
        headLife=GuardParryParticleMath.scaledLifetime(GuardParticleColors.get(GuardParticleColors.TRACER_LIFETIME),lifeFactor);
        double fadeFactor=GuardParryParticleMath.varianceFactor(random.nextDouble(),get(FADEVARIANCE));
        headFade=Math.min(headLife,Math.max(0,(int)Math.round(get(HEADFADE)*fadeFactor)));
        trailLife=GuardParryParticleMath.scaledLifetime(GuardParticleColors.get(GuardParticleColors.TRACER_TRAIL_TICKS),lifeFactor);
        trailFade=Math.min(trailLife,Math.max(0,(int)Math.round(get(TRAILFADE)*fadeFactor)));
        acceleration=.001*get(GRAVITY)*GuardParryParticleMath.varianceFactor(random.nextDouble(),get(GRAVITYVARIANCE));
        retention=GuardParryParticleMath.velocityRetention(Math.min(100,get(VELOCITYLOSS)*GuardParryParticleMath.varianceFactor(random.nextDouble(),get(VELOCITYLOSSVARIANCE))));
        dropDelay=get(DROPDELAY);bounce=get(BOUNCE)/100.0;decay=1-get(BOUNCEDECAY)/100.0;surface=1-get(SURFACEFRICTION)/100.0;
        startColor=GuardParticleColors.get(GuardParticleColors.TRACER_START_COLOR);middleColor=GuardParticleColors.get(GuardParticleColors.TRACER_MIDDLE_COLOR);endColor=GuardParticleColors.get(GuardParticleColors.TRACER_END_COLOR);
        width=.00975f*GuardParticleColors.get(GuardParticleColors.TRACER_SIZE)/100f;headWidth=width*get(HEADSIZE)/100f;
        headOpacity=get(HEADOPACITY)/100f;trailOpacity=get(TRAILOPACITY)/100f;
        squaresPerTick=get(SQUARESPERTICK);headShrink=get(HEADSHRINK);trailShrink=get(TRAILSHRINK);
        boolean visibleTrail=get(TRAILENABLED)!=0 && squaresPerTick>0 && width>0 && trailOpacity>0;
        trail=visibleTrail?new GuardSparkTrail(squaresPerTick,trailLife,headLife):null;
        lifetime=headLife+(trail==null?0:trailLife+1);
        setSprite(sprites.get(0,1));hu0=getU0();hu1=getU1();hv0=getV0();hv1=getV1();
        setSprite(sprites.get(1,1));tu0=getU0();tu1=getU1();tv0=getV0();tv1=getV1();
        lastTick=level.getGameTime();updateBounds();
    }
    static int activeHeads(ClientLevel level) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            long now=level==null?Long.MIN_VALUE:level.getGameTime();
            if(level!=pruneLevel || now!=pruneTime) {
                pruneLevel=level;pruneTime=now;
                HEADS.removeIf(p->{boolean discard=level==null || p.level!=level || !p.isAlive() || now-p.lastTick>2;if(discard)p.remove();return discard;});
            }
            return HEADS.size();
        }
    }
    static void clear() {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            for(var p:HEADS)p.remove();HEADS.clear();pruneLevel=null;pruneTime=Long.MIN_VALUE;
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
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.NO_RENDER; }
    @Override public void tick() {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            lastTick=level.getGameTime();xo=x;yo=y;zo=z;
            if(age++>=lifetime){remove();return;}
            path.clear();
            if(trail!=null)trail.expire(age-1,trailLife);
            if(!stopped && age<=headLife) {
                if(age>dropDelay)yd-=acceleration;
                movePath();xd*=retention;yd*=retention;zd*=retention;
                if(trail!=null)trail.emit(path,age-1,squaresPerTick,1-GuardParryParticleMath.fadeOpacity(age-1,headLife,headFade));
            }
            updateBounds();
        }
    }
    private void settle() { stopped=true;xd=yd=zd=0; }
    private void movePath() {
        if(xd*xd+yd*yd+zd*zd<1.0E-16)return;
        var player=Minecraft.getInstance().player;if(player==null)return;
        double remaining=1,elapsed=0;
        // Point path queries give the exact impact point even at low speed. No secondary trail queries.
        for(int contact=0;contact<2 && remaining>1.0E-5;contact++) {
            Vec3 start=new Vec3(x,y,z),end=start.add(xd*remaining,yd*remaining,zd*remaining);
            long profileStart=GuardParticleCollisionProfiler.begin();
            var hit=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if(profileStart!=0)GuardParticleCollisionProfiler.recordTracer(contact,profileStart,hit.getType()==HitResult.Type.BLOCK);
            if(hit.getType()!=HitResult.Type.BLOCK) {
                path.add(x,y,z,end.x,end.y,end.z,elapsed,1);setPos(end.x,end.y,end.z);return;
            }
            if(hit.isInside()){settle();return;}
            Vec3 point=hit.getLocation();double dx=end.x-x,dy=end.y-y,dz=end.z-z,len=dx*dx+dy*dy+dz*dz;
            double fraction=len<1.0E-16?0:Math.max(0,Math.min(1,((point.x-x)*dx+(point.y-y)*dy+(point.z-z)*dz)/len));
            double at=elapsed+remaining*fraction;
            path.add(x,y,z,point.x,point.y,point.z,elapsed,at);
            double nx=hit.getDirection().getStepX(),ny=hit.getDirection().getStepY(),nz=hit.getDirection().getStepZ();
            setPos(point.x+nx*.003,point.y+ny*.003,point.z+nz*.003);
            double rebound=bounce*Math.pow(decay,Math.min(impacts++,8)),dot=xd*nx+yd*ny+zd*nz;
            xd=GuardParryParticleMath.debrisReflection(xd,nx,dot,rebound,surface);
            yd=GuardParryParticleMath.debrisReflection(yd,ny,dot,rebound,surface);
            zd=GuardParryParticleMath.debrisReflection(zd,nz,dot,rebound,surface);
            // Let two ground rebounds complete before sleeping low-energy motion. Walls do not count.
            if(ny>0 && ++groundImpacts>=3 && Math.abs(yd)<=acceleration*1.6 && xd*xd+zd*zd<.01){settle();return;}
            if(acceleration==0 && xd*xd+yd*yd+zd*zd<1.0E-8){settle();return;}
            remaining*=1-fraction;elapsed=at;
        }
    }
    static void draw(VertexConsumer vertices,Camera camera,Vector3f right,Vector3f up,Vector3f backward,float partial) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            Vec3 eye=camera.getPosition();
            for(var p:HEADS)p.drawOne(vertices,eye,right,up,backward,partial);
        }
    }
    private void updateBounds() {
        minX=Math.min(x,xo);maxX=Math.max(x,xo);minY=Math.min(y,yo);maxY=Math.max(y,yo);minZ=Math.min(z,zo);maxZ=Math.max(z,zo);
        for(int i=0;i<path.count;i++) {
            int p=i*6;
            for(int end=0;end<2;end++,p+=3){minX=Math.min(minX,path.points[p]);maxX=Math.max(maxX,path.points[p]);minY=Math.min(minY,path.points[p+1]);maxY=Math.max(maxY,path.points[p+1]);minZ=Math.min(minZ,path.points[p+2]);maxZ=Math.max(maxZ,path.points[p+2]);}
        }
        if(trail!=null && trail.count>0){minX=Math.min(minX,trail.minX);maxX=Math.max(maxX,trail.maxX);minY=Math.min(minY,trail.minY);maxY=Math.max(maxY,trail.maxY);minZ=Math.min(minZ,trail.minZ);maxZ=Math.max(maxZ,trail.maxZ);}
    }
    private boolean visible(Vec3 eye,Vector3f backward) {
        double pad=Math.max(width,headWidth)*1.414214;
        double dx=Math.max(0,Math.max(minX-pad-eye.x,eye.x-maxX-pad)),dy=Math.max(0,Math.max(minY-pad-eye.y,eye.y-maxY-pad)),dz=Math.max(0,Math.max(minZ-pad-eye.z,eye.z-maxZ-pad));
        if(dx*dx+dy*dy+dz*dz>=4096)return false;
        double cx=(minX+maxX)*.5-eye.x,cy=(minY+maxY)*.5-eye.y,cz=(minZ+maxZ)*.5-eye.z;
        double reach=Math.abs(backward.x)*(maxX-minX)*.5+Math.abs(backward.y)*(maxY-minY)*.5+Math.abs(backward.z)*(maxZ-minZ)*.5+pad;
        return cx*backward.x+cy*backward.y+cz*backward.z<=reach;
    }
    private void drawOne(VertexConsumer out,Vec3 eye,Vector3f right,Vector3f up,Vector3f backward,float partial) {
        if(!visible(eye,backward))return;
        double now=Math.max(0,age-1+partial),hx=xo+(x-xo)*partial,hy=yo+(y-yo)*partial,hz=zo+(z-zo)*partial;
        for(int i=0;i<path.count;i++) {
            if(partial<path.times[i*2])break;
            int p=i*6;double f=path.fraction(i,partial);
            hx=path.points[p]+(path.points[p+3]-path.points[p])*f;
            hy=path.points[p+1]+(path.points[p+4]-path.points[p+1])*f;
            hz=path.points[p+2]+(path.points[p+5]-path.points[p+2])*f;
        }
        if(trail!=null) {
            for(int group=0;group<trail.groups;group++) {
                int slot=trail.groupIndex(group),birth=trail.born[slot];
                if(now-birth>=trailLife)continue;
                float fade=GuardParryParticleMath.fadeOpacity((float)(now-birth),trailLife,trailFade);
                float opacity=trailOpacity*fade;
                if(opacity<.01)continue;
                float captured=trail.cooling[slot];
                int color=GuardParticleColors.tracer(captured+(1-captured)*(1-fade),startColor,middleColor,endColor);
                float size=width*GuardParryParticleMath.sizeScale(age-1-birth,trailLife,trailShrink);
                double rx=right.x*size,ry=right.y*size,rz=right.z*size,ux=up.x*size,uy=up.y*size,uz=up.z*size;
                int start=slot*trail.squaresPerGroup*3;
                for(int i=0;i<trail.squaresPerGroup;i++) {
                    int p=start+i*3;double tx=trail.points[p]-eye.x,ty=trail.points[p+1]-eye.y,tz=trail.points[p+2]-eye.z;
                    if(tx*tx+ty*ty+tz*tz>4096 || tx*backward.x+ty*backward.y+tz*backward.z>width*1.414214)continue;
                    square(out,tx,ty,tz,rx,ry,rz,ux,uy,uz,tu0,tu1,tv0,tv1,opacity,color);
                }
            }
        }
        float headFadeOpacity=GuardParryParticleMath.fadeOpacity((float)now,headLife,headFade);
        float opacity=headOpacity*headFadeOpacity;
        hx-=eye.x;hy-=eye.y;hz-=eye.z;
        if(now>=headLife || opacity<.01 || headWidth==0 || hx*hx+hy*hy+hz*hz>4096 || hx*backward.x+hy*backward.y+hz*backward.z>headWidth*1.414214)return;
        float headSize=headWidth*GuardParryParticleMath.sizeScale(age,headLife,headShrink);
        square(out,hx,hy,hz,right.x*headSize,right.y*headSize,right.z*headSize,up.x*headSize,up.y*headSize,up.z*headSize,hu0,hu1,hv0,hv1,opacity,GuardParticleColors.tracer(1-headFadeOpacity,startColor,middleColor,endColor));
    }
    private static void square(VertexConsumer out,double x,double y,double z,double rx,double ry,double rz,double ux,double uy,double uz,float u0,float u1,float v0,float v1,float opacity,int color) {
        vertex(out,x-rx-ux,y-ry-uy,z-rz-uz,u1,v1,opacity,color);vertex(out,x-rx+ux,y-ry+uy,z-rz+uz,u1,v0,opacity,color);
        vertex(out,x+rx+ux,y+ry+uy,z+rz+uz,u0,v0,opacity,color);vertex(out,x+rx-ux,y+ry-uy,z+rz-uz,u0,v1,opacity,color);
    }
    private static void vertex(VertexConsumer out,double x,double y,double z,float u,float v,float alpha,int color) {
        out.addVertex((float)x,(float)y,(float)z).setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha).setUv(u,v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,1,0);
    }
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites){this.sprites=sprites;}
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            if(GuardParryParticleRenderer.availableCapacity(level)<=0)return null;
                var head=new GuardSparkTracerParticle(level,x,y,z,dx,dy,dz,sprites);HEADS.add(head);return head;
        }
    }
    }
}
