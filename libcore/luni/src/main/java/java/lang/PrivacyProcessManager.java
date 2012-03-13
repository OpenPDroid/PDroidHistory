package java.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Provides privacy handling for {@link java.lang.ProcessManager}
 * @author Svyatoslav Hresyk
 * TODO: test if this works, also test it with root apps
 */
public class PrivacyProcessManager {
    
    private static final int GET_COMMAND_WAIT_MS = 10;
    
    private static final int GET_COMMAND_WAIT_STEP = 5;
    
    /**
     * Verifies if the current process has privacy access permission
     * to the specified setting
     * @param setting name of the setting file (e.g., systemLogsSetting or 
     *          externalStorageSetting)
     * @return boolean true if permission is granted or false otherwise
     */
    public static boolean hasPrivacyPermission(String setting, int pid) {
        String packageName = null;
//        String uid = null;
        boolean output = true;
        try {
            packageName = getPackageName();
//            uid = getUid();
        } catch (Exception e) {
            System.err.println("PrivacyProcessManager: could not find package name or UID");
            e.printStackTrace();
        }
        try {
            PrivacyFileReader freader = null;
            
            // get the command line of starting process
            String commandLineFile = "/proc/" + pid + "/cmdline";
//            System.err.println("PrivacyProcessManager - hasPrivacyPermission: creating command line file FileReader for " + commandLineFile);
            freader = new PrivacyFileReader(commandLineFile);
            String proc = "";
            for (int i = GET_COMMAND_WAIT_MS; (proc = freader.readLine()) == null && i >= 0; i=- GET_COMMAND_WAIT_STEP) {
                try {
                    Thread.sleep(GET_COMMAND_WAIT_STEP);
                } catch (InterruptedException e) {
                    // ignore
                }
//                System.err.println("PrivacyProcessManager - hasPrivacyPermission: read \"" + proc + "\" from " + commandLineFile);
            }
            freader.close();
//            System.err.println("PrivacyProcessManager - hasPrivacyPermission: process: " + proc);
            
            // check if logs are being read
            if (proc != null && proc.trim().length() > 5 && proc.contains("logcat")) {
                // get setting value
                String settingsFilePath = "/data/system/privacy/" + packageName + /*"/" + uid +*/ "/" + setting;
//                System.err.println("PrivacyProcessManager - hasPrivacyPermission: creating settings file FileReader for " + settingsFilePath);
                freader = new PrivacyFileReader(settingsFilePath);
//                System.err.println("PrivacyProcessManager - hasPrivacyPermission: reading first line from " + settingsFilePath);
                String line = freader.readLine();
//                System.err.println("PrivacyProcessManager - hasPrivacyPermission: reading systemLogsSetting from the line");
                int systemLogsSetting = line != null ? Integer.parseInt(line.trim()) : -1;
                freader.close();
                // check permission
                if (systemLogsSetting == 1) output = false;
            }
        } catch (FileNotFoundException e) {
            // no setting for this application; do nothing
        } catch (Exception e) {
            System.err.println("PrivacyProcessManager: could not read privacy settings: " + setting);
            e.printStackTrace();
        }
//        System.err.println("PrivacyProcessManager - hasPrivacyPermission: returning: " + output);
        
        return output;
    }
    
    /**
     * Finds the package name corresponding to the current process
     * @return Current process' package name
     * @throws IOException, FileNotFoundException 
     */
    private static String getPackageName() throws IOException, FileNotFoundException {
        PrivacyFileReader freader = new PrivacyFileReader("/proc/self/cmdline");
        String packageName = freader.readLine().trim();
        freader.close();
        return packageName;
    }
    
    /**
     * Finds the UID corresponding to the current process
     * @return Current process' UID
     * @throws IOException, FileNotFoundException, NumberFormatException, Exception
     */
//    private static String getUid() throws IOException, FileNotFoundException, 
//            NumberFormatException, Exception {
//        PrivacyFileReader freader;
//        try {
//            freader = new PrivacyFileReader("/proc/self/cgroup");
//        } catch (FileNotFoundException e) {
//            // this is most likely root (UID 0)
//            return "0";
//        }
//        String uid = null;
//        String line = "";
//        while (!line.contains("/uid/")) line = freader.readLine();
//        freader.close();
//        if (line != null) {
//            int index = line.indexOf("/uid/");
//            index += "/uid/".length();
//            // make sure the found UID is an int and convert it back to string
//            uid = Integer.parseInt(line.substring(index).trim()) + "";
//        }
//        if (uid != null) return uid;
//        else throw new Exception();
//    }
    
    public static class PrivacyFileReader {
        
        private FileInputStream inputStream;
        
        private BufferedReader buffReader;
        
        public PrivacyFileReader(String path) throws FileNotFoundException {
            inputStream = new FileInputStream(new File(path));
            buffReader = new BufferedReader(new InputStreamReader(inputStream));
        }
        
        public String readLine() throws IOException {
            return buffReader.readLine();
        }
        
        public void close() throws IOException {
            inputStream.close();            
        }
    }
}
