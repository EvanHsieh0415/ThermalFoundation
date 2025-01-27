package cofh.thermal.core.util.managers.dynamo;

import cofh.lib.inventory.FalseIInventory;
import cofh.thermal.core.ThermalCore;
import cofh.thermal.core.init.TCoreRecipeTypes;
import cofh.thermal.core.util.recipes.dynamo.GourmandFuel;
import cofh.thermal.lib.util.managers.SingleItemFuelManager;
import cofh.thermal.lib.util.recipes.ThermalFuel;
import cofh.thermal.lib.util.recipes.internal.IDynamoFuel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class GourmandFuelManager extends SingleItemFuelManager {

    private static final GourmandFuelManager INSTANCE = new GourmandFuelManager();
    protected static final int DEFAULT_ENERGY = 1600;

    public static GourmandFuelManager instance() {

        return INSTANCE;
    }

    private GourmandFuelManager() {

        super(DEFAULT_ENERGY);
    }

    @Override
    public boolean validFuel(ItemStack input) {

        if (input.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent()) {
            return false;
        }
        return getEnergy(input) > 0;
    }

    @Override
    protected void clear() {

        fuelMap.clear();
        convertedFuels.clear();
    }

    public int getEnergy(ItemStack stack) {

        IDynamoFuel fuel = getFuel(stack);
        return fuel != null ? fuel.getEnergy() : getEnergyFromFood(stack);
    }

    public int getEnergyFromFood(ItemStack stack) {

        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem().hasContainerItem(stack)) {
            return 0;
        }
        Food food = stack.getItem().getFoodProperties();
        if (food == null) {
            return 0;
        }
        int energy = food.getNutrition() * DEFAULT_ENERGY;

        if (food.getEffects().size() > 0) {
            for (Pair<EffectInstance, Float> effect : food.getEffects()) {
                if (effect.getFirst().getEffect().getCategory() == EffectType.HARMFUL) {
                    return 0;
                }
            }
            energy *= 2;
        }
        if (food.getSaturationModifier() > 1.0F) {
            energy *= 4;
        }
        if (food.isFastFood()) {
            energy *= 2;
        }
        return energy >= MIN_ENERGY ? energy : 0;
    }

    // region IManager
    @Override
    public void refresh(RecipeManager recipeManager) {

        clear();
        Map<ResourceLocation, IRecipe<FalseIInventory>> recipes = recipeManager.byType(TCoreRecipeTypes.FUEL_GOURMAND);
        for (Map.Entry<ResourceLocation, IRecipe<FalseIInventory>> entry : recipes.entrySet()) {
            addFuel((ThermalFuel) entry.getValue());
        }
        createConvertedRecipes(recipeManager);
    }
    // endregion

    // region CONVERSION
    protected List<GourmandFuel> convertedFuels = new ArrayList<>();

    public List<GourmandFuel> getConvertedFuels() {

        return convertedFuels;
    }

    protected void createConvertedRecipes(RecipeManager recipeManager) {

        ItemStack query;
        for (Item item : ForgeRegistries.ITEMS) {
            query = new ItemStack(item);
            try {
                if (getFuel(query) == null && validFuel(query)) {
                    convertedFuels.add(convert(query, getEnergy(query)));
                }
            } catch (Exception e) { // pokemon!
                ThermalCore.LOG.error(query.getItem().getRegistryName() + " threw an exception when querying the fuel value as the mod author is doing non-standard things in their item code (possibly tag related). It may not display in JEI but should function as fuel.");
            }
        }
    }

    protected GourmandFuel convert(ItemStack item, int energy) {

        return new GourmandFuel(new ResourceLocation(ID_THERMAL, "gourmand_" + item.getItem().getRegistryName().getPath()), energy, singletonList(Ingredient.of(item)), emptyList());
    }
    // endregion
}
