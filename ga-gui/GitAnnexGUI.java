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
	GitAnnexGUI: a git-annex helper

   	2015 - © Andrea Trentini (http://atrent.it) - Giovanni Biscuolo (http://xelera.eu)

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

    ///////////////////////////////////////////////////////
    //TODO: --allrepos o no?

    //TODO: size of file

    //TODO: (jtable) cambiare componente?

    //TODO: user doc

    //TODO: priorita' speed!!! e il collo di bottiglia e' git-annex command

    //TODO: creare cache nominativa per repo? forse no se si riesce a velocizzare la lettura dell'annex

    //TODO: json?!? solo se aumenta la velocita'

    //TODO: (opzionale, non urgente) salva script con nome
    ///////////////////////////////////////////////////////
    //DONE: autodimensionamento colonne JTable
    //DONE: c'e' iteratore solo sui selezionati??? NO
    //DONE: check null pointer??? (non salta piu' fuori)
    //DONE: aggiungere campo numProgressivo?
    //DONE: come mai la splitbar ad un certo punto si blocca? era il borderlayout di destra, aggiunti gli scrollpane ora a posto
    ///////////////////////////////////////////////////////

    // constants
    public final static String MAIN_TITLE="GitAnnexGUI (GPL, beta version) - 2015© Andrea Trentini (http://atrent.it) & Giovanni Biscuolo (http://xelera.eu)";
    public final static String TEMPLATES_DIR="ScriptTemplates";
    public final static String SAVED_STATUS="saved.status";
    public final static int LASTCOLWIDTH=300;

    // DATA attributes
    private AnnexedFiles annexedFiles;
    private Vector<Remote> remotes;

    ////////////////////////////////////////////////////////////////////////
    // NEW gui components
    FilesModel fm;
    //===========================
    private Grep grepComponent;
    class Grep extends JPanel {
        private final static String LABEL=" items, grepped if textfield is not empty => ";
        //
        private JLabel numMatches;
        public void setMatches(int m) {
            setMatches(Integer.toString(m));
        }
        public void setMatches(String m) {
            numMatches.setText("   "+m+LABEL);
        }
        private JTextField grep;
        public String getText() {
            return grep.getText();
        }
        public boolean isEmpty() {
            return getText().length()==0;
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
                    annexedFiles.setGrep(e.getActionCommand());
                    fireTable();
                }
            });
        }

    }
    //===========================
    public void setOrigin(String orig) {
        originComponent.setOrigin(orig);
    }
    //===========================
    private OriginAnnex originComponent;
    class OriginAnnex extends JPanel {
        private JTextField origin;
        private JTextField options;
        //private JButton reload;//non serve accessibile
        //
        public OriginAnnex() {
            //TODO: passare a gridlayout
            setLayout(new BorderLayout());
            // textfield nome file
            origin=new JTextField();
            add(origin,BorderLayout.CENTER);
            origin.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    initFromAnnex();
                }
            });
            // options (to git-annex)  TODO: attenzione che va in arrayoutofbound (perche' il nr tot di item non e' filtrato)
            options=new JTextField("  #--allrepos ? # edit to set options to git-annex                            ");
            add(options,BorderLayout.EAST);
            // bottone reload
            JButton reload = new JButton("Reload annex (NOTE: it could be SLOW!)");
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
        public String getOptions() {
            return options.getText();
        }
    }
    //===========================
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
            add(new JScrollPane(script)/*,BorderLayout.CENTER*/);
            //
            template=new JTextArea("##########\n#{0} is remote\n#{1} is filename\ncd {0}\ngit-annex get {1}\n##########\n");
            template.setBorder(BorderFactory.createLineBorder(Color.black));
            add(new JScrollPane(template),BorderLayout.EAST);
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
            if(c==0) return Integer.toString(r+1); // nr.progressivo

            c--; // salto nr.progr (analogo di shift indietro della shell)
            int rem=remotes.size();

            if(c<rem) {
                return Character.toString(annexedFiles.get(r).getMask(c)); // mask
            }

            c-=rem;

            if(c==0) {
                return annexedFiles.get(r).getFileName(); // filename
            }

            c--;

            if(c==0) {
                return annexedFiles.get(r).getAllMeta(); // metadata
            }

            return "";
        }

        public int getColumnCount() {
            int r=remotes.size()+1+1+1; // +1 per filename, +1 per metadati, +1 per nr.progr
            return r;
        }

        public int getRowCount() {
            //int r=annexedFiles.matching(grepComponent.getText());
            return annexedFiles.size();
        }

        public String getColumnName(int column) {
            if(column==0)
                return "<html>Counter<br>(1 based)<html>";

            int rem=remotes.size();

            if(column==rem+1)
                return "File";

            if(column==rem+2)
                return "Meta";

            return "<html>"+remotes.get(column-1).getName()+"<br>"+annexedFiles.getXonRemote(column-1)+"</html>";
        }
    }


    private void fireTable() {
        if(fm!=null) {
            fm.fireTableDataChanged();
            fm.fireTableStructureChanged();
            /*
            TableColumnModel m=annexedFilesTable.getColumnModel();
            m.getColumn(fm.getColumnCount()-1).setMinWidth(LASTCOLWIDTH);
            m.getColumn(fm.getColumnCount()-2).setMinWidth(LASTCOLWIDTH);
            */
            resizeColumnWidth();
            grepComponent.setMatches(annexedFiles.size());
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
        // DONE: fattorizzare, creare componenti (inner classes) Grep, OriginAnnex, Scripts, qui rimane solo costruzione JTable
        //
        resetData();
        //
        JPanel settingsArea=new JPanel();
        settingsArea.setLayout(new GridLayout(2,1)); // sopra annex, sotto grep
        settingsArea.add(originComponent=new OriginAnnex());
        settingsArea.add(grepComponent=new Grep());
        add(settingsArea,BorderLayout.NORTH);
        //////////////////////////////////////////////////////////////////////////
        // FILE TABLE
        annexedFilesTable=new JTable(fm=new FilesModel());
        annexedFilesTable.setColumnSelectionAllowed(true);
        //annexedFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        annexedFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        annexedFilesTable.getTableHeader().setReorderingAllowed(false);
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

    private void resizeColumnWidth() {
        TableColumnModel columnModel = annexedFilesTable.getColumnModel();

        for (int column = 0; column < annexedFilesTable.getColumnCount(); column++) {
            int width = 50; // Min width

            for (int row = 0; row < annexedFilesTable.getRowCount(); row++) {
                TableCellRenderer renderer = annexedFilesTable.getCellRenderer(row, column);
                Component comp = annexedFilesTable.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width, width);
            }

            columnModel.getColumn(column).setPreferredWidth(width);
        }
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

    /** per ora assolutamente prove di generazione
     *
     * ex-todo: ignorare gli special remotes??? o si puo' lavorarci sopra? (si', basta fare col cp invece che get)
     *
     * TODO: ora va per indice nella JTable, convertire a "chiave"?
     */
    private void generate() {
        int colCount=annexedFilesTable.getColumnCount();
        int rowCount=annexedFilesTable.getRowCount();
        StringBuilder sb=new StringBuilder();
        boolean flagged=false;

        for(int row=0; row<rowCount; row++) {
            //TODO: se voglio lasciare che si possano spostare le colonne bisogna fare il for su tutte ed evitare le colonne File, Counter e Meta
            for(int col=1; col<colCount-2; col++) {
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
                                             remotes.get(col-1).getPath(), // c'e' il nr.progressivo!!!
                                             af.getFileName(),
                                             row+1)
                    );
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
        // DONE: clessidra o progress bar... cfr. https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html#monitors
        resetData();
        // TODO: check if it is a git-annex!
        // list of files
        Command command=new Command(this,originComponent.getOrigin(),"git-annex list "+originComponent.getOptions());
        command.start(); // bloccante...
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

        // TODO: ripensare options (e forse ripensare al wrapper dei dati dell'annex...
        System.err.println("tempo di parsing dei file:"+(System.currentTimeMillis()-starting));
        //
        //System.out.println(remotes.get(1).getPath());
        // metadata
        command=new Command(this,originComponent.getOrigin(),"git-annex metadata "+originComponent.getOptions());
        command.start(); // bloccante...
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


    /** gruppo di file annexed
     */
    class AnnexedFiles /*extends Vector<AnnexedFile>*/ implements Serializable,Iterable<AnnexedFile> {
        private Vector<AnnexedFile> complete;
        private Vector<AnnexedFile> filtered;
        private String grep;

        //
        public AnnexedFiles() {
            complete=new Vector<AnnexedFile>();
            filtered=complete;
        }

        public String getGrep() {
            return grep;
        }

        /** filtered if grep is non empty
         */
        public void setGrep(String g) {
            if(g!=null && g.length()>0) {
                grep=g;
                filtered=new Vector<AnnexedFile>();

                for(AnnexedFile af: complete) {
                    if(af.matches(grep))
                        filtered.add(af);
                }
            } else {
                filtered=complete;
            }
        }

        public Vector<AnnexedFile> getContent() {
            return filtered;
        }

        public int size() {
            return getContent().size();
        }

        public Iterator<AnnexedFile> iterator() {
            return getContent().iterator();
        }

        public boolean add(AnnexedFile af) {
            return getContent().add(af);
        }

        public int getXonRemote(int remote) {
            // conta quante X sulla colonna
            //
            int count=0;

            for(AnnexedFile af: getContent()) {
                if(af.getMask(remote)=='X') count++;
            }

            return count;
        }


        /** devo filtrare solo i matching, restituisce l'indexesimo tra quelli che matchano
         */
        public AnnexedFile get(int index) {
            return getContent().get(index); // ormai e' automaticamente filtrato
        }
    }


    /** un singolo file annexed, con la mappa dei remote su cui e' (o si vorrebbe metterlo)
     */
    class AnnexedFile implements Serializable {
        //TODO: velocizzare oggetto, cacheando le stringhe?
        //
        private String file;
        private char[] remotes; // TODO: a parte 'X' decidere una semantica?
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
            if(grep.length()==0 || grep==null) return true;

            String all=getNameAndMeta();

            //DONE: verificare bene!!! ora sembra prendere anche cose che non dovrebbe

            //cosi' cerca in tutto (ma senza remotes)
            if(all.indexOf(grep)>=0) {
                //System.err.println(all);
                return true;
            }

            //return file.indexOf(grep)>=0;  //cosi' cerca solo nel nome del file e non nei tag!!!
            //return toString().indexOf(grep)>=0;  //cosi' in tutto (compresi remotes)
            return false;
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
            String[] st=annexItem.split(" "); // DONE: problema spazi nei filenames...
            remotes=st[0].toCharArray();
            // prendere il resto degli st (perche' ci potrebbero essere spazi)
            StringBuilder sb=new StringBuilder();

            for(int i=1; i<st.length; i++) {
                sb.append(st[i]);
            }

            file=sb.toString();
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

        public String getNameAndMeta() {
            StringBuilder sb=new StringBuilder();
            sb.append(file);
            //sb.append("[");
            sb.append(getAllMeta());
            //sb.append("]");
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
            sb.append("'|cut -f2 |cut -f1 -d' '|sort|uniq"); // TODO: problema spazi nei filenames...
            String[] cmd = {
                "/bin/bash",
                "-c",
                sb.toString()
                //"ls"
            };
            Command command=new Command(GitAnnexGUI.this,originComponent.getOrigin(),cmd);
            command.start(); // bloccante...

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
                fireTable();
                //grepComponent.updateMatching();
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
class Command { /*extends Thread*/
    private File wd; // working directory
    private String[] cmd;
    private Vector<String> result;
    private Vector<String> err;
    private Process process;
    private Component parent;
    //private ProgressMonitor monit;

    public Command(Component parent,String wd,String cmd) {
        this(parent,new File(wd), new String[] {cmd});
    }

    public Command(Component parent,String wd,String[] cmd) {
        this(parent,new File(wd), cmd);
    }

    public Command(Component parent,File wd,String cmd) {
        this(parent,wd, new String[] {cmd});
    }

    public Command(Component parent,File wd,String[] cmd) {
        this.parent=parent;
        //monit=new ProgressMonitor(parent,"prova","msg",1,20000);
        this.wd=wd;
        this.cmd=cmd;
        result=new Vector<String>();
        err=new Vector<String>();
    }

    public void start() {
        parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            if(cmd.length==1)
                process=Runtime.getRuntime().exec(cmd[0],null,wd);
            else
                process=Runtime.getRuntime().exec(cmd,null,wd);

            System.err.println("start: "+Arrays.toString(cmd)+" in "+wd);
            long starting=System.currentTimeMillis();
            String line="";
            BufferedReader stderr=new BufferedReader(new InputStreamReader(process.getErrorStream()));
            //
            /*    NOTE:  one line of err message cause readline to stuck
            while ((line = stderr.readLine()) != null) {
                System.err.println("in err... "+line);
                err.add(line);
            }
            */
            //System.err.println("err: "+stderr.readLine());  // prendo solo la prima riga // TODO: perche' si blocca la readline?!?
            line="";
            //BufferedReader stdout=new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdout=
                new BufferedReader(
                new InputStreamReader(
                    // TODO: in realta' non funziona...
                    new ProgressMonitorInputStream(parent,"prova",process.getInputStream())
                )
            );

            //
            //int p=0;
            while ((line = stdout.readLine()) != null) {
                //System.out.println("in out... "+line);
                result.add(line);
                //monit.setProgress(p++);
            }

            //System.err.print("end: "+Arrays.toString(cmd));
            System.err.print("end: "+cmd[0]);
            System.err.println(", time (ms): "+(System.currentTimeMillis()-starting) );
        } catch(Exception e) {
            e.printStackTrace();
        }

        //monit.close();
        parent.setCursor(Cursor.getDefaultCursor());
    }

    public Vector<String> getResult() {
        return result;
    }

    public Vector<String> getErr() {
        return err;
    }
}
