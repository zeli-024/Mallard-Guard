package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardDamageRules;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Runtime damage rules edited as part of the main screen's unsaved draft. */
final class GuardDamageSourcesScreen extends Screen {
    private static final int MAX_WIDTH=520, MAX_HEIGHT=350, MARGIN=12, INSET=10;
    private static final int ROW_HEIGHT=24, ROW_CONTROL_HEIGHT=20, TOGGLE_WIDTH=66;
    private static final int LIST_TOP=94, FOOTER_HEIGHT=36, TRACK_WIDTH=8, TRACK_GAP=8;
    private static final int EDGE=0xFF947D9A, TEXT=0xFFF5F0F6, MUTED=0xFFA7A0B0;
    private static final int ENABLED=0xFFB6E8B6, DISABLED=0xFFA7A7A7;
    private static final int SURFACE=0xA4493E55, LIST_SURFACE=0x78302A39;
    private final Screen parent;
    private final List<String> recent, catalog;
    private final Map<String,Boolean> rules;
    private final boolean editable;
    private final Function<String,String> modName;
    private final Consumer<Map<String,Boolean>> accept;
    private int left,top,w,h,offset,rows;
    private boolean all,draggingScroll;
    private double scrollGrab;
    private String query="";
    private List<String> filtered=List.of();
    private final List<String> rowIds=new ArrayList<>();

    GuardDamageSourcesScreen(Screen parent,List<String> recent,List<String> catalog,Map<String,Boolean> rules,boolean editable,Function<String,String> modName,Consumer<Map<String,Boolean>> accept){
        super(Component.literal("Damage Sources"));
        this.parent=parent;this.recent=recent.stream().distinct().limit(10).toList();
        this.catalog=List.copyOf(catalog);this.rules=new LinkedHashMap<>(rules);
        this.editable=editable;this.modName=modName;this.accept=accept;
    }
    private boolean enabled(String id){return rules.getOrDefault(id,GuardDamageRules.defaultAllowed(id));}
    private void filter(){
        String[] terms=query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        filtered=(all?catalog:recent).stream().filter(id->{
            String key=id.toLowerCase(Locale.ROOT),mod=modName.apply(id).toLowerCase(Locale.ROOT);
            for(String term:terms){
                if(term.isEmpty())continue;
                if(term.equals("#enabled")){if(!enabled(id))return false;}
                else if(term.equals("#disabled")){if(enabled(id))return false;}
                else if(term.startsWith("@")){
                    if(!mod.contains(term.substring(1))&&!key.split(":",2)[0].contains(term.substring(1)))return false;
                }else if(!key.contains(term)&&!mod.contains(term))return false;
            }
            return true;
        }).toList();
    }
    private int rowTop(){return top+LIST_TOP;}
    private int listBottom(){return rowTop()+rows*ROW_HEIGHT;}
    private int trackX(){return left+w-INSET-TRACK_WIDTH;}
    private int toggleX(){return trackX()-TRACK_GAP-TOGGLE_WIDTH;}
    private int sourceX(){return left+INSET+Math.min(90,w/5)+8;}
    private int maxScroll(){return Math.max(0,filtered.size()-rows);}
    private int trackHeight(){return rows*ROW_HEIGHT;}
    private int thumbHeight(){return Math.max(16,trackHeight()*rows/Math.max(1,filtered.size()));}
    private int thumbY(){return rowTop()+(maxScroll()==0?0:offset*(trackHeight()-thumbHeight())/maxScroll());}

    @Override protected void init(){
        w=Math.min(MAX_WIDTH,width-2*MARGIN);h=Math.min(MAX_HEIGHT,height-2*MARGIN);
        left=(width-w)/2;top=(height-h)/2;
        rows=Math.max(1,(h-LIST_TOP-FOOTER_HEIGHT)/ROW_HEIGHT);
        filter();offset=Math.clamp(offset,0,maxScroll());rowIds.clear();draggingScroll=false;
        int half=(w-2*INSET-6)/2;
        var recentButton=addRenderableWidget(GuardUi.button("Recent Hits",left+INSET,top+28,half,20,()->selectList(false)));
        recentButton.active=all;
        var allButton=addRenderableWidget(GuardUi.button("All Sources",left+INSET+half+6,top+28,half,20,()->selectList(true)));
        allButton.active=!all;
        EditBox search=addRenderableWidget(new EditBox(font,left+INSET,top+54,w-2*INSET,20,Component.literal("Search damage sources")));
        search.setMaxLength(128);search.setHint(Component.literal("name, @mod, #enabled, #disabled"));search.setValue(query);
        search.setTooltip(Tooltip.create(Component.literal("Filter this list by source ID, name or @mod. Use #enabled or #disabled to filter parry rules.")));
        search.setResponder(text->{query=text;offset=0;filter();refreshRows();});
        refreshRows();
        var done=addRenderableWidget(GuardUi.button("Done",left+w-INSET-78,top+h-INSET-20,78,20,this::onClose));
        done.setTooltip(Tooltip.create(Component.literal("Return to settings. Use Apply there to save these rules.")));
    }
    private void selectList(boolean showAll){all=showAll;query="";offset=0;clearWidgets();init();}
    private void refreshRows(){
        for(var child:List.copyOf(children()))if(child instanceof SourceButton)removeWidget(child);
        rowIds.clear();offset=Math.clamp(offset,0,maxScroll());
        for(int i=0;i<rows&&offset+i<filtered.size();i++){
            String id=filtered.get(offset+i);rowIds.add(id);
            SourceButton b=addRenderableWidget(new SourceButton(toggleX(),rowTop()+i*ROW_HEIGHT+2,id));
            b.active=editable;
            b.setTooltip(Tooltip.create(Component.literal("Mod: "+modName.apply(id)+"\n"+id+"\nDefault: "+(GuardDamageRules.defaultAllowed(id)?"On":"Off")
                +(editable?". Save with Apply in settings.":". Server operator permission required."))));
        }
    }
    private final class SourceButton extends Button {
        SourceButton(int x,int y,String id){
            super(x,y,TOGGLE_WIDTH,ROW_CONTROL_HEIGHT,Component.literal(enabled(id)?"On":"Off"),b->{
                rules.put(id,!enabled(id));filter();refreshRows();
            },message->message.get());
        }
        @Override public void renderWidget(GuiGraphics g,int x,int y,float d){GuardUi.paint(g,this,x,y,false,false);}
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        renderBackground(g,x,y,d);
        g.fill(left,top,left+w,top+h,SURFACE);g.renderOutline(left,top,w,h,EDGE);
        g.drawCenteredString(font,title,width/2,top+10,TEXT);
        int listLeft=left+INSET-2,listRight=left+w-INSET+2;
        g.fill(listLeft,top+80,listRight,listBottom()+2,LIST_SURFACE);
        g.renderOutline(listLeft,top+80,listRight-listLeft,listBottom()+2-(top+80),EDGE);
        g.drawString(font,"Mod",left+INSET+4,top+83,MUTED);
        g.drawString(font,"Damage Source",sourceX(),top+83,MUTED);
        g.drawCenteredString(font,"Parry",toggleX()+TOGGLE_WIDTH/2,top+83,MUTED);
        String hoveredId=null;
        for(int i=0;i<rowIds.size();i++){
            String id=rowIds.get(i);int ry=rowTop()+i*ROW_HEIGHT;
            boolean hovered=x>=listLeft&&x<trackX()-TRACK_GAP&&y>=ry&&y<ry+ROW_HEIGHT;
            if(hovered||(i&1)==0)g.fill(listLeft+1,ry,listRight-1,ry+ROW_HEIGHT,hovered?0x51493E55:0x183D3349);
            int color=enabled(id)?ENABLED:DISABLED;
            g.drawString(font,font.plainSubstrByWidth(modName.apply(id),sourceX()-left-INSET-12),left+INSET+4,ry+8,MUTED);
            g.drawString(font,font.plainSubstrByWidth(id,Math.max(0,toggleX()-sourceX()-8)),sourceX(),ry+8,color);
            if(hovered&&x<toggleX())hoveredId=id;
        }
        if(rowIds.isEmpty())g.drawCenteredString(font,query.isBlank()&&!all?"No recent damage sources":"No matching sources",width/2,rowTop()+12,MUTED);
        if(maxScroll()>0){
            g.fill(trackX(),rowTop(),trackX()+TRACK_WIDTH,listBottom(),0x65302737);
            int sy=thumbY(),sh=thumbHeight();boolean hover=x>=trackX()&&x<trackX()+TRACK_WIDTH&&y>=sy&&y<sy+sh;
            g.fill(trackX()+1,sy,trackX()+TRACK_WIDTH-1,sy+sh,hover||draggingScroll?0xFFE9D7C5:0xFFD8BEAA);
            g.fill(trackX()+TRACK_WIDTH-2,sy,trackX()+TRACK_WIDTH-1,sy+sh,0xFFB29A89);
        }
        g.drawString(font,filtered.size()+" source"+(filtered.size()==1?"":"s")+(editable?"":" · Read only"),left+INSET,top+h-INSET-14,MUTED);
        for(var widget:renderables)widget.render(g,x,y,d);
        if(hoveredId!=null)g.renderTooltip(font,List.of(Component.literal("Mod: "+modName.apply(hoveredId)),Component.literal(hoveredId)),Optional.empty(),x,y);
    }
    private void dragScroll(double y){
        int travel=trackHeight()-thumbHeight();
        if(travel<=0)return;
        int next=Math.clamp((int)Math.round((y-rowTop()-scrollGrab)*maxScroll()/travel),0,maxScroll());
        if(next!=offset){offset=next;refreshRows();}
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&maxScroll()>0&&x>=trackX()-2&&x<trackX()+TRACK_WIDTH+2&&y>=rowTop()&&y<listBottom()){
            draggingScroll=true;
            scrollGrab=y>=thumbY()&&y<thumbY()+thumbHeight()?y-thumbY():thumbHeight()/2.0;
            dragScroll(y);setDragging(true);return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&draggingScroll){dragScroll(y);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&draggingScroll){draggingScroll=false;setDragging(false);return true;}
        return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(x>=left+INSET&&x<left+w-INSET&&y>=rowTop()&&y<listBottom()&&sy!=0){
            int next=Math.clamp(offset-(int)Math.signum(sy),0,maxScroll());
            if(next!=offset){offset=next;refreshRows();}return true;
        }
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void onClose(){accept.accept(new LinkedHashMap<>(rules));Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
