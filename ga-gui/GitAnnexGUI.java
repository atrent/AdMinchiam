import javax.swing.*;
import javax.swing.table.*;
import java.io.*;
import java.awt.*;
import java.util.*;

public class GitAnnexGUI extends JFrame {
    private File originDir; // da "prependere" quando si esegue comando git-annex
    private Vector<String> annexedFiles;
    private Vector<String> remotes;

    /*
        class RemotesModel extends AbstractTableModel {
            public String getValueAt(int r,int c) {
                return remotes.get(r);
            }
            public int getColumnCount() {
                return 1;
            }
            public int getRowCount() {
                return remotes.size();
            }
        }
    */

    class FilesModel extends AbstractTableModel {
        public String getValueAt(int r,int c) {
            if(c<remotes.size()) {
                return annexedFiles.get(r).substring(c,c+1);
            } else {
                return annexedFiles.get(r).substring(c+1);
            }
        }
        public int getColumnCount() {
            return remotes.size()+1;
        }
        public int getRowCount() {
            return annexedFiles.size();
        }
        public String getColumnName(int column) {
            if(column>=remotes.size())
                return "File";
            return remotes.get(column);
        }
    }

    public GitAnnexGUI(File f) {
        super("GitAnnexGUI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);

        annexedFiles=new Vector<String>();
        remotes=new Vector<String>();

        initFromAnnex(f);

        // TODO: usare 2 JTable!!!

        /*
                JTable remotesTable=new JTable(this.new RemotesModel());
                add(remotesTable,BorderLayout.NORTH);
        */

        JTable annexedFilesTable=new JTable(this.new FilesModel());
        annexedFilesTable.setColumnSelectionAllowed(true);
        add(new JScrollPane(annexedFilesTable));

        //resize columns
        for(int i=0; i<remotes.size(); i++) {
            annexedFilesTable.getColumnModel().getColumn(i).setMaxWidth(60);
        }
    }

    public void initFromAnnex(File f) {
        try {
            //TODO: check if it is a git-annex!

            Process process=Runtime.getRuntime().exec("git-annex list",null,f);
            //InputStream stderr=process.getErrorStream();
            BufferedReader stderr=new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader stdout=new BufferedReader(new InputStreamReader(process.getInputStream()));

            //while(stderr.available()>0)System.err.println(stderr.readLine());

            String item=stdout.readLine(); // il primo e' "here" (dovrebbe)
            if(item==null) return; // errore, non inizializzato
            remotes.add(item);

            while((item=stdout.readLine())!=null) {
                //System.out.println(item);
                if(item.startsWith("|") && !item.endsWith("|")) {
                    //other remotes
                    remotes.add(item.replace("|", ""));
                } else {
                    if(!item.endsWith("|")) {
                        // annexed items
                        annexedFiles.add(item);
                    }
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * arg= path iniziale di un primo git-annex, gli altri li evince estraendo info dall'annex stesso
     */
    public static void main(String[] arg) throws Throwable {
        if(arg.length!=1) {
            System.err.println("Missing argument (git-annex path)!");
            System.exit(1);
        }

        File f=new File(arg[0]);
        if(!f.isDirectory()) {
            System.err.println("Argument is not a dir!");
            System.exit(2);
        }

        GitAnnexGUI mainWindow=new GitAnnexGUI(f);
        mainWindow.setVisible(true);

        //TODO: riempire da 'git-annex list'
    }
}
