package axoloti;

import java.nio.file.Path;

import axoloti.AxolotiLibraryWatcher.AxolotiLibraryChangeType;

public interface AxolotiLibraryWatcherListener {
  void LibraryEntryChanged(AxolotiLibraryChangeType type, Path path);
}
