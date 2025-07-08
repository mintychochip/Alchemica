package org.aincraft.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.aincraft.IConfiguration.IYamlConfiguration;
import org.aincraft.IPluginConfiguration;
import org.aincraft.IStorage;
import org.aincraft.container.IDurationStageRegistry;
import org.aincraft.providers.IPotionProvider;
import org.aincraft.providers.IVersionProviders;
import org.aincraft.providers.VersionProviderFactory;
import org.aincraft.storage.DatabaseFactory;
import org.aincraft.storage.Extractor.ResourceExtractor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

final class Internal {

  final Trie potionTrie;
  final IDurationStageRegistry durationRegistry;
  final IPotionDurationMap potionDurationMap;
  final IStorage database;
  final Gson gson;
  final CauldronDao cauldronDao;
  final IVersionProviders versionProviders;

  Internal(
      Trie potionTrie,
      IDurationStageRegistry durationRegistry,
      IPotionDurationMap potionDurationMap,
      IStorage database,
      Gson gson,
      CauldronDao cauldronDao, IVersionProviders versionProviders) {
    this.potionTrie = potionTrie;
    this.durationRegistry = durationRegistry;
    this.potionDurationMap = potionDurationMap;
    this.database = database;
    this.gson = gson;
    this.cauldronDao = cauldronDao;
    this.versionProviders = versionProviders;
  }

  public IVersionProviders getVersionProviders() {
    return versionProviders;
  }

  public CauldronDao getCauldronDao() {
    return cauldronDao;
  }

  public IDurationStageRegistry getDurationRegistry() {
    return durationRegistry;
  }

  public IPotionDurationMap getPotionDurationMap() {
    return potionDurationMap;
  }

  public Trie getPotionTrie() {
    return potionTrie;
  }

  public IStorage getDatabase() {
    return database;
  }

  public static Internal create(Brew brew) {
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

    Trie trie = new PotionTrieFactory(
        new PotionEffectMetaFactory(durationMap), general, potionProvider).create();

    IStorage database = new DatabaseFactory(
        plugin.getLogger(),
        plugin,
        config.get("database"),
        new ResourceExtractor()
    ).create();

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(NamespacedKey.class, new KeyAdapter())
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    CauldronDao cauldronDao = new CauldronDao(database, gson);

    return new Internal(
        trie,
        durationRegistry,
        durationMap,
        database,
        gson,
        cauldronDao,
        versionProviders
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
