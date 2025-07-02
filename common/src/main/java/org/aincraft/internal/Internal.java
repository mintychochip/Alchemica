package org.aincraft.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.aincraft.config.IConfiguration.IYamlConfiguration;
import org.aincraft.config.IPluginConfiguration;
import org.aincraft.container.IDurationStageRegistry;
import org.aincraft.IPotionTrie;
import org.aincraft.IRegistry;
import org.aincraft.IStorage;
import org.aincraft.container.RegistrableItem;
import org.aincraft.internal.Node.ConsumerNode;
import org.aincraft.storage.DatabaseFactory;
import org.aincraft.storage.Extractor.ResourceExtractor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

final class Internal {

  private final IRegistry<RegistrableItem> itemRegistry;
  private final KeyParser keyParser;
  private final IPotionTrie potionTrie;
  private final IDurationStageRegistry durationRegistry;
  private final IPotionDurationMap potionDurationMap;
  private final IStorage database;
  private final Gson gson;
  private final CauldronDao cauldronDao;

  Internal(
      IRegistry<RegistrableItem> itemRegistry,
      KeyParser keyParser,
      IPotionTrie potionTrie,
      IDurationStageRegistry durationRegistry,
      IPotionDurationMap potionDurationMap,
      IStorage database,
      Gson gson,
      CauldronDao cauldronDao) {
    this.itemRegistry = itemRegistry;
    this.keyParser = keyParser;
    this.potionTrie = potionTrie;
    this.durationRegistry = durationRegistry;
    this.potionDurationMap = potionDurationMap;
    this.database = database;
    this.gson = gson;
    this.cauldronDao = cauldronDao;
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

  public IRegistry<RegistrableItem> getItemRegistry() {
    return itemRegistry;
  }

  public IPotionTrie getPotionTrie() {
    return potionTrie;
  }

  public IStorage getDatabase() {
    return database;
  }

  public KeyParser getKeyParser() {
    return keyParser;
  }

  public static Internal create(Brew brew) {
    Plugin plugin = brew.getPlugin();
    IPluginConfiguration config = brew.getPluginConfiguration();
    IYamlConfiguration general = config.getGeneralConfiguration();

    IRegistry<RegistrableItem> itemRegistry = new SimpleRegistry<>();

    KeyParser keyParser = new KeyParser(itemRegistry);
    IDurationStageRegistry durationRegistry = new DurationStageRegistryFactory(plugin,
        general).create();
    IPotionDurationMap durationMap = new PotionDurationMapFactory(plugin, general,
        durationRegistry).create();

    List<ConsumerNode> effectNodes = new EffectNodeRegistryFactory(plugin, general,
        keyParser).create();
    List<ConsumerNode> modifierNodes = new ModifierNodeRegistryFactory(plugin, general, keyParser,
        durationRegistry).create();

    IPotionTrie trie = new PotionTrieFactory(effectNodes, modifierNodes).create();

    IStorage database = new DatabaseFactory(
        plugin.getLogger(),
        plugin,
        config.getDatabaseConfiguration(),
        new ResourceExtractor()
    ).create();

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Key.class, new KeyAdapter())
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    CauldronDao cauldronDao = new CauldronDao(database, gson);

    return new Internal(
        itemRegistry,
        keyParser,
        trie,
        durationRegistry,
        durationMap,
        database,
        gson,
        cauldronDao
    );
  }

  private static final class KeyAdapter extends TypeAdapter<Key> {

    @Override
    public void write(JsonWriter out, Key value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.namespace() + ":" + value.value());
    }

    @Override
    public Key read(JsonReader in) throws IOException {
      JsonToken peek = in.peek();
      if (peek == JsonToken.NULL) {
        return null;
      }
      String raw = in.nextString();
      String[] split = raw.split(":");
      return new NamespacedKey(split[0], split[1]);
    }
  }
}
