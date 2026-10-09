package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.zeli.mallardguard.GuardParticleConfig;
import dev.zeli.mallardguard.GuardParticleColors;
import dev.zeli.mallardguard.GuardParticles;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Vector3f;

/** Shared particle implementation with distinct Flying Sparks and Spark Debris behavior. */
@EventBusSubscriber(modid=MallardGuard.ID,value=Dist.CLIENT)
public final class GuardParryParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final int tailSamples;
    private final float tailSize;
    private final double bounceStrength,bounceRetention,surfaceRetention;
    enum Type { FLYING_SPARK, SPARK_DEBRIS }
    private final Type type;
    private final double colorSample;
    private final boolean electric;
    private final double[] tailPositions;
    private final int[] tailAges;
    private final int headLifetime;
    private int tailCursor,tailCount;
    private double tailReach,eyeX,eyeY,eyeZ;
    private final int fadeTicks,dropDelay,animationFrameTicks;
    private final double retention,acceleration;
    private int animationFrame,nextAnimationTick,quietSurfaceContacts,impacts;
    private boolean stopped;
    private float headX,headY,headZ,u0,u1,v0,v1;
    long lastTick;

    private GuardParryParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,Type type) {
        super(level,x,y,z);this.sprites=sprites;this.type=type;xd=dx;yd=dy;zd=dz;hasPhysics=false;
        tailSamples=type==Type.SPARK_DEBRIS?GuardParticleConfig.DEBRIS_TAIL:0;
        tailSize=type==Type.SPARK_DEBRIS?GuardParticleConfig.DEBRIS_TAIL_SIZE/100.0F:0;
        bounceStrength=type==Type.SPARK_DEBRIS?GuardParticleConfig.DEBRIS_BOUNCE/100.0:0;
        bounceRetention=type==Type.SPARK_DEBRIS?1-GuardParticleConfig.DEBRIS_DECAY/100.0:0;
        surfaceRetention=type==Type.SPARK_DEBRIS?1-GuardParticleConfig.DEBRIS_FRICTION/100.0:0;
        tailPositions=type==Type.SPARK_DEBRIS?new double[tailSamples*3]:null;
        tailAges=type==Type.SPARK_DEBRIS?new int[tailSamples]:null;
        if(type==Type.SPARK_DEBRIS) {
            double motionVariation=GuardParticleConfig.DEBRIS_MOTION_VARIANCE/100.0;
            lifetime=Math.max(1,(int)Math.round(GuardParticleColors.get(GuardParticleColors.DEBRIS_LIFETIME)*(1+(random.nextDouble()*2-1)*0.25*motionVariation)));
            fadeTicks=Math.min(lifetime,Math.max(0,(int)Math.round(GuardParticleConfig.DEBRIS_FADE*(1+(random.nextDouble()*2-1)*(2.0/7)*GuardParticleConfig.DEBRIS_FADE_VARIANCE/100.0))));
            dropDelay=GuardParticleConfig.DEBRIS_DELAY;
            double loss=GuardParticleConfig.DEBRIS_LOSS*(1+(random.nextDouble()*2-1)*(0.0125/0.0325)*motionVariation);
            retention=GuardParryParticleMath.velocityRetention(Math.clamp(loss,0.0,100.0));
            acceleration=0.001*GuardParticleConfig.DEBRIS_GRAVITY*(1+(random.nextDouble()*2-1)*(0.0125/0.0775)*motionVariation);
            quadSize=0.0005F*GuardParticleConfig.DEBRIS_SIZE*GuardParticleColors.get(GuardParticleColors.DEBRIS_SIZE)/100F*(1+(random.nextFloat()*2-1)*0.20F*GuardParticleConfig.DEBRIS_SIZE_VARIANCE/100.0F);
        } else {
            boolean lingering=random.nextFloat()<0.12F;
            lifetime=GuardParryParticleMath.flyingSparkLifetime(GuardParticleColors.get(GuardParticleColors.FLYING_LIFETIME),random.nextDouble(),lingering);
            fadeTicks=GuardParryParticleMath.flyingSparkFadeDuration(GuardParticleConfig.FADE_TICKS,lifetime,random.nextDouble(),lingering);
            dropDelay=Math.max(0,GuardParticleConfig.DROP_DELAY_TICKS+random.nextInt(7)-3);
            double speed=Math.sqrt(dx*dx+dy*dy+dz*dz);
            double fastness=Math.min(1,speed/(1.30*GuardParticleConfig.SPEED_SCALE));
            retention=GuardParryParticleMath.velocityRetention(GuardParticleConfig.VELOCITY_LOSS*(0.80+random.nextDouble()*0.40-0.10*(fastness-0.5)));
            acceleration=0.035*GuardParticleConfig.GRAVITY_PERCENT/100.0*(0.90+random.nextDouble()*0.20);
            // 50% retains the selected 0.55–1.60 spawn-size range.
            float variation=(random.nextFloat()*2-1)*0.525F*GuardParticleColors.get(GuardParticleColors.FLYING_SIZE_VARIANCE)/50F;
            quadSize=0.095F*(float)GuardParticleConfig.SIZE_SCALE*(1.075F+variation)*GuardParticleColors.get(GuardParticleColors.FLYING_SIZE)/100F;
        }
        colorSample=random.nextDouble();
        setBaseColor(GuardParticleColors.FLYING_COLOR.get(),GuardParticleColors.get(GuardParticleColors.DEBRIS_COLOR));
        headLifetime=lifetime;
        if(type==Type.SPARK_DEBRIS)lifetime+=tailSamples;
        electric=type==Type.FLYING_SPARK && random.nextFloat()<0.20F;
        animationFrame=random.nextInt(8);animationFrameTicks=4+random.nextInt(4);
        nextAnimationTick=1+random.nextInt(animationFrameTicks);
        updateSprite();lastTick=level.getGameTime();
    }
    void setBaseColor(int flying, int debris) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            int color=type==Type.FLYING_SPARK?GuardParticleColors.flying(flying,colorSample):GuardParticleColors.debris(debris,colorSample);
            setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f);
        }
    }
    boolean belongsTo(ClientLevel current) { return level==current; }
    double distanceSquared(Vec3 eye) { double a=x-eye.x,b=y-eye.y,c=z-eye.z;return a*a+b*b+c*c; }
    boolean visible(Vec3 eye,Vector3f backward,double distanceSquared) {
        double radius=64+quadSize+tailReach;if(distanceSquared>=radius*radius)return false;
        double padding=quadSize+tailReach+Math.abs(x-xo)+Math.abs(y-yo)+Math.abs(z-zo);
        return (x-eye.x)*backward.x+(y-eye.y)*backward.y+(z-eye.z)*backward.z<=padding;
    }
    @Override public void tick() {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            lastTick=level.getGameTime();xo=x;yo=y;zo=z;
            if(age++>=lifetime){remove();return;}
            if(!stopped && age<=headLifetime) {
                if(type==Type.SPARK_DEBRIS && tailSamples>0 && tailSize>0)rememberHead();
                if(age>dropDelay && acceleration!=0)yd-=acceleration;
                moveHead();xd*=retention;yd*=retention;zd*=retention;
                if(type==Type.SPARK_DEBRIS)updateTailReach();
            }
            if(type==Type.FLYING_SPARK && !electric && age>=nextAnimationTick){animationFrame=(animationFrame+1)%8;nextAnimationTick+=animationFrameTicks;updateSprite();}
        }
    }
    private void rememberHead() {
        int base=tailCursor*3;
        tailPositions[base]=x;tailPositions[base+1]=y;tailPositions[base+2]=z;
        tailAges[tailCursor]=age-1;
        tailCursor=(tailCursor+1)%tailSamples;tailCount=Math.min(tailSamples,tailCount+1);
    }
    private void updateTailReach() {
        tailReach=0;
        for(int i=0;i<tailCount;i++) {
            int base=i*3;
            double distance=Math.abs(x-tailPositions[base])+Math.abs(y-tailPositions[base+1])+Math.abs(z-tailPositions[base+2]);
            tailReach=Math.max(tailReach,distance);
        }
    }
    private void updateSprite() {
        setSprite(type==Type.SPARK_DEBRIS?sprites.get(0,1):sprites.get(electric?8:animationFrame,8));
        u0=getU0();u1=getU1();v0=getV0();v1=getV1();
    }
    private void settle() { stopped=true;xd=yd=zd=0; }
    private void moveHead() {
        double speedSquared=xd*xd+yd*yd+zd*zd;
        if(speedSquared==0)return;
        if(speedSquared<=0.25) { moveSlow();return; }
        moveFast();
    }
    private boolean restingContact(boolean supported,int axes,double incomingSpeedSquared) {
        if(axes==0){quietSurfaceContacts=0;return false;}
        if(type==Type.SPARK_DEBRIS) {
            if(supported && impacts>=2 && Math.abs(yd)<=acceleration*1.6 && xd*xd+zd*zd<0.01) {
                settle();return true;
            }
            return false;
        }
        if(supported && incomingSpeedSquared<0.0025){settle();return true;}
        // A wall alone cannot support a falling spark. Require repeated multi-axis contacts
        // before sleeping a low-energy particle whose motion has no gravity.
        if(acceleration==0 && axes>=2 && incomingSpeedSquared<0.0009) {
            if(++quietSurfaceContacts>=2){settle();return true;}
        } else quietSurfaceContacts=0;
        return false;
    }
    private void moveSlow() {
        double dx=xd,dy=yd,dz=zd;
        // A small collision volume follows the visible point instead of the sprite's width.
        long profileStart=GuardParticleCollisionProfiler.begin();
        AABB box=new AABB(x-0.005,y-0.005,z-0.005,x+0.005,y+0.005,z+0.005);
        Vec3 motion=Entity.collideBoundingBox(null,new Vec3(dx,dy,dz),box,level,java.util.List.of());
        if(profileStart!=0)GuardParticleCollisionProfiler.record(type,GuardParticleCollisionProfiler.SLOW,profileStart,
            Math.abs(dx-motion.x)>1.0E-9 || Math.abs(dy-motion.y)>1.0E-9 || Math.abs(dz-motion.z)>1.0E-9);
        setPos(x+motion.x,y+motion.y,z+motion.z);
        boolean hitX=Math.abs(dx-motion.x)>1.0E-9;
        boolean hitY=Math.abs(dy-motion.y)>1.0E-9;
        boolean hitZ=Math.abs(dz-motion.z)>1.0E-9;
        int axes=(hitX?1:0)+(hitY?1:0)+(hitZ?1:0);
        if(restingContact(hitY && dy<0,axes,dx*dx+dy*dy+dz*dz))return;
        if(axes!=0) {
            // Reflect blocked components; retain the existing energy loss on each impact.
            if(type==Type.SPARK_DEBRIS) {
                double rebound=debrisRebound();
                xd=hitX?-dx*rebound:dx*surfaceRetention;
                yd=hitY?-dy*rebound:dy*surfaceRetention;
                zd=hitZ?-dz*rebound:dz*surfaceRetention;
            } else {
                xd=(hitX?-dx:dx)*0.62;yd=(hitY?-dy:dy)*0.62;zd=(hitZ?-dz:dz)*0.62;
            }
        }
    }
    private double debrisRebound() {
        return bounceStrength*Math.pow(bounceRetention,Math.min(impacts++,8));
    }
    private void moveFast() {
        var player=Minecraft.getInstance().player;
        if(player==null)return;
        double remaining=1;
        // Check actual movement every tick; at most two contacts bound corner work.
        for(int contact=0;contact<2 && remaining>0.0001;contact++) {
            long profileStart=GuardParticleCollisionProfiler.begin();
            Vec3 start=new Vec3(x,y,z),end=start.add(xd*remaining,yd*remaining,zd*remaining);
            var hit=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            boolean blocked=hit.getType()==HitResult.Type.BLOCK;
            if(profileStart!=0)GuardParticleCollisionProfiler.record(type,contact==0?GuardParticleCollisionProfiler.FAST_FIRST:GuardParticleCollisionProfiler.FAST_SECOND,profileStart,blocked);
            if(!blocked){setPos(end.x,end.y,end.z);quietSurfaceContacts=0;return;}
            Vec3 point=hit.getLocation();double dx=end.x-start.x,dy=end.y-start.y,dz=end.z-start.z;
            double lengthSquared=dx*dx+dy*dy+dz*dz;
            double fraction=lengthSquared<1.0E-16?0:Math.max(0,Math.min(1,((point.x-start.x)*dx+(point.y-start.y)*dy+(point.z-start.z)*dz)/lengthSquared));
            double nx=hit.getDirection().getStepX(),ny=hit.getDirection().getStepY(),nz=hit.getDirection().getStepZ();
            setPos(point.x+nx*0.003,point.y+ny*0.003,point.z+nz*0.003);
            if(hit.isInside()){settle();return;}
            if(restingContact(ny>0,1,xd*xd+yd*yd+zd*zd))return;
            double dot=xd*nx+yd*ny+zd*nz;
            if(type==Type.SPARK_DEBRIS) {
                double rebound=debrisRebound();
                xd=GuardParryParticleMath.debrisReflection(xd,nx,dot,rebound,surfaceRetention);
                yd=GuardParryParticleMath.debrisReflection(yd,ny,dot,rebound,surfaceRetention);
                zd=GuardParryParticleMath.debrisReflection(zd,nz,dot,rebound,surfaceRetention);
            } else {
                xd=GuardParryParticleMath.flyingSparkReflection(xd,nx,dot,0.62);yd=GuardParryParticleMath.flyingSparkReflection(yd,ny,dot,0.62);zd=GuardParryParticleMath.flyingSparkReflection(zd,nz,dot,0.62);
            }
            remaining*=1-fraction;
        }
    }
    void prepare(Camera camera,float partial) {
        Vec3 eye=camera.getPosition();eyeX=eye.x;eyeY=eye.y;eyeZ=eye.z;
        headX=(float)(xo+(x-xo)*partial-eye.x);headY=(float)(yo+(y-yo)*partial-eye.y);headZ=(float)(zo+(z-zo)*partial-eye.z);
    }
    void drawHead(VertexConsumer vertices,Vector3f right,Vector3f up,float partial) {
        float now=Math.max(0,age-1+partial);if(now>=lifetime)return;
        if(type==Type.SPARK_DEBRIS) {
            float size=quadSize*tailSize,rx=right.x*size,ry=right.y*size,rz=right.z*size,ux=up.x*size,uy=up.y*size,uz=up.z*size;
            for(int i=0;i<tailCount;i++) {
                float elapsed=now-tailAges[i];
                if(elapsed<0 || elapsed>=GuardParticleConfig.DEBRIS_TAIL_LIFETIME)continue;
                alpha=0.85F*(1-elapsed/GuardParticleConfig.DEBRIS_TAIL_LIFETIME);if(alpha<0.01F)continue;
                int base=i*3;
                float tx=(float)(tailPositions[base]-eyeX);
                float ty=(float)(tailPositions[base+1]-eyeY);
                float tz=(float)(tailPositions[base+2]-eyeZ);
                quad(vertices,tx,ty,tz,rx,ry,rz,ux,uy,uz);
            }
        }
        if(now>=headLifetime)return;
        alpha=GuardParryParticleMath.fadeOpacity(now,headLifetime,fadeTicks);if(alpha<0.01F)return;
        quad(vertices,right,up,headX,headY,headZ,quadSize);
    }
    private void quad(VertexConsumer vertices,Vector3f right,Vector3f up,float x,float y,float z,float size) {
        float rx=right.x*size,ry=right.y*size,rz=right.z*size,ux=up.x*size,uy=up.y*size,uz=up.z*size;
        quad(vertices,x,y,z,rx,ry,rz,ux,uy,uz);
    }
    private void quad(VertexConsumer vertices,float x,float y,float z,float rx,float ry,float rz,float ux,float uy,float uz) {
        vertex(vertices,x-rx-ux,y-ry-uy,z-rz-uz,u1,v1);vertex(vertices,x-rx+ux,y-ry+uy,z-rz+uz,u1,v0);
        vertex(vertices,x+rx+ux,y+ry+uy,z+rz+uz,u0,v0);vertex(vertices,x+rx-ux,y+ry-uy,z+rz-uz,u0,v1);
    }
    private void vertex(VertexConsumer vertices,float x,float y,float z,float u,float v) {
        vertices.addVertex(x,y,z).setColor(rCol,gCol,bCol,alpha).setUv(u,v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,1,0);
    }
    @Override public void remove() {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) { super.remove(); }
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.NO_RENDER; }
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GuardParticles.CENTER_IMPACT.get(), GuardImpactParticle.Provider::new);
        event.registerSpriteSet(GuardParticles.SPARK_TRACER.get(),GuardSparkTracerParticle.Provider::new);
        event.registerSpriteSet(GuardParticles.FLYING_SPARK.get(),sprites -> new Provider(sprites,Type.FLYING_SPARK));
        event.registerSpriteSet(GuardParticles.SPARK_DEBRIS.get(),sprites -> new Provider(sprites,Type.SPARK_DEBRIS));
    }
    private record Provider(SpriteSet sprites,Type type) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType particleType,ClientLevel level,double x,double y,double z,double dx,double dy,double dz) {
        synchronized (GuardParryParticleRenderer.STATE_LOCK) {
            if(!GuardParryParticleRenderer.hasRoom(level))return null;
                var spark=new GuardParryParticle(level,x,y,z,dx,dy,dz,sprites,type);GuardParryParticleRenderer.add(spark);return spark;
        }
    }
    }
}
