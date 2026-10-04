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
    private GuardUiLayout() {}
}
