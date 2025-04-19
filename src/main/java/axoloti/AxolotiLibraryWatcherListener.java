package axoloti;

import java.nio.file.Path;

public interface AxolotiLibraryWatcherListener {
  void LibraryEntryChanged(AxolotiLibraryChangeType type, Path path);
}
