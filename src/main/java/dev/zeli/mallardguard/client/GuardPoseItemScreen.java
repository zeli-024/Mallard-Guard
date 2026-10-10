package dev.zeli.mallardguard.client;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import dev.zeli.mallardguard.GuardItemRules;
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
    private final RuleDraft selected;
    private int handMode;
    private final boolean showHandMode;
    private String filter="";
    private String defaultAllowed,defaultBlocked;
    private boolean ruleMode,combined,filtersOpen;
    private final RuleDraft blocked=new RuleDraft(List.of());
    private Set<String> savedBlocked=Set.of();
    private BiConsumer<String,String> acceptRules;
    private Button selectAllButton;
    private String ruleTitle="Assign items",notice="";
    private boolean countMode;
    private int assignmentCount=5;
    private EditBox countInput;
    private final LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();
    private String savedCounts="";
    private int categoryMask;
    private final Map<Item,Integer> categoryBits=new HashMap<>();
    private Button ruleButton;
    private List<String> assignedEntries=List.of();
    private final Map<String,Item> itemById=new HashMap<>();
    private EditBox search,ruleInput;
    private String ruleText="";
    private int dragBar;
    private double dragGrab;
    private int left,top,panelWidth,scroll,rows,columns,bottom,assignedLeft,assignedWidth,assignedScroll,assignedRows;
    private final List<Item> all=new ArrayList<>(),matches=new ArrayList<>();
    /** Ordered edit data with serialization and compilation invalidated only by mutations. */
    private static final class RuleDraft extends LinkedHashSet<String> {
        private String encoded;
        private final GuardItemRules.Source source=new GuardItemRules.Source();
        RuleDraft(Collection<String> entries) { addAll(entries); }
        @Override public boolean add(String value) { boolean changed=super.add(value);if(changed)encoded=null;return changed; }
        @Override public boolean remove(Object value) { boolean changed=super.remove(value);if(changed)encoded=null;return changed; }
        @Override public void clear() { super.clear();encoded=null; }
        @Override public boolean addAll(Collection<? extends String> values) { boolean changed=false;for(String value:values)changed|=add(value);return changed; }
        @Override public Iterator<String> iterator() {
            Iterator<String> iterator=super.iterator();
            return new Iterator<>() {
                public boolean hasNext(){return iterator.hasNext();}
                public String next(){return iterator.next();}
                public void remove(){iterator.remove();encoded=null;}
            };
        }
        String text() { if(encoded==null)encoded=String.join(",",this);return encoded; }
        GuardItemRules.Compiled compiled() { return source.get(text()); }
    }
    private final List<Button> rowRemovers=new ArrayList<>();
    private final List<EditBox> rowCounts=new ArrayList<>();
    private boolean bindingRows;
    private record State(List<String> items,int handMode){}
    private State saved;
    GuardPoseItemScreen(Screen parent,List<String> items,int handMode,BiConsumer<List<String>,Integer> accept){this(parent,items,handMode,accept,true);}
    GuardPoseItemScreen(Screen parent,List<String> items,int handMode,BiConsumer<List<String>,Integer> accept,boolean showHandMode){super(Component.literal("Assign items"));this.parent=parent;this.accept=accept;this.showHandMode=showHandMode;selected=new RuleDraft(items);this.handMode=handMode;saved=new State(List.copyOf(selected),handMode);for(Item i:BuiltInRegistries.ITEM)if(i!=Items.AIR){all.add(i);categoryBits.put(i,GuardItemSearch.categories(i));itemById.put(BuiltInRegistries.ITEM.getKey(i).toString(),i);}filter();}

    GuardPoseItemScreen(Screen parent,String title,String rules,Consumer<String> accept){
        this(parent,rules.isBlank()?List.of():Arrays.stream(rules.split(",")).map(String::trim).toList(),0,(items,hand)->accept.accept(String.join(",",items)),false);
        ruleMode=true;ruleTitle=title;
    }
    GuardPoseItemScreen(Screen parent,String assignments,Consumer<String> accept){
        this(parent,dev.zeli.mallardguard.GuardItemBlockCounts.parse(assignments),accept);
    }
    private GuardPoseItemScreen(Screen parent,Map<String,Integer> assignments,Consumer<String> accept){
        this(parent,new ArrayList<>(assignments.keySet()),0,(items,hand)->{},false);
        countMode=ruleMode=true;ruleTitle="Item Block Counts";
        counts.putAll(assignments);savedCounts=countRules();countAccept=accept;
    }
    GuardPoseItemScreen(Screen parent,String title,String whitelist,String blacklist,BiConsumer<String,String> accept){
        this(parent,title,whitelist,value->{});combined=true;acceptRules=accept;
        if(!blacklist.isBlank())blocked.addAll(Arrays.stream(blacklist.split(",")).map(String::trim).toList());
        savedBlocked=Set.copyOf(blocked);
    }
    GuardPoseItemScreen restrictItems(java.util.function.Predicate<Item> eligible){all.removeIf(item->!eligible.test(item));filter();return this;}
    GuardPoseItemScreen assignmentDefaults(String allowed,String denied){defaultAllowed=allowed;defaultBlocked=denied;return this;}
    private void resetAssignments(){
        selected.clear();blocked.clear();counts.clear();handMode=0;
        if(defaultAllowed!=null&&!defaultAllowed.isBlank())selected.addAll(Arrays.stream(defaultAllowed.split(",")).map(String::trim).toList());
        if(defaultBlocked!=null&&!defaultBlocked.isBlank())blocked.addAll(Arrays.stream(defaultBlocked.split(",")).map(String::trim).toList());
    }
    record Assignments(String whitelist,String blacklist,String blockCounts){}
    private Consumer<Assignments> acceptAssignments;
    GuardPoseItemScreen(Screen parent,String title,String whitelist,String blacklist,String blockCounts,Consumer<Assignments> accept){
        this(parent,title,whitelist,blacklist,(allowed,denied)->{});
        countMode=true;counts.putAll(dev.zeli.mallardguard.GuardItemBlockCounts.parse(blockCounts));
        savedCounts=countRules();acceptAssignments=accept;
    }
    private GuardUiLayout.Assignment layout;
    private Consumer<String> countAccept;
    private String countRules(){return dev.zeli.mallardguard.GuardItemBlockCounts.encode(counts);}
    private void setCount(String id,int count){
        if(count<0||count>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_COUNT)return;
        Integer old=counts.put(id,count);
        if(countRules().length()>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_LENGTH){if(old==null)counts.remove(id);else counts.put(id,old);notice="Assignment list is full.";return;}
        boolean added=!combined&&selected.add(id);if(added)rebuild();
    }
    private int gridTop(){return layout.gridY(filtersOpen,ruleMode||showHandMode);}
    private String rules(){return selected.text();}
    private String queryRule(){return ruleText.trim().toLowerCase(Locale.ROOT);}
    private boolean validQuery(){String q=queryRule();return !q.isEmpty()&&!q.contains(",")&&GuardItemRules.valid(q);}
    private String ruleAction(){
        String q=queryRule();boolean remove=selected.contains(q);
        if(q.startsWith("@"))return remove?"Remove Mod Rule":"Assign Entire Mod";
        if(q.startsWith("#"))return remove?"Remove Tag Rule":"Assign Tag";
        if(q.contains(":"))return remove?"Remove Item":"Assign Item";
        return remove?"Remove Keyword":"Assign Keyword";
    }
    private int assignedTop(){return layout.assignedY(countMode);}
    private int maxAssignedScroll(){return Math.max(0,assignedEntries.size()-assignedRows);}
    private void toggleRule(){if(!validQuery())return;String q=queryRule();if(!selected.remove(q)){selected.add(q);if(rules().length()>GuardItemRules.MAX_LENGTH){selected.remove(q);notice="Assignment list is full. Remove unused rules.";}}rebuild();}
    private static String ruleLabel(String value){if(value.startsWith("!"))return "Exception: "+ruleLabel(value.substring(1));return value.startsWith("@")?"Mod: "+value.substring(1):value.startsWith("#")?"Tag: "+value.substring(1):value.contains(":")?"Item: "+value:"Keyword: "+value;}
    private int maxScroll(){return Math.max(0,(matches.size()+columns-1)/columns-rows);}
    private String entryRule(String value){return combined?value.substring(2):value;}
    private String assignmentLabel(String stored){
        String rule=entryRule(stored);if(!combined)return ruleLabel(rule);
        if(rule.startsWith("!"))return (blockedEntry(stored)?"Blacklist exception: ":"Whitelist exception: ")+rule.substring(1);
        return (stored.startsWith("= ")?"Block limit: ":blockedEntry(stored)?"Blocked: ":"Allowed: ")+ruleLabel(rule);
    }
    private boolean blockedEntry(String value){return combined&&value.startsWith("- ");}
    private void removeAssignment(String value){if(blockedEntry(value))blocked.remove(entryRule(value));else selected.remove(entryRule(value));counts.remove(entryRule(value));rebuild();}

    private boolean selected(Item item){return ruleMode&&(!countMode||combined)?selected.compiled().matches(item.getDefaultInstance()):selected.contains(BuiltInRegistries.ITEM.getKey(item).toString());}

    private void apply(boolean close){
        if(combined){
            if(!GuardItemRules.valid(rules())||!GuardItemRules.valid(blocked.text())){notice="Assignment list is full. Remove unused rules.";return;}
            if(acceptAssignments!=null){acceptAssignments.accept(new Assignments(rules(),blocked.text(),countRules()));savedCounts=countRules();}
            else acceptRules.accept(rules(),blocked.text());savedBlocked=Set.copyOf(blocked);saved=new State(List.copyOf(selected),handMode);
            if(close)onClose();else Minecraft.getInstance().setScreen(this);return;
        }
        if(countMode){countAccept.accept(countRules());savedCounts=countRules();saved=new State(List.copyOf(selected),handMode);if(close)onClose();else Minecraft.getInstance().setScreen(this);return;}
        if(!ruleMode&&selected.size()>dev.zeli.mallardguard.GuardPoseLibrary.MAX_ITEMS){notice="Maximum 2,048 items per assignment list.";return;}
        if(ruleMode&&!GuardItemRules.valid(rules())){notice="Check the assignment rules.";return;}accept.accept(List.copyOf(selected),handMode);saved=new State(List.copyOf(selected),handMode);if(close)onClose();else Minecraft.getInstance().setScreen(this);}
    private void filter(){
        matches.clear();
        for(Item item:all)if(GuardItemSearch.matches(item,filter)&&(categoryMask==0||(categoryBits.getOrDefault(item,16)&categoryMask)!=0))matches.add(item);
        scroll=0;notice="";
        if(selectAllButton!=null){selectAllButton.setMessage(Component.literal(panelWidth<190?"All":"Select All"));selectAllButton.active=!matches.isEmpty();}
    }
    private void updateRuleButton(){
        if(ruleButton!=null){ruleButton.active=validQuery();ruleButton.setMessage(Component.literal(combined?"Assign Rule":ruleAction()));}
    }
    private void rebuild(){clearWidgets();init();}
    @Override protected void init(){
        layout=GuardUiLayout.assignment(width,height);
        int available=layout.width();
        left=layout.left();top=layout.top();bottom=layout.bottom();
        panelWidth=layout.browserWidth();assignedLeft=layout.assignedLeft();assignedWidth=layout.assignedWidth();
        columns=Math.max(1,(panelWidth-26)/GuardUiLayout.ITEM_STEP);
        rows=Math.max(1,(layout.contentBottom()-gridTop())/GuardUiLayout.ITEM_STEP);
        assignedRows=Math.max(1,(layout.contentBottom()-assignedTop())/GuardUiLayout.ITEM_STEP);
        refreshEntries();
        scroll=Math.min(scroll,maxScroll());assignedScroll=Math.min(assignedScroll,maxAssignedScroll());
        ruleButton=null;countInput=null;ruleInput=null;
        search=addRenderableWidget(GuardUi.editBox(font,left+10,layout.searchY(),panelWidth-24,"Search items"));
        search.setMaxLength(256);search.setValue(filter);
        search.setResponder(text->{filter=text;filter();});
        search.setHint(Component.literal("Search name, ID, #tag or @mod"));
        search.setTooltip(GuardUi.tooltip("Search the item browser.\nNames, IDs, #tags and @mods are supported.\nSearch never changes your assignments."));
        addRenderableWidget(GuardUi.button("?",assignedLeft+assignedWidth-24,top+3,20,20,this::help));
        int tabWidth=layout.tabWidth(),tabY=layout.tabsY();
        addRenderableWidget(GuardUi.button("Items",left+8,tabY,tabWidth,20,()->{filtersOpen=false;rebuild();}));
        addRenderableWidget(GuardUi.button("Filters",left+12+tabWidth,tabY,tabWidth,20,()->{filtersOpen=true;rebuild();}));
        selectAllButton=addRenderableWidget(GuardUi.button(panelWidth<190?"All":"Select All",left+panelWidth-(panelWidth<190?72:80),tabY,panelWidth<190?58:66,20,()->selectAll(false)));
        selectAllButton.active=!matches.isEmpty();selectAllButton.visible=!filtersOpen;
        selectAllButton.setTooltip(GuardUi.tooltip(combined?"Select every matching item, including offscreen rows.\nLeft: allow; right: block.":"Assign every matching item, including offscreen rows."));
        if(filtersOpen)addFilters();
        else if(ruleMode&&(!countMode||combined)){
            int buttonWidth=Math.min(88,panelWidth/3);
            ruleInput=addRenderableWidget(GuardUi.editBox(font,left+10,layout.ruleY(),panelWidth-buttonWidth-30,"Assignment rule"));
            ruleInput.setMaxLength(256);ruleInput.setValue(ruleText);ruleInput.setHint(Component.literal("@mod, #tag, ID or !exception"));
            ruleInput.setResponder(text->{ruleText=text;updateRuleButton();});
            ruleInput.setTooltip(GuardUi.tooltip("Enter a group rule or ! exception.\nThis field assigns rules; Search only filters browsing."));
            ruleButton=addRenderableWidget(GuardUi.button(combined?"Assign Rule":ruleAction(),left+panelWidth-buttonWidth-14,layout.ruleY(),buttonWidth,20,()->{if(combined)assignRules(List.of(queryRule()),false);else toggleRule();}));
            updateRuleButton();
            ruleButton.setTooltip(GuardUi.tooltip(combined?"Assign the entered rule. Left: allow; right: block.\nEither click removes a rule already assigned.":"Assign or remove the entered rule."));
        }else if(showHandMode){
            addRenderableWidget(GuardUi.button("Hand: "+new String[]{"Off","Combine","Strict"}[handMode],left+8,layout.ruleY(),panelWidth-22,20,()->{handMode=(handMode+1)%3;rebuild();})).setTooltip(GuardUi.tooltip("Off: held items only. Combine: items and empty hands.\nStrict: empty hands only. Server eligibility still applies."));
        }
        if(countMode){
            countInput=addRenderableWidget(GuardUi.editBox(font,assignedLeft+assignedWidth-58,layout.tabsY(),40,"New item block limit"));
            countInput.setMaxLength(3);countInput.setValue(Integer.toString(assignmentCount));
            countInput.setFilter(GuardPoseItemScreen::validCountInput);
            countInput.setResponder(text->{try{assignmentCount=Integer.parseInt(text);}catch(NumberFormatException ignored){}});
            countInput.setTooltip(GuardUi.tooltip("New allowed items use this block limit.\n0 means unlimited; existing limits are editable below."));
        }
        addAssignedControls();
        int edge=assignedLeft+assignedWidth,actionWidth=layout.actionWidth();
        addRenderableWidget(GuardUi.button("Reset",left+8,layout.footerY(),actionWidth,20,()->GuardUi.confirm(this,"Reset assignments?",defaultAllowed==null?"Clear this screen's assignments and exceptions.\nApply saves the reset.":"Restore vanilla equipment with axes blocked.\nApply saves the reset.",this::resetAssignments)));
        addRenderableWidget(GuardUi.button("Apply",edge-2*actionWidth-12,layout.footerY(),actionWidth,20,()->apply(true)));
        addRenderableWidget(GuardUi.button("Close",edge-actionWidth-8,layout.footerY(),actionWidth,20,this::onClose));
    }
    private static boolean validCountInput(String text){return text.matches("[0-9]{0,3}")&&(text.isEmpty()||Integer.parseInt(text)<=dev.zeli.mallardguard.GuardItemBlockCounts.MAX_COUNT);}
    private void refreshEntries(){
        if(!combined){assignedEntries=List.copyOf(selected);return;}
        var entries=new ArrayList<String>();selected.forEach(value->entries.add("+ "+value));blocked.forEach(value->entries.add("- "+value));
        counts.keySet().stream().filter(id->!selected.contains(id)&&!blocked.contains(id)).forEach(id->entries.add("= "+id));
        assignedEntries=List.copyOf(entries);
    }
    private void addFilters(){
        int cell=(panelWidth-22)/2;
        for(int k=0;k<GuardItemSearch.CATEGORIES.length;k++){
            final int bit=1<<k;String category=GuardItemSearch.CATEGORIES[k];
            Button check=addRenderableWidget(GuardUi.button(font.plainSubstrByWidth(((categoryMask&bit)!=0?"[x] ":"[ ] ")+category,cell-8),left+8+(k%2)*(cell+4),gridTop()+(k/2)*22,cell,20,()->{categoryMask^=bit;filter();rebuild();}));
            check.setTooltip(GuardUi.tooltip(category+". Filter browsing only.\nChecked categories combine; no checks shows everything."));
        }
        addRenderableWidget(GuardUi.button("Clear",left+panelWidth-(panelWidth<190?72:80),layout.tabsY(),panelWidth<190?58:66,20,()->{categoryMask=0;filter="";filter();rebuild();}));
    }
    private String visibleEntry(int row) {
        int index=assignedScroll+row;
        return index<assignedEntries.size()?assignedEntries.get(index):null;
    }
    private void addAssignedControls(){
        rowRemovers.clear();rowCounts.clear();
        for(int r=0;r<assignedRows;r++){
            final int row=r;int y=assignedTop()+r*GuardUiLayout.ITEM_STEP;
            EditBox amount=null;
            if(countMode){
                amount=addRenderableWidget(GuardUi.editBox(font,assignedLeft+assignedWidth-78,y,32,"Item block limit"));
                amount.setMaxLength(3);amount.setFilter(GuardPoseItemScreen::validCountInput);amount.setHint(Component.literal("–"));
                amount.setResponder(text->{
                    if(bindingRows)return;
                    String entry=visibleEntry(row);if(entry==null)return;String id=entryRule(entry);
                    if(text.isEmpty()){counts.remove(id);return;}
                    try{setCount(id,Integer.parseInt(text));}catch(NumberFormatException ignored){}
                });
                amount.setTooltip(GuardUi.tooltip("Block limit: 0–100; 0 is unlimited."));
            }
            rowCounts.add(amount);
            rowRemovers.add(addRenderableWidget(GuardUi.button("x",assignedLeft+assignedWidth-38,y,20,20,()->{
                String entry=visibleEntry(row);if(entry!=null)removeAssignment(entry);
            })));
        }
        bindAssignedRows();
    }
    private void bindAssignedRows(){
        bindingRows=true;
        try{
            for(int r=0;r<rowRemovers.size();r++){
                String entry=visibleEntry(r);Button remove=rowRemovers.get(r);remove.visible=entry!=null;
                if(entry!=null)remove.setTooltip(GuardUi.tooltip("Remove "+entry));
                EditBox amount=rowCounts.get(r);
                if(amount!=null){
                    String id=entry==null?null:entryRule(entry);amount.visible=id!=null&&itemById.containsKey(id);
                    amount.setValue(amount.visible&&counts.containsKey(id)?Integer.toString(counts.get(id)):"");
                }
            }
        }finally{bindingRows=false;}
    }
    private boolean over(Button button,double x,double y){return button!=null&&button.visible&&button.active&&x>=button.getX()&&x<button.getX()+button.getWidth()&&y>=button.getY()&&y<button.getY()+button.getHeight();}
    @Override public boolean mouseClicked(double x,double y,int b){
        if(b==0&&beginScrollDrag(x,y))return true;
        if(b==1&&combined){if(over(selectAllButton,x,y)){selectAll(true);return true;}if(over(ruleButton,x,y)){if(validQuery())assignRules(List.of(queryRule()),true);return true;}}
        if(!filtersOpen&&(b==0||b==1)&&y>=gridTop()&&y<gridTop()+rows*GuardUiLayout.ITEM_STEP&&x>=left+8&&x<left+8+columns*GuardUiLayout.ITEM_STEP){
            int c=(int)(x-left-8)/GuardUiLayout.ITEM_STEP,r=(int)(y-gridTop())/GuardUiLayout.ITEM_STEP,i=(scroll+r)*columns+c;
            if(i<matches.size()){
                String id=BuiltInRegistries.ITEM.getKey(matches.get(i)).toString();
                if(clearItemAssignment(id,true))return true;
                if(combined){assignRules(List.of(id),b==1);return true;}
                if(countMode){try{int count=Integer.parseInt(countInput.getValue());if(count<0||count>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_COUNT)throw new NumberFormatException();setCount(id,count);}catch(NumberFormatException invalid){notice="Enter a count from 0 to 100.";}return true;}
                if(!selected.remove(id)){if(ruleMode&&selected(matches.get(i))){notice="Selected by a group rule. Remove that rule to exclude it.";return true;}if(!ruleMode&&selected.size()>=dev.zeli.mallardguard.GuardPoseLibrary.MAX_ITEMS){notice="Maximum 2,048 items per assignment list.";return true;}boolean exception=ruleMode&&selected.remove("!"+id);selected.add(id);if(ruleMode&&rules().length()>GuardItemRules.MAX_LENGTH){selected.remove(id);if(exception)selected.add("!"+id);notice="Assignment list is full. Remove unused rules.";}}
                rebuild();return true;
            }
        }
        return super.mouseClicked(x,y,b);
    }
    private boolean clearItemAssignment(String id,boolean inherited){
        Item item=itemById.get(id);
        boolean groupRules=ruleMode&&(!countMode||combined)&&item!=null;
        boolean assigned=selected.contains(id)||blocked.contains(id)||counts.containsKey(id)
            ||inherited&&groupRules&&(selected.compiled().matches(item.getDefaultInstance())
                ||blocked.compiled().matches(item.getDefaultInstance()));
        if(!assigned)return false;
        var oldAllowed=new LinkedHashSet<>(selected);var oldBlocked=new LinkedHashSet<>(blocked);
        var oldCounts=new LinkedHashMap<>(counts);
        selected.remove(id);blocked.remove(id);counts.remove(id);
        selected.remove("!"+id);blocked.remove("!"+id);
        // Removing an exact rule must also remove selection inherited from broader rules.
        if(groupRules){
            if(selected.compiled().matches(item.getDefaultInstance()))selected.add("!"+id);
            if(blocked.compiled().matches(item.getDefaultInstance()))blocked.add("!"+id);
        }
        if(ruleMode&&(!countMode||combined)&&(!GuardItemRules.valid(rules())||!GuardItemRules.valid(blocked.text()))){
            selected.clear();selected.addAll(oldAllowed);blocked.clear();blocked.addAll(oldBlocked);
            counts.clear();counts.putAll(oldCounts);notice="No room for the item exception. Remove an unused rule first.";
            return true;
        }
        rebuild();return true;
    }
    private void selectAll(boolean blacklist){
        List<String> ids=matches.stream().map(item->BuiltInRegistries.ITEM.getKey(item).toString()).toList();
        if(combined){assignRules(ids,blacklist);return;}
        var old=new LinkedHashSet<>(selected);var oldCounts=new LinkedHashMap<>(counts);
        if(countMode){int count;try{count=Integer.parseInt(countInput.getValue());if(count<0||count>100)throw new NumberFormatException();}catch(NumberFormatException bad){notice="Enter a count from 0 to 100.";return;}for(String id:ids){selected.add(id);counts.put(id,count);}}
        else selected.addAll(ids);
        if(countMode?countRules().length()>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_LENGTH:ruleMode?rules().length()>GuardItemRules.MAX_LENGTH:selected.size()>dev.zeli.mallardguard.GuardPoseLibrary.MAX_ITEMS){selected.clear();selected.addAll(old);counts.clear();counts.putAll(oldCounts);notice="Too many assignments. Narrow your filters.";return;}
        rebuild();
    }
    private void assignRules(List<String> entries,boolean blacklist){
        if(entries.isEmpty()||entries.stream().anyMatch(q->!GuardItemRules.valid(q)||q.isBlank()||q.contains(",")))return;
        var opposite=blacklist?selected:blocked;
        if(countMode&&!blacklist&&countInput!=null&&countInput.getValue().isEmpty()){notice="Enter a count from 0 to 100.";return;}
        if(entries.size()==1&&clearItemAssignment(entries.get(0),false))return;
        String oppositeRules=opposite.text();
        GuardItemRules.Compiled oppositeCompiled=opposite.compiled();
        // Exact IDs use the existing registry index; no opposite rules means no overlap scan.
        List<String> conflicts=opposite.isEmpty()?List.of():entries.stream().filter(q->{
            if(q.startsWith("!"))return false;
            Item exact=itemById.get(q);
            if(exact!=null)return oppositeCompiled.matches(exact.getDefaultInstance());
            GuardItemRules.Compiled candidate=GuardItemRules.compile(q);
            return all.stream().anyMatch(item->candidate.matches(item.getDefaultInstance())&&oppositeCompiled.matches(item.getDefaultInstance()));
        }).toList();
        if(conflicts.isEmpty()){commitRules(entries,blacklist,false);return;}
        String example=conflicts.get(0),label=blacklist?"Block":"Allow";
        GuardUi.choices(this,"Conflicting item rules",conflicts.size()+" assignment(s) overlap the "+(blacklist?"whitelist":"blacklist")+".\n\nExample: "+example+"\nMatching rules: "+font.plainSubstrByWidth(oppositeRules,250)+"\n\n"+label+" adds ! exceptions to the opposite list.\nBroader mod, tag and keyword rules stay in place.",
            new GuardUi.Choice(label+" Items",()->commitRules(entries,blacklist,true)),
            new GuardUi.Choice("Keep Existing",()->Minecraft.getInstance().setScreen(this)),
            new GuardUi.Choice("Cancel",()->Minecraft.getInstance().setScreen(this)));
    }
    private void commitRules(List<String> entries,boolean blacklist,boolean exceptions){
        var oldAllowed=new LinkedHashSet<>(selected);var oldBlocked=new LinkedHashSet<>(blocked);var oldCounts=new LinkedHashMap<>(counts);
        var target=blacklist?blocked:selected;var opposite=blacklist?selected:blocked;
        for(String q:entries){if(countMode&&!blacklist&&itemById.containsKey(q))counts.put(q,assignmentCount);target.remove(q.startsWith("!")?q.substring(1):"!"+q);target.add(q);if(!q.startsWith("!")){opposite.remove(q);if(exceptions)opposite.add("!"+q);}}
        if(countRules().length()>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_LENGTH||!GuardItemRules.valid(rules())||!GuardItemRules.valid(blocked.text())){counts.clear();counts.putAll(oldCounts);selected.clear();selected.addAll(oldAllowed);blocked.clear();blocked.addAll(oldBlocked);notice="Rule list is full. Narrow your filters.";Minecraft.getInstance().setScreen(this);return;}
        Minecraft.getInstance().setScreen(this);
    }
    private void help(){
        String text=countMode&&!combined?"BLOCK COUNTS\nEnter 0–100, then click an unassigned item. 0 means unlimited.\nEither click removes an assigned item; edit counts in Assigned.\nSelect All applies that count to every search result.\n\nASSIGNED WINDOW\nEdit a count directly or use x to remove its override.\nItem overrides beat compatibility and default limits.":combined?"ITEM ELIGIBILITY\nLeft-click allows an item. Right-click blocks it.\nEither click removes a selected item.\nGroup selections get a !item_id exception.\nSelect All affects every matching item, including offscreen rows.\n\nSEARCH AND RULES\nSearch only filters items. Enter assignments in the Rule field.\n@minecraft: entire mod\n#minecraft:swords: exact tag\nminecraft:stone: exact item\ntacz: IDs or tag IDs containing that keyword\n!#minecraft:swords: exception within its own list\n\nCONFLICTS\nMatching exceptions override that list's broader rules.\nAn active blacklist still beats the whitelist.\nThe popup can add an exception for you.":"ITEM ASSIGNMENTS\nClick an item to assign or remove it.\nGroup selections get a !item_id exception.\nSelect All assigns every matching result.\n\nSEARCH AND FILTERS\nSearch names, IDs, #tags or @mods.\nCategory filters only change the browser.\nAssigned always shows your saved entries.";
        if(defaultAllowed!=null)text="MOB GEAR\nAssignments control generated equipment only.\nVanilla equipment is allowed by default, except axes.\nReset restores these defaults.\n\n"+text;
        if(countMode&&combined)text+="\n\nBLOCK LIMITS\nNew allowed items use the limit beside Assigned.\nEdit individual limits below; blank uses the default.\n0 means unlimited. Group rules do not assign block limits.";
        GuardUi.choices(this,"Assignment Help",text,new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(x>=assignedLeft&&x<assignedLeft+assignedWidth&&y>=assignedTop()&&y<assignedTop()+assignedRows*GuardUiLayout.ITEM_STEP){
            int next=Math.max(0,Math.min(assignedScroll-(int)Math.signum(sy),maxAssignedScroll()));
            if(next!=assignedScroll){assignedScroll=next;bindAssignedRows();}return true;
        }
        if(!filtersOpen&&x>=left&&x<left+panelWidth&&y>=gridTop()&&y<gridTop()+rows*GuardUiLayout.ITEM_STEP){scroll=Math.max(0,Math.min(scroll-(int)Math.signum(sy),maxScroll()));return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();
        int available=assignedLeft+assignedWidth-left;
        GuardUi.panel(g,left-4,top-5,available+8,bottom-top+5,false);
        GuardUi.panel(g,assignedLeft,top+32,assignedWidth,layout.contentBottom()-top-32,true);
        g.drawString(font,font.plainSubstrByWidth(ruleMode?ruleTitle:"Assign Items",available-50),left+8,top+7,GuardUi.TEXT);
        g.fill(left+4,top+29,left+available-4,top+30,GuardUi.BORDER);
        g.fill(assignedLeft-6,top+32,assignedLeft-5,bottom-56,GuardUi.BORDER);
        g.drawString(font,"Assigned ("+assignedEntries.size()+")",assignedLeft+8,top+40,GuardUi.TEXT);
        Item hovered=null;String hoveredRule=null;
        GuardItemRules.Compiled allowedRules=selected.compiled(),blockedRules=blocked.compiled();
        if(!filtersOpen)for(int r=0;r<rows;r++)for(int c=0;c<columns;c++){
            int i=(scroll+r)*columns+c;if(i>=matches.size())continue;
            Item item=matches.get(i);int px=left+8+c*GuardUiLayout.ITEM_STEP,py=gridTop()+r*GuardUiLayout.ITEM_STEP;
            boolean over=x>=px&&x<px+21&&y>=py&&y<py+21;
            boolean blockedMatch=combined&&blockedRules.matches(item.getDefaultInstance()),allowedMatch=ruleMode&&(!countMode||combined)?allowedRules.matches(item.getDefaultInstance()):selected.contains(BuiltInRegistries.ITEM.getKey(item).toString());
            g.fill(px,py,px+21,py+21,blockedMatch?0xAA823B48:allowedMatch?0xAA55735C:0x77211B2A);
            g.renderOutline(px,py,21,21,over?GuardUi.HIGHLIGHT:GuardUi.BORDER);
            g.renderItem(item.getDefaultInstance(),px+2,py+2);if(over)hovered=item;
        }
        if(assignedEntries.isEmpty())g.drawString(font,"No assignments",assignedLeft+8,assignedTop()+6,GuardUi.MUTED);
        for(int r=0;r<assignedRows&&assignedScroll+r<assignedEntries.size();r++){
            String stored=assignedEntries.get(assignedScroll+r),entry=entryRule(stored);int py=assignedTop()+r*GuardUiLayout.ITEM_STEP;
            Item item=itemById.get(entry);int labelEnd=assignedLeft+assignedWidth-(countMode?84:44);
            g.fill(assignedLeft+6,py,labelEnd,py+21,0x77211B2A);
            if(combined)g.fill(assignedLeft+6,py,assignedLeft+8,py+21,blockedEntry(stored)?0xFFE79999:0xFFADD4AE);
            int tx=assignedLeft+11,limit=Math.max(0,labelEnd-tx-3);
            if(item!=null){g.renderItem(item.getDefaultInstance(),tx,py+2);tx+=20;limit=Math.max(0,limit-20);g.drawString(font,font.plainSubstrByWidth(item.getDescription().getString(),limit),tx,py+2,GuardUi.TEXT);g.drawString(font,font.plainSubstrByWidth(combined?(stored.startsWith("= ")?"Limit override":blockedEntry(stored)?"Blocked":"Allowed"):entry,limit),tx,py+12,GuardUi.MUTED);}
            else g.drawString(font,font.plainSubstrByWidth(ruleLabel(entry),limit),tx,py+6,GuardUi.TEXT);
            if(x>=assignedLeft+6&&x<labelEnd&&y>=py&&y<py+21)hoveredRule=assignmentLabel(stored);
        }
        if(!filtersOpen)renderScrollbar(g,1,x,y);
        renderScrollbar(g,2,x,y);
        if(countMode)g.drawString(font,font.plainSubstrByWidth("New item limit",assignedWidth-76),assignedLeft+8,layout.tabsY()+6,GuardUi.MUTED);
        String hint=notice.isEmpty()?(filtersOpen?"Filters affect browsing only.":combined?"Left: allow | Right: block | Click again: remove":countMode?"Click again to remove. Edit saved limits in Assigned.":"Either click removes an assigned item."):notice;
        g.drawString(font,font.plainSubstrByWidth(hint,available-16),left+8,bottom-46,notice.isEmpty()?GuardUi.MUTED:0xFFE79999);
        g.flush();for(var widget:renderables)widget.render(g,x,y,d);
        int tabWidth=layout.tabWidth(),tabX=left+8+(filtersOpen?tabWidth+4:0);
        g.fill(tabX+2,top+82,tabX+tabWidth-2,top+84,GuardUi.HIGHLIGHT);
        if(hovered!=null)g.renderTooltip(font,List.of(hovered.getDescription().getVisualOrderText(),Component.literal(BuiltInRegistries.ITEM.getKey(hovered).toString()).getVisualOrderText()),x,y);
        else if(hoveredRule!=null)g.renderTooltip(font,List.of(Component.literal(hoveredRule).getVisualOrderText()),x,y);
    }
    private int barX(int bar){return (bar==1?left+panelWidth:assignedLeft+assignedWidth)-12;}
    private int barTop(int bar){return bar==1?gridTop():assignedTop();}
    private int barRows(int bar){return bar==1?rows:assignedRows;}
    private int barMax(int bar){return bar==1?maxScroll():maxAssignedScroll();}
    private int barOffset(int bar){return bar==1?scroll:assignedScroll;}
    private int trackHeight(int bar){return barRows(bar)*GuardUiLayout.ITEM_STEP-2;}
    private GuardUiLayout.Scrollbar scrollbar(int bar){return new GuardUiLayout.Scrollbar(barX(bar),barTop(bar),trackHeight(bar),barRows(bar),barRows(bar)+barMax(bar),barOffset(bar));}
    private boolean beginScrollDrag(double x,double y){
        for(int bar=1;bar<=2;bar++){
            if(bar==1&&filtersOpen||barMax(bar)==0)continue;
            if(!scrollbar(bar).contains(x,y))continue;
            dragBar=bar;dragGrab=scrollbar(bar).grab(y);
            moveScrollDrag(y);return true;
        }
        return false;
    }
    private void moveScrollDrag(double y){
        int next=scrollbar(dragBar).scrollAt(y,dragGrab);
        if(dragBar==1)scroll=next;
        else if(assignedScroll!=next){assignedScroll=next;bindAssignedRows();}
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&dragBar!=0){moveScrollDrag(y);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&dragBar!=0){dragBar=0;return true;}
        return super.mouseReleased(x,y,button);
    }
    private void renderScrollbar(GuiGraphics g,int bar,int mouseX,int mouseY){GuardUi.scrollbar(g,scrollbar(bar),mouseX,mouseY,dragBar==bar);}
    @Override public void onClose(){if(handMode!=saved.handMode()||!selected.equals(new LinkedHashSet<>(saved.items()))||countMode&&!countRules().equals(savedCounts)||combined&&!blocked.equals(savedBlocked))GuardUi.choices(this,"Unsaved assignments","Apply assignments or discard these edits.",new GuardUi.Choice("Apply & Close",()->apply(true)),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));else Minecraft.getInstance().setScreen(parent);}
    @Override public void removed(){dragBar=0;super.removed();}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
}
