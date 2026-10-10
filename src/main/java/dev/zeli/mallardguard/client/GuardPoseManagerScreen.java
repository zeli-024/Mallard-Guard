package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuardPoseManagerScreen extends Screen {
    static final class Draft {
        List<GuardPoseLibrary.Pose> poses;boolean enabled=true;
        Draft(List<GuardPoseLibrary.Pose> poses){this.poses=new ArrayList<>(poses.stream().map(GuardPoseLibrary.Pose::copy).toList());}
        boolean same(Draft other){return other!=null&&enabled==other.enabled&&GuardPoseLibrary.samePoses(poses,other.poses);}
        Draft copy(){Draft d=new Draft(poses);d.enabled=enabled;return d;}
    }
    final Screen parent;final Consumer<Draft> accept;final boolean editable;
    Draft draft,saved;private int left,top,w,rows,start,panelHeight,listTop,footer,split,listWidth,trackX,trackHeight;
    private String focusedId;
    private boolean draggingScroll;
    private double scrollGrab;
    private boolean selecting;
    private final Set<String> selectedIds=new HashSet<>();
    GuardPoseManagerScreen(Screen parent,Draft draft,boolean editable,Consumer<Draft> accept){super(Component.literal("Punchy Presets"));this.parent=parent;this.draft=draft.copy();saved=this.draft.copy();this.editable=editable;this.accept=accept;}
    private boolean changed(){return !draft.same(saved);}
    boolean applyDraft(){accept.accept(draft.copy());if(parent instanceof GuardConfigScreen config&&!config.applyExternalChanges())return false;saved=draft.copy();return true;}
    private void rebuild(){clearWidgets();init();}
    private void commit(){if(applyDraft())Minecraft.getInstance().setScreen(this);}
    private List<GuardPoseLibrary.Pose> selected(){return draft.poses.stream().filter(p->selectedIds.contains(p.id)).toList();}
    private Button button(String label,int x,int y,int width,Runnable action,String tip){var b=addRenderableWidget(GuardUi.button(label,x,y,width,20,action));b.setTooltip(GuardUi.tooltip(tip));return b;}
    private void motion(List<GuardPoseLibrary.Pose> targets){
        if(targets.isEmpty())return;
        Minecraft.getInstance().setScreen(new GuardPoseMotionScreen(this,targets.getFirst(),value->{for(var p:targets){System.arraycopy(value.values,19,p.values,19,4);p.maintainHeld=value.maintainHeld;}commit();}));
    }
    private void assign(List<GuardPoseLibrary.Pose> targets){
        if(targets.isEmpty())return;var first=targets.getFirst();
        Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,first.items,first.handMode,(items,mode)->{for(var p:targets){p.items=new ArrayList<>(items);p.handMode=mode;}commit();}));
    }
    private void rename(GuardPoseLibrary.Pose pose){Minecraft.getInstance().setScreen(new GuardPoseNameScreen(this,pose.name,draft.poses,name->{pose.name=name;commit();}));}
    private void delete(List<GuardPoseLibrary.Pose> targets){
        Set<String> ids=new HashSet<>();for(var p:targets)if(!p.preset)ids.add(p.id);if(ids.isEmpty())return;
        GuardUi.choices(this,"Delete "+ids.size()+" presets?","Built-in presets are retained.",new GuardUi.Choice("Delete",()->{draft.poses.removeIf(p->ids.contains(p.id));selectedIds.removeAll(ids);commit();}),new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));
    }
    private void toggle(List<GuardPoseLibrary.Pose> targets,boolean enabled){for(var p:targets){p.enabled=enabled;p.values[0]=enabled?1:0;}commit();}
    @Override protected void init(){
        w=GuardUiLayout.wideWidth(width);top=GuardUiLayout.screenTop(height);panelHeight=height-top-6;
        left=(width-w)/2;footer=top+panelHeight-30;
        split=left+w*40/100;listWidth=split-left-26;listTop=top+58;
        rows=Math.max(1,(footer-listTop-10)/25);trackHeight=rows*25;
        trackX=split-10;start=Math.clamp(start,0,Math.max(0,draft.poses.size()-rows));
        if(focusedId==null||draft.poses.stream().noneMatch(p->p.id.equals(focusedId)))focusedId=draft.poses.isEmpty()?null:draft.poses.getFirst().id;
        button("Create Preset",left+10,top+29,Math.min(112,listWidth),()->Minecraft.getInstance().setScreen(new GuardPoseEditorScreen(this,GuardPoseLibrary.create())),"Create a custom parrying preset.").active=editable&&draft.poses.size()<GuardPoseLibrary.MAX_POSES;
        button(selecting?"Finish Selection":"Batch Select",split+10,top+29,left+w-split-20,()->{selecting=!selecting;selectedIds.clear();rebuild();},"Select presets for motion, assignments, enable/disable or deletion.").active=editable;
        for(int i=start;i<Math.min(draft.poses.size(),start+rows);i++){
            var pose=draft.poses.get(i);int y=listTop+(i-start)*25;
            var row=addRenderableWidget(new Button(left+10,y,listWidth,22,Component.literal(pose.name),ignored->{
                if(selecting){if(!selectedIds.remove(pose.id))selectedIds.add(pose.id);}else focusedId=pose.id;
                rebuild();
            },message->message.get()){
                @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){
                    boolean chosen=selecting?selectedIds.contains(pose.id):pose.id.equals(focusedId),hover=isMouseOver(mx,my);
                    g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),chosen?0x98493E55:hover?0x785C4D68:0x28211B2A);
                    int nameX=getX()+7;
                    if(selecting){int cx=getX()+5,cy=getY()+5;g.renderOutline(cx,cy,12,12,chosen?GuardUi.WARM:GuardUi.BORDER);if(chosen)g.fill(cx+3,cy+3,cx+9,cy+9,0xFFFFFFFF);nameX+=17;}
                    else if(chosen)g.fill(getX()+1,getY()+2,getX()+3,getY()+getHeight()-2,0xFFFFFFFF);
                    g.drawString(font,font.plainSubstrByWidth(pose.name,getWidth()-(nameX-getX())-9),nameX,getY()+7,pose.enabled?GuardUi.TEXT:0xFF978C9E);
                }
            });
            row.setTooltip(GuardUi.tooltip(pose.name+(pose.preset?" · Built-in preset":" · Custom preset")));
        }
        var focus=draft.poses.stream().filter(p->p.id.equals(focusedId)).findFirst().orElse(null);
        List<GuardPoseLibrary.Pose> targets=selecting?selected():focus==null?List.of():List.of(focus);
        int x=split+10,areaWidth=left+w-10-x,gap=6,bw=(areaWidth-gap)/2;
        int bh=Math.clamp((footer-listTop-52)/4,12,22),step=bh+6,ay=listTop+28;
        if(selecting){
            boolean all=!draft.poses.isEmpty()&&selectedIds.size()==draft.poses.size();
            var select=button(all?"Clear Selection":"Select All",x,ay,areaWidth,()->{selectedIds.clear();if(!all)for(var p:draft.poses)selectedIds.add(p.id);rebuild();},"Select or clear every preset.");select.setHeight(bh);select.active=editable;
        }else{
            actionButton("Edit Placement","edit",x,ay,areaWidth,bh,()->{if(focus!=null)Minecraft.getInstance().setScreen(new GuardPoseEditorScreen(this,focus));},"Edit hand and weapon placement.",editable&&focus!=null);
            var rename=addRenderableWidget(GuardUi.iconButton("edit","Rename",left+w-30,listTop,18,()->rename(focus)));rename.active=editable&&focus!=null;
        }
        actionButton("Motion Values","motion",x,ay+step,areaWidth,bh,()->motion(targets),"Edit entry, release and easing.",editable&&!targets.isEmpty());
        actionButton("Item Assignments","assign",x,ay+step*2,areaWidth,bh,()->assign(targets),"Assign items or empty hands. Explicit assignments override automatic exclusions.",editable&&!targets.isEmpty());
        boolean allEnabled=!targets.isEmpty()&&targets.stream().allMatch(p->p.enabled);
        actionButton(allEnabled?"On":"Off","",x,ay+step*3,bw,bh,()->toggle(targets,!allEnabled),allEnabled?"Disable selected presets.":"Enable selected presets.",editable&&!targets.isEmpty());
        actionButton("Delete","delete",x+bw+gap,ay+step*3,bw,bh,()->delete(targets),"Delete custom presets. Built-in presets cannot be deleted.",editable&&targets.stream().anyMatch(p->!p.preset));
        button("?",left+w-116,footer,22,()->GuardUi.choices(this,"Preset Help","PRESETS\nChoose a preset to edit placement, motion or item assignments.\nCreate Preset adds your own animation.\n\nBATCH SELECTION\nSelect several presets to change their motion or assignments together.\nBuilt-in presets cannot be deleted.\n\nSAVING\nSave commits your edits. Preview only plays the draft.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this))),"Help with presets and batch editing.");
        button("Close",left+w-88,footer,80,this::onClose,"Close the preset menu.");
    }
    private void actionButton(String label,String symbol,int x,int y,int width,int height,Runnable action,String tip,boolean active){
        var b=addRenderableWidget(new Button(x,y,width,height,Component.literal(label),ignored->action.run(),message->message.get()){
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float d){
                GuardUi.paint(g,this,mx,my,false,false);
                if(!symbol.isEmpty()&&getWidth()>=80)GuardUi.drawIcon(g,symbol,getX()+4,getY()+(getHeight()-12)/2,this.active?GuardUi.TEXT:0xFF766D7E);
            }
        });b.active=active;b.setTooltip(GuardUi.tooltip(tip));
    }
    private int maxScroll(){return Math.max(0,draft.poses.size()-rows);}
    private GuardUiLayout.Scrollbar scrollbar(){return new GuardUiLayout.Scrollbar(trackX,listTop,trackHeight,rows,draft.poses.size(),start);}
    private void scrollTo(double y){
        int next=scrollbar().scrollAt(y,scrollGrab);
        next=Math.clamp(next,0,maxScroll());if(next!=start){start=next;rebuild();}
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&scrollbar().contains(x,y)){
            draggingScroll=true;scrollGrab=scrollbar().grab(y);scrollTo(y);return true;
        }return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(draggingScroll&&button==0){scrollTo(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&draggingScroll){draggingScroll=false;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(x>=left+8&&x<split&&y>=listTop&&y<listTop+trackHeight){start=Math.clamp(start-(int)Math.signum(sy),0,maxScroll());rebuild();return true;}return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();
        GuardUi.panel(g,left,top,w,panelHeight,false);
        g.drawString(font,title,left+10,top+10,GuardUi.TEXT);
        GuardUi.panel(g,left+7,listTop-3,split-left-12,footer-listTop-3,true);
        GuardUi.panel(g,split+5,listTop-3,w-(split-left)-12,footer-listTop-3,false);
        var focus=draft.poses.stream().filter(p->p.id.equals(focusedId)).findFirst().orElse(null);
        String heading=selecting?selectedIds.size()+" selected":focus==null?"Select a preset":focus.name;
        int detailsWidth=left+w-split-44;
        g.drawString(font,font.plainSubstrByWidth(heading,detailsWidth),split+10,listTop+3,GuardUi.TEXT);
        g.drawString(font,draft.poses.size()+" presets",left+10,footer+6,0xFFC3B4CA);
        GuardUi.scrollbar(g,scrollbar(),x,y,draggingScroll);
        for(var widget:renderables)widget.render(g,x,y,d);
    }
    @Override public void onClose(){if(changed())GuardUi.choices(this,"Unsaved presets","",new GuardUi.Choice("Save",()->{if(applyDraft())Minecraft.getInstance().setScreen(parent);}),new GuardUi.Choice("Close",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));else Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
    @Override public void removed(){draggingScroll=false;super.removed();}

}
