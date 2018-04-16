package com.kreezcraft.beverages;

import com.kreezcraft.beverages.block.ModBlocks;
import com.kreezcraft.beverages.item.ModItems;
import com.kreezcraft.beverages.potion.ModPotion;
import com.kreezcraft.beverages.potion.PotionEvents;
import com.kreezcraft.beverages.proxy.CommonProxy;
import com.kreezcraft.beverages.recipe.ModRecipes;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Reference.MODID, name="Brewery", version = Reference.VERSION, dependencies = "required-before:guideapi")
public class Beverages {

    @SidedProxy(clientSide = "org.teamavion.brewery.proxy.ClientProxy", serverSide = "org.teamavion.brewery.proxy.ServerProxy")
    public static CommonProxy proxy;

    @Mod.Instance(value = Reference.MODID)
    public static Beverages instance;

    @EventHandler
    public void preInt(FMLPreInitializationEvent e){
        ModBlocks.init();
        ModItems.init();
        ModPotion.init();
        ModBlocks.register();
        ModItems.register();
        ModRecipes.register();
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent e){
        proxy.init();
        new PotionEvents();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e){
        proxy.postInit();
    }
}
