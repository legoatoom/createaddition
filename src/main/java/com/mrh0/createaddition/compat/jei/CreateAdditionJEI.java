package com.mrh0.createaddition.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.index.CAItems;
import com.mrh0.createaddition.recipe.charging.ChargingRecipe;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;

@JeiPlugin
public class CreateAdditionJEI implements IModPlugin {

	private static final ResourceLocation ID = new ResourceLocation(CreateAddition.MODID, "jei_plugin");

	@Override
	@Nonnull
	public ResourceLocation getPluginUid() {
		return ID;
	}
	
	public IIngredientManager ingredientManager;
	final List<CreateRecipeCategory<?>> ALL = new ArrayList<>();
	
	final CreateRecipeCategory<?> rolling = register("rolling", RollingMillCategory::new)
		.recipes(RollingRecipe.TYPE)
		.catalyst(CABlocks.ROLLING_MILL::get)
		.build();
	
	final CreateRecipeCategory<?> charging = register("charging", ChargingCategory::new)
		.recipes(ChargingRecipe.TYPE)
		.catalyst(CABlocks.TESLA_COIL::get)
		.build();

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		ALL.forEach(registration::addRecipeCategories);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		ingredientManager = registration.getIngredientManager();
		ALL.forEach(c -> c.recipes.forEach(s -> registration.addRecipes(s.get(), c.getUid())));
		
		List<ConversionRecipe> r1 = new ArrayList<>();
		//r1.add(ConversionRecipe.create(AllItems.CHROMATIC_COMPOUND.asStack(), CAItems.OVERCHARGED_ALLOY.asStack()));

		registration.addRecipes(r1, new ResourceLocation("create:mystery_conversion"));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		ALL.forEach(c -> c.recipeCatalysts.forEach(s -> registration.addRecipeCatalyst(s.get(), c.getUid())));
		
		registration.addRecipeCatalyst(new ItemStack(CAItems.DIAMOND_GRIT_SANDPAPER.get()), new ResourceLocation(Create.ID, "sandpaper_polishing"));
		//registration.addRecipeCatalyst(new ItemStack(CAItems.DIAMOND_GRIT_SANDPAPER.get()), new ResourceLocation(Create.ID, "deploying"));
	}
	
	private <T extends Recipe<?>> CategoryBuilder<T> register(String name, Supplier<CreateRecipeCategory<T>> supplier) {
		return new CategoryBuilder<T>(name, supplier);
	}
	
	private class CategoryBuilder<T extends Recipe<?>> {
		CreateRecipeCategory<T> category;

		CategoryBuilder(String name, Supplier<CreateRecipeCategory<T>> category) {
			this.category = category.get();
			this.category.setCategoryId(name);
		}

		CategoryBuilder<T> catalyst(Supplier<ItemLike> supplier) {
			return catalystStack(() -> new ItemStack(supplier.get()
				.asItem()));
		}

		CategoryBuilder<T> catalystStack(Supplier<ItemStack> supplier) {
			category.recipeCatalysts.add(supplier);
			return this;
		}

		CategoryBuilder<T> recipes(RecipeType<?> recipeTypeEntry) {
			category.recipes.add(() -> (List<T>) findRecipesByType(recipeTypeEntry));
			return this;
		}

		CreateRecipeCategory<T> build() {
			ALL.add(category);
			return category;
		}
	}
	
	static List<Recipe<?>> findRecipesByType(RecipeType<?> type) {
		return findRecipes(r -> r.getType() == type);
	}
	
	@SuppressWarnings("resource")
	static List<Recipe<?>> findRecipes(Predicate<Recipe<?>> predicate) {
		return Minecraft.getInstance().level.getRecipeManager()
			.getRecipes()
			.stream()
			.filter(predicate)
			.collect(Collectors.toList());
	}
}
