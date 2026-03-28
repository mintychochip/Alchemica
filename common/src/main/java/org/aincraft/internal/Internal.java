package org.aincraft.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.IPluginConfiguration;
import org.aincraft.IStorage;
import org.aincraft.container.IDurationStageRegistry;
import org.aincraft.container.LocationKey;
import org.aincraft.dao.CauldronDao;
import org.aincraft.dao.ICauldron;
import org.aincraft.dao.IDao;
import org.aincraft.dao.IPlayerSettings;
import org.aincraft.dao.PlayerSettingsDao;
import org.aincraft.command.BrewCommand;
import org.aincraft.gui.GuiListener;
import org.aincraft.io.RecipeYmlWriter;
import org.aincraft.providers.IPotionProvider;
import org.aincraft.providers.IVersionProviders;
import org.aincraft.providers.VersionProviderFactory;
import org.aincraft.storage.DatabaseFactory;
import org.aincraft.storage.Extractor.ResourceExtractor;
import org.aincraft.wizard.LoreCaptureManager;
import org.aincraft.wizard.WizardSessionFactory;
import org.aincraft.wizard.WizardSessionManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

final class Internal {

  final RecipeRegistry recipeRegistry;
  final IDurationStageRegistry durationRegistry;
  final IPotionDurationMap potionDurationMap;
  final IStorage database;
  final Gson gson;
  final IDao<ICauldron, LocationKey> cauldronDao;
  final IVersionProviders versionProviders;
  final Set<Material> stirrers;
  final IDao<IPlayerSettings, UUID> playerSettingsDao;
  final WizardSessionManager wizardSessionManager;
  final LoreCaptureManager loreCaptureManager;
  final RecipeYmlWriter recipeYmlWriter;
  final WizardSessionFactory wizardSessionFactory;

  Internal(
      RecipeRegistry recipeRegistry,
      IDurationStageRegistry durationRegistry,
      IPotionDurationMap potionDurationMap,
      IStorage database,
      Gson gson,
      IDao<ICauldron, LocationKey> cauldronDao, IVersionProviders versionProviders,
      Set<Material> stirrers,
      IDao<IPlayerSettings, UUID> playerSettingsDao,
      WizardSessionManager wizardSessionManager,
      LoreCaptureManager loreCaptureManager,
      RecipeYmlWriter recipeYmlWriter,
      WizardSessionFactory wizardSessionFactory) {
    this.recipeRegistry = recipeRegistry;
    this.durationRegistry = durationRegistry;
    this.potionDurationMap = potionDurationMap;
    this.database = database;
    this.gson = gson;
    this.cauldronDao = cauldronDao;
    this.versionProviders = versionProviders;
    this.stirrers = stirrers;
    this.playerSettingsDao = playerSettingsDao;
    this.wizardSessionManager = wizardSessionManager;
    this.loreCaptureManager = loreCaptureManager;
    this.recipeYmlWriter = recipeYmlWriter;
    this.wizardSessionFactory = wizardSessionFactory;
  }

  public IVersionProviders getVersionProviders() {
    return versionProviders;
  }

  public IDurationStageRegistry getDurationRegistry() {
    return durationRegistry;
  }

  public IPotionDurationMap getPotionDurationMap() {
    return potionDurationMap;
  }

  public RecipeRegistry getRecipeRegistry() {
    return recipeRegistry;
  }

  public IStorage getDatabase() {
    return database;
  }

  public static Internal create(Brew brew, BrewAPIImpl brewAPI) {
    Plugin plugin = brew.getPlugin();
    IPluginConfiguration config = brew.getPluginConfiguration();
    VersionProviderFactory versionProviderFactory = brew.getVersionProviderFactory();
    IVersionProviders versionProviders = versionProviderFactory.create();
    IYamlConfiguration general = config.get("general");
    IPotionProvider potionProvider = versionProviders.getPotionProvider();

    IDurationStageRegistry durationRegistry = new DurationStageRegistryFactory(plugin,
        general).create();
    IPotionDurationMap durationMap = new PotionDurationMapFactory(plugin, general,
        durationRegistry, potionProvider).create();

    IStorage database = new DatabaseFactory(
        plugin.getLogger(),
        plugin,
        config.get("database"),
        new ResourceExtractor()
    ).create();

    IDao<IPlayerSettings, UUID> playerSettingsDao = new PlayerSettingsDao(database);

    IYamlConfiguration potions = config.get("potions");
    RecipeRegistry trie = new RecipeRegistryFactory(
        new PotionEffectMetaFactory(durationMap), general, potions, potionProvider)
        .create(brewAPI);

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(NamespacedKey.class, new KeyAdapter())
        .excludeFieldsWithoutExposeAnnotation()
        .create();
    Set<Material> stirrers = new StirrerSetFactory(general).create();
    IDao<ICauldron, LocationKey> cauldronDao = new CauldronDao(database, gson);

    java.io.File dataFolder = plugin.getDataFolder();
    java.io.File generalFile = new java.io.File(dataFolder, "general.yml");
    java.io.File potionsFile = new java.io.File(dataFolder, "potions.yml");
    WizardSessionManager sessionManager = new WizardSessionManager();
    LoreCaptureManager loreManager = new LoreCaptureManager();
    RecipeYmlWriter ymlWriter = new RecipeYmlWriter(generalFile, potionsFile);
    WizardSessionFactory sessionFactory = new WizardSessionFactory(generalFile, potionsFile);

    return new Internal(
        trie,
        durationRegistry,
        durationMap,
        database,
        gson,
        cauldronDao,
        versionProviders, stirrers, playerSettingsDao,
        sessionManager,
        loreManager,
        ymlWriter,
        sessionFactory
    );
  }

  private static final class KeyAdapter extends TypeAdapter<NamespacedKey> {

    @Override
    public void write(JsonWriter out, NamespacedKey value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.getNamespace() + ":" + value.getKey());
    }

    @Override
    public NamespacedKey read(JsonReader in) throws IOException {
      JsonToken peek = in.peek();
      if (peek == JsonToken.NULL) {
        return null;
      }
      return Brew.createKey(in.nextString());
    }
  }
}
