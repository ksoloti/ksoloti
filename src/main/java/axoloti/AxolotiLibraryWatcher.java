package axoloti;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;

import java.awt.EventQueue;

import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

public class AxolotiLibraryWatcher implements Runnable {

  public enum AxolotiLibraryChangeType {
    Unknown,
    Deleted,
    Created,
    Modified
  }
  
  private static AxolotiLibraryWatcher singleton = null;

  private WatchService watcher;
  private Map<WatchKey, Path> keys;
  private boolean recursive;
  private boolean trace;

  private ArrayList<AxolotiLibraryWatcherListener> listeners = new ArrayList<AxolotiLibraryWatcherListener>();

  public static AxolotiLibraryWatcher getAxolotiLibraryWatcher() {
    if (singleton == null)
      singleton = new AxolotiLibraryWatcher();
    return singleton;
  }

  private AxolotiLibraryWatcher() {
    try {
      this.watcher = FileSystems.getDefault().newWatchService();
      this.keys = new HashMap<WatchKey,Path>();
      this.recursive = true;
    } catch (Exception ex) {}
  }

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  @Override
  public void run() {
    processEvents();
  }


  public void addListener(AxolotiLibraryWatcherListener listener) {
      listeners.add(listener);
  }

  public void removeConnectionStatusListener(AxolotiLibraryWatcherListener listener) {
      listeners.remove(listener);
  }

  /**
   * Register the given directory with the WatchService
   */
  private void register(Path dir) throws IOException {
    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    if (trace) {
      Path prev = keys.get(key);
      if (prev == null) {
        System.out.format("register: %s\n", dir);
      } else {
        if (!dir.equals(prev)) {
          System.out.format("update: %s -> %s\n", prev, dir);
        }
      }
    }
    keys.put(key, dir);
  }

  /**
   * Register the given directory, and all its sub-directories, with the
   * WatchService.
   */
  private void registerAll(final Path start) throws IOException {
    // register directory and sub-directories
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  
  public void AddFolder(String folder) {
    System.out.format("Scanning folder %s ...\n", folder);
    Path path = Paths.get(folder);
    try {
      registerAll(path);
    } catch (Exception ex) {
    }
  }

  // public void AddAxolotiLib(AxolotiLibrary lib) throws IOException {
      
  //     Path dir = Paths.get(lib.getLocalLocation() + "/objects");
  //     System.out.format("Scanning %s ...\n", dir);
  //     registerAll(dir);
  //     System.out.println("Done.");

  //     // enable trace after initial registration
  //     this.trace = true;
  // }

  /**
   * Process all events for keys queued to the watcher
   */
  void processEvents() {
      while(true) {
          // wait for key to be signalled
          WatchKey key;
          try {
              key = watcher.take();
          } catch (InterruptedException x) {
              return;
          }

          Path dir = keys.get(key);
          if (dir == null) {
              System.err.println("WatchKey not recognized!!");
              continue;
          }

          for (WatchEvent<?> event: key.pollEvents()) {
              @SuppressWarnings("rawtypes")
              WatchEvent.Kind kind = event.kind();

              // TBD - provide example of how OVERFLOW event is handled
              if (kind == OVERFLOW) {
                  continue;
              }

              // Context for directory entry event is the file name of entry
              WatchEvent<Path> ev = cast(event);
              Path name = ev.context();
              Path child = dir.resolve(name);

              // print out event
              System.out.format("%s: %s\n", event.kind().name(), child);
              EventQueue.invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    AxolotiLibraryChangeType ct;
                    if(kind == ENTRY_CREATE) {
                      ct = AxolotiLibraryChangeType.Created;
                    } else if(kind == ENTRY_MODIFY) {
                      ct = AxolotiLibraryChangeType.Modified;
                    } else if(kind == ENTRY_DELETE) {
                      ct = AxolotiLibraryChangeType.Deleted;
                    } else {
                      ct = AxolotiLibraryChangeType.Unknown;
                    }

                    for(AxolotiLibraryWatcherListener listener : listeners) {
                      listener.LibraryEntryChanged(ct, child);
                    }
                  }
              });

              // if directory is created, and watching recursively, then
              // register it and its sub-directories
              if (recursive && (kind == ENTRY_CREATE)) {
                  try {
                      if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                          registerAll(child);
                      }
                  } catch (IOException x) {
                      // ignore to keep sample readbale
                  }
              }
          }

          // reset key and remove from set if directory no longer accessible
          boolean valid = key.reset();
          if (!valid) {
              keys.remove(key);

              // all directories are inaccessible
              if (keys.isEmpty()) {
                  break;
              }
          }
      }
    }

  
}