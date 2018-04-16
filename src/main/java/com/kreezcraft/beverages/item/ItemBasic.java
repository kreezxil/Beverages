package com.kreezcraft.beverages.item;

import com.kreezcraft.beverages.Reference;

import net.minecraft.item.Item;

public class ItemBasic extends Item {

    ItemBasic(String unlocalizedName, String registryName, int maxStackSize) {
        setCreativeTab(Reference.BREWERY_TAB);
        setMaxStackSize(maxStackSize);
        setUnlocalizedName(unlocalizedName);
        setRegistryName(registryName);
    }
}