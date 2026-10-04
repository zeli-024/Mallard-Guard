package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.*;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuardPoseNameScreen extends Screen {
    private final Screen parent;
    private final List<GuardPoseLibrary.Pose> poses;
    private final Consumer<String> accept;
    private String name;
    private int left,top,w;
    GuardPoseNameScreen(Screen parent,String name,List<GuardPoseLibrary.Pose> poses,Consumer<String> accept){
        super(Component.literal("Name Preset"));this.parent=parent;this.name=name;this.poses=poses;this.accept=accept;
    }
    @Override protected void init(){
        w=Math.min(340,width-24);left=(width-w)/2;top=(height-116)/2;
        EditBox box=addRenderableWidget(new EditBox(font,left+8,top+29,w-16,20,Component.literal("Preset name")){
            @Override public void renderWidget(GuiGraphics g,int x,int y,float d){g.fill(getX(),getY(),getX()+getWidth(),getY()+20,0xA0211B2A);g.renderOutline(getX(),getY(),getWidth(),20,isFocused()?0xFFD8BEAA:0xFF947D9A);g.drawString(GuardPoseNameScreen.this.font,GuardPoseNameScreen.this.font.plainSubstrByWidth(getValue(),getWidth()-12)+(isFocused()?"_":""),getX()+4,getY()+6,0xFFF5F0F6);}
        });
        box.setMaxLength(64);box.setValue(name);box.setResponder(value->name=value);
        box.setTooltip(Tooltip.create(Component.literal("Leave blank for a random unused name.")));
        addRenderableWidget(GuardUi.button("Randomize Name",left+8,top+53,w-16,18,()->box.setValue(GuardPoseNames.unused(poses))));
        int bw=(w-22)/2;
        addRenderableWidget(GuardUi.button("Save",left+8,top+87,bw,20,()->accept.accept(name.isBlank()?GuardPoseNames.unused(poses):name.strip())));
        addRenderableWidget(GuardUi.button("Back",left+14+bw,top+87,bw,20,this::onClose));
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();GuardUi.panel(g,left,top,w,116,false);g.drawCenteredString(font,title,width/2,top+9,0xFFF5F0F6);for(var widget:renderables)widget.render(g,x,y,d);
    }
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
