package axoloti;

import static axoloti.MainFrame.mainframe;

import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.Boards.BoardDetail;

public class DeviceConnector implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(DeviceConnector.class.getName());

  private static DeviceConnector singleton = null;
  private int connectCount = 0;
  private boolean activeConnect = false;
  
  public static DeviceConnector getDeviceConnector() {
    if (singleton == null)
      singleton = new DeviceConnector();
    return singleton;
  }

  private DeviceConnector() {
  }
  
  public void tryToConnect(int seconds) {
    connectCount = seconds;
    activeConnect = true;
  }

  public void backgroundConnect() {
    connectCount = Integer.MAX_VALUE;
    activeConnect = false;
  }

  public boolean isTryingToReconnect() {
    return connectCount != 0;
  }

  public void cancel(){
    connectCount = 0;
    activeConnect = true;
  }  

  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }

      try {
        if(connectCount > 0) {
          activeConnect = true; // ARCFATAL
          MainFrame.prefs.getBoards().scanBoards();
          Boards boards = MainFrame.prefs.getBoards();

          BoardDetail boardDetail = boards.getSelectedBoardDetail();

          if(boardDetail.isAttached) {
            if(activeConnect) {
              LOGGER.log(Level.INFO, "{0} is available, connecting now.", boardDetail.serialNumber);
            }

            connectCount = 0;
            mainframe.doConnect();
            // todo the work!
          } else {
            if( activeConnect) {
              LOGGER.log(Level.INFO, "Looking for {0}", boardDetail.serialNumber);
            }
            connectCount--;
            if(connectCount == 0) {
              if(activeConnect) {
                LOGGER.log(Level.SEVERE, "Timeout looking for {0}", boardDetail.serialNumber);
              } else {
                connectCount = Integer.MAX_VALUE;
              }
            }
          }
        }
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }

      // if (queue.isEmpty() && serialconnection.isConnected()) {
      // queue.add(new QCmdPing());
      // }
    }
  }
}
