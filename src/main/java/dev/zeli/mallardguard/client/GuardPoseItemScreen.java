package dev.zeli.mallardguard.client;

import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Registry-backed creative item picker. Empty assignments use server guard eligibility. */
final class GuardPoseItemScreen extends Screen {
    private final Screen parent;
    private final BiConsumer<List<String>,Integer> accept;
    private final LinkedHashSet<String> selected;
    private int handMode;
    private final boolean showHandMode;
    private String filter="";
    private EditBox search;
    private int left,top,panelWidth,scroll,rows,columns,bottom;
    private final List<Item> all=new ArrayList<>(),matches=new ArrayList<>();
    private record State(List<String> items,int handMode){}
    private State saved;
    GuardPoseItemScreen(Screen parent,List<String> items,int handMode,BiConsumer<List<String>,Integer> accept){this(parent,items,handMode,accept,true);}
    GuardPoseItemScreen(Screen parent,List<String> items,int handMode,BiConsumer<List<String>,Integer> accept,boolean showHandMode){super(Component.literal("Assign items"));this.parent=parent;this.accept=accept;this.showHandMode=showHandMode;selected=new LinkedHashSet<>(items);this.handMode=handMode;saved=new State(List.copyOf(selected),handMode);for(Item i:BuiltInRegistries.ITEM)if(i!=Items.AIR)all.add(i);filter();}


    private void apply(boolean close){accept.accept(List.copyOf(selected),handMode);saved=new State(List.copyOf(selected),handMode);if(close)onClose();else Minecraft.getInstance().setScreen(this);}
    private void filter(){matches.clear();String q=filter.toLowerCase(Locale.ROOT);for(Item i:all)if(BuiltInRegistries.ITEM.getKey(i).toString().contains(q)||i.getDescription().getString().toLowerCase(Locale.ROOT).contains(q))matches.add(i);scroll=0;}
    private void rebuild(){clearWidgets();init();}
    @Override protected void init(){
        panelWidth=Math.min(360,width-32);left=(width-panelWidth)/2;columns=Math.max(1,(panelWidth-8)/23);rows=Math.max(1,Math.min(6,(height-168)/23));int panelHeight=148+rows*23;top=Math.max(8,(height-panelHeight)/2);bottom=top+panelHeight;
        search=addRenderableWidget(new EditBox(font,left+6,top+23,panelWidth-12,20,Component.literal("Search creative items")){
            @Override public boolean keyPressed(int key,int scan,int mods){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_A&&(mods&org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL)!=0)return true;return super.keyPressed(key,scan,mods&~org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT);}
            @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return false;}
            @Override public void renderWidget(GuiGraphics g,int x,int y,float d){g.fill(getX(),getY(),getX()+getWidth(),getY()+20,0xA0211B2A);g.renderOutline(getX(),getY(),getWidth(),20,0xFF947D9A);g.drawString(GuardPoseItemScreen.this.font,GuardPoseItemScreen.this.font.plainSubstrByWidth(getValue().isEmpty()?"Search name or mod:item":getValue(),getWidth()-8),getX()+4,getY()+6,0xFFF5F0F6);}
        });search.setMaxLength(128);search.setValue(filter);search.setResponder(s->{filter=s;filter();});
        Button hands=addRenderableWidget(GuardUi.button("Hand: "+new String[]{"Off","Combine","Strict"}[handMode],left+6,bottom-91,panelWidth-12,20,()->{handMode=(handMode+1)%3;rebuild();}));
        hands.visible=showHandMode;hands.active=showHandMode;
        hands.setTooltip(Tooltip.create(Component.literal("Off excludes empty hands. Combine adds empty hands. Strict uses empty hands only. Server guard rules still apply. Default: Off.")));
        int w=(panelWidth-24)/3;
        addRenderableWidget(GuardUi.button("Reset",left+6,bottom-39,w,20,()->GuardUi.confirm(this,"Reset assignments?","Clear assignments and set Hand to Off. Unassigned poses use damage tools without a use action.",()->{selected.clear();handMode=0;})));
        addRenderableWidget(GuardUi.button("Apply",left+12+w,bottom-39,w,20,()->GuardUi.apply(this,()->apply(false),()->apply(true))));
        addRenderableWidget(GuardUi.button("Close",left+18+2*w,bottom-39,w,20,this::onClose));

    }
    @Override public boolean mouseClicked(double x,double y,int b){
        if(b==0&&y>=top+49&&y<top+49+rows*23&&x>=left+5&&x<left+5+columns*23){int c=(int)(x-left-5)/23,r=(int)(y-top-49)/23,i=(scroll+r)*columns+c;if(i<matches.size()){String id=BuiltInRegistries.ITEM.getKey(matches.get(i)).toString();if(!selected.remove(id))selected.add(id);return true;}}
        return super.mouseClicked(x,y,b);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(x<left||x>=left+panelWidth||y<top+49||y>=top+49+rows*23)return super.mouseScrolled(x,y,sx,sy);scroll=Math.clamp(scroll-(int)Math.signum(sy),0,Math.max(0,(matches.size()+columns-1)/columns-rows));return true;}
    @Override public void render(GuiGraphics g,int x,int y,float d){g.flush();renderBackground(g,x,y,d);g.flush();GuardUi.panel(g,left-4,top-5,panelWidth+8,bottom-top-2,false);g.drawCenteredString(font,"ASSIGN ITEMS · "+selected.size()+" selected",width/2,top+5,0xFFF5F0F6);Item hovered=null;
        for(int r=0;r<rows;r++)for(int c=0;c<columns;c++){int i=(scroll+r)*columns+c;if(i>=matches.size())continue;Item item=matches.get(i);int px=left+5+c*23,py=top+49+r*23;boolean over=x>=px&&x<px+21&&y>=py&&y<py+21;String id=BuiltInRegistries.ITEM.getKey(item).toString();g.fill(px,py,px+21,py+21,selected.contains(id)?0xBB806891:0x77211B2A);g.renderOutline(px,py,21,21,over?0xFFD8BEAA:0xFF947D9A);g.renderItem(item.getDefaultInstance(),px+2,py+2);if(over)hovered=item;}
        g.flush();for(var widget:renderables)widget.render(g,x,y,d);if(hovered!=null)g.renderTooltip(font,List.of(hovered.getDescription().getVisualOrderText(),Component.literal(BuiltInRegistries.ITEM.getKey(hovered).toString()).getVisualOrderText()),x,y);
    }
    @Override public void onClose(){if(handMode!=saved.handMode()||!selected.equals(new LinkedHashSet<>(saved.items())))GuardUi.choices(this,"Unsaved assignments","Apply assignments or discard these edits.",new GuardUi.Choice("Apply & Close",()->apply(true)),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));else Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
