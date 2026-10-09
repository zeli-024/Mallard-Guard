package dev.zeli.mallardguard.client;

/** HSV conversions and triangle coordinates shared by the color picker. */
final class GuardColorPickerMath {
    private GuardColorPickerMath() {}
    static int rgb(double hue,double saturation,double value) {
        hue=hue-Math.floor(hue);saturation=clamp(saturation);value=clamp(value);
        double h=hue*6,f=h-Math.floor(h),p=value*(1-saturation),q=value*(1-f*saturation),t=value*(1-(1-f)*saturation);
        double r,g,b;
        switch((int)h){case 0->{r=value;g=t;b=p;}case 1->{r=q;g=value;b=p;}case 2->{r=p;g=value;b=t;}case 3->{r=p;g=q;b=value;}case 4->{r=t;g=p;b=value;}default->{r=value;g=p;b=q;}}
        return (int)Math.round(r*255)<<16|(int)Math.round(g*255)<<8|(int)Math.round(b*255);
    }
    static double[] hsv(int rgb) {
        double r=(rgb>>16&255)/255.0,g=(rgb>>8&255)/255.0,b=(rgb&255)/255.0,max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),d=max-min,h=.1;
        if(d>0){h=max==r?((g-b)/d)%6:max==g?(b-r)/d+2:(r-g)/d+4;h/=6;if(h<0)h+=1;}
        return new double[]{h,max==0?0:d/max,max};
    }
    // Triangle corners: selected hue at (106,64), white at (38,28), black at (38,100).
    static double[] weights(double x,double y){double pure=(x-38)/68,black=(y-28-36*pure)/72;return new double[]{pure,1-pure-black,black};}
    static double[] selection(double x,double y){double[] w=weights(x,y);for(int i=0;i<3;i++)w[i]=Math.max(0,w[i]);double sum=w[0]+w[1]+w[2],v=(w[0]+w[1])/sum;return new double[]{v==0?0:w[0]/(w[0]+w[1]),v};}
    static double[] point(double saturation,double value){double pure=saturation*value,white=(1-saturation)*value,black=1-value;return new double[]{106*pure+38*(white+black),64*pure+28*white+100*black};}
    private static double clamp(double v){return Math.max(0,Math.min(1,v));}
}
