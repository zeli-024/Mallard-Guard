package dev.zeli.mallardguard;

/** Optional client event logging; bounded suppression prevents repeated packet spam. */
public final class GuardDiagnostics {
    public static final int GUARD=1,DAMAGE=2,ANIMATION=4,CONFIG=8,MOB=16,PUNCHY_REACTION=32,PUNCHY_HANDS=64,PUNCHY_PRIORITY=128;
    public static final int PARTICLE_COLLISIONS=256;
    public static final int DEFAULT_EVENT_GROUPS=255, ALL=511;
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("Mallard Guard/Debug");
    private static final String[] LAST=new String[9];
    private static final long[] TIMES=new long[9];
    private GuardDiagnostics(){}
    public static boolean enabled(int group){return (GuardConfig.DEBUG_CLIENT_FLAGS.get()&group)!=0;}
    public static void event(int group,String message){
        if(!enabled(group))return;
        int index=Integer.numberOfTrailingZeros(group);if(index<0||index>=LAST.length)return;
        long now=System.nanoTime();
        if(message.equals(LAST[index])&&now-TIMES[index]<2_000_000_000L)return;
        LAST[index]=message;TIMES[index]=now;LOGGER.info("{}",message);
    }
    public static void clear(){java.util.Arrays.fill(LAST,null);java.util.Arrays.fill(TIMES,0);}
}
