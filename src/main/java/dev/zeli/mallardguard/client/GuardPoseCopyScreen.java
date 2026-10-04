package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPoseLibrary;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuardPoseCopyScreen extends Screen {
    private final Screen parent;
    private final List<GuardPoseLibrary.Pose> poses;
    private final String current;
    private final Consumer<GuardPoseLibrary.Pose> accept;
    private int left,top,panelWidth,rows,start,panelHeight;
    GuardPoseCopyScreen(Screen parent,List<GuardPoseLibrary.Pose> poses,String current,Consumer<GuardPoseLibrary.Pose> accept){
        super(Component.literal("Copy Preset"));this.parent=parent;this.poses=poses.stream().map(GuardPoseLibrary.Pose::copy).toList();this.current=current;this.accept=accept;
    }
    @Override protected void init(){
        panelWidth=Math.min(360,width-24);rows=Math.max(1,Math.min(8,(height-84)/24));panelHeight=rows*24+64;
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;start=Math.clamp(start,0,Math.max(0,poses.size()-rows));
        for(int i=start;i<Math.min(poses.size(),start+rows);i++){
            var pose=poses.get(i);var button=addRenderableWidget(GuardUi.button(pose.name,left+8,top+28+(i-start)*24,panelWidth-16,20,()->{accept.accept(pose.copy());Minecraft.getInstance().setScreen(parent);}));
            button.active=!pose.id.equals(current);button.setTooltip(Tooltip.create(Component.literal(button.active?"Copy this preset into the current draft. Save to commit.":"You are already editing this preset.")));
        }
        addRenderableWidget(GuardUi.button("Back",left+8,top+32+rows*24,panelWidth-16,20,this::onClose));
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(x>=left&&x<left+panelWidth&&y>=top+28&&y<top+28+rows*24){start=Math.clamp(start-(int)Math.signum(sy),0,Math.max(0,poses.size()-rows));clearWidgets();init();return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){
        g.flush();renderBackground(g,x,y,delta);g.flush();GuardUi.panel(g,left,top,panelWidth,panelHeight,false);
        g.drawCenteredString(font,title,width/2,top+10,0xFFF5F0F6);for(var widget:renderables)widget.render(g,x,y,delta);
    }
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
