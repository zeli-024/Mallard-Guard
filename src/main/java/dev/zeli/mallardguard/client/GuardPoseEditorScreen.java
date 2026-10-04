package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class GuardPoseEditorScreen extends GuardPreviewScreen {
    private final GuardPoseManagerScreen parent;
    private GuardPoseLibrary.Pose pose,saved;
    private int group,left,top,panelWidth,panelHeight;
    private boolean previewOn=true,previewStarted;


    private void mirrorGuardHand(){for(int axis=0;axis<3;axis++){pose.values[7+axis]=pose.values[1+axis]*(axis==0?-1:1);pose.values[10+axis]=pose.values[4+axis]*(axis==0?1:-1);}preview();rebuild();}
    private void copyPreset(){
        Minecraft.getInstance().setScreen(new GuardPoseCopyScreen(this,parent.draft.poses,pose.id,source->{
            String id=pose.id,name=pose.name;boolean builtIn=pose.preset,enabled=pose.enabled;
            pose=source.copy();pose.id=id;pose.name=name;pose.preset=builtIn;pose.enabled=enabled;pose.values[0]=enabled?1:0;
            previewStarted=false;
        }));
    }
    private static final String[] GROUPS={"Main Hand","Off Hand","Weapon","Motion"};
    private static final String[] AXES={"Move X","Move Y","Move Z","Rotate X","Rotate Y","Rotate Z"};
    GuardPoseEditorScreen(GuardPoseManagerScreen parent,GuardPoseLibrary.Pose pose){super("Parry Stance Editor");this.parent=parent;this.pose=pose.copy();saved=this.pose.copy();}
    private boolean changed(){return !GuardPoseLibrary.samePose(pose,saved);}
    private void rebuild(){clearWidgets();init();}
    private void preview(){if(previewOn){PunchyGuardCompat.livePreviewPose(pose.values);previewStarted=true;}}
    private static boolean position(int field){return (field-1)%6<3;}
    private static double limit(int field){return position(field)?20:180;}
    private double offset(int field){return pose.values[field]/(position(field)?10.0:1.0);}
    private Button button(String text,int x,int y,int w,Runnable action,String tip){var b=addRenderableWidget(GuardUi.button(text,x,y,w,20,action));b.setTooltip(Tooltip.create(Component.literal(tip)));b.setHeight(18);return b;}
    @Override protected void init(){
        panelWidth=Math.min(480,width-16);panelHeight=206;left=(width-panelWidth)/2;top=Math.max(8,(height-panelHeight)/2);
        int inner=panelWidth-16,gap=6,col=(inner-gap)/2,x=left+8,right=x+col+gap;
        for(int i=0;i<GROUPS.length;i++){
            final int n=i;
            int tx=x+i*inner/GROUPS.length,tw=(i+1)*inner/GROUPS.length-i*inner/GROUPS.length;
            var tab=addRenderableWidget(new Button(tx,top+26,tw,20,Component.literal(GROUPS[i]),ignored->{group=n;rebuild();},message->message.get()){
                @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){GuardUi.paint(g,this,mx,my,n==group,false);}
            });tab.active=n!=group;
            tab.setTooltip(Tooltip.create(Component.literal("Edit "+GROUPS[i].toLowerCase(Locale.ROOT)+" values.")));
        }
        if(group<3){
            for(int axis=0;axis<3;axis++){
                var move=addRenderableWidget(new PoseSlider(x,top+52+axis*22,col,1+group*6+axis));move.setHeight(18);move.active=parent.editable;
                var rotate=addRenderableWidget(new PoseSlider(right,top+52+axis*22,col,4+group*6+axis));rotate.setHeight(18);rotate.active=parent.editable;
            }
            if(group==1){
                int third=(inner-12)/3;
                button("Held: "+(pose.otherHandHeldOnly?"On":"Off"),x,top+120,third,()->{pose.otherHandHeldOnly=!pose.otherHandHeldOnly;rebuild();},"Only animate the supporting hand when an eligible or assigned item is held. Default: Off.").active=parent.editable;
                button("Assign Items",x+third+6,top+120,third,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,pose.otherHandItems,0,(items,mode)->pose.otherHandItems=new ArrayList<>(items),false)),"Items that use the supporting-hand placement. An empty list follows guard eligibility.").active=parent.editable;
                button("Mirror Main",x+2*(third+6),top+120,third,this::mirrorGuardHand,"Copy mirrored main-hand placement and rotation.").active=parent.editable;
            }else if(group==2){
                button("Assign Weapon Items",x,top+120,inner,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,pose.weaponItems,0,(items,mode)->pose.weaponItems=new ArrayList<>(items),false)),"Items that receive weapon offsets. An empty list uses every item assigned to this preset.").active=parent.editable;
            }else{
                button("Assign Main-Hand Items",x,top+120,inner,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,pose.items,pose.handMode,(items,mode)->{pose.items=new ArrayList<>(items);pose.handMode=mode;})),"Choose items or empty hands that trigger this preset. Placement follows the actual guarding hand.").active=parent.editable;
            }
        }else if(group==3){
            motion("Enter",x,top+52,col,19,2000,"Time to raise this pose, in milliseconds.");
            motion("Return",x,top+74,col,20,2000,"Total return time, including empty-offhand lowering, in milliseconds.");
            motion("Ease Strength",x,top+96,col,22,200,"Strength of the selected curve. Range: 0–200%.");
            button("Curve: "+new String[]{"Linear","Ease In","Ease Out","In/Out"}[pose.values[21]],right,top+52,col,()->{pose.values[21]=(pose.values[21]+1)%4;PunchyGuardCompat.previewMotion(pose.values);rebuild();},"Entry and return movement curve.").active=parent.editable;
            button("Hold: "+(pose.maintainHeld?"On":"Off"),right,top+74,col,()->{pose.maintainHeld=!pose.maintainHeld;PunchyGuardCompat.previewMaintain(pose.maintainHeld);rebuild();},"On holds the pose until guard ends. Off plays it once per guard attempt. Default: On.").active=parent.editable;
        }
        int third=(inner-12)/3;
        button("Copy Preset",x,top+144,third,this::copyPreset,"Copy another preset into this draft.").active=parent.editable;
        button("Preview: "+(previewOn?"On":"Off"),x+third+6,top+144,third,()->{previewOn=!previewOn;if(previewOn){PunchyGuardCompat.previewPose(pose.values,pose.maintainHeld);previewStarted=true;}else{PunchyGuardCompat.clearPreview();previewStarted=false;}rebuild();},"Play this preset's entry and return.");
        visibility=button("Hide UI",x+2*(third+6),top+144,third,this::toggleVisibility,"Fade controls. Click anywhere to show them again.");
        int action=Math.min(90,(inner-6)/2);
        button("Save",left+panelWidth-8-2*action-6,top+180,action,this::save,"Name and save this preset.").active=parent.editable;
        button("Close",left+panelWidth-8-action,top+180,action,this::onClose,"Close the editor. Unsaved edits require confirmation.");
        if(previewOn&&!previewStarted){PunchyGuardCompat.previewPose(pose.values,pose.maintainHeld);previewStarted=true;}
    }
    private void motion(String label,int x,int y,int w,int field,int maximum,String tip){var slider=addRenderableWidget(new AbstractSliderButton(x,y,w,20,Component.empty(),pose.values[field]/(double)maximum){
        {updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(label+": "+pose.values[field]+(field==22?"%":" ms")));}
        @Override protected void applyValue(){int next=(int)Math.round(value*maximum);if(next!=pose.values[field]){pose.values[field]=next;PunchyGuardCompat.previewMotion(pose.values);}}
        @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(!active||!isMouseOver(x,y))return false;value=Math.clamp(value+Math.signum(sy)*(field==22?1.0:10.0)*(hasShiftDown()?10:1)/maximum,0,1);applyValue();updateMessage();return true;}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){GuardUi.slider(g,this,value,pose.values[field]!=saved.values[field]);}
    });slider.setHeight(18);slider.active=parent.editable;slider.setTooltip(Tooltip.create(Component.literal(tip)));}

    private void save(){Minecraft.getInstance().setScreen(new GuardPoseNameScreen(this,pose.name,parent.draft.poses,name->{pose.name=name;commitSave();}));}
    private void commitSave(){int existing=-1;for(int i=0;i<parent.draft.poses.size();i++)if(parent.draft.poses.get(i).id.equals(pose.id)){existing=i;break;}if(existing<0)parent.draft.poses.add(pose.copy());else parent.draft.poses.set(existing,pose.copy());if(parent.applyDraft()){saved=pose.copy();Minecraft.getInstance().setScreen(parent);}}
    @Override protected boolean blurPreview(){return false;}
    @Override protected boolean fadesWhileAdjusting(AbstractSliderButton slider){return slider instanceof PoseSlider;}
    @Override public void render(GuiGraphics g,int x,int y,float d){
        beginPreview(g);
        GuardUi.panel(g,left,top,panelWidth,panelHeight,false);
        g.drawString(font,font.plainSubstrByWidth(pose.name,panelWidth-20),left+10,top+10,GuardUi.TEXT);
        g.drawString(font,changed()?"Unsaved edits":"Saved preset",left+10,top+185,changed()?0xFFE7CD87:GuardUi.MUTED);
        for(var widget:renderables)widget.render(g,x,y,d);
        endPreview(g);
    }
    @Override public void removed(){PunchyGuardCompat.clearPreview();previewStarted=false;super.removed();}
    @Override public void onClose(){if(changed())GuardUi.choices(this,"Unsaved preset","Save or discard these changes.",new GuardUi.Choice("Save",this::save),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));else Minecraft.getInstance().setScreen(parent);}
    private final class PoseSlider extends AbstractSliderButton {
        final int field;boolean typing;String text="";
        PoseSlider(int x,int y,int w,int field){super(x,y,w,20,net.minecraft.network.chat.Component.empty(),Math.clamp((offset(field)+limit(field))/(2*limit(field)),0,1));this.field=field;updateMessage();setTooltip(Tooltip.create(net.minecraft.network.chat.Component.literal(AXES[(field-1)%6]+" from the resting hand position. Drag, scroll, or type a value. Zero leaves that axis unchanged. Range: "+(position(field)?"-20 to 20 model pixels":"-180 to 180 degrees")+".")));}
        @Override public void onClick(double x,double y){typing=false;super.onClick(x,y);}
        @Override public boolean mouseClicked(double x,double y,int b){if(active&&visible&&isMouseOver(x,y)&&b==1){typing=true;text="";setFocused(true);return true;}return super.mouseClicked(x,y,b);}
        @Override public boolean charTyped(char c,int modifiers){if(!active||!isFocused()||!(Character.isDigit(c)||c=='-'||c=='.'))return false;if(!typing){typing=true;text="";}if(text.length()<8)text+=c;parse();return true;}
        @Override public boolean keyPressed(int key,int scan,int mods){if(typing&&key==GLFW.GLFW_KEY_BACKSPACE){if(!text.isEmpty())text=text.substring(0,text.length()-1);parse();return true;}if(typing&&(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)){parse();typing=false;return true;}if(!typing&&(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT)){boolean handled=super.keyPressed(key,scan,mods);return handled;}return super.keyPressed(key,scan,mods);}
        private void parse(){try{double n=Double.parseDouble(text);if(Double.isFinite(n)&&n>=-limit(field)&&n<=limit(field)){value=(n+limit(field))/(2*limit(field));applyValue();updateMessage();}}catch(NumberFormatException ignored){}}
        @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(!active||!isMouseOver(x,y))return false;value=Math.clamp(value+Math.signum(sy)*(position(field)?.1:1)*(hasShiftDown()?10:1)/(2*limit(field)),0,1);applyValue();updateMessage();return true;}
        @Override protected void updateMessage(){setMessage(net.minecraft.network.chat.Component.literal((position(field)?"Move ":"Rot ")+"XYZ".charAt((field-1)%3)+": "+(offset(field)==(int)offset(field)?Integer.toString((int)offset(field)):String.format(Locale.ROOT,"%.1f",offset(field)))));}
        @Override protected void applyValue(){double n=value*(2*limit(field))-limit(field);int scale=position(field)?10:1;int next=Math.clamp((int)Math.round(n*scale),GuardPoseSettings.min(field),GuardPoseSettings.max(field));if(next!=pose.values[field]){valueChanged();pose.values[field]=next;preview();}}
        @Override public void renderWidget(GuiGraphics g,int x,int y,float d){
            Component previous=getMessage();
            if(typing)setMessage(Component.literal(AXES[(field-1)%6]+": "+text+"_"));
            GuardUi.slider(g,this,value,pose.values[field]!=saved.values[field]);
            if(typing)setMessage(previous);
        }
    }
}
