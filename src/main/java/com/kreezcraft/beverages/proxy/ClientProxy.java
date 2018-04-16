package com.kreezcraft.beverages.proxy;

import com.kreezcraft.beverages.block.ModBlocks;
import com.kreezcraft.beverages.item.ModItems;

@SuppressWarnings("unused")
public class ClientProxy implements CommonProxy {

    @Override
    public void preInit() {
    }

    @Override
    public void init() {
        ModBlocks.registerRenders();
        ModItems.registerRenders();
    }

    @Override
    public void postInit() {

    }
}
