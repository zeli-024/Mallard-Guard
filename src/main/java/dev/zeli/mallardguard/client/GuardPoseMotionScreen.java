package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPoseLibrary;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuardPoseMotionScreen extends Screen {
    private final Screen parent;
    private final Consumer<GuardPoseLibrary.Pose> accept;
    private final GuardPoseLibrary.Pose original;
    private GuardPoseLibrary.Pose draft;
    private int left,top,w;
    GuardPoseMotionScreen(Screen parent,GuardPoseLibrary.Pose pose,Consumer<GuardPoseLibrary.Pose> accept){super(Component.literal("Motion Values"));this.parent=parent;this.original=pose.copy();this.draft=pose.copy();this.accept=accept;}
    private void rebuild(){clearWidgets();init();}
    @Override protected void init(){
        w=Math.min(360,width-24);left=(width-w)/2;top=Math.max(4,(height-172)/2);
        slider("Enter",19,500,top+27,"Time to move into guard, in milliseconds.\nDrag: 10 ms; scroll: 1 ms; Shift-scroll: 10 ms.");
        slider("Return",20,500,top+51,"Return transition time in milliseconds.\nA newer animation can interrupt it.");
        slider("Ease Strength",22,200,top+75,"Strength of the easing curve, 0–200%.");
        int bw=(w-22)/2;
        addRenderableWidget(GuardUi.button("Easing: "+new String[]{"Linear","Ease In","Ease Out","Ease In/Out"}[draft.values[21]],left+8,top+99,bw,20,()->{draft.values[21]=(draft.values[21]+1)%4;rebuild();}));
        addRenderableWidget(GuardUi.button("Hold: "+(draft.maintainHeld?"On":"Off"),left+14+bw,top+99,bw,20,()->{draft.maintainHeld=!draft.maintainHeld;rebuild();})).setTooltip(Tooltip.create(Component.literal("On maintains the pose while guarding. Off plays entry and return once.")));
        addRenderableWidget(GuardUi.button("?",left+w-30,top+4,22,18,()->GuardUi.choices(this,"Motion Help","TIMING\nEnter blends into guard; Return blends back out.\nA newer animation interrupts the current one.\n\nTUNING\nDrag timing in 10 ms steps. Scroll for 1 ms; Shift-scroll for 10 ms.\nEasing changes the curve. Hold maintains the pose while guarding.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)))));
        int action=(w-22)/2;
        addRenderableWidget(GuardUi.button("Save",left+8,top+140,action,20,()->accept.accept(draft.copy())));
        addRenderableWidget(GuardUi.button("Close",left+14+action,top+140,action,20,this::onClose));
    }
    private void slider(String label,int field,int max,int y,String tooltip){
        var slider=addRenderableWidget(new AbstractSliderButton(left+8,y,w-16,20,Component.empty(),draft.values[field]/(double)max){
            {updateMessage();}
            @Override protected void updateMessage(){setMessage(Component.literal(label+": "+draft.values[field]+(field==22?"%":" ms")));}
            @Override protected void applyValue(){int step=GuardUi.dragStep(0,max,1);draft.values[field]=Math.clamp((int)Math.round(value*max/step)*step,0,max);value=draft.values[field]/(double)max;}
            @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(!active||!isMouseOver(x,y)||sy==0)return false;draft.values[field]=Math.clamp(draft.values[field]+(int)Math.signum(sy)*(hasShiftDown()?10:1),0,max);value=draft.values[field]/(double)max;updateMessage();return true;}
            @Override public void renderWidget(GuiGraphics g,int x,int my,float d){GuardUi.slider(g,this,value,draft.values[field]!=original.values[field]);}
        });slider.setTooltip(Tooltip.create(Component.literal(tooltip)));
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){g.flush();renderBackground(g,x,y,d);g.flush();GuardUi.panel(g,left,top,w,172,false);g.drawCenteredString(font,title,width/2,top+9,GuardUi.TEXT);for(var widget:renderables)widget.render(g,x,y,d);}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
}
