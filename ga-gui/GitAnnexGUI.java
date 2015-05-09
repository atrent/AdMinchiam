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
    public final static int LASTCOLWIDTH=300;

    // DATA attributes
    private AnnexedFiles annexedFiles;
    private Vector<Remote> remotes;

    // NEW gui components
    FilesModel fm;
    //
    private Grep grepComponent;
    class Grep extends JPanel {
        private final static String LABEL="   Nr. of items: ";
        //
        private JLabel numMatches;
        public void setMatches(int m) {
            setMatches(Integer.toString(m));
        }
        public void setMatches(String m) {
            numMatches.setText(LABEL+m+"   ");
        }
        private JTextField grep;
        public String getText() {
            return grep.getText();
        }
        public boolean isEmpty() {
            return getText().length()==0;
        }

        public void updateMatching() {
            int matching=annexedFiles.matching(getText());
            System.err.println("matching: "+matching);
            setMatches(matching);
            fireTable();
        }

        public Grep() {
            setLayout(new BorderLayout());
            //
            numMatches=new JLabel();
            setMatches("NA");
            add(numMatches,BorderLayout.WEST);
            //
            grep=new JTextField();
            add(grep,BorderLayout.CENTER);
            grep.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    updateMatching();
                }
            });
            updateMatching();
        }

    }
    //
    public void setOrigin(String orig) {
        originComponent.setOrigin(orig);
    }
    private OriginAnnex originComponent;
    class OriginAnnex extends JPanel {
        private JTextField origin;
        //private JButton reload;//non serve accessibile
        //
        public OriginAnnex() {
            setLayout(new BorderLayout());
            // textfield nome file
            origin=new JTextField();
            add(origin,BorderLayout.CENTER);
            origin.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    initFromAnnex();
                }
            });
            // bottone reload
            JButton reload = new JButton("Reload annex");
            add(reload,BorderLayout.WEST);
            reload.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    initFromAnnex();
                }
            });
        }
        public void setOrigin(String f) {
            origin.setText(f);
        }
        public String getOrigin() {
            return origin.getText();
        }
    }
    //
    private Scripts scriptsComponent;
    class Scripts extends JPanel {
        private JTextArea script;
        public void setScript(String sc) {
            script.setText(sc);
        }
        //
        private JTextArea template;
        public String getTemplate() {
            return template.getText();
        }
        //
        private JComboBox<File> scripts;
        public String getSelected() {
            return scripts.getSelectedItem().toString();
        }
        public Scripts() {
            setLayout(new BorderLayout());//forse no
            //
            JButton gen = new JButton("Generate script from current template+selection");
            add(gen,BorderLayout.NORTH);
            gen.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    generate();
                }
            });
            //
            script=new JTextArea("Generated script");
            //script.setBorder(BorderFactory.createLineBorder(Color.black));
            add(script/*,BorderLayout.CENTER*/);
            //
            template=new JTextArea("##########\n#{0} is remote\n#{1} is filename\ncd {0}\ngit-annex get {1}\n##########\n");
            template.setBorder(BorderFactory.createLineBorder(Color.black));
            add(template,BorderLayout.EAST);
            //
            scripts=new JComboBox<File>();
            add(scripts,BorderLayout.SOUTH);
            File dir=new File(TEMPLATES_DIR);

            if(dir.isDirectory()) {
                for(File scr: dir.listFiles()) {
                    scripts.addItem(scr);
                }
            } else throw new RuntimeException("missing templates dir!");

            scripts.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    //System.err.println("script: "+scripts.getSelectedItem());
                    template.setText(getScript());
                }
            });
        }

    }

    // MAIN gui component
    private JTable annexedFilesTable;         // TODO: fare celle editabili?


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
            int r=annexedFiles.matching(grepComponent.getText());
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
        if(fm!=null) {
            fm.fireTableDataChanged();
            fm.fireTableStructureChanged();
            TableColumnModel m=annexedFilesTable.getColumnModel();
            m.getColumn(fm.getColumnCount()-1).setMinWidth(LASTCOLWIDTH);
            m.getColumn(fm.getColumnCount()-2).setMinWidth(LASTCOLWIDTH);
        }
    }

    private void resetData() {
        annexedFiles=new AnnexedFiles();
        remotes=new Vector<Remote>();
    }

    public GitAnnexGUI() {
        super(MAIN_TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
        //////////////////////////////////////////////////////////////////////////
        // TODO: fattorizzare, creare componenti (inner classes) Grep, OriginAnnex, Scripts
        // qui rimane solo costruzione JTable
        //
        resetData();
        //
        JPanel settingsArea=new JPanel();
        settingsArea.setLayout(new GridLayout(2,1)); // sopra annex, sotto grep
        settingsArea.add(grepComponent=new Grep());
        settingsArea.add(originComponent=new OriginAnnex());
        add(settingsArea,BorderLayout.NORTH);
        //////////////////////////////////////////////////////////////////////////
        // FILE TABLE
        annexedFilesTable=new JTable(fm=new FilesModel());
        annexedFilesTable.setColumnSelectionAllowed(true);
        //annexedFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        annexedFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        //
        //add(new JScrollPane(annexedFilesTable));
        JSplitPane pane=
            new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(annexedFilesTable),
            scriptsComponent=new Scripts()
        );
        //
        //tbl.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS); // TODO: non fa quello che dico! appare ma non scrolla horiz
        add(pane);
        pane.setDividerLocation(800);
    }

    private String getScript() {
        try {
            byte[] encoded =
                Files.readAllBytes(
                    Paths.get(scriptsComponent.getSelected())
                );
            return new String(encoded);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return "dummy";
    }

    private void _DELETE_fillScriptsCombo() {
    }

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
                        MessageFormat.format(scriptsComponent.getTemplate(),
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

        scriptsComponent.setScript(sb.toString());
    }

    private void initFromAnnex() {
        //TODO: clessidra o progress bar...
        resetData();
        // TODO: check if it is a git-annex!
        // list of files
        Command command=new Command(originComponent.getOrigin(),"git-annex list");
        command.start(); // bloccante
        //
        long starting=System.currentTimeMillis();

        //
        for(String item: command.getResult()) {
            if(item.indexOf("here")==0) remotes.add(new Remote(item)); //il primo e' "here" (dovrebbe), inizia per "here"
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

        //
        System.err.println("tempo di parsing dei file:"+(System.currentTimeMillis()-starting));
        //
        //System.out.println(remotes.get(1).getPath());
        // metadata
        command=new Command(originComponent.getOrigin(),"git-annex metadata");
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
            if(grepComponent.isEmpty()) return super.get(index);

            //TODO: verificare bene funzionamento
            String txt=grepComponent.getText();
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

            //return file.indexOf(grep)>=0;  //cosi' cerca solo nel nome del file e non nei tag!!!
            return toString().indexOf(grep)>=0;  //cosi' in tutto
            //TODO: verificare bene!!! ora sembra prendere anche cose che non dovrebbe
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
            if(remote.equals("here")) return originComponent.getOrigin();

            if(remote.equals("web")) return "web";

            StringBuilder sb=new StringBuilder();
            sb.append("git remote -v |grep '");
            sb.append(remote);
            sb.append("'|cut -f2 |cut -f1 -d' '|sort|uniq");
            String[] cmd = {
                "/bin/bash",
                "-c",
                sb.toString()
                //"ls"
            };
            Command command=new Command(originComponent.getOrigin(),cmd);
            command.start();

            if(command.getResult().size()>0)
                return command.getResult().get(0);
            else return "";
        }
    }

    public boolean loadStatus() {
        System.err.println("loading...");

        try {
            File status=new File(SAVED_STATUS);

            if(status.isFile()) {
                ObjectInputStream in=new ObjectInputStream(new FileInputStream(status));
                originComponent.setOrigin(in.readObject().toString());
                annexedFiles=(AnnexedFiles)in.readObject();
                remotes=(Vector<Remote>)in.readObject();
                System.err.println("loaded...");
                //
                //fireTable();
                grepComponent.updateMatching();
                //
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
            out.writeObject(originComponent.getOrigin());
            out.writeObject(annexedFiles);
            out.writeObject(remotes);
            out.flush();
            out.close();
            System.err.println("saved!");
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

        GitAnnexGUI mainWindow=new GitAnnexGUI();
        mainWindow.setOrigin(arg[0]);
        mainWindow.setVisible(true);

        if(!mainWindow.loadStatus()) {
            mainWindow.initFromAnnex();
        }
    }
}



/** utility class to spawn a command and read result
 */
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
            long starting=System.currentTimeMillis();
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

            System.err.print("end: "+Arrays.toString(cmd));
            System.err.println(", time (ms): "+(System.currentTimeMillis()-starting) );
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

//TODO: priorita' speed!!! e il collo di bottiglia e' git-annex command

//TODO: creare cache nominativa per repo?

//DONE: c'e' iteratore solo sui selezionati??? NO

//TODO: aggiungere campo numProgressivo?

//TODO: json?!? solo se aumenta la velocita'

//DONE: check null pointer??? (non salta piu' fuori)

//TODO: autodimensionamento colonne JTable

//TODO: (jtable) cambiare componente?

//TODO: (opzionale) salva script con nome
