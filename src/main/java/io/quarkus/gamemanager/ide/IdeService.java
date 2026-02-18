package io.quarkus.gamemanager.ide;

import java.nio.file.Path;

public interface IdeService {
  void startInIde(Path gameDir);
}
