package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Ingredient that matches a container of fluid */
@SuppressWarnings("unused")  // API
public class FluidContainerIngredient implements ICustomIngredient {
  public static final Identifier ID = Mantle.getResource("fluid_container");
  public static final Serializer SERIALIZER = new Serializer();

  /** Ingredient to use for matching */
  private final FluidIngredient fluidIngredient;
  /** Internal ingredient to display the ingredient recipe viewers */
  @Nullable
  private final Ingredient display;
  protected FluidContainerIngredient(FluidIngredient fluidIngredient, @Nullable Ingredient display) {
    this.fluidIngredient = fluidIngredient;
    this.display = display;
  }

  /** Creates an instance from a fluid ingredient with a display container */
  public static Ingredient fromIngredient(FluidIngredient ingredient, Ingredient display) {
    return new FluidContainerIngredient(ingredient, display).toVanilla();
  }

  /** Creates an instance from a fluid ingredient with no display, not recommended */
  public static Ingredient fromIngredient(FluidIngredient ingredient) {
    return new FluidContainerIngredient(ingredient, null).toVanilla();
  }

  /** Creates an instance from a fluid ingredient with a display container */
  public static Ingredient fromFluid(FluidObject<?> fluid) {
    return fromIngredient(fluid.ingredient(FluidType.BUCKET_VOLUME), Ingredient.of(fluid));
  }

  @Override
  public boolean test(ItemStack stack) {
    // first, must have a fluid capability
    if (stack.isEmpty()) {
      return false;
    }
    // work on a single-count copy as we need to simulate draining
    ItemAccess access = ItemAccess.forStack(stack.copyWithCount(1));
    ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
    // second, must contain enough fluid in a single tank
    if (handler == null || handler.size() != 1) {
      return false;
    }
    FluidResource contained = handler.getResource(0);
    if (contained.isEmpty()) {
      return false;
    }
    Fluid fluid = contained.getFluid();
    int amount = fluidIngredient.getAmount(fluid);
    if (amount != handler.getAmountAsInt(0) || !fluidIngredient.test(fluid)) {
      return false;
    }
    // alright, we know it has the fluid, the question is just whether draining the fluid will give us the desired result
    // simulate the drain in a transaction that we never commit, so the item copy is left untouched
    try (Transaction tx = Transaction.openRoot()) {
      int drained = handler.extract(0, contained, amount, tx);
      // we need an exact match, and we need the resulting container item to be the same as the item stack's container item
      ItemResource result = access.getResource();
      return drained == amount && result.matches(stack.getItem().getCraftingRemainder());
    }
  }

  @Override
  public Stream<Holder<Item>> items() {
    // no container? unfortunately hard to display this recipe so show nothing
    return display == null ? Stream.empty() : display.items();
  }

  public JsonElement toJson() {
    JsonElement element = fluidIngredient.serialize();
    JsonObject json;
    if (element.isJsonObject()) {
      json = element.getAsJsonObject();
    } else {
      json = new JsonObject();
      json.add("fluid", element);
    }
    if (display != null) {
      json.add("display", IngredientLoadable.ALLOW_EMPTY.serialize(display));
    }
    return json;
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return MantleRecipes.FLUID_CONTAINER_INGREDIENT.get();
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof FluidContainerIngredient that && fluidIngredient.equals(that.fluidIngredient) && Objects.equals(display, that.display);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fluidIngredient, display);
  }

  /** Serializer logic */
  public static class Serializer {
    public FluidContainerIngredient parse(JsonObject json) {
      FluidIngredient fluidIngredient;
      // if we have fluid and its not a primitive, then its nested
      if (json.has("fluid") && !json.get("fluid").isJsonPrimitive()) {
        fluidIngredient = FluidIngredient.LOADABLE.getIfPresent(json, "fluid");
      } else {
        fluidIngredient = FluidIngredient.LOADABLE.convert(json, "fluid");
      }
      Ingredient display = null;
      if (json.has("display")) {
        display = IngredientLoadable.ALLOW_EMPTY.convert(JsonHelper.getElement(json, "display"), "display");
      }
      return new FluidContainerIngredient(fluidIngredient, display);
    }

    public FluidContainerIngredient parse(FriendlyByteBuf buffer) {
      FluidIngredient fluidIngredient = FluidIngredient.LOADABLE.decode(buffer);
      Ingredient display = null;
      if (buffer.readBoolean()) {
        display = Ingredient.CONTENTS_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
      }
      return new FluidContainerIngredient(fluidIngredient, display);
    }

    public void write(FriendlyByteBuf buffer, FluidContainerIngredient ingredient) {
      FluidIngredient.LOADABLE.encode(buffer, ingredient.fluidIngredient);
      if (ingredient.display != null) {
        buffer.writeBoolean(true);
        Ingredient.CONTENTS_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, ingredient.display);
      } else {
        buffer.writeBoolean(false);
      }
    }

    public MapCodec<FluidContainerIngredient> codec() {
      return MapCodec.assumeMapUnsafe(Codec.PASSTHROUGH.xmap(dynamic -> {
        JsonElement json = dynamic.convert(JsonOps.INSTANCE).getValue();
        return parse(json.getAsJsonObject());
      }, ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson())));
    }

    public StreamCodec<RegistryFriendlyByteBuf,FluidContainerIngredient> streamCodec() {
      return StreamCodec.of(this::write, this::parse);
    }
  }
}
