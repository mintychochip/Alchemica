package org.aincraft.container;

import org.aincraft.internal.IDurationStage;

public interface IDurationStageRegistry extends IRegistry<IDurationStage> {
  IDurationStage step(IDurationStage current, int steps);
}
