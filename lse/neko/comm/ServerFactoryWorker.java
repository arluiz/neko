package lse.neko.comm;

// java imports:
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;


class ServerFactoryWorker {

    public String processRequest(String request)
        throws WantsToQuit
    {

        StringTokenizer tokens = new StringTokenizer(request);
        if (!tokens.hasMoreTokens()) {
            return "error Malformed command";
        }
        String command = tokens.nextToken().toLowerCase();

        if (command.equals("quit")) {
            throw new WantsToQuit("ok");
        } else if (command.equals("create")) {
            return create(tokens);
        } else if (command.equals("kill")) {
            return kill(tokens);
        } else if (command.equals("set")) {
            return set(tokens);
        } else {
            return "error Malformed command";
        }

    }

    public static final int OUTPUT_MAX_LENGTH = 10000;

    private boolean hasOnlyChars(String s, String chars) {

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (-1 == chars.indexOf(c)) {
                return false;
            }
        }
        return true;

    }

    private String create(StringTokenizer tokens) {

        Process server;
        try {

            // verify the parameters that affect the command
            String outFile = (String) params.remove("outfile");
            if (outFile != null
                && (!hasOnlyChars(outFile,
                                  "0123456789"
                                  + "abcdefghijklmnopqrstuvwxyz"
                                  + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                  + "-_+./,")
                    || outFile.startsWith("-")
                    || outFile.startsWith("+")
                    ))
            {
                return "error Parameter outfile has shell metacharacters";
            }

            if (outFile != null) {

                // only proceed if outfile does not exist
                // (this gives some security)
                File outF = new File(outFile);
                if (outF.exists()) {
                    return "error Parameter outfile already exists"
                        + "-- aborting for security";
                }

                // create the directories that lead to outfile
                outF.getParentFile().mkdirs();

            }

            String id = (String) params.get("id");
            if (id != null && !hasOnlyChars(id, "0123456789")) {
                return "error Parameter id should be an integer";
            }

            String javaOptions = (String) params.get("javaoptions");
            if (javaOptions == null) {
                javaOptions = "";
            }
            // some security checks would be needed

            // construct the command
            String command = "start_server ";
            String tmpOutFile =
                File.createTempFile("server_"
                                    + System.getProperty("user.name") + "_",
                                    ".out").getPath();
            if (id != null) {
                tmpOutFile += "." + id;
            }
            command += "-l -o " + tmpOutFile + " ";
            if (outFile != null) {
                command += "-m " + tmpOutFile + " -m " + outFile + " ";
            }
            command +=
                "-- java "
                + javaOptions
                + "lse.neko.comm.Slave"; // removed $*
            if (tokens.hasMoreTokens()) {
                String port = tokens.nextToken();
                if (tokens.hasMoreTokens()) {
                    return "error Malformed create command";
                }
                command = command + " " + port;
            }

            // start the command
            System.out.println("Executing: " + command);
            server = Runtime.getRuntime().exec(command);

            // read the command's output
            // (but at most OUTPUT_MAX_LENGTH characters)
            InputStream rawis = server.getInputStream();
            BufferedReader is =
                new BufferedReader(new InputStreamReader(rawis));
            char[] buffer = new char[OUTPUT_MAX_LENGTH];
            int read = is.read(buffer, 0, buffer.length);
            if (read == -1) { read = 0; }
            String output = new String(buffer, 0, read);

            // process the output
            String reply = processOutput(output);

            // wait for the subprocess to quit
            int exit = server.waitFor();
            if (exit != 0) {
                return "error server exited with " + exit + "\n" + reply;
            } else {
                return reply;
            }

        } catch (IOException ex) {
            return "error in exec: " + ex.getMessage();
        } catch (InterruptedException ex) {
            return "error in exec: wait interrupted";
        }

    }

    private String processOutput(String output) {

        try {

            StringTokenizer tokens = new StringTokenizer(output);
            if (!tokens.nextToken().equals("pid")) {
                throw new Exception();
            }
            int pid = Integer.parseInt(tokens.nextToken());
            if (pid <= 0) {
                throw new Exception();
            }
            String reply = "created " + pid;

            if (tokens.hasMoreTokens()) {
                if (!tokens.nextToken().equals("port")) {
                    throw new Exception();
                }
                int port = Integer.parseInt(tokens.nextToken());
                if (port <= 0) {
                    throw new Exception();
                }
                reply = reply + " port " + port;
            }

            pids.add(new Integer(pid));
            return reply;

        } catch (Exception ex) {

            return "error " + output;

        }

    }

    private Set pids = new HashSet();

    private String kill(StringTokenizer tokens) {

        Process server;
        try {

            Set p;
            if (tokens.hasMoreTokens()) {
                int pid = Integer.parseInt(tokens.nextToken());
                if (tokens.hasMoreTokens()) {
                    return "error Malformed kill command";
                }
                if (!pids.contains(new Integer(pid))) {
                    return "error Process " + pid
                        + " not created by this factory";
                }
                p = new HashSet();
                p.add(new Integer(pid));
            } else {
                p = new HashSet(pids);
            }

            Iterator iter = p.iterator();
            while (iter.hasNext()) {
                int pid = ((Integer) iter.next()).intValue();

                // start the command
                String command = "kill_server " + pid;
                server = Runtime.getRuntime().exec(command);

                // wait for the subprocess to quit
                int exit = server.waitFor();
                if (exit != 0) {
                    return "error kill exited with " + exit + "\n";
                }

                // remove from the set of active servers
                pids.remove(new Integer(pid));
            }

            return "ok";

        } catch (IOException ex) {
            return "error in exec: " + ex.getMessage();
        } catch (InterruptedException ex) {
            return "error in exec: wait interrupted";
        } catch (NumberFormatException ex) {
            return "error Malformed kill command";
        }

    }

    private Map params = new HashMap();

    private String set(StringTokenizer tokens) {

        try {

            String param = tokens.nextToken();
            String value = tokens.nextToken();
            try {
                value += tokens.nextToken("");
            } catch (NoSuchElementException ex) {
            }
            params.put(param, value);
            //System.out.println("param "+param+" value "+value);

        } catch (NoSuchElementException ex) {
            return "error Malformed command";
        }

        return "ok";

    }

}

