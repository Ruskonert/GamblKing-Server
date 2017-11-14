package com.ruskonert.GamblKing.engine.connect.server;

import com.ruskonert.GamblKing.engine.program.ProgramManager;
import com.ruskonert.Gamblking.util.SecurityUtil;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class Update
{
    private static Map<String, File> updateFiles = new HashMap<>();
    public static Map<String, File> getUpdateFiles()  { return updateFiles; }

    public synchronized static void update()
    {
        updateFiles(new File("update"));
    }

    private static void updateFiles(File path)
    {
        Label label = ProgramManager.getPreloadComponent().getStatusLabel();
        for(File file : path.listFiles())
        {
            if(file.isDirectory())
            {
                Update.updateFiles(file);
            }
            else
            {
                String sha256 = null;
                try
                {
                    sha256 = SecurityUtil.Companion.extractFileHashSHA256(file.getPath());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                updateFiles.put(sha256, file);
                sendTo(label, "register update files from " + sha256);
            }
            try
            {
                Thread.sleep(10L);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void sendTo(Label label, String message)
    {
        Platform.runLater(() -> label.setText(message));
    }
}
