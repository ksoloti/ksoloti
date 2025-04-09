package axoloti;

import static axoloti.MainFrame.mainframe;

import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.Boards.BoardDetail;

public class DeviceConnector implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(DeviceConnector.class.getName());

  private static DeviceConnector singleton = null;
  private int connectCount = 0;
  
  public static DeviceConnector getDeviceConnector() {
    if (singleton == null)
      singleton = new DeviceConnector();
    return singleton;
  }

  public void tryToConnect(int seconds) {
    connectCount = seconds;
  }

  public boolean isTryingToReconnect() {
    return connectCount != 0;
  }

  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
      if(connectCount > 0) {
        MainFrame.prefs.getBoards().scanBoards();
        Boards boards = MainFrame.prefs.getBoards();

        BoardDetail boardDetail = boards.getSelectedBoardDetail();

        if(boardDetail.isConnected) {
          LOGGER.log(Level.INFO, "{0} is available, connecting now.", boardDetail.serialNumber);
          connectCount = 0;
          mainframe.doConnect();
          // todo the work!
        } else {
          LOGGER.log(Level.INFO, "Looking for {0}", boardDetail.serialNumber);
          connectCount--;
          if(connectCount == 0) {
            LOGGER.log(Level.SEVERE, "Timedout looking for {0}", boardDetail.serialNumber);
          }
        }
      }

      // if (queue.isEmpty() && serialconnection.isConnected()) {
      // queue.add(new QCmdPing());
      // }
    }
  }
}
