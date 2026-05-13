package autismclient.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

final class PackUtilLocalCraftingRegistry {
    private static final Object LOCK = new Object();

    private static ResourceManager cachedResourceManager;
    private static int cachedModCount = -1;
    private static List<LocalCraftingRecipe> cachedRecipes = List.of();

    private PackUtilLocalCraftingRegistry() {
    }

    static List<LocalCraftingRecipe> getRecipes(Minecraft mc) {
        if (mc == null) return List.of();

        ResourceManager resourceManager = mc.getResourceManager();
        int modCount = FabricLoader.getInstance().getAllMods().size();

        synchronized (LOCK) {
            if (cachedResourceManager == resourceManager && cachedModCount == modCount && !cachedRecipes.isEmpty()) {
                return cachedRecipes;
            }

            cachedRecipes = loadRecipes(
                resourceManager,
                mc.getConnection() != null ? mc.getConnection().enabledFeatures() : FeatureFlagSet.of()
            );
            cachedResourceManager = resourceManager;
            cachedModCount = modCount;
            return cachedRecipes;
        }
    }

    static final class LocalCraftingRecipe {
        final String recipeKey;
        final ItemStack result;
        final int width;
        final int height;
        final boolean shaped;
        final List<Ingredient> gridIngredients;
        final List<Ingredient> requirements;
        final String signature;

        LocalCraftingRecipe(
            String recipeKey,
            ItemStack result,
            int width,
            int height,
            boolean shaped,
            List<Ingredient> gridIngredients,
            List<Ingredient> requirements,
            String signature
        ) {
            this.recipeKey = recipeKey;
            this.result = result.copy();
            this.width = width;
            this.height = height;
            this.shaped = shaped;
            this.gridIngredients = List.copyOf(gridIngredients);
            this.requirements = List.copyOf(requirements);
            this.signature = signature;
        }

        boolean fits(int gridWidth, int gridHeight) {
            if (shaped) return width <= gridWidth && height <= gridHeight;
            return requirements.size() <= gridWidth * gridHeight;
        }
    }

    private static final class ParsedIngredient {
        final Ingredient ingredient;
        final String signature;

        private ParsedIngredient(Ingredient ingredient, String signature) {
            this.ingredient = ingredient;
            this.signature = signature;
        }
    }

    private static List<LocalCraftingRecipe> loadRecipes(ResourceManager resourceManager, FeatureFlagSet enabledFeatures) {
        LinkedHashMap<String, LocalCraftingRecipe> byKey = new LinkedHashMap<>();
        loadRecipesFromModRoots(byKey, enabledFeatures);
        mergeResourceManagerRecipes(byKey, resourceManager, enabledFeatures);

        List<LocalCraftingRecipe> recipes = new ArrayList<>(byKey.values());
        recipes.sort(Comparator.comparing(recipe -> recipe.recipeKey, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(recipes);
    }

    private static void loadRecipesFromModRoots(Map<String, LocalCraftingRecipe> byKey, FeatureFlagSet enabledFeatures) {
        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            for (Path rootPath : modContainer.getRootPaths()) {
                Path dataRoot = rootPath.resolve("data");
                if (!Files.exists(dataRoot)) continue;

                try (Stream<Path> paths = Files.walk(dataRoot)) {
                    for (Path recipePath : paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName() != null && path.getFileName().toString().endsWith(".json"))
                        .toList()) {
                        LocalCraftingRecipe recipe = parseRecipePath(dataRoot, recipePath, enabledFeatures);
                        if (recipe != null) {
                            byKey.put(recipe.recipeKey, recipe);
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void mergeResourceManagerRecipes(Map<String, LocalCraftingRecipe> byKey, ResourceManager resourceManager, FeatureFlagSet enabledFeatures) {
        if (resourceManager == null) return;

        Map<Identifier, Resource> resources = resourceManager.listResources("recipe", id -> id.getPath().endsWith(".json"));
        if (resources.isEmpty()) return;

        List<Map.Entry<Identifier, Resource>> ordered = new ArrayList<>(resources.entrySet());
        ordered.sort(Comparator.comparing(entry -> entry.getKey().toString(), String.CASE_INSENSITIVE_ORDER));

        for (Map.Entry<Identifier, Resource> entry : ordered) {
            LocalCraftingRecipe recipe = parseRecipe(entry.getKey(), entry.getValue(), enabledFeatures);
            if (recipe != null) {
                byKey.put(recipe.recipeKey, recipe);
            }
        }
    }

    private static LocalCraftingRecipe parseRecipePath(Path dataRoot, Path recipePath, FeatureFlagSet enabledFeatures) {
        try {
            Path relative = dataRoot.relativize(recipePath);
            if (relative.getNameCount() < 3) return null;

            String namespace = relative.getName(0).toString();
            String folder = relative.getName(1).toString();
            if (!"recipe".equals(folder)) return null;

            String path = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
            Identifier resourceId = Identifier.fromNamespaceAndPath(namespace, path);

            try (BufferedReader reader = Files.newBufferedReader(recipePath)) {
                return parseRecipe(resourceId, reader, enabledFeatures);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalCraftingRecipe parseRecipe(Identifier resourceId, Resource resource, FeatureFlagSet enabledFeatures) {
        try (BufferedReader reader = resource.openAsReader()) {
            return parseRecipe(resourceId, reader, enabledFeatures);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static LocalCraftingRecipe parseRecipe(Identifier resourceId, BufferedReader reader, FeatureFlagSet enabledFeatures) {
        try {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String type = getString(root, "type");
            if (!"minecraft:crafting_shaped".equals(type) && !"minecraft:crafting_shapeless".equals(type)) {
                return null;
            }

            ItemStack result = parseResult(root.getAsJsonObject("result"));
            if (result == null || result.isEmpty() || !result.isItemEnabled(enabledFeatures)) {
                return null;
            }

            String recipeKey = normalizeRecipeKey(resourceId);
            return "minecraft:crafting_shaped".equals(type)
                ? parseShaped(recipeKey, result, root)
                : parseShapeless(recipeKey, result, root);
        } catch (IllegalStateException | JsonParseException ignored) {
            return null;
        }
    }

    private static LocalCraftingRecipe parseShaped(String recipeKey, ItemStack result, JsonObject root) {
        JsonArray pattern = root.getAsJsonArray("pattern");
        JsonObject key = root.getAsJsonObject("key");
        if (pattern == null || key == null || pattern.isEmpty()) return null;

        int height = pattern.size();
        int width = 0;
        for (JsonElement rowElement : pattern) {
            if (!rowElement.isJsonPrimitive()) return null;
            width = Math.max(width, rowElement.getAsString().length());
        }
        if (width <= 0) return null;

        List<Ingredient> gridIngredients = new ArrayList<>(width * height);
        List<Ingredient> requirements = new ArrayList<>();
        List<String> signatureParts = new ArrayList<>();

        for (JsonElement rowElement : pattern) {
            String row = rowElement.getAsString();
            for (int col = 0; col < width; col++) {
                char symbol = col < row.length() ? row.charAt(col) : ' ';
                if (symbol == ' ') {
                    gridIngredients.add(null);
                    signatureParts.add("_");
                    continue;
                }

                JsonElement ingredientElement = key.get(String.valueOf(symbol));
                ParsedIngredient parsed = parseIngredient(ingredientElement);
                if (parsed == null || parsed.ingredient.isEmpty()) {
                    return null;
                }

                gridIngredients.add(parsed.ingredient);
                requirements.add(parsed.ingredient);
                signatureParts.add(parsed.signature);
            }
        }

        String signature = buildSignature(result, true, width, height, signatureParts);
        return new LocalCraftingRecipe(recipeKey, result, width, height, true, gridIngredients, requirements, signature);
    }

    private static LocalCraftingRecipe parseShapeless(String recipeKey, ItemStack result, JsonObject root) {
        JsonArray ingredients = root.getAsJsonArray("ingredients");
        if (ingredients == null || ingredients.isEmpty()) return null;

        List<Ingredient> gridIngredients = new ArrayList<>(ingredients.size());
        List<Ingredient> requirements = new ArrayList<>(ingredients.size());
        List<String> signatureParts = new ArrayList<>(ingredients.size());

        for (JsonElement ingredientElement : ingredients) {
            ParsedIngredient parsed = parseIngredient(ingredientElement);
            if (parsed == null || parsed.ingredient.isEmpty()) {
                return null;
            }

            gridIngredients.add(parsed.ingredient);
            requirements.add(parsed.ingredient);
            signatureParts.add(parsed.signature);
        }

        signatureParts.sort(String.CASE_INSENSITIVE_ORDER);
        String signature = buildSignature(result, false, ingredients.size(), 1, signatureParts);
        return new LocalCraftingRecipe(recipeKey, result, ingredients.size(), 1, false, gridIngredients, requirements, signature);
    }

    private static ItemStack parseResult(JsonObject resultObject) {
        if (resultObject == null) return ItemStack.EMPTY;

        Identifier itemId = parseIdentifier(resultObject, "id", "item");
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item == null) return ItemStack.EMPTY;

        int count = Math.max(1, rootInt(resultObject, "count", 1));
        return new ItemStack(item, count);
    }

    private static ParsedIngredient parseIngredient(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;

        LinkedHashMap<Identifier, Item> resolvedItems = new LinkedHashMap<>();
        collectItems(element, resolvedItems);
        if (resolvedItems.isEmpty()) return null;

        Ingredient ingredient = Ingredient.of(resolvedItems.values().stream());
        if (ingredient.isEmpty()) return null;

        TreeSet<String> orderedIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Identifier id : resolvedItems.keySet()) {
            orderedIds.add(id.toString());
        }

        return new ParsedIngredient(ingredient, String.join(",", orderedIds));
    }

    private static void collectItems(JsonElement element, Map<Identifier, Item> resolvedItems) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectItems(child, resolvedItems);
            }
            return;
        }

        if (element.isJsonPrimitive()) {
            String raw = element.getAsString().trim();
            if (raw.isEmpty()) return;
            if (raw.startsWith("#")) addTagItems(raw.substring(1), resolvedItems);
            else addItem(raw, resolvedItems);
            return;
        }

        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();

        Identifier itemId = parseIdentifier(object, "id", "item");
        if (itemId != null) {
            addItem(itemId.toString(), resolvedItems);
        }

        Identifier tagId = parseIdentifier(object, "tag");
        if (tagId != null) {
            addTagItems(tagId.toString(), resolvedItems);
        }
    }

    private static void addItem(String rawId, Map<Identifier, Item> resolvedItems) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item != null) {
            resolvedItems.put(id, item);
        }
    }

    private static void addTagItems(String rawTagId, Map<Identifier, Item> resolvedItems) {
        Identifier tagId = Identifier.tryParse(rawTagId);
        if (tagId == null) return;

        TagKey<Item> key = TagKey.create(Registries.ITEM, tagId);
        for (Holder<Item> entry : BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
            Item item = entry.value();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null) {
                resolvedItems.put(itemId, item);
            }
        }
    }

    private static String buildSignature(ItemStack result, boolean shaped, int width, int height, Collection<String> parts) {
        Identifier resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        String resultKey = resultId == null ? result.getHoverName().getString() : resultId.toString();
        return resultKey + "|" + result.getCount() + "|" + (shaped ? "shaped" : "shapeless") + "|" + width + "x" + height + "|" + String.join(";", parts);
    }

    private static String normalizeRecipeKey(Identifier resourceId) {
        String path = resourceId.getPath();
        if (path.startsWith("recipe/")) {
            path = path.substring("recipe/".length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path).toString();
    }

    private static Identifier parseIdentifier(JsonObject object, String... keys) {
        if (object == null || keys == null) return null;
        for (String key : keys) {
            if (key == null || !object.has(key)) continue;
            JsonElement element = object.get(key);
            if (element != null && element.isJsonPrimitive()) {
                return Identifier.tryParse(element.getAsString());
            }
        }
        return null;
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return "";
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static int rootInt(JsonObject object, String key, int fallback) {
        if (object == null || key == null || !object.has(key)) return fallback;
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return fallback;
        try {
            return element.getAsInt();
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
