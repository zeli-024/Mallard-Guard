package dev.zeli.mallardguard.client;

/** Shared geometry for settings panels, dropdown arrows and per-setting actions. */
final class GuardUiLayout {
    static final int OUTER_MARGIN=8;
    static final int SIDEBAR_MIN=150, SIDEBAR_MAX=166, CONTENT_MAX=640;
    static final int PANEL_INSETS=40, SIDEBAR_LEFT_INSET=6, PANEL_GAP=20;
    static final int CONTROL_GAP=4, UNDO_SIZE=20;
    static final int ARROW_SIZE=7, ARROW_THICKNESS=1, ARROW_SLOT=18, SECTION_ARROW_WIDTH=20;
    static final float UNDO_ICON_SCALE=1.65F;
    static final int DIVIDER_INSET=20, DIVIDER_COLOR=0xFF907D9A;
    record Panels(int sidebarLeft,int sidebarWidth,int contentLeft,int contentWidth) {}
    static Panels panels(int screenWidth){
        int sidebar=Math.min(SIDEBAR_MAX,Math.max(SIDEBAR_MIN,screenWidth/4));
        int content=Math.max(12,Math.min(CONTENT_MAX,screenWidth-2*OUTER_MARGIN-sidebar-PANEL_INSETS));
        int left=(screenWidth-sidebar-content-PANEL_INSETS)/2+SIDEBAR_LEFT_INSET;
        return new Panels(left,sidebar,left+sidebar+PANEL_GAP,content);
    }
    static int wideWidth(int screenWidth){
        Panels p=panels(screenWidth);return Math.min(screenWidth-16,p.contentLeft()+p.contentWidth()+14-(p.sidebarLeft()-6));
    }
    static int screenTop(int screenHeight){return Math.min(14,Math.max(6,screenHeight/16));}
    static final int CONTROL_HEIGHT=20, ITEM_STEP=23, ITEM_SIZE=21, ASSIGNMENT_INSET=8;
    record Assignment(int left,int top,int browserWidth,int assignedLeft,int assignedWidth,int bottom,int tabWidth,int actionWidth) {
        int width(){return assignedLeft+assignedWidth-left;}
        int searchY(){return top+38;}
        int tabsY(){return top+64;}
        int ruleY(){return top+90;}
        int assignedY(boolean counts){return top+(counts?90:64);}
        int gridY(boolean filters,boolean controls){return top+(filters?94:controls?116:90);}
        int footerY(){return bottom-28;}
        int contentBottom(){return bottom-56;}
    }
    static Assignment assignment(int width,int height){
        int available=wideWidth(width),gap=12;
        int assigned=Math.max(128,(available-gap)*2/5),browser=available-gap-assigned;
        int left=(width-available)/2;
        return new Assignment(left,screenTop(height),browser,left+browser+gap,assigned,height-6,
            Math.min(58,(browser-88)/2),Math.min(80,(available-36)/3));
    }
    static final int SCROLL_WIDTH=8, SCROLL_MIN_THUMB=16;
    record Scrollbar(int x,int y,int height,int visible,int total,int offset) {
        int max(){return Math.max(0,total-visible);}
        int thumbHeight(){return Math.min(height,Math.max(SCROLL_MIN_THUMB,height*Math.max(1,visible)/Math.max(1,total)));}
        int thumbY(){return y+(max()==0?0:Math.max(0,Math.min(max(),offset))*(height-thumbHeight())/max());}
        boolean contains(double mx,double my){return max()>0&&mx>=x-2&&mx<x+SCROLL_WIDTH+2&&my>=y&&my<y+height;}
        double grab(double my){return my>=thumbY()&&my<thumbY()+thumbHeight()?my-y-Math.max(0,Math.min(max(),offset))*(height-thumbHeight())/(double)Math.max(1,max()):thumbHeight()/2.0;}
        int scrollAt(double my,double grab){int travel=height-thumbHeight();return travel<=0?0:Math.max(0,Math.min(max(),(int)Math.round((my-y-grab)*max()/travel)));}
    }
    private GuardUiLayout() {}
}
