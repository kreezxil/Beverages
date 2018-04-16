package org.teamavion.brewery.block.tile;

import org.teamavion.brewery.item.ModItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.teamavion.brewery.recipe.BreweryRecipeHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TileBrewery extends TileEntity implements ITickable {

    public static final int CAPACITY = 9;

    private List<Ingredient> ingredientList = new ArrayList<>(0);
    private int liquidMB, maxLiquidMB, temperature, ingredientCount, time;
    private boolean temperatureSwitchOn;

    public TileBrewery() {
        temperature = 22;
        liquidMB = 0;
        maxLiquidMB = 0;
        this.markDirty();
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            captureItems();
            if (!temperatureSwitchOn && isLit()) {
                time = 0;
                temperatureSwitchOn = true;
            }
            if (temperatureSwitchOn && !isLit()) {
                time = 0;
                temperatureSwitchOn = false;
            }
            if (isLit()) {
                if (time < 1000)
                    time++;
                if (liquidMB > 0 &&(time >= timeToIncrease()) && temperature < 101) {
                    temperature++;
                    //System.out.println(temperature);
                    time = 0;
                    sync(this);
                }
            }
            if (!isLit()) {
                if (time < 1000)
                    time++;
                if (liquidMB > 0 && (time >= timeToDecrease()) && temperature > 22) {
                    temperature--;
                    time = 0;
                    sync(this);
                }
            }
            if ((temperature > 100) && liquidMB > 0) {
                sync(this);
                liquidMB--;
                if(liquidMB == 0) {
                    temperature = 22;
                    maxLiquidMB = 0;
                    ingredientList = new ArrayList<>(0);
                }
            }

            for (Ingredient ingredient : ingredientList) {
                ingredient.temperature = this.temperature;
                sync(this);
            }
        }
    }

   public boolean createPotion(int size, boolean isSplash) {
       //If there isn't enough space in the bottle, return false
       if(!((liquidMB-((size+1)*1000)) >= 0))
           return false;

       ItemStack potion = null;
       switch (size) {
           case 0:
               potion = new ItemStack(isSplash ? ModItems.potionSplash : ModItems.potionSmall);
               break;
           case 1:
               potion = new ItemStack(ModItems.potionMedium);
               break;
           case 2:
               potion = new ItemStack(ModItems.potionLarge);
               break;
       }
       potion.setTagCompound(formPotionNBT());
       EntityItem potionEntity = new EntityItem(world, this.getPos().getX(), this.getPos().getY() + 1, this.getPos().getZ(), potion);
       potionEntity.motionY = ThreadLocalRandom.current().nextGaussian() * 0.05000000074505806D + 0.20000000298023224D;
       world.spawnEntity(potionEntity);

       liquidMB-=((size+1)*1000);
       ingredientCount = 0;
       if(liquidMB == 0) {
           temperature = 22;
           maxLiquidMB = 0;
           ingredientList = new ArrayList<>(0);
       }
       sync(this);

       return true;
    }

    public boolean addWater(){
        if(!(ingredientCount > 0) && (liquidMB+1000) <= 3000){
            liquidMB+=1000;
            maxLiquidMB = liquidMB;
            sync(this);
            return true;
        }
        return false;
    }

    private NBTTagCompound formPotionNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        for (int i = 0; i < ingredientList.size(); i++) {
            compound.setInteger("potion_ID_" + i, ingredientList.get(i).id);
            compound.setShort("potion_grade_" + i, BreweryRecipeHandler.getPotionGrade(ingredientList.get(i).id, ingredientList.get(i).amount, ingredientList.get(i).time, ingredientList.get(i).temperature,1, false, false, maxLiquidMB));
        }
        return compound;
    }

    public void captureItems() {
        List<EntityItem> entityItems = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(new BlockPos(this.getPos().getX(), this.getPos().getY()+1, this.getPos().getZ()), this.getPos()));
        for (int i = 0; i < entityItems.size(); i++) {
            System.out.println("There is an Item on top of me");
            if (addIngredient(entityItems.get(i).getEntityItem().getItem())) {
                ItemStack hold = entityItems.get(i).getEntityItem().copy();
                entityItems.get(i).setDead();
                if (hold.getCount() > 1) {
                    hold.setCount(hold.getCount() - 1);
                    world.spawnEntity(new EntityItem(world, this.getPos().getX(), this.getPos().getY() + 0.5, this.getPos().getZ(), hold));
                }
            }
        }
    }

    private boolean isLit() {
        return !world.isRemote && (world.getBlockState(this.getPos().down()).getBlock() == Blocks.FIRE
                || world.getBlockState(this.getPos().down(2)).getBlock() == Blocks.FIRE && !world.getBlockState(this.getPos().down()).isFullBlock());
    }

    private int timeToIncrease(){
        return 80;
    }

    private int timeToDecrease(){
        return 20;
    }

    public boolean addIngredient(Item item) {
        //Check if a new ingredient should be added
        if (BreweryRecipeHandler.isIngredient(item) && ingredientCount < CAPACITY && !world.isRemote) {
            //Check if ingredient already exists
            for (Ingredient ingredient : ingredientList) {
                if (ingredient.id == BreweryRecipeHandler.getPotionId(item)) {
                    ingredient.amount++;
                    ingredient.time = getNewIngredientTime(ingredient);
                    ingredientCount++;
                    return true;
                }
            }
            //Add new ingredient
            ingredientList.add(new Ingredient(BreweryRecipeHandler.getPotionId(item), 1, 22, 0));
            ingredientCount++;
            return true;
        }
        return false;
    }

    public int getTemperature(){
        return temperature;
    }

    public int getLiquidMB() {
        return liquidMB;
    }

    private int getNewIngredientTime(Ingredient ingredient) {
        return (int) (ingredient.time * ingredient.amount - 1.0 / ingredient.amount);
    }

    private class Ingredient {
        int id;
        int amount;
        int temperature;
        int time;

        Ingredient(int id, int amount, int temperature, int time)
        {
            this.id = id;
            this.amount = amount;
            this.temperature = temperature;
            this.time = time;
        }
        Ingredient(int[] array) {
            id = array[0];
            amount = array[1];
            temperature = array[2];
            time = array[3];
        }

        int[] getArray() {
            return new int[]{id, amount, temperature, time};
        }
    }


    /* NBT STUFF */


    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        temperature = compound.getInteger("temperature");
        liquidMB = compound.getInteger("liquidMB");
        maxLiquidMB = compound.getShort("maxLiquidMB");
        ingredientCount = compound.getInteger("ingredientCount");
        ingredientList = new ArrayList<>(0);
        for (int i = 0; i < ingredientCount; i++) {
            //TODO: Mystery to be solved for a rainy day.
            if(compound.hasKey("potion_"+i))
                ingredientList.add(new Ingredient(compound.getIntArray("potion_" + i)));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if(compound == null)
            return compound;
        compound.setInteger("temperature", temperature);
        compound.setInteger("liquidMB", liquidMB);
        compound.setInteger("maxLiquidMB", maxLiquidMB);
        compound.setInteger("ingredientCount", ingredientCount);
        for (int i = 0; i < ingredientList.size(); i++) {
            compound.setIntArray("potion_" + i, ingredientList.get(i).getArray());
        }
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
    }

    public static void sync(TileEntity tileEntity) {
        if (tileEntity.getWorld() != null) {
            IBlockState state = tileEntity.getWorld().getBlockState(tileEntity.getPos());
            tileEntity.getWorld().notifyBlockUpdate(tileEntity.getPos(), state, state, 3);
        }
    }
}