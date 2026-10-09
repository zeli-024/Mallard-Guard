package dev.zeli.mallardguard.client;

/** Only the current tick's physical path, including at most two impact contacts. */
final class GuardSparkPath {
    final double[] points=new double[12],times=new double[4],lengths=new double[2];
    int count;
    double totalLength;
    void clear() { count=0;totalLength=0; }
    void add(double ax,double ay,double az,double bx,double by,double bz,double from,double to) {
        double dx=bx-ax,dy=by-ay,dz=bz-az,len=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(count==2 || len<1.0E-6 || to<=from)return;
        int p=count*6,t=count*2;
        points[p]=ax;points[p+1]=ay;points[p+2]=az;points[p+3]=bx;points[p+4]=by;points[p+5]=bz;
        times[t]=from;times[t+1]=to;lengths[count]=len;totalLength+=len;count++;
    }
    double fraction(int segment,double partial) {
        return Math.max(0,Math.min(1,(partial-times[segment*2])/(times[segment*2+1]-times[segment*2])));
    }
}
