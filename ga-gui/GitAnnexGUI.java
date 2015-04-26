import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.io.*;
import java.util.*;

public class GitAnnexGUI extends JFrame {
    private File originDir; // da "prependere" quando si esegue comando git-annex
    private Vector<String> annexedFiles;
    private Vector<Remote> remotes;

    // gui components
    private JTable annexedFilesTable;
    private JTextArea textScript;

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
            return remotes.get(column).getName();
        }
    }

    public GitAnnexGUI(File f) {
        super("GitAnnexGUI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);

        originDir=f;

        annexedFiles=new Vector<String>();
        remotes=new Vector<Remote>();

        initMenu();

        initFromAnnex(f);

        annexedFilesTable=new JTable(this.new FilesModel());
        annexedFilesTable.setColumnSelectionAllowed(true);
        //add(new JScrollPane(annexedFilesTable));

        //resize columns
        for(int i=0; i<remotes.size(); i++) {
            annexedFilesTable.getColumnModel().getColumn(i).setMaxWidth(60);
        }

        textScript=new JTextArea("Generated script");
        //add(textScript,BorderLayout.EAST);

        JSplitPane pane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JScrollPane(annexedFilesTable),new JScrollPane(textScript));
        add(pane);
        pane.setDividerLocation(700);
    }

    private void initMenu() {
        // ///////////////////////////////////////
        // MENU
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // ///////
        // // FILE
        JMenu mnFile = new JMenu("Selections");
        menuBar.add(mnFile);

        JMenuItem mntmGen = new JMenuItem("Generate");
        mnFile.add(mntmGen);
        mntmGen.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                generate();
            }
        });

        /*
                JMenuItem mntmSel = new JMenuItem("prova select");
                mnFile.add(mntmSel);
                mntmSel.addActionListener(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        provaSelect();
                    }
                });
        */
    }

    /*
        private void provaSelect() {
            int colCount=annexedFilesTable.getColumnCount();
            //ListSelectionModel model = annexedFilesTable.getSelectionModel();
            //model.clearSelection();
            for(int col=0; col<colCount; col++) {
                //model.addSelectionInterval(col, col);
                annexedFilesTable.setRowSelectionInterval(col, col);
                annexedFilesTable.addRowSelectionInterval(col, col);
                System.out.println(col);
            }
            annexedFilesTable.repaint();
        }
    */

    /** per ora assolutamente prove di generazione
     *
     * TODO: ignorare gli special remotes??? o si puo' lavorarci sopra? (si', basta fare col cp invece che get)
     */
    private void generate() {
        int colCount=annexedFilesTable.getColumnCount();
        int rowCount=annexedFilesTable.getRowCount();

        StringBuilder sb=new StringBuilder();

        boolean flagged=false;
        for(int row=0; row<rowCount; row++) {
            for(int col=0; col<colCount; col++) {
                if(annexedFilesTable.isCellSelected(row, col)) {
                    //sb.append(annexedFilesTable.getValueAt(row, col));
                    sb.append("cd ");
                    sb.append(remotes.get(col).getPath());
                    sb.append("\n");
                    sb.append("git-annex get ");
                    sb.append(annexedFilesTable.getValueAt(row,colCount-1));
                    sb.append("\n");
                    //sb.append(",c:");
                    //sb.append(col);
                    //sb.append("=");
                    //sb.append(annexedFilesTable.isCellSelected(row, col));
                    sb.append("\n");
                    flagged=true;
                }
            }
            if(flagged) {
                sb.append("\n");
                flagged=false;
            }
        }
        textScript.setText(sb.toString());
    }

    private void initFromAnnex(File f) {
        try {
            //TODO: check if it is a git-annex!

            Process process=Runtime.getRuntime().exec("git-annex list",null,f);
            //InputStream stderr=process.getErrorStream();
            BufferedReader stderr=new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader stdout=new BufferedReader(new InputStreamReader(process.getInputStream()));

            //while(stderr.available()>0)System.err.println(stderr.readLine());

            String item=stdout.readLine(); // il primo e' "here" (dovrebbe)
            if(item==null) return; // errore, non inizializzato
            remotes.add(new Remote(item));

            while((item=stdout.readLine())!=null) {
                //System.out.println(item);
                if(item.startsWith("|") && !item.endsWith("|")) {
                    //other remotes
                    remotes.add(new Remote(item.replace("|", "")));
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

        System.out.println(remotes.get(1).getPath());

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

    class Remote {
        private String name,path;

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        /** si inizializza direttamente dalla stringa di git annex list
         */
        public Remote(String rname) {
            //System.err.println(rname);
            name=rname;
            path=getRemotePath(name);
        }

        public String toString() {
            StringBuilder sb=new StringBuilder();
            //sb.append(new String(new char[pos+1]).replace("\0", "_")); // PADDING!!!!!!
            sb.append(name);
            sb.append(":");
            sb.append(path);
            return sb.toString();
        }

        public String getRemotePath(String remote) {
            if(remote.equals("here")) return originDir.toString();

            //System.err.println(remote);

            StringBuilder sb=new StringBuilder();
            sb.append("git remote -v |grep ");
            sb.append(remote);
            sb.append("|cut -f2 |cut -f1 -d' '|sort|uniq");

            //System.err.println(sb);

            String[] cmd = {
                "/bin/bash",
                "-c",
                sb.toString()
                //"ls"
            };


            String item="errore";
            try {
                String line="niente";

                Process process=Runtime.getRuntime().exec(cmd,null,originDir);

                /*
                BufferedReader stderr=new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = stderr.readLine()) != null) {
                    System.err.println(line);
                }
                */

                BufferedReader stdout=new BufferedReader(new InputStreamReader(process.getInputStream()));
                /*
                while ((line = stdout.readLine()) != null) {
                    System.out.println(line);
                }
                */

                item=stdout.readLine();

            } catch(Exception e) {
                e.printStackTrace();
            }
            return item;
        }
    }
}
