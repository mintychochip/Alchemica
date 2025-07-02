package org.aincraft.container;

import org.aincraft.IRegistry;
import org.aincraft.IDurationStage;

public interface IDurationStageRegistry extends IRegistry<IDurationStage> {
  IDurationStage step(IDurationStage current, int steps);
}
