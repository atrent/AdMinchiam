import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.text.*;

/**
 * 	2015 - © Andrea Trentini (http://atrent.it) - Giovanni Biscuolo (http://xelera.eu)
 *
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class GitAnnexGUI extends JFrame {
    // constants
    public final static String MAIN_TITLE="GitAnnexGUI";
    public final static String TEMPLATES_DIR="ScriptTemplates";
    public final static String SAVED_STATUS="saved.status";

    // attributes
    //private String originDir; // da "prependere" quando si esegue comando git-annex
    private AnnexedFiles annexedFiles;
    private Vector<Remote> remotes;

    // gui components
    private JTable annexedFilesTable;         // TODO: fare celle editabili?
    private JTextArea textScript;
    private JTextArea templateScript;
    private JTextField origin;
    private JTextField grep;
    private JTextField matchesNum;
    private JComboBox<File> scripts;

    public void setOrigin(String f) {
        origin.setText(f);
    }
    public String getOrigin() {
        if(origin!=null)
            return origin.getText();
        else
            return "";
    }

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


    /** TODO: ripensarla un po'?
     */
    class FilesModel extends AbstractTableModel {
        public String getValueAt(int r,int c) {
            int size=remotes.size();
            //System.err.println("inner size:"+remotes.size());

            if(c<size) {
                return Character.toString(annexedFiles.get(r).getMask(c)); // mask
            }

            if(c==size) {
                return annexedFiles.get(r).getFileName(); // filename
            }

            if(c==size+1) {
                return annexedFiles.get(r).getAllMeta(); // metadata
            }

            return "";
        }


        public int getColumnCount() {
            int r=remotes.size()+1+1; // +1 per filename, +1 per metadati
            return r;
        }

        public int getRowCount() {
            int r=annexedFiles.matching();
            return r;
        }

        public String getColumnName(int column) {
            if(column==remotes.size())
                return "File";

            if(column==remotes.size()+1)
                return "Meta";

            return annexedFiles.getXonRemote(column)+":"+remotes.get(column).getName();
        }
    }


    private void fireTable() {
        ((AbstractTableModel)annexedFilesTable.getModel()).fireTableDataChanged();
        ((AbstractTableModel)annexedFilesTable.getModel()).fireTableStructureChanged();
    }
    private void reset() {
        if(grep!=null) grep.setText("");

        annexedFiles=new AnnexedFiles();
        remotes=new Vector<Remote>();
    }

    public GitAnnexGUI() {
        super(MAIN_TITLE);
        //
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
        //
        //originDir=f;
        //setTitle(MAIN_TITLE+": "+originDir);
        //
        initMenu();
        //
        reset();
        //
        matchesNum=new JTextField("nr. of matches");
        matchesNum.setEditable(false);
        //
        grep=new JTextField(/*"grepping text here (no matches with this string, change it!)"*/);
        //add(grep,BorderLayout.NORTH);
        grep.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                //System.err.println("matching: "+annexedFiles.matching());
                matchesNum.setText(annexedFiles.matching()+" matches");
                //System.err.println("grep changed");
                fireTable();
            }
        });
        JPanel settingsArea=new JPanel();
        settingsArea.setLayout(new BorderLayout());
        settingsArea.add(grep);
        settingsArea.add(matchesNum,BorderLayout.WEST);
        settingsArea.add(origin=new JTextField(),BorderLayout.NORTH);
        origin.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                initFromAnnex();
            }
        });
        settingsArea.add(new JLabel("<<<=== insert grepping string"),BorderLayout.EAST);
        add(settingsArea,BorderLayout.NORTH);
        // FILE TABLE
        annexedFilesTable=new JTable(this.new FilesModel());
        annexedFilesTable.setColumnSelectionAllowed(true);
        //add(new JScrollPane(annexedFilesTable));
        //
        textScript=new JTextArea("Generated script");
        //add(textScript,BorderLayout.EAST);
        templateScript=new JTextArea("##########\n#{0} is remote\n#{1} is filename\ncd {0}\ngit-annex get {1}\n##########\n");
        //add(templateScript,BorderLayout.EAST);
        // SCRIPT AREA
        JPanel scriptArea=new JPanel();
        scriptArea.setLayout(new BorderLayout());
        scriptArea.add(templateScript);
        scriptArea.add(scripts=new JComboBox<File>(),BorderLayout.NORTH);
        scripts.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                //System.err.println("script: "+scripts.getSelectedItem());
                templateScript.setText(getScript());
            }
        });
        fillScriptsCombo();
        //
        JScrollPane tbl;
        JSplitPane pane=
            new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            tbl=new JScrollPane(annexedFilesTable),
            new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(scriptArea),
                new JScrollPane(textScript)
            )
        );
        tbl.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS); // TODO: non fa quello che dico! appare ma non scrolla horiz
        add(pane);
        pane.setDividerLocation(800);
    }

    private String getScript() {
        try {
            byte[] encoded =
                Files.readAllBytes(
                    Paths.get(scripts.getSelectedItem().toString())
                );
            return new String(encoded);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return "dummy";
    }

    private void fillScriptsCombo() {
        File dir=new File(TEMPLATES_DIR);

        if(dir.isDirectory()) {
            for(File script: dir.listFiles()) {
                scripts.addItem(script);
            }
        } else throw new RuntimeException("missing templates dir!");
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        /*
        menuBar.add(new JMenu("prova1"));
        menuBar.add(new JMenu("prova2"));
        menuBar.add(new JMenu("prova3"));
        */
        //JMenu mnFile = new JMenu("Selections");
        //menuBar.add(mnFile);
        //
        //JMenuItem gen = new JMenuItem("Generate");
        JButton gen = new JButton("Generate");
        //mnFile.add(gen);
        menuBar.add(gen);
        gen.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                generate();
            }
        });
        //
        //JMenuItem reload = new JMenuItem("Reload");
        JButton reload = new JButton("Reload");
        //mnFile.add(reload);
        menuBar.add(reload);
        reload.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                initFromAnnex();
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
        //menuBar.add(new JToolBar());
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
     * ex-todo: ignorare gli special remotes??? o si puo' lavorarci sopra? (si', basta fare col cp invece che get)
     *
     * TODO: ora va per indice nella JTable, convertire a "chiave"
     */
    private void generate() {
        int colCount=annexedFilesTable.getColumnCount();
        int rowCount=annexedFilesTable.getRowCount();
        StringBuilder sb=new StringBuilder();
        boolean flagged=false;

        for(int row=0; row<rowCount; row++) {
            for(int col=0; col<colCount-2; col++) {
                if(annexedFilesTable.isCellSelected(row, col)) {
                    // prendi nome
                    String name=annexedFilesTable.getValueAt(row, colCount-2).toString();
                    //System.err.println("name: "+name);
                    // prendi annexed, cerca nome in vector
                    AnnexedFile af=null;
                    int index=0;

                    for(; index<annexedFiles.size(); index++) {
                        af=annexedFiles.get(index);

                        if(af.getFileName().equals(name)) break;
                    }

                    if(af==null) throw new RuntimeException("annexed file not found!");

                    sb.append(
                        MessageFormat.format(templateScript.getText(),
                                             remotes.get(col).getPath(),
                                             af.getFileName())
                    );
                    /* OLD, statico
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
                    */
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

    private void initFromAnnex() {
        //TODO: clessidra o progress bar...
        reset();
        // TODO: check if it is a git-annex!
        // list of files
        Command command=new Command(getOrigin(),"git-annex list");
        command.start();

        for(String item: command.getResult()) {
            if(item.equals("here")) remotes.add(new Remote(item)); //il primo e' "here" (dovrebbe)
            else if(item.startsWith("|") && !item.endsWith("|")) {
                //other remotes
                remotes.add(new Remote(item.replace("|", "")));
            } else {
                if(!item.endsWith("|")) {
                    // annexed items
                    annexedFiles.add(new AnnexedFile(item));
                }
            }
        }

        //System.out.println(remotes.get(1).getPath());
        // metadata
        command=new Command(getOrigin(),"git-annex metadata");
        command.start();
        StringBuffer sb=new StringBuffer();
        int annexed=0;

        for(String item: command.getResult()) {
            sb.append(item);
            sb.append("\n");

            if(item.equals("ok")) {
                annexedFiles.get(annexed).setMeta(sb.toString());
                sb=new StringBuffer();
                annexed++;
            }
        }

        saveStatus();
        fireTable();
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

        GitAnnexGUI mainWindow=new GitAnnexGUI();
        mainWindow.setOrigin(arg[0]);
        mainWindow.setVisible(true);

        if(!mainWindow.loadStatus()) {
            mainWindow.initFromAnnex();
        }
    }

    class AnnexedFiles extends Vector<AnnexedFile> implements Serializable {

        public int getXonRemote(int remote) {
            int count=0;

            for(AnnexedFile af: this) {
                if(af.getMask(remote)=='X') count++;
            }

            return count;
        }


        /** devo filtrare solo i matching, restituisce l'indexesimo che matcha
         */
        public AnnexedFile get(int index) {
            if(grep==null) return super.get(index);

            //
            String txt=grep.getText();

            if(txt.length()==0) return super.get(index);

            //
            int i=0;
            boolean found=false;

            while(index>=0 && i<size()) {
                if(super.get(i).matches(txt)) {
                    index--;
                    found=true;
                }

                i++;
            }

            if(found)return super.get(i-1);
            else return super.get(i);
        }

        public int matching() {
            return matching(grep.getText());
        }

        public int matching(String grep) {
            int count=0;

            for(AnnexedFile af: annexedFiles) {
                if(af.matches(grep)) count++;
            }

            return count;
        }
    }


    /** un singolo file annexed, con la mappa dei remote su cui e' (o si vorrebbe metterlo)
     */
    class AnnexedFile implements Serializable {
        private String file;
        private char[] remotes; // TODO: a parte 'X' decidere una semantica
        private Hashtable<String,String> metadata;

        public String getMeta(String key) {
            //initMeta();
            return metadata.get(key);
        }

        public String getAllMeta() {
            //initMeta();
            return metadata.toString();
        }

        /** pericoloso? fare anche un equals con AnnexedFile?
         */
        /*
        public boolean equals(Object nome) {
            System.out.println("equals...");
            return file.equals(nome.toString());
        }
        */

        public void clearMeta() {
            metadata.clear();
        }

        public boolean matches(String grep) {
            if(grep.length()==0) return true;

            return toString().indexOf(grep)>0;
        }


        /** formato:
         * nomefile
         * tags..
         * tags..
         * ok
         */
        public void setMeta(String meta) {
            String[] lines=meta.split("\n");

            if(!lines[lines.length-1].equals("ok"))
                throw new RuntimeException("metadata record does not end in 'ok'!");

            String receivedName=lines[0].substring(lines[0].indexOf(" ")+1);

            if(receivedName.equals(getFileName()))
                throw new RuntimeException("metadata filename does not match! "+getFileName()+" vs. "+receivedName);

            for(int l=1; l<lines.length-1; l++) {
                if(lines[l].indexOf("=")>0) {
                    String[] split=lines[l].split("=");
                    metadata.put(split[0].trim(),split[1].trim());
                }
            }
        }


        /** invoke as late as possible, costly!!! */
        /*
        public void _initMeta() {
            if(metadata.size()!=0) return;

            Command c=new Command(originDir,"git-annex metadata "+file);
            c.start();
            for(String meta: c.getResult()) {
                //System.err.println("meta: "+meta);
                if(meta.indexOf("=")>0) {
                    String[] split=meta.split("=");
                    //System.err.println(split[0]+","+split[1]);
                    metadata.put(split[0],split[1]);
                }
            }

        }
        */

        public String getFileName() {
            return file;
        }

        public char getMask(int i) {
            if(i>=0 && i<remotes.length) return remotes[i];

            return '!';
        }

        /** si inizializza direttamente dalla stringa di git annex list
         */
        public AnnexedFile(String annexItem) {
            String[] st=annexItem.split(" ");
            remotes=st[0].toCharArray();
            file=st[1];
            metadata=new Hashtable<String,String>();
        }

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append(remotes);
            sb.append(":");
            sb.append(file);
            //sb.append("[");
            sb.append(getAllMeta());
            //sb.append("]");
            return sb.toString();
        }

        public String nameAndMeta() {
            StringBuilder sb=new StringBuilder();
            sb.append(file);
            sb.append("[");
            sb.append(getAllMeta());
            sb.append("]");
            return sb.toString();
        }
    }

    class Remote implements Serializable {
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
            name=rname;
            path=getRemotePath(name);
        }

        /*
        public boolean isSpecial(){
        	return
        		path=="web" ||
        		path.indexOf("@")>0 ||

        }
        */

        public String toString() {
            StringBuilder sb=new StringBuilder();
            //sb.append(new String(new char[pos+1]).replace("\0", "_")); // PADDING!!!!!!
            sb.append(name);
            sb.append(":");
            sb.append(path);
            return sb.toString();
        }

        public String getRemotePath(String remote) {
            if(remote.equals("here")) return getOrigin();

            if(remote.equals("web")) return "web";

            StringBuilder sb=new StringBuilder();
            sb.append("git remote -v |grep ");
            sb.append(remote);
            sb.append("|cut -f2 |cut -f1 -d' '|sort|uniq");
            String[] cmd = {
                "/bin/bash",
                "-c",
                sb.toString()
                //"ls"
            };
            Command command=new Command(getOrigin(),cmd);
            command.start();
            return command.getResult().get(0);
        }
    }

    public boolean loadStatus() {
        System.err.println("loading...");

        try {
            File status=new File(SAVED_STATUS);

            if(status.isFile()) {
                ObjectInputStream in=new ObjectInputStream(new FileInputStream(status));
                setOrigin(in.readObject().toString());
                annexedFiles=(AnnexedFiles)in.readObject();
                remotes=(Vector<Remote>)in.readObject();
                System.err.println("loaded...");
                fireTable();
                return true; // se ha caricato
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    public void saveStatus() {
        System.err.println("saving...");

        try {
            File status=new File(SAVED_STATUS);
            ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(status));
            out.writeObject(getOrigin());
            out.writeObject(annexedFiles);
            out.writeObject(remotes);
            out.flush();
            out.close();
            System.err.println("saved!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}




class Command {
    private File wd;
    private String[] cmd;
    private Vector<String> result;
    private Vector<String> err;
    private Process process;

    public Command(String wd,String cmd) {
        this(new File(wd), new String[] {cmd});
    }

    public Command(String wd,String[] cmd) {
        this(new File(wd), cmd);
    }

    public Command(File wd,String cmd) {
        this(wd, new String[] {cmd});
    }

    public Command(File wd,String[] cmd) {
        this.wd=wd;
        this.cmd=cmd;
        result=new Vector<String>();
        err=new Vector<String>();
    }

    public void start() {
        try {
            if(cmd.length==1)
                process=Runtime.getRuntime().exec(cmd[0],null,wd);
            else
                process=Runtime.getRuntime().exec(cmd,null,wd);

            System.err.println("start: "+Arrays.toString(cmd));
            String line="";
            BufferedReader stderr=new BufferedReader(new InputStreamReader(process.getErrorStream()));
            /*    NOTE:  one line of err message cause readline to stuck
            while ((line = stderr.readLine()) != null) {
                System.err.println("in err... "+line);
                err.add(line);
            }
            */
            //System.err.println("err: "+stderr.readLine());  // prendo solo la prima riga // TODO: perche' si blocca la readline?!?
            line="";
            BufferedReader stdout=new BufferedReader(new InputStreamReader(process.getInputStream()));

            while ((line = stdout.readLine()) != null) {
                //System.out.println("in out... "+line);
                result.add(line);
            }

            //System.err.println("end: "+Arrays.toString(cmd));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Vector<String> getResult() {
        return result;
    }

    public Vector<String> getErr() {
        return err;
    }


}
