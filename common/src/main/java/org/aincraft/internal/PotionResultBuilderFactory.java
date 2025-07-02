package org.aincraft.internal;

import java.util.ArrayList;
import java.util.HashMap;
import org.aincraft.IFactory;
import org.aincraft.IPotionResult.IPotionResultBuilder;
import org.aincraft.IVersionAdapter;
import org.aincraft.internal.PotionResult.PotionResultBuilder;

final class PotionResultBuilderFactory implements IFactory<IPotionResultBuilder> {

  private final PotionEffectMetaFactory metaFactory;
  private final IVersionAdapter versionAdapter;

  PotionResultBuilderFactory(IPotionDurationMap durationMap, IVersionAdapter versionAdapter) {
    this.metaFactory = new PotionEffectMetaFactory(durationMap);
    this.versionAdapter = versionAdapter;
  }

  @Override
  public IPotionResultBuilder create() {
    return new PotionResultBuilder(metaFactory, versionAdapter, null, new ArrayList<>(), new HashMap<>());
  }
}
