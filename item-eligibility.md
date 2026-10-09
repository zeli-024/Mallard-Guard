# Item eligibility

Item Whitelist, Item Blacklist, Shield Item Whitelist, and Shield Item Blacklist now open the shared item assignment grid.

Search supports names, IDs, #tags, and @mod namespaces. The category checkboxes filter Blocks, Tools, Consumables, Shields, and Other. Multiple checked categories include items from any of them; no checks shows all items. Tools includes items with positive attack-damage modifiers. Categories can overlap. Filters only affect browsing.

Click an item to assign its exact ID. The action names explain the assignment: Assign Entire Mod, Assign Tag, Assign Item, or Assign Keyword. Searching an already assigned rule offers its corresponding Remove action. The ? button explains the syntax and how filters differ from saved rules. Apply commits the assignment list to the settings screen; the main menu Apply saves the configuration. Reset clears the list.

Assigned is a separate, permanently visible panel beside the browsing grid. It shows every assignment regardless of search filters. Individual items show their icon, name, and ID. Group rules are labeled Mod, Tag, or Keyword. Each entry has an x button to remove it. This view is shared by eligibility, shield recognition, Punchy main-hand/preset, supporting-hand, and weapon item assignment screens.

| Saved rule | Meaning |
| --- | --- |
| `minecraft:stone` | Exact item ID |
| `#minecraft:planks` | Membership in that item tag |
| `@tacz` | Exact mod namespace |
| `tacz` | Substring in item IDs or item tag IDs |
| `!tacz:rifle` | Exclude that item from broader rules in this list |

Display names are searchable for browsing, but never decide gameplay eligibility. Each item rule list and block-count list supports up to 262,144 characters. Select All uses every matching item, including offscreen results. Large config saves travel in bounded chunks and apply only after complete receipt. Either click deselects an assigned item. If a mod, tag or keyword rule still selects it, the editor adds `!item_id` to that same list. When both lists select it, both receive an exception. Reassigning with left-click or right-click removes the exception from the chosen list. Exceptions appear in Assigned and can be removed there; they exclude an item from their own list, rather than blacklisting it globally. The general blacklist always wins.

Empty Hand requires both hands to be completely empty and its own toggle to be enabled. An occupied but ineligible hand cannot make the other empty hand eligible.

Tool Requirement Default allows attack-damage tools without a use action. Any (previously All) allows attack-damage items with use actions too. Off removes the tool requirement for held items. Recognized shields and whitelisted items may qualify separately; all modes respect the blacklist. None of the modes enables empty-hand guarding by itself.
